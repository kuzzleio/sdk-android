package io.kuzzle.sdk.util

import org.json.JSONObject

import java.util.Date

import io.kuzzle.sdk.core.Options
import io.kuzzle.sdk.listeners.OnQueryDoneListener

class QueryObject {
    /**
     * @return Query content
     */
    /**
     * @param query Query content
     */
    var query: JSONObject? = null
    /**
     * @return Controller action name to execute
     */
    /**
     * @param action Controller action name to execute
     */
    var action: String? = null
    /**
     * @return Query options
     */
    /**
     * @param options Query options
     */
    var options: Options? = null
    /**
     * @return Callback to invoke with the query result
     */
    /**
     * @param cb Callback to invoke with the query result
     */
    var cb: OnQueryDoneListener? = null
    /**
     * @return Query timestamp (Epoch time)
     */
    /**
     * @param timestamp Query timestamp (Epoch time)
     */
    var timestamp: Date? = null
}
