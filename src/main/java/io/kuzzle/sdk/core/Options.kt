package io.kuzzle.sdk.core

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import io.kuzzle.sdk.enums.CollectionType
import io.kuzzle.sdk.enums.Mode
import io.kuzzle.sdk.responses.SearchResult

class Options {
    // Default values
    private var autoQueue = false
    private var autoReconnect = true
    private var autoReplay = false
    private var autoResubscribe = true
    private var headers = JSONObject()
    private var _volatile = JSONObject()
    private var queueMaxSize = 500
    private var queueTTL = 120000
    private var reconnectionDelay: Long = 1000
    private var ifExist = "error"
    private var connect = Mode.AUTO
    private var offlineMode = Mode.MANUAL
    private var replayInterval = 10
    private var queuable = true
    private var defaultIndex: String? = null
    private var replaceIfExist = false
    private var refresh: String? = null
    private var from: Long? = null
    private var size: Long? = null
    private var port: Int? = 7512
    private var scroll: String? = null
    private var previous: SearchResult? = null
    private var scrollId: String? = null
    private var retryOnConflict = 0

    // MemoryStorage specific options
    private var start: Long? = null
    private var end: Long? = null
    private var unit: String? = null
    private var withcoord = false
    private var withdist = false
    private var count: Long? = null
    private var sort: String? = null
    private var match: String? = null
    private var ex: Long? = null
    private var nx = false
    private var px: Long? = null
    private var xx = false
    private var alpha = false
    private var by: String? = null
    private var direction: String? = null
    private var get: Array<String>? = null
    private var limit: Array<Int>? = null
    private var ch = false
    private var incr = false
    private var aggregate: String? = null
    private var weights: Array<Int>? = null

    // Used for getting collections
    private var collectionType = CollectionType.ALL

    constructor() {
        // Default constructor
    }

    @Throws(JSONException::class)
    constructor(originalOptions: Options) {
        this.autoQueue = originalOptions.autoQueue
        this.autoReconnect = originalOptions.autoReconnect
        this.autoResubscribe = originalOptions.autoResubscribe
        this.headers = JSONObject(originalOptions.headers.toString())
        this._volatile = JSONObject(originalOptions._volatile.toString())
        this.queueMaxSize = originalOptions.queueMaxSize
        this.queueTTL = originalOptions.queueTTL
        this.reconnectionDelay = originalOptions.reconnectionDelay
        this.ifExist = originalOptions.ifExist
        this.connect = originalOptions.connect
        this.offlineMode = originalOptions.offlineMode
        this.replayInterval = originalOptions.replayInterval
        this.queuable = originalOptions.queuable
        this.defaultIndex = originalOptions.defaultIndex
        this.replaceIfExist = originalOptions.replaceIfExist
        this.refresh = originalOptions.refresh
        this.from = originalOptions.from
        this.size = originalOptions.size
        this.port = originalOptions.port
        this.scroll = originalOptions.scroll
        this.previous = originalOptions.previous
        this.scrollId = originalOptions.scrollId
    }

    /**
     * autoReconnect option getter
     *
     * @return isAutoReconnection option value
     */
    fun isAutoReconnect(): Boolean {
        return autoReconnect
    }

    /**
     * autoReconnect option setter
     *
     * @param autoReconnect New autoReconnect option value
     * @return this
     */
    fun setAutoReconnect(autoReconnect: Boolean): Options {
        this.autoReconnect = autoReconnect
        return this
    }

    /**
     * headers getter
     *
     * @return headers option value
     */
    fun getHeaders(): JSONObject {
        return headers
    }

    /**
     * headers setter
     *
     * @param headers New headers value
     * @return this
     */
    fun setHeaders(headers: JSONObject): Options {
        this.headers = headers
        return this
    }

    /**
     * exists option getter
     *
     * @return exists option value
     */
    fun getIfExist(): String {
        return ifExist
    }

    /**
     * exists option setter
     *
     * @param value new exists option value
     * @return this
     */
    fun setIfExist(value: String): Options {
        if (value !== "error" && value !== "replace") {
            throw IllegalArgumentException("Invalid value for option 'ifExists': " + value)
        }

        this.ifExist = value
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
     * Sets volatile data.
     *
     * @param _volatile new volatile data value
     * @return this
     */
    fun setVolatile(_volatile: JSONObject): Options {
        this._volatile = _volatile
        return this
    }

    /**
     * Gets connect property value
     *
     * @return the connect property value
     */
    fun getConnect(): Mode {
        return connect
    }

    /**
     * Sets connect property value
     *
     * @param connect New connect property value
     * @return this
     */
    fun setConnect(connect: Mode): Options {
        this.connect = connect
        return this
    }

    /**
     * reconnectionDelay property getter
     *
     * @return the reconnectionDelay property value
     */
    fun getReconnectionDelay(): Long {
        return reconnectionDelay
    }

    /**
     * reconnectionDelay property setter
     *
     * @param reconnectionDelay New reconnectionDelay property value
     * @return this
     */
    fun setReconnectionDelay(reconnectionDelay: Long): Options {
        this.reconnectionDelay = reconnectionDelay
        return this
    }

    /**
     * offlineMode property getter
     *
     * @return the offlineMode option value
     */
    fun getOfflineMode(): Mode {
        return offlineMode
    }

    /**
     * offlineMode property setter
     *
     * @param offlineMode New offlineMode value
     * @return this
     */
    fun setOfflineMode(offlineMode: Mode): Options {
        this.offlineMode = offlineMode
        return this
    }

    /**
     * queueTTL property getter
     *
     * @return queueTTL property value
     */
    fun getQueueTTL(): Int {
        return queueTTL
    }

    /**
     * queueTTL property setter
     *
     * @param queueTTL New queueTTL value
     * @return this
     */
    fun setQueueTTL(queueTTL: Int): Options {
        this.queueTTL = queueTTL
        return this
    }

    /**
     * autoReplay property getter
     *
     * @return autoReplay property value
     */
    fun isAutoReplay(): Boolean {
        return autoReplay
    }

    /**
     * autoReplay property setter
     *
     * @param autoReplay New autoReplay value
     * @return this
     */
    fun setAutoReplay(autoReplay: Boolean): Options {
        this.autoReplay = autoReplay
        return this
    }

    /**
     * queuable property getter
     *
     * @return queuable property value
     */
    fun isQueuable(): Boolean {
        return queuable
    }

    /**
     * queuable property setter
     *
     * @param queuable New queuable value
     * @return this
     */
    fun setQueuable(queuable: Boolean): Options {
        this.queuable = queuable
        return this
    }

    /**
     * queueMaxSize property getter
     *
     * @return queueMaxSize value
     */
    fun getQueueMaxSize(): Int {
        return queueMaxSize
    }

    /**
     * queueMaxSize property setter
     *
     * @param queueMaxSize New queueMaxSize value
     * @return this
     */
    fun setQueueMaxSize(queueMaxSize: Int): Options {
        this.queueMaxSize = queueMaxSize
        return this
    }

    /**
     * replayInterval property getter
     *
     * @return replayInterval property value
     */
    fun getReplayInterval(): Int {
        return replayInterval
    }

    /**
     * replayInterval property setter
     *
     * @param replayInterval New replayInterval value
     * @return this
     */
    fun setReplayInterval(replayInterval: Int): Options {
        this.replayInterval = replayInterval
        return this
    }

    /**
     * autoResubscribe property getter
     *
     * @return autoResubscribe property value
     */
    fun isAutoResubscribe(): Boolean {
        return autoResubscribe
    }

    /**
     * autoResubscribe property setter
     *
     * @param autoResubscribe New autoResubscribe value
     * @return this
     */
    fun setAutoResubscribe(autoResubscribe: Boolean): Options {
        this.autoResubscribe = autoResubscribe
        return this
    }

    /**
     * collectionType property getter
     *
     * @return collectionType property value
     */
    fun getCollectionType(): CollectionType {
        return collectionType
    }

    /**
     * collectionType property setter
     *
     * @param type New collectionType value
     * @return this
     */
    fun setCollectionType(type: CollectionType): Options {
        this.collectionType = type
        return this
    }

    /**
     * defaultIndex property setter
     *
     * @param index New defaultIndex value
     * @return this
     */
    fun setDefaultIndex(index: String): Options {
        this.defaultIndex = index
        return this
    }

    /**
     * defaultIndex property getter
     *
     * @return defaultIndex property value
     */
    fun getDefaultIndex(): String? {
        return this.defaultIndex
    }

    /**
     * autoQueue property setter
     *
     * @param autoQueue New autoQueue value
     * @return this
     */
    fun setAutoQueue(autoQueue: Boolean): Options {
        this.autoQueue = autoQueue
        return this
    }

    /**
     * autoQueue property getter
     *
     * @return autoQueue property value
     */
    fun isAutoQueue(): Boolean {
        return this.autoQueue
    }

    /**
     * replaceIfExist property setter
     *
     * @param replace New replaceIfExist value
     * @return this
     */
    fun setReplaceIfExist(replace: Boolean): Options {
        this.replaceIfExist = replace
        return this
    }

    /**
     * replaceIfExist property getter
     *
     * @return replaceIfExist property value
     */
    fun isReplaceIfExist(): Boolean {
        return this.replaceIfExist
    }

    /**
     * from property getter
     * @return from property value
     */
    fun getFrom(): Long? {
        return from
    }

    /**
     * from property setter
     * @param  from New from value
     * @return this
     */
    fun setFrom(from: Long?): Options {
        this.from = from
        return this
    }

    /**
     * size property getter
     * @return size property value
     */
    fun getSize(): Long? {
        return size
    }

    /**
     * size property setter
     * @param  size New size value
     * @return this
     */
    fun setSize(size: Long?): Options {
        this.size = size
        return this
    }

    /**
     * port property setter
     * @param  port New port value
     * @return this
     */
    fun setPort(port: Int?): Options {
        this.port = port
        return this
    }

    /**
     * port property getter
     * @return port property value
     */
    fun getPort(): Int? {
        return this.port
    }

    /**
     * refresh property getter
     * @return refresh property value
     */
    fun getRefresh(): String? {
        return refresh
    }

    /**
     * refresh property setter
     * @param  refresh New refresh property value
     * @return         [description]
     */
    fun setRefresh(refresh: String): Options {
        this.refresh = refresh
        return this
    }

    /**
     * scroll property getter
     * @return scroll property value
     */
    fun getScroll(): String? {
        return scroll
    }

    /**
     * scroll property setter
     * @param  scroll New scroll value
     * @return this
     */
    fun setScroll(scroll: String): Options {
        this.scroll = scroll
        return this
    }

    /**
     * previous property getter
     * @return previous property value
     */
    fun getPrevious(): SearchResult? {
        return previous
    }

    /**
     * previous property setter
     * @param  previous New previous value
     * @return this
     */
    fun setPrevious(previous: SearchResult): Options {
        this.previous = previous
        return this
    }

    /**
     * scrollId property getter
     * @return scrollId property value
     */
    fun getScrollId(): String? {
        return scrollId
    }

    /**
     * scrollId property setter
     * @param  scrollId New scrollId value
     * @return this
     */
    fun setScrollId(scrollId: String): Options {
        this.scrollId = scrollId
        return this
    }

    /**
     * retryOnConflict property getter
     * @return retryOnConflict property value
     */
    fun getRetryOnConflict(): Int {
        return retryOnConflict
    }

    /**
     * retryOnConflict property setter
     * @param  retryOnConflict New retryOnConflict value
     * @return this
     */
    fun setRetryOnConflict(retryOnConflict: Int): Options {
        if (retryOnConflict < 0) {
            throw IllegalArgumentException("Invalid value for the retryOnConflict option (positive or null integer allowed)")
        }

        this.retryOnConflict = retryOnConflict
        return this
    }

    /**
     * start property getter
     * @return start property value
     */
    fun getStart(): Long? {
        return start
    }

    /**
     * start property setter
     * @param  start New start value
     * @return this
     */
    fun setStart(start: Long?): Options {
        this.start = start
        return this
    }

    /**
     * end property getter
     * @return end property value
     */
    fun getEnd(): Long? {
        return end
    }

    /**
     * end property setter
     * @param  end New end value
     * @return this
     */
    fun setEnd(end: Long?): Options {
        this.end = end
        return this
    }

    /**
     * unit property getter
     * @return unit property value
     */
    fun getUnit(): String? {
        return unit
    }

    /**
     * unit property setter
     * @param  unit New unit value
     * @return this
     */
    fun setUnit(unit: String): Options {
        this.unit = unit
        return this
    }

    /**
     * withcoord property getter
     * @return withcoord property value
     */
    fun getWithcoord(): Boolean {
        return withcoord
    }

    /**
     * withcoord property setter
     * @param  withcoord New withcoord value
     * @return this
     */
    fun setWithcoord(withcoord: Boolean): Options {
        this.withcoord = withcoord
        return this
    }

    /**
     * withdist property getter
     * @return withdist property value
     */
    fun getWithdist(): Boolean {
        return withdist
    }

    /**
     * withdist property setter
     * @param  withdist New withdist value
     * @return this
     */
    fun setWithdist(withdist: Boolean): Options {
        this.withdist = withdist
        return this
    }

    /**
     * count property getter
     * @return count property value
     */
    fun getCount(): Long? {
        return count
    }

    /**
     * count property setter
     * @param  count New count value
     * @return this
     */
    fun setCount(count: Long?): Options {
        this.count = count
        return this
    }

    /**
     * sort property getter
     * @return sort property value
     */
    fun getSort(): String? {
        return sort
    }

    /**
     * sort property setter
     * @param  sort New sort value
     * @return this
     */
    fun setSort(sort: String): Options {
        this.sort = sort
        return this
    }

    /**
     * match property getter
     * @return match property value
     */
    fun getMatch(): String? {
        return match
    }

    /**
     * match property setter
     * @param  match New match value
     * @return this
     */
    fun setMatch(match: String): Options {
        this.match = match
        return this
    }

    /**
     * ex property getter
     * @return ex property value
     */
    fun getEx(): Long? {
        return ex
    }

    /**
     * ex property setter
     * @param  ex New ex value
     * @return this
     */
    fun setEx(ex: Long?): Options {
        this.ex = ex
        return this
    }

    /**
     * nx property getter
     * @return nx property value
     */
    fun getNx(): Boolean {
        return nx
    }

    /**
     * nx property setter
     * @param  nx New nx value
     * @return this
     */
    fun setNx(nx: Boolean): Options {
        this.nx = nx
        return this
    }

    /**
     * px property getter
     * @return px property value
     */
    fun getPx(): Long? {
        return px
    }

    /**
     * px property setter
     * @param  px New px value
     * @return this
     */
    fun setPx(px: Long?): Options {
        this.px = px
        return this
    }

    /**
     * xx property getter
     * @return xx property value
     */
    fun getXx(): Boolean {
        return xx
    }

    /**
     * xx property setter
     * @param  xx New xx value
     * @return this
     */
    fun setXx(xx: Boolean): Options {
        this.xx = xx
        return this
    }

    /**
     * alpha property getter
     * @return alpha property value
     */
    fun getAlpha(): Boolean {
        return alpha
    }

    /**
     * alpha property setter
     * @param  alpha New alpha value
     * @return this
     */
    fun setAlpha(alpha: Boolean): Options {
        this.alpha = alpha
        return this
    }

    /**
     * by property getter
     * @return by property value
     */
    fun getBy(): String? {
        return by
    }

    /**
     * by property setter
     * @param  by New by value
     * @return this
     */
    fun setBy(by: String): Options {
        this.by = by
        return this
    }

    /**
     * direction property getter
     * @return direction property value
     */
    fun getDirection(): String? {
        return direction
    }

    /**
     * direction property setter
     * @param  direction New direction value
     * @return this
     */
    fun setDirection(direction: String): Options {
        this.direction = direction
        return this
    }

    /**
     * get property getter
     * @return get property value
     */
    fun getGet(): Array<String>? {
        return get
    }

    /**
     * get property setter
     * @param  get New get value
     * @return this
     */
    fun setGet(get: Array<String>): Options {
        this.get = get
        return this
    }

    /**
     * limit property getter
     * @return limit property value
     */
    fun getLimit(): Array<Int>? {
        return limit
    }

    /**
     * limit property setter
     * @param  limit New limit value
     * @return this
     */
    fun setLimit(limit: Array<Int>): Options {
        this.limit = limit
        return this
    }

    /**
     * ch property getter
     * @return ch property value
     */
    fun getCh(): Boolean {
        return ch
    }

    /**
     * ch property setter
     * @param  ch New ch value
     * @return this
     */
    fun setCh(ch: Boolean): Options {
        this.ch = ch
        return this
    }

    /**
     * incr property getter
     * @return incr property value
     */
    fun getIncr(): Boolean {
        return incr
    }

    /**
     * incr property setter
     * @param  incr New incr value
     * @return this
     */
    fun setIncr(incr: Boolean): Options {
        this.incr = incr
        return this
    }

    /**
     * aggregate property getter
     * @return aggregate property value
     */
    fun getAggregate(): String? {
        return aggregate
    }

    /**
     * aggregate property setter
     * @param  aggregate New aggregate value
     * @return this
     */
    fun setAggregate(aggregate: String): Options {
        this.aggregate = aggregate
        return this
    }

    /**
     * weights property getter
     * @return weights property value
     */
    fun getWeights(): Array<Int>? {
        return weights
    }

    /**
     * weights property setter
     * @param  weights New weights value
     * @return this
     */
    fun setWeights(weights: Array<Int>): Options {
        this.weights = weights
        return this
    }
}
