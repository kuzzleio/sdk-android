package io.kuzzle.sdk.core

import android.webkit.WebView
import android.webkit.WebViewClient

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.util.Calendar
import java.util.Date
import java.util.Queue
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

import io.kuzzle.sdk.enums.Event
import io.kuzzle.sdk.enums.Mode
import io.kuzzle.sdk.listeners.EventListener
import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener
import io.kuzzle.sdk.responses.TokenValidity
import io.kuzzle.sdk.security.Security
import io.kuzzle.sdk.security.User
import io.kuzzle.sdk.state.KuzzleQueue
import io.kuzzle.sdk.state.States
import io.kuzzle.sdk.util.EventList
import io.kuzzle.sdk.util.OfflineQueueLoader
import io.kuzzle.sdk.util.QueryObject
import io.kuzzle.sdk.util.QueueFilter
import io.kuzzle.sdk_android.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.EngineIOException

/**
 * The type Kuzzle.
 */
open class Kuzzle
/**
 * Constructor
 *
 * @param host - Target host name or IP address
 * @param options - Request options
 * @param connectionCallback - On success callback listener
 * @throws URISyntaxException
 */
@Throws(URISyntaxException::class)
@JvmOverloads constructor(protected var host: String, options: Options? = null, protected var connectionCallback: ResponseListener<Void>? = null) {
    private val MAX_EMIT_TIMEOUT = 10
    private val EVENT_TIMEOUT = 200

    protected var eventListeners = ConcurrentHashMap<Event, EventList>()
    /**
     * Connection socket getter
     *
     * @return Connection socket
     */
    /**
     * Connection socket setter
     *
     * @param socket - New connection socket
     */
    open var socket: Socket? = null
        protected set
    protected var collections: MutableMap<String, Map<String, Collection>> = ConcurrentHashMap()
    /**
     * autoReconnect option getter
     *
     * @return autoReconnect option value
     */
    var isAutoReconnect = true
        protected set
    protected var headers: JSONObject? = JSONObject()
    protected var _volatile: JSONObject
    protected var port: Int? = null
    /**
     * Connection status getter
     * @return Connection status
     */
    var state = States.INITIALIZING
    /**
     * reconnectionDelay option getter
     *
     * @return reconnectionDelay value
     */
    var reconnectionDelay: Long = 0
        protected set
    protected var autoResubscribe: Boolean = false
    protected var autoQueue: Boolean = false
    protected var autoReplay: Boolean = false

    protected var queueFilter: QueueFilter = QueueFilter { true }

    protected var replayInterval: Long = 0
    protected var queuing = false
    protected var defaultIndex: String? = null
    protected var requestHistory = ConcurrentHashMap<String, Date>()
    protected var offlineQueue = KuzzleQueue<QueryObject>()
    protected var queueTTL: Int = 0
    protected var queueMaxSize: Int = 0
    var jwtToken: String? = null

    /*
   This property contains the centralized subscription list in the following format:
    roomId:
      kuzzleRoomID_1: kuzzleRoomInstance_1,
      kuzzleRoomID_2: kuzzleRoomInstance_2,
      ...
    pending: // pending subscriptions
      kuzzleRoomID_x: kuzzleRoomInstance_x,
      ...

   This was made to allow multiple subscriptions on the same set of filters,
   something that Kuzzle does not permit.
   This structure also allows renewing subscriptions after a connection loss
   */
    protected var subscriptions = ConcurrentHashMap<String, ConcurrentHashMap<String, Room>>()

    private var offlineQueueLoader: OfflineQueueLoader? = null

    /**
     * Security static class
     */
    var security: Security

    private var loginCallback: ResponseListener<JSONObject>? = null

    var memoryStorage: MemoryStorage

    /**
     * Gets kuzzle web view client.
     *
     * @return the kuzzle web view client
     */
    open val kuzzleWebViewClient: KuzzleWebViewClient
        get() = KuzzleWebViewClient()

    /**
     * Is valid sate boolean.
     *
     * @return current state validity
     */
    protected open val isValidState: Boolean
        get() {
            when (this.state) {
                States.INITIALIZING, States.READY, States.DISCONNECTED, States.ERROR, States.OFFLINE -> return true
            }
            return false
        }

    /**
     * Returns the current SDK version.
     *
     * @return Current SDK version
     */
    val sdkVersion: String
        get() = BuildConfig.VERSION_NAME

    /**
     * Getter for the pendingSubscriptions private property
     *
     * @return pendingSubscriptions property value
     */
    open val pendingSubscriptions: Map<String, Room>
        get() = this.subscriptions["pending"]

    class QueryArgs {
        var controller: String? = null
        var action: String? = null
        var index: String? = null
        var collection: String? = null
    }

    /**
     * Emit an event to all registered listeners
     * An event cannot be emitted multiple times before a timeout has been reached.
     *
     * @param event - Event name to emit
     * @param args - Event payload
     */
    open fun emitEvent(event: Event, vararg args: Any) {
        val now = System.currentTimeMillis()

        if (this.eventListeners.containsKey(event)) {
            val l = this.eventListeners[event]

            if (l.lastEmitted < now - this.EVENT_TIMEOUT) {
                for (e in l.values) {
                    e.trigger(*args)
                }

                l.lastEmitted = now
            }
        }
    }

    init {
        if (host == null || host.isEmpty()) {
            throw IllegalArgumentException("Host name/address can't be empty")
        }

        val opt = options ?: Options()

        this.autoQueue = opt.isAutoQueue
        this.isAutoReconnect = opt.isAutoReconnect
        this.autoReplay = opt.isAutoReplay
        this.autoResubscribe = opt.isAutoResubscribe
        this.defaultIndex = opt.defaultIndex
        this.headers = opt.headers
        this._volatile = opt.volatile
        this.port = opt.port
        this.queueMaxSize = opt.queueMaxSize
        this.queueTTL = opt.queueTTL
        this.reconnectionDelay = opt.reconnectionDelay
        this.replayInterval = opt.replayInterval.toLong()

        if (opt.offlineMode == Mode.AUTO) {
            this.autoResubscribe = true
            this.autoReplay = this.autoResubscribe
            this.autoQueue = this.autoReplay
            this.isAutoReconnect = this.autoQueue
        }
        if (opt.connect == Mode.AUTO) {
            connect()
        } else {
            this.state = States.READY
        }

        this.security = Security(this)
        this.memoryStorage = MemoryStorage(this)
        this.subscriptions.put("pending", ConcurrentHashMap())
    }

    /**
     * Constructor
     *
     * @param host - Target Kuzzle host name or IP address
     * @param cb - On success connection callback listener
     * @throws URISyntaxException
     */
    @Throws(URISyntaxException::class)
    constructor(host: String, cb: ResponseListener<Void>) : this(host, null, cb) {
    }

    /**
     * Adds a listener to a Kuzzle global event. When an event is triggered,
     * listeners are called in the order of their insertion.
     *
     * @param kuzzleEvent - Name of the global event to subscribe to
     * @param listener - Response callback listener
     * @return this
     */
    fun addListener(kuzzleEvent: Event, listener: EventListener): Kuzzle {
        this.isValid()

        val e = object : io.kuzzle.sdk.util.Event(kuzzleEvent) {
            override fun trigger(vararg args: Any) {
                listener.trigger(*args)
            }
        }

        if (!eventListeners.containsKey(kuzzleEvent)) {
            eventListeners.put(kuzzleEvent, EventList())
        }

        eventListeners[kuzzleEvent].put(listener, e)
        return this
    }

    /**
     * Check an authentication token validity
     *
     * @param token - Token to check (JWT)
     * @param listener - Response callback listener
     */
    fun checkToken(token: String, listener: ResponseListener<TokenValidity>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.checkToken: listener required")
        }

        if (token == null || token.isEmpty()) {
            throw IllegalArgumentException("Kuzzle.checkToken: token required")
        }

        try {
            val args = QueryArgs()
            args.controller = "auth"
            args.action = "checkToken"
            val request = JSONObject()
            request.put("body", JSONObject().put("token", token))
            this.query(args, request, Options().setQueuable(false), object : OnQueryDoneListener {

                override fun onSuccess(response: JSONObject) {
                    try {
                        val validity = TokenValidity()
                        val result = response.getJSONObject("result")
                        validity.isValid = result.getBoolean("valid")
                        if (validity.isValid) {
                            validity.expiresAt = Date(result.getLong("expiresAt"))
                        } else {
                            validity.state = result.getString("state")
                        }
                        listener.onSuccess(validity)
                    } catch (e: JSONException) {
                        throw RuntimeException()
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
     * Connects to a Kuzzle instance using the provided host and port.
     *
     * @return this
     * @throws URISyntaxException
     */
    @Throws(URISyntaxException::class)
    fun connect(): Kuzzle {
        if (!this.isValidState) {
            if (connectionCallback != null) {
                connectionCallback!!.onSuccess(null)
                return this
            }
        }

        if (this.socket != null) {
            this.disconnect()
        }

        this.socket = createSocket()

        this@Kuzzle.state = States.CONNECTING

        if (socket != null) {
            socket!!.once(Socket.EVENT_CONNECT) {
                this@Kuzzle.state = States.CONNECTED

                this@Kuzzle.renewSubscriptions()
                this@Kuzzle.dequeue()
                this@Kuzzle.emitEvent(Event.connected)

                if (this@Kuzzle.connectionCallback != null) {
                    this@Kuzzle.connectionCallback!!.onSuccess(null)
                }
            }
        }

        if (socket != null) {
            socket!!.once(Socket.EVENT_CONNECT_ERROR) { args ->
                this@Kuzzle.state = States.ERROR
                this@Kuzzle.emitEvent(Event.error, *args)

                if (connectionCallback != null) {
                    val error = JSONObject()
                    try {
                        error.put("message", (args[0] as EngineIOException).message)
                        error.put("code", (args[0] as EngineIOException).code)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                    connectionCallback!!.onError(error)
                }
            }
        }

        if (socket != null) {
            socket!!.once(Socket.EVENT_DISCONNECT) {
                this@Kuzzle.state = States.OFFLINE
                if (!this@Kuzzle.isAutoReconnect) {
                    this@Kuzzle.disconnect()
                }
                if (this@Kuzzle.autoQueue) {
                    this@Kuzzle.queuing = true
                }

                this@Kuzzle.emitEvent(Event.disconnected)
            }
        }

        if (socket != null) {
            socket!!.once(Socket.EVENT_RECONNECT) {
                this@Kuzzle.state = States.CONNECTED

                if (this@Kuzzle.jwtToken != null) {
                    this@Kuzzle.checkToken(jwtToken!!, object : ResponseListener<TokenValidity> {
                        override fun onSuccess(response: TokenValidity) {
                            if (!response.isValid) {
                                this@Kuzzle.jwtToken = null
                                this@Kuzzle.emitEvent(Event.tokenExpired)
                            }

                            this@Kuzzle.reconnect()
                        }

                        override fun onError(error: JSONObject) {
                            this@Kuzzle.jwtToken = null
                            this@Kuzzle.emitEvent(Event.tokenExpired)
                            this@Kuzzle.reconnect()
                        }
                    })
                } else {
                    this@Kuzzle.reconnect()
                }
            }
        }

        if (socket != null) {
            socket!!.connect()
        }

        return this
    }

    /**
     * Collection object factory. Default index must be set.
     *
     * @param collection - Data collection name
     * @return Instantiated Collection object
     */
    fun collection(collection: String): Collection {
        this.isValid()
        if (this.defaultIndex == null) {
            throw IllegalArgumentException("Collection: unable to create a new data collection object: no index specified")
        }

        return this.collection(collection, this.defaultIndex!!)
    }

    /**
     * Collection object factory
     *
     * @param collection - Data collection name
     * @param index - Parent data index name
     * @return Instantiated Collection object
     */
    fun collection(collection: String, index: String): Collection {
        this.isValid()
        if (index == null && this.defaultIndex == null) {
            throw IllegalArgumentException("Collection: unable to create a new data collection object: no index specified")
        }

        if (!this.collections.containsKey(collection)) {
            val col = ConcurrentHashMap<String, Collection>()
            col.put(collection, Collection(this, collection, index))
            this.collections.put(index, col)
        }
        return this.collections[index].get(collection)
    }

    /**
     * [.createIndex]
     */
    fun createIndex(index: String, cb: ResponseListener<JSONObject>): Kuzzle {
        return createIndex(index, null, cb)
    }

    /**
     * Create a new data index
     *
     * @param index - index name to create
     * @param options - Request options
     * @param cb - Response callback listener
     * @return this
     */
    @JvmOverloads
    fun createIndex(index: String, options: Options? = null, cb: ResponseListener<JSONObject>? = null): Kuzzle {
        if (index == null && defaultIndex == null) {
            throw IllegalArgumentException("Collection.createIndex: index required")
        }

        val args = QueryArgs()
        args.controller = "index"
        args.action = "create"

        val request = JSONObject()
        try {
            request.put("index", index ?: defaultIndex)
            this.query(args, request, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        cb!!.onSuccess(response.getJSONObject("result"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    cb!!.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * Empties the offline queue without replaying it.
     *
     * @return this
     */
    fun flushQueue(): Kuzzle {
        this.getOfflineQueue().clear()
        return this
    }

    /**
     * [.getAllStatistics]
     */
    fun getAllStatistics(listener: ResponseListener<Array<JSONObject>>) {
        this.getAllStatistics(null, listener)
    }

    /**
     * Get all Kuzzle usage statistics frames
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun getAllStatistics(options: Options?, listener: ResponseListener<Array<JSONObject>>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.getAllStatistics: listener required")
        }

        this.isValid()
        try {
            val args = QueryArgs()
            args.controller = "server"
            args.action = "getAllStats"
            this.query(args, null, options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    try {
                        val hits = `object`.getJSONObject("result").getJSONArray("hits")
                        val frames = arrayOfNulls<JSONObject>(hits.length())

                        for (i in 0 until hits.length()) {
                            frames[i] = hits.getJSONObject(i)
                        }

                        listener.onSuccess(frames)
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
     * [.getStatistics]
     */
    fun getStatistics(listener: ResponseListener<Array<JSONObject>>) {
        this.getStatistics(null, listener)
    }

    /**
     * Get Kuzzle usage statistics
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun getStatistics(options: Options?, listener: ResponseListener<Array<JSONObject>>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.getStatistics: listener required")
        }
        this.isValid()
        val body = JSONObject()
        val data = JSONObject()

        try {
            body.put("body", data)
            val args = QueryArgs()
            args.controller = "server"
            args.action = "getLastStats"
            this.query(args, body, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(arrayOf(response.getJSONObject("result")))
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
     * [.getStatistics]
     */
    fun getStatistics(timestamp: Long, listener: ResponseListener<Array<JSONObject>>) {
        this.getStatistics(timestamp, null, listener)
    }

    /**
     * Get Kuzzle usage statistics starting from a provided timestamp
     *
     * @param timestamp - Statistic starting time to retrieve
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun getStatistics(timestamp: Long, options: Options?, listener: ResponseListener<Array<JSONObject>>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.getStatistics: listener required")
        }

        this.isValid()
        val body = JSONObject()
        val data = JSONObject()

        try {
            data.put("since", timestamp)
            body.put("body", data)
            val args = QueryArgs()
            args.controller = "server"
            args.action = "getStats"
            this.query(args, body, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val hits = response.getJSONObject("result").getJSONArray("hits")
                        val stats = arrayOfNulls<JSONObject>(hits.length())

                        for (i in 0 until hits.length()) {
                            stats[i] = hits.getJSONObject(i)
                        }

                        listener.onSuccess(stats)
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
     * [.getServerInfo]
     */
    fun getServerInfo(listener: ResponseListener<JSONObject>) {
        this.getServerInfo(null, listener)
    }

    /**
     * Gets server info.
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun getServerInfo(options: Options?, listener: ResponseListener<JSONObject>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.getServerInfo: listener required")
        }
        val args = QueryArgs()
        args.controller = "server"
        args.action = "info"
        try {
            this.query(args, null, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result").getJSONObject("serverInfo"))
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
     * [.listCollections]
     */
    fun listCollections(listener: ResponseListener<Array<JSONObject>>) {
        this.listCollections(null, null, listener)
    }

    /**
     * [.listCollections]
     */
    fun listCollections(index: String, listener: ResponseListener<Array<JSONObject>>) {
        this.listCollections(index, null, listener)
    }

    /**
     * List data collections on the default data index
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun listCollections(options: Options, listener: ResponseListener<Array<JSONObject>>) {
        this.listCollections(null, options, listener)
    }

    /**
     * List data collections
     *
     * @param index - Parent data index name
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun listCollections(index: String?, options: Options?, listener: ResponseListener<Array<JSONObject>>) {
        var index = index
        var options = options
        if (index == null) {
            if (this.defaultIndex == null) {
                throw IllegalArgumentException("Kuzzle.listCollections: index required")
            } else {
                index = this.defaultIndex
            }
        }
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.listCollections: listener required")
        }
        try {
            val args = QueryArgs()
            args.controller = "collection"
            args.action = "list"
            args.index = index
            val query = JSONObject()
            if (options == null) {
                options = Options()
            }
            val body = JSONObject().put("type", options.collectionType)
            query.put("body", body)
            this.query(args, query, options, object : OnQueryDoneListener {
                override fun onSuccess(collections: JSONObject) {
                    try {
                        val result = collections.getJSONObject("result").getJSONArray("collections")
                        val cols = arrayOfNulls<JSONObject>(result.length())

                        for (i in 0 until result.length()) {
                            cols[i] = result.getJSONObject(i)
                        }

                        listener.onSuccess(cols)
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
     * [.listIndexes]
     */
    fun listIndexes(listener: ResponseListener<Array<String>>) {
        this.listIndexes(null, listener)
    }

    /**
     * List data indexes
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun listIndexes(options: Options?, listener: ResponseListener<Array<String>>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.listIndexes: listener required")
        }
        val args = QueryArgs()
        args.controller = "index"
        args.action = "list"
        try {
            this.query(args, null, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val array = response.getJSONObject("result").getJSONArray("hits")
                        val length = array.length()
                        val indexes = arrayOfNulls<String>(length)
                        for (i in 0 until length) {
                            indexes[i] = array.getString(i)
                        }
                        listener.onSuccess(indexes)
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
     * [.login]
     */
    fun login(strategy: String) {
        this.login(strategy, null, -1, null)
    }

    /**
     * [.login]
     */
    fun login(strategy: String, credentials: JSONObject) {
        this.login(strategy, credentials, -1, null)
    }

    /**
     * [.login]
     */
    fun login(strategy: String, expiresIn: Int) {
        this.login(strategy, null, expiresIn, null)
    }

    /**
     * [.login]
     */
    fun login(strategy: String, credentials: JSONObject, expiresIn: Int) {
        this.login(strategy, credentials, expiresIn, null)
    }

    /**
     * [.login]
     */
    fun login(strategy: String, credentials: JSONObject, listener: ResponseListener<JSONObject>) {
        this.login(strategy, credentials, -1, listener)
    }

    /**
     * [.login]
     */
    fun login(strategy: String, listener: ResponseListener<JSONObject>) {
        this.login(strategy, null, -1, listener)
    }

    /**
     * [.login]
     */
    fun login(strategy: String, expiresIn: Int, listener: ResponseListener<JSONObject>) {
        this.login(strategy, null, expiresIn, listener)
    }

    /**
     * Log- Strategy name to use for the authentication
     *
     * @param strategy - Strategy name to use for the authentication
     * @param credentials - Login credentials
     * @param expiresIn - Token expiration delay
     * @param listener - Response callback listener
     */
    fun login(strategy: String, credentials: JSONObject?, expiresIn: Int, listener: ResponseListener<JSONObject>?) {
        if (strategy == null) {
            throw IllegalArgumentException("Kuzzle.login: cannot authenticate to Kuzzle without an authentication strategy")
        }

        this.loginCallback = listener

        try {
            val options = Options()
            val query = JSONObject()
            var body = JSONObject()
            if (credentials != null) {
                body = credentials
            }

            if (expiresIn >= 0) {
                query.put("expiresIn", expiresIn)
            }

            query.put("strategy", strategy)

            query.put("body", body)
            val args = QueryArgs()
            args.controller = "auth"
            args.action = "login"
            options.isQueuable = false

            this.query(args, query, options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    try {
                        val result = `object`.getJSONObject("result")

                        if (result.has("jwt")) {
                            this@Kuzzle.setJwtToken(result.getString("jwt"))
                        }

                        listener?.onSuccess(result)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    try {
                        emitEvent(Event.loginAttempt, JSONObject()
                                .put("success", false)
                                .put("error", error))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * WebViewClient to forward kuzzle's jwt token after an OAuth authentication
     */
    protected open inner class KuzzleWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.contains("code=")) {
                Thread(Runnable {
                    try {
                        val conn = URI.create(url).toURL().openConnection() as HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.useCaches = false

                        val br = BufferedReader(InputStreamReader(conn.inputStream))
                        val sb = StringBuilder()
                        var line: String
                        while ((line = br.readLine()) != null) {
                            sb.append(line)
                        }
                        br.close()

                        val response = JSONObject(sb.toString())
                        if (response.isNull("error")) {
                            this@Kuzzle.setJwtToken(response)

                            if (loginCallback != null) {
                                loginCallback!!.onSuccess(response.getJSONObject("result"))
                            }
                        } else {
                            emitEvent(Event.loginAttempt, JSONObject()
                                    .put("success", false)
                                    .put("error", response.getJSONObject("error")))
                            if (loginCallback != null) {
                                loginCallback!!.onError(response.getJSONObject("error"))
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }).start()
            } else {
                view.loadUrl(url)
            }
            return true
        }
    }

    /**
     * Disconnect from Kuzzle and invalidate this instance.
     * Does not fire a disconnected event.
     */
    fun disconnect() {
        if (this.socket != null) {
            this.socket!!.close()
        }

        this.socket = null
        this.collections.clear()
        this.state = States.DISCONNECTED
    }

    /**
     * [.logout]
     */
    fun logout(): Kuzzle {
        return this.logout(null)
    }

    /**
     * Logout method
     *
     * @param listener - Response callback listener
     * @return this
     */
    fun logout(listener: ResponseListener<Void>?): Kuzzle {
        val options = Options()

        options.isQueuable = false

        try {
            val args = QueryArgs()
            args.controller = "auth"
            args.action = "logout"

            this.query(args, JSONObject(), options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    listener?.onSuccess(null)
                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        this@Kuzzle.jwtToken = null
        return this
    }

    /**
     * [.now]
     */
    fun now(listener: ResponseListener<Date>) {
        this.now(null, listener)
    }

    /**
     * Returns the current Kuzzle UTC timestamp
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun now(options: Options?, listener: ResponseListener<Date>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.now: listener required")
        }
        this.isValid()
        try {
            val args = QueryArgs()
            args.controller = "server"
            args.action = "now"
            this.query(args, null, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(Date(response.getJSONObject("result").getLong("now")))
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
     * [.query]
     */
    @Throws(JSONException::class)
    fun query(queryArgs: QueryArgs, query: JSONObject): Kuzzle {
        return this.query(queryArgs, query, null, null)
    }

    /**
     * [.query]
     */
    @Throws(JSONException::class)
    fun query(queryArgs: QueryArgs, query: JSONObject, options: Options): Kuzzle {
        return this.query(queryArgs, query, options, null)
    }

    /**
     * [.query]
     */
    @Throws(JSONException::class)
    fun query(queryArgs: QueryArgs, query: JSONObject, listener: OnQueryDoneListener): Kuzzle {
        return this.query(queryArgs, query, null, listener)
    }

    /**
     * This is a low-level method, exposed to allow advanced SDK users to bypass high-level methods.
     * Base method used to send queries to Kuzzle
     *
     * @param queryArgs - API route description
     * @param query - Query content
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun query(queryArgs: QueryArgs, query: JSONObject?, options: Options?, listener: OnQueryDoneListener?): Kuzzle {
        this.isValid()
        val `object` = query ?: JSONObject()

        if (`object`.isNull("requestId")) {
            `object`.put("requestId", UUID.randomUUID().toString())
        }

        `object`
                .put("action", queryArgs.action)
                .put("controller", queryArgs.controller)

        // Global volatile data
        val _volatile = JSONObject()
        val ite = this._volatile.keys()
        while (ite.hasNext()) {
            val key = ite.next() as String
            _volatile.put(key, this._volatile.get(key))
        }

        // Volatile data for this query
        if (options != null) {
            if (!options.isQueuable && this.state != States.CONNECTED) {
                discardRequest(listener, `object`)
                return this
            }

            if (options.refresh != null) {
                `object`.put("refresh", options.refresh)
            }

            if (options.volatile != null) {
                val iterator = options.volatile.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next() as String
                    _volatile.put(key, options.volatile.get(key))
                }
            }

            if (options.from != null) {
                `object`.put("from", options.from)
            }

            if (options.size != null) {
                `object`.put("size", options.size)
            }

            if (options.scroll != null) {
                `object`.put("scroll", options.scroll)
            }

            if (options.scrollId != null) {
                `object`.put("scrollId", options.scrollId)
            }
        }

        _volatile.put("sdkVersion", this.sdkVersion)
        `object`.put("volatile", _volatile)

        if (queryArgs.collection != null) {
            `object`.put("collection", queryArgs.collection)
        }

        if (queryArgs.index != null) {
            `object`.put("index", queryArgs.index)
        }

        this.addHeaders(`object`, this.headers)

        /*
     * Do not add the token for the checkToken route, to avoid getting a token error when
     * a developer simply wish to verify his token
     */
        if (this.jwtToken != null && !(queryArgs.controller == "auth" && queryArgs.action == "checkToken")) {
            `object`.put("jwt", this.jwtToken)
        }

        if (this.state == States.CONNECTED || options != null && !options.isQueuable) {
            emitRequest(`object`, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener?.onSuccess(response)
                }

                override fun onError(error: JSONObject?) {
                    if (error != null) {
                        listener!!.onError(error)
                    }
                }
            })
        } else if (this.queuing || options != null && options.isQueuable || this.state == States.INITIALIZING || this.state == States.CONNECTING) {
            cleanQueue()

            if (queueFilter.filter(`object`)) {
                val o = QueryObject()
                o.timestamp = Date()
                o.cb = listener
                o.query = `object`
                this.offlineQueue.addToQueue(o)
                this@Kuzzle.emitEvent(Event.offlineQueuePush, o)
            }
        } else {
            discardRequest(listener, `object`)
        }

        return this
    }

    /**
     * Removes all listeners, either from all events
     *
     * @return this
     */
    fun removeAllListeners(): Kuzzle {
        this.eventListeners.clear()
        return this
    }

    /**
     * Remove all listeners kuzzle from the provided event name
     *
     * @param event - Event name
     * @return this
     */
    fun removeAllListeners(event: Event): Kuzzle {
        if (eventListeners.containsKey(event)) {
            eventListeners[event].clear()
        }

        return this
    }

    /**
     * Removes a listener from an event.
     *
     * @param event - Event name
     * @param listener - Response callback listener
     * @return this
     */
    fun removeListener(event: Event, listener: EventListener): Kuzzle {
        if (eventListeners.containsKey(event)) {
            eventListeners[event].remove(listener)
        }

        return this
    }

    /**
     * Renew all registered subscriptions. Usually called after:
     * - a connection, if subscriptions occurred before
     * - a reconnection
     * - after a successful login attempt, to subscribe with the new credentials
     */
    protected open fun renewSubscriptions() {
        for (roomSubscriptions in subscriptions.values) {
            for (room in roomSubscriptions.values) {
                room.renew(room.listener, room.subscribeListener)
            }
        }
    }

    /**
     * Replays the requests queued during offline mode.
     * Works only if the SDK is not in a disconnected state, and if the autoReplay option is set to false.
     *
     * @return this
     */
    fun replayQueue(): Kuzzle {
        if (this.state != States.OFFLINE && !this.autoReplay) {
            this.cleanQueue()
            this.dequeue()
        }
        return this
    }

    /**
     * [.setHeaders]
     */
    @Throws(JSONException::class)
    fun setHeaders(content: JSONObject): Kuzzle {
        return this.setHeaders(content, false)
    }

    /**
     * Helper function allowing to set headers while chaining calls.
     * If the replace argument is set to true, replace the current headers with the provided content.
     * Otherwise, it appends the content to the current headers, only replacing already existing values
     *
     * @param content - New headers content
     * @param replace - false = append to existing headers (default) - true = replace
     * @return this
     */
    fun setHeaders(content: JSONObject?, replace: Boolean): Kuzzle {
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
     * Starts the requests queuing. Works only during offline mode, and if the autoQueue option is set to false.
     *
     * @return this
     */
    fun startQueuing(): Kuzzle {
        if (this.state == States.OFFLINE && !this.autoQueue) {
            this.queuing = true
        }
        return this
    }

    /**
     * Stops the requests queuing. Works only during offline mode, and if the autoQueue option is set to false.
     *
     * @return this
     */
    fun stopQueuing(): Kuzzle {
        if (this.state == States.OFFLINE && !this.autoQueue) {
            this.queuing = false
        }
        return this
    }

    /**
     * Handles network reconnection
     */
    private fun reconnect() {
        if (this.autoResubscribe) {
            this.renewSubscriptions()
        }

        if (this.autoReplay) {
            this.cleanQueue()
            this.dequeue()
        }

        this.emitEvent(Event.reconnected)
    }

    /**
     * Create a new connection socket
     * @return created socket
     * @throws URISyntaxException
     */
    @Throws(URISyntaxException::class)
    protected open fun createSocket(): Socket {
        val opt = IO.Options()
        opt.forceNew = false
        opt.reconnection = this.isAutoReconnect
        opt.reconnectionDelay = this.reconnectionDelay
        return IO.socket("http://" + host + ":" + this.port, opt)
    }

    /**
     * Emit request.
     *
     * @param request - Request to emit
     * @param listener - Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    protected open fun emitRequest(request: JSONObject, listener: OnQueryDoneListener?) {
        val now = Date()
        val c = Calendar.getInstance()
        c.time = now
        c.add(Calendar.SECOND, -MAX_EMIT_TIMEOUT)

        if (this.jwtToken != null || listener != null) {
            socket!!.once(request.get("requestId").toString()) { args ->
                try {
                    // checking token expiration
                    if (!(args[0] as JSONObject).isNull("error") && (args[0] as JSONObject).getJSONObject("error").getString("message") == "Token expired" && (args[0] as JSONObject).getString("action") != "logout") {
                        emitEvent(Event.tokenExpired, listener)
                    }

                    if (listener != null) {
                        if (!(args[0] as JSONObject).isNull("error")) {
                            listener.onError((args[0] as JSONObject).getJSONObject("error"))
                        } else {
                            listener.onSuccess(args[0] as JSONObject)
                        }
                    }
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }
        }

        socket!!.emit("kuzzle", request)

        // Track requests made to allow Room.subscribeToSelf to work
        this.requestHistory.put(request.getString("requestId"), Date())

        // Clean history from requests made more than 10s ago
        val ite = requestHistory.keys.iterator()

        while (ite.hasNext()) {
            if (this.requestHistory[ite.next()].before(c.time)) {
                ite.remove()
            }
        }
    }

    /**
     * Helper function ensuring that this Kuzzle object is still valid before performing a query
     */
    open fun isValid() {
        if (this.state == States.DISCONNECTED) {
            throw IllegalStateException("This Kuzzle object has been invalidated. Did you try to access it after a disconnect call?")
        }
    }

    /**
     * Helper function copying headers to the query data
     *
     * @param query - Query to update
     * @param headers - Headers to set
     */
    fun addHeaders(query: JSONObject, headers: JSONObject?) {
        try {
            val iterator = headers!!.keys()
            while (iterator.hasNext()) {
                val key = iterator.next() as String
                if (query.isNull(key)) {
                    query.put(key, headers.get(key))
                }
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * autoResubscribe option getter
     *
     * @return autoResubscribe option value
     */
    fun isAutoResubscribe(): Boolean {
        return this.autoResubscribe
    }

    /**
     * autoResubscribe option setter
     *
     * @param resubscribe - autoResubscribe new value
     * @return this
     */
    fun setAutoResubscribe(resubscribe: Boolean): Kuzzle {
        this.autoResubscribe = resubscribe
        return this
    }

    /**
     * Global headers getter
     *
     * @return global headers
     */
    fun getHeaders(): JSONObject? {
        return this.headers
    }

    /**
     * Add pending subscription kuzzle.
     *
     * @param id - Room instance unique ID
     * @param room - Associated Room instance
     * @return this
     */
    fun addPendingSubscription(id: String, room: Room): Kuzzle {
        this.subscriptions["pending"].put(id, room)
        return this
    }

    /**
     * Delete a pending subscription
     *
     * @param id - Unique ID of the Room instance to delete
     * @return this
     */
    open fun deletePendingSubscription(id: String): Kuzzle {
        val pending = this.subscriptions["pending"]

        pending?.remove(id)

        return this
    }

    /**
     * Add a new subscription
     *
     * @param roomId - Subscription identifier
     * @param id - Room instance unique ID
     * @param kuzzleRoom - Associated Room instance
     * @return this
     */
    fun addSubscription(roomId: String, id: String, kuzzleRoom: Room): Kuzzle {
        var room: ConcurrentHashMap<String, Room>? = this.subscriptions[roomId]

        if (room == null) {
            room = ConcurrentHashMap()
            this.subscriptions.put(id, room)
        }

        room.put(id, kuzzleRoom)

        return this
    }

    /**
     * Add a query to the offline queue
     *
     * @param query - Query to queue
     * @return this
     */
    fun setOfflineQueue(query: QueryObject): Kuzzle {
        this.offlineQueue.addToQueue(query)
        return this
    }

    /**
     * Offline queue getter
     *
     * @return offline queue
     */
    fun getOfflineQueue(): Queue<QueryObject> {
        return this.offlineQueue.queue
    }


    /**
     * Sets offline queue filter.
     *
     * @param queueFilter - Offline queue global filter
     * @return this
     */
    fun setQueueFilter(queueFilter: QueueFilter): Kuzzle {
        this.queueFilter = queueFilter
        return this
    }

    /**
     * Offline queue filter getter
     *
     * @return Offline queue filter
     */
    fun getQueueFilter(): QueueFilter {
        return this.queueFilter
    }

    /**
     * autoReplay option getter
     *
     * @return autoReplay option value
     */
    fun isAutoReplay(): Boolean {
        return autoReplay
    }


    /**
     * Kuzzle server port getter
     *
     * @return Kuzzle server port
     */
    fun getPort(): Int {
        return port!!
    }

    /**
     * Kuzzle server port setter
     *
     * @param port - New Kuzzle server port
     * @return this
     */
    fun setPort(port: Int): Kuzzle {
        this.port = port
        return this
    }

    /**
     * Kuzzle server host name getter
     *
     * @return Kuzzle server host name
     */
    fun getHost(): String {
        return this.host
    }

    /**
     * Kuzzle server host name setter
     *
     * @param host - New host name
     * @return this
     */
    fun setHost(host: String): Kuzzle {
        this.host = host
        return this
    }

    /**
     * autoReplay option setter
     *
     * @param autoReplay - New autoReplay option value
     * @return this
     */
    fun setAutoReplay(autoReplay: Boolean): Kuzzle {
        this.autoReplay = autoReplay
        return this
    }

    /**
     * queueMaxSize option setter
     *
     * @param newMaxSize - New queueMaxSize value
     * @return this
     */
    fun setQueueMaxSize(newMaxSize: Int): Kuzzle {
        this.queueMaxSize = Math.max(0, newMaxSize)
        return this
    }

    /**
     * queueMaxSize option getter
     *
     * @return queueMaxSize option value
     */
    fun getQueueMaxSize(): Int {
        return this.queueMaxSize
    }

    /**
     * autoQueue option getter
     *
     * @return autoQueue option value
     */
    fun isAutoQueue(): Boolean {
        return autoQueue
    }

    /**
     * autoQueue option setter
     *
     * @param autoQueue - New autoQueue option value
     * @return this
     */
    fun setAutoQueue(autoQueue: Boolean): Kuzzle {
        this.autoQueue = autoQueue
        return this
    }

    /**
     * Clean up the queue, ensuring the queryTTL and queryMaxSize properties are respected
     */
    private fun cleanQueue() {
        val now = Date()
        val cal = Calendar.getInstance()
        cal.time = now
        cal.add(Calendar.MILLISECOND, -queueTTL)

        if (this.queueTTL > 0) {
            var o: QueryObject
            while ((o = offlineQueue.queue.peek() as QueryObject) != null) {
                if (o.timestamp.before(cal.time)) {
                    offlineQueue.queue.poll()
                } else {
                    break
                }
            }
        }

        val size = this.offlineQueue.queue.size
        if (this.queueMaxSize > 0 && size > this.queueMaxSize) {
            var i = 0
            while (offlineQueue.queue.peek() != null && size - this.queueMaxSize >= i) {
                this.offlineQueue.queue.poll()
                i++
            }
        }
    }

    private fun mergeOfflineQueueWithLoader() {
        val additionalOfflineQueue = this.offlineQueueLoader!!.load()
        try {
            for (additionalQuery in additionalOfflineQueue) {
                for (offlineQuery in this.offlineQueue) {
                    if (additionalQuery.query != null && additionalQuery.query.has("requestId") && additionalQuery.query.has("action") && additionalQuery.query.has("controller")) {
                        if (offlineQuery.query.getString("requestId") != additionalQuery.query.getString("requestId")) {
                            this.offlineQueue.addToQueue(additionalOfflineQueue.dequeue())
                        } else {
                            additionalOfflineQueue.dequeue()
                        }
                    } else {
                        throw IllegalArgumentException("Invalid offline queue request. One or more missing properties: requestId, action, controller.")
                    }
                }
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }

    /**
     * Play all queued requests, in order.
     */
    private fun dequeue() {
        if (offlineQueueLoader != null) {
            this.mergeOfflineQueueWithLoader()
        }
        if (this.offlineQueue.queue.size > 0) {
            try {
                val query = this.offlineQueue.queue.poll() as QueryObject
                this.emitRequest(query.query, query.cb)
                this.emitEvent(Event.offlineQueuePop, query)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }

            val timer = Timer(UUID.randomUUID().toString())
            timer.schedule(object : TimerTask() {
                override fun run() {
                    dequeue()
                }
            }, Math.max(0, this.replayInterval))
        } else {
            this.queuing = false
        }
    }

    /**
     * Delete a suscription
     *
     * @param roomId - Subscription unique ID
     * @param id  - Room instance unique ID
     * @return this
     */
    open fun deleteSubscription(roomId: String, id: String): Kuzzle {
        if (this.subscriptions.containsKey(roomId)) {
            this.subscriptions[roomId].remove(id)

            if (this.subscriptions[roomId].isEmpty()) {
                this.subscriptions.remove(roomId)
            }
        }

        return this
    }

    /**
     * Gets subscriptions.
     *
     * @param roomId - Subscription unique ID
     * @return matching registered subscriptions
     */
    fun getSubscriptions(roomId: String?): Map<String, Room>? {
        return if (roomId != null && this.subscriptions.containsKey(roomId)) {
            this.subscriptions[roomId]
        } else null

    }

    /**
     * requestHistory getter
     *
     * @return Request history
     */
    open fun getRequestHistory(): Map<String, Date> {
        return requestHistory
    }

    /**
     * Default index getter
     *
     * @return Default index value
     */
    fun getDefaultIndex(): String? {
        return this.defaultIndex
    }

    /**
     * Default index setter
     *
     * @param index - New default index name
     * @return this
     */
    fun setDefaultIndex(index: String): Kuzzle {
        if (index == null || index.isEmpty()) {
            throw IllegalArgumentException("Kuzzle.setDefaultIndex: index required")
        }
        this.defaultIndex = index
        return this
    }

    /**
     * Set a new JWT and trigger the 'loginAttempt' event.
     *
     * @param jwt - New authentication JSON Web Token
     * @return this
     */
    fun setJwtToken(jwt: String): Kuzzle {
        this.jwtToken = jwt
        try {
            this.renewSubscriptions()
            this.emitEvent(Event.loginAttempt, JSONObject().put("success", true))
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * Unset the authentication token and cancel all subscriptions
     *
     * @return this
     */
    fun unsetJwtToken(): Kuzzle {
        this.jwtToken = null

        for (roomSubscriptions in subscriptions.values) {
            for (room in roomSubscriptions.values) {
                room.unsubscribe()
            }
        }
        return this
    }

    /**
     * Sets the authentication token from a Kuzzle response object
     *
     * @param response - A Kuzzle API response
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun setJwtToken(response: JSONObject): Kuzzle {
        val result: JSONObject

        if (response == null) {
            throw IllegalArgumentException("Cannot set token from a null Kuzzle response")
        }

        if (response.has("result") && (result = response.getJSONObject("result")).has("jwt")) {
            this.jwtToken = result.getString("jwt")
            this.renewSubscriptions()
            this.emitEvent(Event.loginAttempt, JSONObject().put("success", true))
        } else {
            this.emitEvent(Event.loginAttempt, JSONObject()
                    .put("success", false)
                    .put("error", "Cannot find a valid JWT token in the following object: " + response.toString())
            )
        }

        return this
    }

    /**
     * Authentication token getter
     *
     * @return Current JWT
     */
    fun getJwtToken(): String? {
        return this.jwtToken
    }

    /**
     * queueTTL option getter
     *
     * @param newTTL - New queueTTL value
     * @return this
     */
    fun setQueueTTL(newTTL: Int): Kuzzle {
        this.queueTTL = Math.max(0, newTTL)
        return this
    }

    /**
     * queueTTL option getter
     *
     * @return queue ttl value
     */
    fun getQueueTTL(): Int {
        return this.queueTTL
    }

    /**
     * Global volatile data setter
     *
     * @param _volatile - New global volatile data content
     * @return this
     */
    fun setVolatile(_volatile: JSONObject): Kuzzle {
        this._volatile = _volatile
        return this
    }

    /**
     * Global volatile data getter
     *
     * @return Global volatile data
     */
    fun getVolatile(): JSONObject {
        return this._volatile
    }


    /**
     * replayInterval option setter
     *
     * @param interval - New replayInterval value
     * @return this
     */
    fun setReplayInterval(interval: Long): Kuzzle {
        this.replayInterval = Math.max(0, interval)
        return this
    }

    /**
     * replayInterval option getter
     *
     * @return replayInterval value
     */
    fun getReplayInterval(): Long {
        return this.replayInterval
    }

    /**
     * Offline queue loader setter
     *
     * @param offlineQueueLoader - Offline queue loader function
     */
    fun setOfflineQueueLoader(offlineQueueLoader: OfflineQueueLoader) {
        this.offlineQueueLoader = offlineQueueLoader
    }

    /**
     * [.updateSelf]
     */
    fun updateSelf(content: JSONObject, listener: ResponseListener<*>): Kuzzle {
        return updateSelf(content, null, listener)
    }

    /**
     * Update the currently authenticated user informations
     *
     * @param content - Current user infos to update
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    @JvmOverloads
    fun updateSelf(content: JSONObject, options: Options? = null, listener: ResponseListener<JSONObject>? = null): Kuzzle {
        val args = QueryArgs()
        args.controller = "auth"
        args.action = "updateSelf"

        try {
            val query = JSONObject().put("body", content)
            this.query(args, query, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener?.onSuccess(response)
                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * Retrieves current user information
     *
     * @param listener - Response callback listener
     */
    fun whoAmI(listener: ResponseListener<User>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.whoAmI: listener required")
        }

        try {
            val args = QueryArgs()
            args.controller = "auth"
            args.action = "getCurrentUser"
            val request = JSONObject()

            this.query(args, request, null, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        listener.onSuccess(User(this@Kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
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
     * [.refreshIndex]
     */
    fun refreshIndex(): Kuzzle {
        return this.refreshIndex(null, null, null)
    }

    /**
     * [.refreshIndex]
     */
    fun refreshIndex(index: String): Kuzzle {
        return this.refreshIndex(index, null, null)
    }

    /**
     * [.refreshIndex]
     */
    fun refreshIndex(index: String, listener: ResponseListener<JSONObject>): Kuzzle {
        return this.refreshIndex(index, null, listener)
    }

    /**
     * [.refreshIndex]
     */
    fun refreshIndex(index: String, options: Options): Kuzzle {
        return this.refreshIndex(index, options, null)
    }

    /**
     * [.refreshIndex]
     */
    fun refreshIndex(options: Options): Kuzzle {
        return this.refreshIndex(null, options, null)
    }

    /**
     * Forces the default data index to refresh on each modification
     *
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    fun refreshIndex(options: Options, listener: ResponseListener<JSONObject>): Kuzzle {
        return this.refreshIndex(null, options, listener)
    }

    /**
     * [.refreshIndex]
     */
    fun refreshIndex(listener: ResponseListener<JSONObject>): Kuzzle {
        return this.refreshIndex(null, null, listener)
    }

    /**
     * Forces the provided data index to refresh on each modification
     *
     * @param index - Data index name
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    fun refreshIndex(index: String?, options: Options?, listener: ResponseListener<JSONObject>?): Kuzzle {
        var index = index
        if (index == null) {
            if (this.defaultIndex == null) {
                throw IllegalArgumentException("Kuzzle.refreshIndex: index required")
            } else {
                index = this.defaultIndex
            }
        }

        val args = QueryArgs()
        args.index = index
        args.controller = "index"
        args.action = "refresh"
        val request = JSONObject()

        try {
            this.query(args, request, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener == null) {
                        return
                    }

                    try {
                        val result = response.getJSONObject("result")

                        listener.onSuccess(result)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    if (listener == null) {
                        return
                    }

                    listener.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.getAutoRefresh]
     */
    fun getAutoRefresh(listener: ResponseListener<Boolean>) {
        this.getAutoRefresh(null, null, listener)
    }

    /**
     * [.getAutoRefresh]
     */
    fun getAutoRefresh(index: String, listener: ResponseListener<Boolean>) {
        this.getAutoRefresh(index, null, listener)
    }

    /**
     * Gets the autoRefresh value for the default data index
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun getAutoRefresh(options: Options, listener: ResponseListener<Boolean>) {
        this.getAutoRefresh(null, options, listener)
    }

    /**
     * Gets the autoRefresh value for the provided data index name
     *
     * @param index - Data index name
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun getAutoRefresh(index: String?, options: Options?, listener: ResponseListener<Boolean>) {
        var index = index
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.getAutoRefresh: listener required")
        }

        if (index == null) {
            if (this.defaultIndex == null) {
                throw IllegalArgumentException("Kuzzle.getAutoRefresh: index required")
            } else {
                index = this.defaultIndex
            }
        }

        val args = QueryArgs()
        args.index = index
        args.controller = "index"
        args.action = "getAutoRefresh"
        val request = JSONObject()

        try {
            this.query(args, request, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getBoolean("result")
                        listener.onSuccess(result)
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
     * [.setAutoRefresh]
     */
    fun setAutoRefresh(autoRefresh: Boolean): Kuzzle {
        return this.setAutoRefresh(null, autoRefresh, null, null)
    }

    /**
     * [.setAutoRefresh]
     */
    fun setAutoRefresh(autoRefresh: Boolean, listener: ResponseListener<Boolean>): Kuzzle {
        return this.setAutoRefresh(null, autoRefresh, null, listener)
    }

    /**
     * [.setAutoRefresh]
     */
    fun setAutoRefresh(index: String, autoRefresh: Boolean): Kuzzle {
        return this.setAutoRefresh(index, autoRefresh, null, null)
    }

    /**
     * [.setAutoRefresh]
     */
    fun setAutoRefresh(autoRefresh: Boolean, options: Options): Kuzzle {
        return this.setAutoRefresh(null, autoRefresh, options, null)
    }

    /**
     * [.setAutoRefresh]
     */
    fun setAutoRefresh(index: String, autoRefresh: Boolean, options: Options): Kuzzle {
        return this.setAutoRefresh(index, autoRefresh, options, null)
    }

    /**
     * [.setAutoRefresh]
     */
    fun setAutoRefresh(index: String, autoRefresh: Boolean, listener: ResponseListener<Boolean>): Kuzzle {
        return this.setAutoRefresh(index, autoRefresh, null, listener)
    }

    /**
     * autoRefresh status setter for the default data index
     *
     * @param autoRefresh - New autoRefresh property value
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    fun setAutoRefresh(autoRefresh: Boolean, options: Options, listener: ResponseListener<Boolean>): Kuzzle {
        return this.setAutoRefresh(null, autoRefresh, options, listener)
    }

    /**
     * autorefresh status setter for the provided data index name
     *
     * @param index - Data index name
     * @param autoRefresh - New autoRefresh property value
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    fun setAutoRefresh(index: String?, autoRefresh: Boolean, options: Options?, listener: ResponseListener<Boolean>?): Kuzzle {
        var index = index
        if (index == null) {
            if (this.defaultIndex == null) {
                throw IllegalArgumentException("Kuzzle.setAutoRefresh: index required")
            } else {
                index = this.defaultIndex
            }
        }

        val args = QueryArgs()
        args.index = index
        args.controller = "index"
        args.action = "setAutoRefresh"
        val request: JSONObject

        try {
            request = JSONObject().put("body", JSONObject().put("autoRefresh", autoRefresh))
            this.query(args, request, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    if (listener == null) {
                        return
                    }

                    try {
                        val result = response.getBoolean("result")
                        listener.onSuccess(result)
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.getMyRights]
     */
    fun getMyRights(listener: ResponseListener<Array<JSONObject>>): Kuzzle {
        return getMyRights(null, listener)
    }

    /**
     * Gets the rights array for the currently logged user.
     *
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    fun getMyRights(options: Options?, listener: ResponseListener<Array<JSONObject>>): Kuzzle {
        if (listener == null) {
            throw IllegalArgumentException("Security.getMyRights: listener is mandatory.")
        }
        try {
            this@Kuzzle.query(buildQueryArgs("getMyRights"), JSONObject(), options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val hits = response.getJSONObject("result").getJSONArray("hits")
                        val rights = arrayOfNulls<JSONObject>(hits.length())

                        for (i in 0 until hits.length()) {
                            rights[i] = hits.getJSONObject(i)
                        }

                        listener.onSuccess(rights)
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

        return this
    }

    fun buildQueryArgs(controller: String?, action: String): io.kuzzle.sdk.core.Kuzzle.QueryArgs {
        val args = io.kuzzle.sdk.core.Kuzzle.QueryArgs()
        args.action = action
        args.controller = "security"
        if (controller != null) {
            args.controller = controller
        }
        args.action = action
        return args
    }

    /**
     * Helper function meant to easily build the first Kuzzle.query() argument
     *
     * @param action - API controller action name
     * @return JSONObject - Kuzzle.query() 1st argument object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    protected fun buildQueryArgs(action: String): io.kuzzle.sdk.core.Kuzzle.QueryArgs {
        return buildQueryArgs(null, action)
    }

    /**
     * Invokes a query listener with a custom error message and status
     *
     * @param listener - Response callback listener
     * @param query    - discarded query
     * @throws  JSONException
     */
    @Throws(JSONException::class)
    protected fun discardRequest(listener: OnQueryDoneListener?, query: JSONObject) {
        if (listener != null) {
            val err = JSONObject()
                    .put("status", 400)
                    .put("message", "Unable to execute request: not connected to a Kuzzle server.\nDiscarded request: " + query.toString())

            listener.onError(err)
        }
    }

    /**
     * [.createMyCredentials]
     */
    fun createMyCredentials(strategy: String, credentials: JSONObject, listener: ResponseListener<JSONObject>): Kuzzle {
        return createMyCredentials(strategy, credentials, null, listener)
    }

    /**
     * Create credentials of the specified strategy for the current user.
     *
     * @param strategy - impacted strategy name
     * @param credentials - credentials to create
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    @JvmOverloads
    fun createMyCredentials(strategy: String, credentials: JSONObject, options: Options? = null, listener: ResponseListener<JSONObject>? = null): Kuzzle {
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("body", credentials)
            this@Kuzzle.query(buildQueryArgs("auth", "createMyCredentials"), body, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener?.onSuccess(response.getJSONObject("result"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.deleteMyCredentials]
     */
    fun deleteMyCredentials(strategy: String, listener: ResponseListener<JSONObject>): Kuzzle {
        return deleteMyCredentials(strategy, null, listener)
    }

    /**
     * Delete credentials of the specified strategy for the current user.
     *
     * @param strategy- Name of the strategy to remove
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    @JvmOverloads
    fun deleteMyCredentials(strategy: String, options: Options? = null, listener: ResponseListener<JSONObject>? = null): Kuzzle {
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
            this@Kuzzle.query(buildQueryArgs("auth", "deleteMyCredentials"), body, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener?.onSuccess(response.getJSONObject("result"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.getMyCredentials]
     */
    fun getMyCredentials(strategy: String, listener: ResponseListener<JSONObject>) {
        getMyCredentials(strategy, null, listener)
    }

    /**
     * Get credential information of the specified strategy for the current user.
     *
     * @param strategy - Strategy name to get
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun getMyCredentials(strategy: String, options: Options?, listener: ResponseListener<JSONObject>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.getMyCredentials: listener is mandatory")
        }
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
            this@Kuzzle.query(buildQueryArgs("auth", "getMyCredentials"), body, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result"))
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
     * [.updateMyCredentials]
     */
    fun updateMyCredentials(strategy: String, credentials: JSONObject, listener: ResponseListener<JSONObject>): Kuzzle {
        return updateMyCredentials(strategy, credentials, null, listener)
    }

    /**
     * Update credentials of the specified strategy for the current user.
     *
     * @param strategy - Strategy name to update
     * @param credentials - Updated credentials content
     * @param options - Request options
     * @param listener - Response callback listener
     * @return this
     */
    @JvmOverloads
    fun updateMyCredentials(strategy: String, credentials: JSONObject, options: Options? = null, listener: ResponseListener<JSONObject>? = null): Kuzzle {
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("body", credentials)
            this@Kuzzle.query(buildQueryArgs("auth", "updateMyCredentials"), body, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener?.onSuccess(response.getJSONObject("result"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener?.onError(error)
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * [.validateMyCredentials]
     */
    fun validateMyCredentials(strategy: String, credentials: JSONObject, listener: ResponseListener<Boolean>) {
        validateMyCredentials(strategy, credentials, null, listener)
    }

    /**
     * Validate credentials of the specified strategy for the current user.
     *
     * @param strategy - Strategy name to validate
     * @param credentials - Credentials content
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun validateMyCredentials(strategy: String, credentials: JSONObject, options: Options?, listener: ResponseListener<Boolean>) {
        if (listener == null) {
            throw IllegalArgumentException("Kuzzle.validateMyCredentials: listener is mandatory")
        }
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("body", credentials)
            this@Kuzzle.query(buildQueryArgs("auth", "validateMyCredentials"), body, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getBoolean("result"))
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
}
/**
 * Constructor
 *
 * @param host - Target Kuzzle host name or IP address
 * @throws URISyntaxException
 */
/**
 * Constructor
 *
 * @param host - Target Kuzzle host name or IP address
 * @param options - Request options
 * @throws URISyntaxException
 */
/**
 * [.createIndex]
 */
/**
 * [.updateSelf]
 */
/**
 * [.updateSelf]
 */
/**
 * [.createMyCredentials]
 */
/**
 * [.createMyCredentials]
 */
/**
 * [.deleteMyCredentials]
 */
/**
 * [.deleteMyCredentials]
 */
/**
 * [.updateMyCredentials]
 */
/**
 * [.updateMyCredentials]
 */
