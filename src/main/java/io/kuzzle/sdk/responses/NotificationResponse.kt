package io.kuzzle.sdk.responses

import org.json.JSONException
import org.json.JSONObject

import io.kuzzle.sdk.core.Collection
import io.kuzzle.sdk.core.Kuzzle
import io.kuzzle.sdk.core.Document
import io.kuzzle.sdk.enums.Scope
import io.kuzzle.sdk.enums.State
import io.kuzzle.sdk.enums.Users

class NotificationResponse
/**
 * Response notification representation
 * @see [SDK Reference](http://docs.kuzzle.io/sdk-reference/essentials/notifications/)
 *
 * @param  kuzzle Kuzzle instance to attach
 * @param  object Raw Kuzzle API notification
 */
(kuzzle: Kuzzle, `object`: JSONObject) {
    /**
     * @return Notification status value
     */
    var status: Int = 0
        private set
    /**
     * @return Impacted data index value
     */
    var index: String? = null
        private set
    /**
     * @return Impacted data collection value
     */
    var collection: String? = null
        private set
    /**
     * @return Invoked Kuzzle API controller
     */
    var controller: String? = null
        private set
    /**
     * @return Executed API controller action
     */
    var action: String? = null
        private set
    /**
     * @return Notification state
     */
    var state: State? = null
        private set
    /**
     * @return Notification scope
     */
    var scope: Scope? = null
        private set
    /**
     * @return Notification users state
     */
    var users: Users? = null
        private set
    /**
     * @return Notification volatile data
     */
    var volatile: JSONObject? = null
        private set
    /**
     * @return Origin request unique ID
     */
    var requestId: String? = null
        private set
    /**
     * @return Notification content
     */
    var document: Document? = null
        private set
    /**
     * @return Notification raw content
     */
    var result: JSONObject? = null
        private set

    init {
        try {
            this.status = `object`.getInt("status")
            this.index = `object`.getString("index")
            this.collection = `object`.getString("collection")
            this.controller = `object`.getString("controller")
            this.action = `object`.getString("action")
            this.state = if (`object`.isNull("state")) null else State.valueOf(`object`.getString("state").toUpperCase())
            this.volatile = if (`object`.isNull("volatile")) JSONObject() else `object`.getJSONObject("volatile")
            this.requestId = if (`object`.isNull("requestId")) null else `object`.getString("requestId")
            this.result = if (`object`.isNull("result")) null else `object`.getJSONObject("result")
            this.scope = if (`object`.isNull("scope")) null else Scope.valueOf(`object`.getString("scope").toUpperCase())
            this.users = if (`object`.isNull("user")) null else Users.valueOf(`object`.getString("user").toUpperCase())
            if (!`object`.getJSONObject("result").isNull("_source")) {
                val content = `object`.getJSONObject("result")
                val id = content.getString("_id")
                val meta = if (content.isNull("_meta")) JSONObject() else content.getJSONObject("_meta")
                content.remove("_id")
                content.remove("_meta")
                this.document = Document(Collection(kuzzle, this.collection!!, this.index!!), id, content, meta)
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    }
}
