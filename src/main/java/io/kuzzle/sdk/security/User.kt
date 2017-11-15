package io.kuzzle.sdk.security

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.Arrays
import java.util.ArrayList

import io.kuzzle.sdk.core.Kuzzle
import io.kuzzle.sdk.core.Options
import io.kuzzle.sdk.security.Profile
import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener

/**
 * This class handles users management in Kuzzle
 */
class User
/**
 * Instantiates a new Kuzzle user.
 *
 * @param kuzzle  Kuzzle instance to attach
 * @param id      User unique identifier
 * @param content User content
 * @param meta User metadata
 * @throws JSONException
 */
@Throws(JSONException::class)
constructor(kuzzle: Kuzzle, id: String, content: JSONObject?, meta: JSONObject) : AbstractSecurityDocument(kuzzle, id, null, meta) {
    private val profileIds = ArrayList<String>()

    private var credentials = JSONObject()

    init {
        this.deleteActionName = "deleteUser"
        this.updateActionName = "updateUser"

        if (content != null) {
            this.content = JSONObject(content.toString())

            if (content.has("profileIds")) {
                val profiles = content.getJSONArray("profileIds")

                for (i in 0 until profiles.length()) {
                    this.profileIds.add(profiles.getString(i))
                }
            }
        }
    }

    /**
     * Set a new profiles set for this user
     *
     * @param profileIds Array of profile identifiers
     * @return this
     */
    fun setProfiles(profileIds: Array<String>): User {
        if (profileIds == null) {
            throw IllegalArgumentException("User.setProfiles: you must provide an array of profiles IDs strings")
        }

        this.profileIds.clear()
        this.profileIds.addAll(Arrays.asList(*profileIds))

        return this
    }

    /**
     * Adds a new profile to the profiles set of this user
     *
     * @param profile Profile unique ID to add
     * @return this
     */
    fun addProfile(profile: String): User {
        if (profile == null) {
            throw IllegalArgumentException("User.addProfile: you must provide a string")
        }

        this.profileIds.add(profile)

        return this
    }

    /**
     * Replace a user in Kuzzle with this object's content
     *
     * @param options  Request optional arguments
     * @param listener -Callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun replace(options: Options? = null, listener: ResponseListener<User>? = null): User {
        val data = this.serialize()

        if (listener != null) {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("replaceUser"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener.onSuccess(this@User)
                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("replaceUser"), data, options!!)
        }

        return this
    }

    /**
     * [.replace]
     */
    @Throws(JSONException::class)
    fun replace(listener: ResponseListener<User>): User {
        return replace(null, listener)
    }

    /**
     * Create this user into Kuzzle.
     *
     * @param options  Request optional arguments
     * @param listener Callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun create(options: Options? = null, listener: ResponseListener<User>? = null): User {
        val data = this.creationSerialize()

        if (listener != null) {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createUser"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener.onSuccess(this@User)
                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createUser"), data, options!!)
        }

        return this
    }

    /**
     * [.create]
     */
    @Throws(JSONException::class)
    fun create(listener: ResponseListener<User>): User {
        return create(null, listener)
    }

    /**
     * Saves this object's content as a restricted user into Kuzzle.
     *
     * @param options  Request optional arguments
     * @param listener Callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun saveRestricted(options: Options? = null, listener: ResponseListener<User>? = null): User {
        val data = this.serialize()

        if (listener != null) {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createRestrictedUser"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener.onSuccess(this@User)
                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createRestrictedUser"), data, options!!)
        }

        return this
    }

    /**
     * [.saveRestricted]
     */
    @Throws(JSONException::class)
    fun saveRestricted(listener: ResponseListener<User>): User {
        return saveRestricted(null, listener)
    }

    /**
     * Return a JSONObject representing a serialized version of this object
     *
     * @return serialized version of this object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    override fun serialize(): JSONObject {
        val data = JSONObject().put("_id", this.id)
        val content = JSONObject(this.content.toString())

        if (this.profileIds.size > 0) {
            content.put("profileIds", JSONArray(this.profileIds))
        }

        data.put("body", content)

        return data
    }

    /**
     * Return a JSONObject representing a serialized version of this object
     *
     * @return serialized version of this object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun creationSerialize(): JSONObject {
        val data = JSONObject().put("_id", this.id)
        val body = JSONObject()
        val content = JSONObject(this.content.toString())
        val credentials = JSONObject(this.credentials.toString())

        if (this.profileIds.size > 0) {
            content.put("profileIds", JSONArray(this.profileIds))
        }

        body.put("content", content)
        body.put("credentials", credentials)
        data.put("body", body)

        return data
    }

    /**
     * @return the associated profile identifiers
     */
    fun getProfileIds(): Array<String> {
        return this.profileIds.toTypedArray()
    }

    /**
     * [.getProfiles]
     */
    @Throws(JSONException::class)
    fun getProfiles(listener: ResponseListener<Array<Profile>>) {
        getProfiles(null, listener)
    }

    /**
     * Gets the profile objects from the associated profile identifiers
     *
     * @param  options Request optional arguments
     * @param  listener  Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun getProfiles(options: Options?, listener: ResponseListener<Array<Profile>>?) {
        if (listener == null) {
            throw IllegalArgumentException("User.getProfiles: a valid ResponseListener object is required")
        }

        val profiles = arrayOfNulls<Profile>(this.profileIds.size)

        if (this.profileIds.size == 0) {
            listener.onSuccess(profiles)
            return
        }

        // using an array to allow these variables to be final
        // while keeping the possibility to change their value
        val fetched = intArrayOf(0)
        val errored = booleanArrayOf(false)

        for (i in this.profileIds.indices) {
            this.kuzzleSecurity.fetchProfile(this.profileIds[i], options, object : ResponseListener<Profile> {
                override fun onSuccess(response: Profile) {
                    profiles[fetched[0]] = response
                    fetched[0]++

                    if (fetched[0] == this@User.profileIds.size) {
                        listener.onSuccess(profiles)
                    }
                }

                override fun onError(error: JSONObject) {
                    // prevents triggering the listener multiple times
                    if (errored[0]) {
                        return
                    }

                    errored[0] = true
                    listener.onError(error)
                }
            })
        }
    }

    /**
     * User credentials setter
     * @param credentials New credentials value
     * @return  this
     */
    fun setCredentials(credentials: JSONObject): User {
        this.credentials = credentials

        return this
    }
}
/**
 * [.replace]
 */
/**
 * [.replace]
 */
/**
 * [.create]
 */
/**
 * [.create]
 */
/**
 * [.saveRestricted]
 */
/**
 * [.saveRestricted]
 */
