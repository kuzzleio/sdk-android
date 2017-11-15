package io.kuzzle.sdk.core

import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import io.kuzzle.sdk.enums.Event
import io.kuzzle.sdk.enums.Scope
import io.kuzzle.sdk.enums.State
import io.kuzzle.sdk.enums.Users
import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.SubscribeListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener
import io.kuzzle.sdk.responses.NotificationResponse
import io.kuzzle.sdk.state.States
import io.socket.emitter.Emitter

open class Room
/**
 * This object is the result of a subscription request, allowing to manipulate the subscription itself.
 * In Kuzzle, you don't exactly subscribe to a room or a topic but, instead, you subscribe to documents.
 * What it means is that, to subscribe, you provide to Kuzzle a set of matching filters.
 * Once you have subscribed, if a pub/sub message is published matching your filters, or if a matching stored
 * document change (because it is created, updated or deleted), then you'll receive a notification about it.
 *
 * @param kuzzleDataCollection Data collection to link
 * @param options              Subscription options
 */
@JvmOverloads constructor(protected var dataCollection: Collection, options: RoomOptions? = null) {

    private val id = UUID.randomUUID().toString()
    /**
     * Linked data collection name getter
     *
     * @return linked data collection name
     */
    var collection: String
        protected set
    protected var filters = JSONObject()
    protected var headers: JSONObject? = null
    protected var _volatile: JSONObject
    protected var subscribeToSelf: Boolean = false
    /**
     * roomId property getter
     *
     * @return roomId property value
     */
    var roomId: String? = null
        protected set
    protected var kuzzle: Kuzzle
    protected var channel: String
    protected var scope: Scope
    protected var state: State
    protected var users: Users
    /**
     * listener property getter
     *
     * @return listener property value
     */
    var listener: ResponseListener<NotificationResponse>
        protected set

    // Used to avoid subscription renewals to trigger multiple times because of
    // multiple but similar events
    private var lastRenewal: Long = 0
    private val renewalDelay: Long = 500

    // Used to delay method calls when subscription is in progress
    protected var subscribing = false
    private val queue = ArrayList<Runnable>()
    /**
     * subscribeListener property getter
     * @return subscribeListener property value
     */
    var subscribeListener: SubscribeListener? = null
        private set

    private val isReady: Boolean
        get() = this.kuzzle.state == States.CONNECTED && !this.subscribing

    init {
        val opts = options ?: RoomOptions()

        if (dataCollection == null) {
            throw IllegalArgumentException("Room: missing dataCollection")
        }
        dataCollection.kuzzle.isValid()
        this.kuzzle = dataCollection.kuzzle
        this.collection = dataCollection.collection

        try {
            this.headers = JSONObject(dataCollection.getHeaders().toString())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        this.subscribeToSelf = opts.isSubscribeToSelf
        this._volatile = opts.volatile
        this.scope = opts.scope
        this.state = opts.state
        this.users = opts.users
    }

    /**
     * Returns the number of other subscriptions on that room.
     *
     * @param listener Response callback listener
     */
    fun count(listener: ResponseListener<Int>) {
        if (listener == null) {
            throw IllegalArgumentException("Room.count: a callback listener is required")
        }

        // Delays this call until after the subscription is finished
        if (!this.isReady) {
            this.queue.add(Runnable { this@Room.count(listener) })
            return
        }

        if (this.roomId == null) {
            throw IllegalStateException("Room.count: cannot count subscriptions on an inactive room")
        }

        try {
            val data = JSONObject().put("body", JSONObject().put("roomId", this.roomId))
            this.kuzzle.addHeaders(data, this.headers)

            this.kuzzle.query(this.dataCollection.makeQueryArgs("realtime", "count"), data, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result").getInt("count"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Call after renew.
     *
     * @param args the args
     */
    protected open fun callAfterRenew(args: Any?) {
        if (args == null) {
            throw IllegalArgumentException("Room.renew: response required")
        }

        try {
            val requestId = if ((args as JSONObject).has("requestId")) args.getString("requestId") else null

            if (args.getString("type") == "TokenExpired") {
                this@Room.kuzzle.setJwtToken(null)
                this@Room.kuzzle.emitEvent(Event.tokenExpired)
            }

            if (requestId != null && this@Room.kuzzle.requestHistory.containsKey(requestId)) {
                if (this@Room.subscribeToSelf) {
                    listener.onSuccess(NotificationResponse(kuzzle, args as JSONObject?))
                }
                this@Room.kuzzle.requestHistory.remove(requestId)
            } else {
                listener.onSuccess(NotificationResponse(kuzzle, args as JSONObject?))
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * [.renew]
     */
    fun renew(listener: ResponseListener<NotificationResponse>): Room {
        return this.renew(null, listener, null)
    }

    /**
     * [.renew]
     */
    fun renew(listener: ResponseListener<NotificationResponse>, subscribeResponseListener: SubscribeListener): Room {
        return this.renew(null, listener, subscribeResponseListener)
    }

    /**
     * Renew the subscription. Force a resubscription using the same filters
     * if no new ones are provided.
     * Unsubscribes first if this Room was already listening to events.
     *
     * @param filters  Subscription filters, using Kuzzle DSL
     * @param listener Response callback listener
     * @return this
     */
    fun renew(filters: JSONObject?, listener: ResponseListener<NotificationResponse>, subscribeResponseListener: SubscribeListener?): Room {
        val now = System.currentTimeMillis()

        if (listener == null) {
            throw IllegalArgumentException("Room.renew: a callback listener is required")
        }

        // Skip subscription renewal if another one was performed just a moment before
        if (this.lastRenewal > 0 && now - this.lastRenewal <= this.renewalDelay) {
            return this
        }

        if (filters != null) {
            this.filters = filters
        }

        /*
      If not yet connected, registers itself into the subscriptions list and wait for the
      main Kuzzle object to renew subscriptions once online
     */
        if (this.kuzzle.state != States.CONNECTED) {
            this.listener = listener
            this.subscribeListener = subscribeResponseListener
            this.kuzzle.addPendingSubscription(this.id, this)
            return this
        }

        if (this.subscribing) {
            this.queue.add(Runnable { this@Room.renew(filters, listener, subscribeResponseListener) })

            return this
        }

        this.unsubscribe()
        this.roomId = null
        this.subscribing = true
        this.listener = listener
        this.subscribeListener = subscribeResponseListener
        this.kuzzle.addPendingSubscription(this.id, this)

        try {
            val options = Options()
            val subscribeQuery = JSONObject()
                    .put("body", this.filters)
                    .put("scope", this.scope.toString().toLowerCase())
                    .put("state", this.state.toString().toLowerCase())
                    .put("users", this.users.toString().toLowerCase())

            options.volatile = this._volatile
            this.kuzzle.addHeaders(subscribeQuery, this.headers)

            Thread(Runnable {
                try {
                    this@Room.kuzzle.query(this@Room.dataCollection.makeQueryArgs("realtime", "subscribe"), subscribeQuery, options, object : OnQueryDoneListener {
                        override fun onSuccess(args: JSONObject) {
                            try {
                                this@Room.kuzzle.deletePendingSubscription(this@Room.id)
                                this@Room.subscribing = false
                                this@Room.lastRenewal = System.currentTimeMillis()

                                val result = args.getJSONObject("result")
                                this@Room.channel = result.getString("channel")
                                this@Room.roomId = result.getString("roomId")
                                subscribeResponseListener?.done(null, this@Room)
                            } catch (e: JSONException) {
                                throw RuntimeException(e)
                            }

                            this@Room.kuzzle.addSubscription(this@Room.roomId!!, this@Room.id, this@Room)

                            this@Room.kuzzle.socket!!.on(this@Room.channel) { args -> callAfterRenew(args[0]) }

                            this@Room.dequeue()
                        }

                        override fun onError(arg: JSONObject) {
                            this@Room.subscribing = false
                            this@Room.queue.clear()
                            subscribeResponseListener?.done(arg, null)
                        }
                    })
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }).start()

        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * Unsubscribes from Kuzzle.
     * Stop listening immediately. If there is no listener left on that room,
     * sends an unsubscribe request to Kuzzle, once
     * pending subscriptions reaches 0, and only if there is still no listener on that room.
     * We wait for pending subscriptions to finish to avoid unsubscribing while
     * another subscription on that room is
     *
     * @return this
     */
    open fun unsubscribe(): Room {
        if (!this.isReady) {
            this.queue.add(Runnable { this@Room.unsubscribe() })
            return this
        }

        if (this.roomId == null) {
            return this
        }

        try {
            val data = JSONObject().put("body", JSONObject().put("roomId", this.roomId))
            this.kuzzle.addHeaders(data, this.headers)

            this.kuzzle.socket!!.off(this@Room.channel)
            this.kuzzle.deleteSubscription(this.roomId!!, this.id)

            if (this.kuzzle.getSubscriptions(this.roomId) == null) {
                val roomId = this.roomId
                if (this.kuzzle.pendingSubscriptions.isEmpty()) {
                    this.kuzzle.query(this.dataCollection.makeQueryArgs("realtime", "unsubscribe"), data)
                } else {
                    val timer = Timer(UUID.randomUUID().toString())
                    unsubscribeTask(timer, roomId, data).run()
                }
            }

            this.roomId = null
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * Unsubscribe task timer task.
     *
     * @param timer  the timer
     * @param roomId the room id
     * @param data   the data
     * @return the timer task
     */
    protected open fun unsubscribeTask(timer: Timer, roomId: String, data: JSONObject): TimerTask {
        return object : TimerTask() {
            override fun run() {
                try {
                    if (this@Room.kuzzle.pendingSubscriptions.isEmpty()) {
                        if (this@Room.kuzzle.getSubscriptions(roomId) == null) {
                            this@Room.kuzzle.query(this@Room.dataCollection.makeQueryArgs("realtime", "unsubscribe"), data)
                        }
                    } else {
                        timer.schedule(unsubscribeTask(timer, roomId, data), 100)
                    }
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }

            }
        }
    }

    /**
     * Subscription filters getter
     *
     * @return subscription filters
     */
    fun getFilters(): JSONObject {
        return filters
    }

    /**
     * Subscription filters setter.
     * renew must be called for this to take effect
     *
     * @param filters New subscription filters
     * @return this
     */
    fun setFilters(filters: JSONObject): Room {
        this.filters = filters
        return this
    }

    /**
     * headers property getters
     *
     * @return headers value
     */
    fun getHeaders(): JSONObject? {
        return this.headers
    }

    /**
     * [.setHeaders]
     */
    fun setHeaders(content: JSONObject): Room {
        return this.setHeaders(content, false)
    }

    /**
     * Subscription headers setter
     *
     * @param content - new headers content
     * @param replace - default: false = append the content, true = replace
     * @return this
     */
    fun setHeaders(content: JSONObject?, replace: Boolean): Room {
        if (this.headers == null) {
            this.headers = JSONObject()
        }
        if (replace) {
            this.headers = content
        } else {
            if (content != null) {
                try {
                    val ite = content.keys()
                    while (ite.hasNext()) {
                        val key = ite.next() as String
                        this.headers!!.put(key, content.get(key))
                    }
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }

            }
        }
        return this
    }

    /**
     * Gets volatile data.
     *
     * @return the volatile property
     */
    fun getVolatile(): JSONObject {
        return _volatile
    }

    /**
     * Sets volatile metadata.
     *
     * @param _volatile New volatile data value
     * @return this
     */
    fun setVolatile(_volatile: JSONObject): Room {
        this._volatile = _volatile
        return this
    }

    /**
     * subscribeToSelf property getter
     *
     * @return subscribeToSelf property value
     */
    fun isSubscribeToSelf(): Boolean {
        return subscribeToSelf
    }

    /**
     * subscribeToSelf property setter
     *
     * @param subscribeToSelf New subscribeToSelf value
     * @return this
     */
    fun setSubscribeToSelf(subscribeToSelf: Boolean): Room {
        this.subscribeToSelf = subscribeToSelf
        return this
    }

    /**
     * Runs all queued methods called while subscription was in progress
     */
    protected open fun dequeue() {
        if (this.queue.size > 0) {
            val threadPool = Executors.newSingleThreadExecutor()

            for (r in this.queue) {
                threadPool.execute(r)
            }

            threadPool.shutdown()

            try {
                threadPool.awaitTermination(1, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                // do nothing
            } finally {
                this.queue.clear()
            }
        }
    }
}
/**
 * Constructor
 *
 * @param kuzzleDataCollection Data collection to link
 */
