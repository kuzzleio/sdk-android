package io.kuzzle.sdk.security

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.Arrays

import io.kuzzle.sdk.core.Kuzzle
import io.kuzzle.sdk.core.Options
import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener

/**
 * This class handles profiles management in Kuzzle
 */
class Profile
/**
 * Instantiates a new Kuzzle profile.
 *
 * @param kuzzle  Kuzzle instance to attach
 * @param id      Profile unique ID
 * @param content Profile content
 * @param meta Profile metadata
 * @throws JSONException
 */
@Throws(JSONException::class)
constructor(kuzzle: Kuzzle, id: String, content: JSONObject?, meta: JSONObject) : AbstractSecurityDocument(kuzzle, id, null, meta) {
    private var policies = ArrayList<JSONObject>()

    init {
        this.deleteActionName = "deleteProfile"
        this.updateActionName = "updateProfile"

        if (content != null) {
            this.content = JSONObject(content.toString())

            if (content.has("policies")) {
                val arr = content.getJSONArray("policies")

                for (i in 0 until arr.length()) {
                    this.policies.add(arr.getJSONObject(i))
                }

                this.content.remove("policies")
            }
        }
    }

    /**
     * Save this profile in Kuzzle
     *
     * @param options  Request optional arguments
     * @param listener Callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun save(options: Options?, listener: ResponseListener<Profile>?): Profile {
        val data: JSONObject

        if (this.policies.size == 0) {
            throw IllegalArgumentException("Cannot save the profile " + this.id + ": no policy defined")
        }

        data = this.serialize()

        if (listener != null) {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceProfile"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    listener.onSuccess(this@Profile)
                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(this.kuzzleSecurity.buildQueryArgs("createOrReplaceProfile"), data, options!!)
        }

        return this
    }

    /**
     * [.save]
     */
    @Throws(JSONException::class)
    fun save(listener: ResponseListener<Profile>): Profile {
        return this.save(null, listener)
    }

    /**
     * [.save]
     */
    @Throws(JSONException::class)
    fun save(options: Options): Profile {
        return this.save(options, null)
    }

    /**
     * [.save]
     */
    @Throws(JSONException::class)
    fun save(): Profile {
        return this.save(null, null)
    }

    /**
     * Add a new role to the list of allowed roles of this profile
     *
     * @param policy Policy to add to this profile
     * @return this
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class)
    fun addPolicy(policy: JSONObject): Profile {
        if (!policy.has("roleId")) {
            throw IllegalArgumentException("The policy must have, at least, a roleId set.")
        }

        this.policies.add(policy)
        return this
    }

    /**
     * Add a new policy to the list of policies of this profile via its roleId
     *
     * @param  roleId Name of the role to add to this profile
     * @return this
     */
    fun addPolicy(roleId: String): Profile {
        val policy = JSONObject()
        try {
            policy.put("roleId", roleId)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        this.policies.add(policy)
        return this
    }

    /**
     * Replace the current policies list with a new one
     *
     * @param policies New policies list
     * @return this
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class)
    fun setPolicies(policies: Array<JSONObject>): Profile {
        for (policy in policies) {
            if (!policy.has("roleId")) {
                throw IllegalArgumentException("All pocicies must have at least a roleId set.")
            }
        }

        this.policies = ArrayList(Arrays.asList(*policies))

        return this
    }

    /**
     * Replace the current policies list with a new one via its rolesIds
     *
     * @param roleIds New roles identifiers list
     * @return this
     */
    fun setPolicies(roleIds: Array<String>): Profile {
        this.policies.clear()

        try {
            for (roleId in roleIds) {
                this.policies.add(JSONObject().put("roleId", roleId))
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return this
    }

    /**
     * Serialize the content of this object to a JSON Object
     * @return a serialized version of this object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    override fun serialize(): JSONObject {
        val data = JSONObject()
        val content = JSONObject(this.content.toString())

        if (this.policies.size > 0) {
            content.put("policies", JSONArray(this.policies))
        }

        data
                .put("_id", this.id)
                .put("body", content)

        return data
    }

    /**
     * Return the list of the policies assigned to this profile
     *
     * @return a JSONArray of policies objects
     */
    fun getPolicies(): Array<JSONObject> {
        return this.policies.toTypedArray()
    }
}
