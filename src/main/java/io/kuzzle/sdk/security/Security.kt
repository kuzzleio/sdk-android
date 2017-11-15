package io.kuzzle.sdk.security

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.util.ArrayList
import java.util.Arrays

import io.kuzzle.sdk.core.Kuzzle
import io.kuzzle.sdk.core.Options
import io.kuzzle.sdk.enums.Policies
import io.kuzzle.sdk.listeners.ResponseListener
import io.kuzzle.sdk.listeners.OnQueryDoneListener
import io.kuzzle.sdk.responses.SecurityDocumentList
import io.kuzzle.sdk.util.Scroll

/**
 * Kuzzle security API
 */
class Security
/**
 * Instantiates a new Kuzzle security instance
 *
 * @param kuzzle Kuzzle instance to attach
 */
(private val kuzzle: Kuzzle) {

    /**
     * Helper function meant to easily build the first Kuzzle.query() argument
     *
     * @param action Security controller action name
     * @return Kuzzle.query() 1st argument object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    protected fun buildQueryArgs(controller: String?, action: String): Kuzzle.QueryArgs {
        val args = io.kuzzle.sdk.core.Kuzzle.QueryArgs()
        args.action = action
        args.controller = "security"

        if (controller != null) {
            args.controller = controller
        }
        return args
    }

    @Throws(JSONException::class)
    fun buildQueryArgs(action: String): Kuzzle.QueryArgs {
        return buildQueryArgs(null, action)
    }

    /**
     * Retrieves a single Role using its unique Role ID
     *
     * @param id       Unique role ID
     * @param options  Optional query arguments
     * @param listener Response callback listener
     */
    fun fetchRole(id: String, options: Options?, listener: ResponseListener<Role>) {
        val data: JSONObject

        if (id == null) {
            throw IllegalArgumentException("Security.fetchRole: a role ID is required")
        }

        if (listener == null) {
            throw IllegalArgumentException("Security.fetchRole: a listener is required")
        }

        try {
            data = JSONObject().put("_id", id)
            this.kuzzle.query(buildQueryArgs("getRole"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        listener.onSuccess(Role(this@Security.kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
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
     * [.fetchRole]
     */
    fun fetchRole(id: String, listener: ResponseListener<Role>) {
        fetchRole(id, null, listener)
    }


    /**
     * Executes a search on roles using a set of filters
     *
     * @param filters  Search filters (see ElasticSearch filters)
     * @param options  Optional query arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun searchRoles(filters: JSONObject, options: Options?, listener: ResponseListener<SecurityDocumentList>) {
        if (filters == null) {
            throw IllegalArgumentException("Security.searchRoles: filters cannot be null")
        }

        if (listener == null) {
            throw IllegalArgumentException("Security.searchRoles: a callback listener is required")
        }

        val data = JSONObject().put("body", filters)

        this.kuzzle.query(buildQueryArgs("searchRoles"), data, options, object : OnQueryDoneListener {
            override fun onSuccess(response: JSONObject) {
                try {
                    val result = response.getJSONObject("result")
                    val documents = result.getJSONArray("hits")
                    val documentsLength = documents.length()
                    val roles = ArrayList<AbstractSecurityDocument>()

                    for (i in 0 until documentsLength) {
                        val document = documents.getJSONObject(i)
                        roles.add(Role(this@Security.kuzzle, document.getString("_id"), document.getJSONObject("_source"), document.getJSONObject("_meta")))
                    }

                    listener.onSuccess(SecurityDocumentList(roles, result.getLong("total")))
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }

            }

            override fun onError(error: JSONObject) {
                listener.onError(error)
            }
        })
    }

    /**
     * [.searchRoles]
     */
    @Throws(JSONException::class)
    fun searchRoles(filters: JSONObject, listener: ResponseListener<SecurityDocumentList>) {
        searchRoles(filters, null, listener)
    }

    /**
     * Create a new role in Kuzzle.
     * If the same role already exists: throw an error by default
     * (see the replaceIfExist request optional argument)
     *
     * @param id       New role ID
     * @param content  New role rights definitions
     * @param options  Request optional parameters
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun createRole(id: String, content: JSONObject, options: Options? = null, listener: ResponseListener<Role>? = null) {
        var action = "createRole"

        if (id == null || content == null) {
            throw IllegalArgumentException("Security.createRole: cannot create a role without an ID or a content")
        }

        val data = JSONObject().put("_id", id).put("body", content)

        if (options != null && options.isReplaceIfExist) {
            action = "createOrReplaceRole"
        }

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs(action), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        listener.onSuccess(Role(this@Security.kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs(action), data, options!!)
        }
    }

    /**
     * [.createRole]
     */
    @Throws(JSONException::class)
    fun createRole(id: String, content: JSONObject, listener: ResponseListener<Role>) {
        createRole(id, content, null, listener)
    }

    /**
     * Delete a role from Kuzzle
     *
     * @param id       ID of the role to delete
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun deleteRole(id: String, options: Options? = null, listener: ResponseListener<String>? = null): Security {
        if (id == null) {
            throw IllegalArgumentException("Security.deleteRole: cannot delete role without an ID")
        }

        val data = JSONObject().put("_id", id)

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs("deleteRole"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result").getString("_id"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs("deleteRole"), data, options!!)
        }

        return this
    }

    /**
     * [.deleteRole]
     */
    @Throws(JSONException::class)
    fun deleteRole(id: String, listener: ResponseListener<String>): Security {
        return deleteRole(id, null, listener)
    }

    /**
     * Update a role's content
     *
     * @param id ID of the role to update
     * @param content Role content to update
     * @param options Request options
     * @param listener Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun updateRole(id: String, content: JSONObject, options: Options? = null, listener: ResponseListener<Role>? = null): Security {
        if (id == null) {
            throw IllegalArgumentException("Security.updateRole: cannot update role without an ID")
        }

        val data = JSONObject().put("_id", id)
        data.put("body", content)

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs("updateRole"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(Role(this@Security.kuzzle, response.getJSONObject("result").getString("_id"), response.getJSONObject("result").getJSONObject("_source"), response.getJSONObject("result").getJSONObject("_meta")))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs("updateRole"), data, options!!)
        }

        return this
    }

    /**
     * [.updateRole]
     */
    @Throws(JSONException::class)
    fun updateRole(id: String, content: JSONObject, listener: ResponseListener<Role>): Security {
        return updateRole(id, content, null, listener)
    }

    /**
     * Instantiate a new Role object.
     * Does not automatically create it in Kuzzle
     *
     * @param id Role unique identifier
     * @param content Role content
     * @param meta Role metadata
     * @return new Role object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun role(id: String, content: JSONObject, meta: JSONObject): Role {
        return Role(this.kuzzle, id, content, meta)
    }

    /**
     * [.role]
     */
    @Throws(JSONException::class)
    fun role(id: String, content: JSONObject): Role {
        return Role(this.kuzzle, id, content, null)
    }

    /**
     * [.role]
     */
    @Throws(JSONException::class)
    fun role(id: String): Role {
        return Role(this.kuzzle, id, null, null)
    }

    /**
     * Get a specific profile from kuzzle
     *
     * @param id ID of the profile to retrieve
     * @param options Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun fetchProfile(id: String, options: Options?, listener: ResponseListener<Profile>) {
        if (id == null) {
            throw IllegalArgumentException("Security.fetchProfile: cannot get 'null' profile")
        }

        if (listener == null) {
            throw IllegalArgumentException("Security.fetchProfile: a listener callback is required")
        }

        val data = JSONObject().put("_id", id)

        this.kuzzle.query(buildQueryArgs("getProfile"), data, options, object : OnQueryDoneListener {
            override fun onSuccess(response: JSONObject) {
                try {
                    val result = response.getJSONObject("result")
                    val formattedPolicies = JSONArray()
                    val policies = result.getJSONObject("_source").getJSONArray("policies")

                    for (i in 0 until policies.length()) {
                        val formattedPolicy = JSONObject()
                                .put("roleId", policies.getJSONObject(i).getString("roleId"))
                        if ((policies.get(i) as JSONObject).has("restrictedTo")) {
                            formattedPolicy.put("restrictedTo", policies.getJSONObject(i).getJSONArray("restrictedTo"))
                        }
                        if ((policies.get(i) as JSONObject).has("allowInternalIndex")) {
                            formattedPolicy.put("allowInternalIndex", policies.getJSONObject(i).getBoolean("allowInternalIndex"))
                        }
                        formattedPolicies.put(formattedPolicy)
                    }

                    result.getJSONObject("_source").remove("policies")
                    result.getJSONObject("_source").put("policies", formattedPolicies)

                    listener.onSuccess(Profile(this@Security.kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }

            }

            override fun onError(error: JSONObject) {
                listener.onError(error)
            }
        })
    }

    /**
     * [.fetchProfile]
     */
    @Throws(JSONException::class)
    fun fetchProfile(id: String, listener: ResponseListener<Profile>) {
        fetchProfile(id, null, listener)
    }

    /**
     * Executes a search on profiles according to a set of filters
     *
     * @param filters  Search filters
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun searchProfiles(filters: JSONObject, options: Options?, listener: ResponseListener<SecurityDocumentList>) {
        if (filters == null) {
            throw IllegalArgumentException("Security.searchProfiles: cannot perform a search on null filters")
        }

        if (listener == null) {
            throw IllegalArgumentException("Security.searchProfiles: a listener callback is required")
        }

        val data = JSONObject().put("body", filters)

        this.kuzzle.query(buildQueryArgs("searchProfiles"), data, options, object : OnQueryDoneListener {
            override fun onSuccess(response: JSONObject) {
                try {
                    val result = response.getJSONObject("result")
                    val documents = result.getJSONArray("hits")
                    val documentsLength = documents.length()
                    val profiles = ArrayList<AbstractSecurityDocument>()

                    for (i in 0 until documentsLength) {
                        val document = documents.getJSONObject(i)
                        profiles.add(Profile(this@Security.kuzzle, document.getString("_id"), document.getJSONObject("_source"), document.getJSONObject("_meta")))
                    }

                    val scroll = Scroll()

                    if (result.has("scrollId")) {
                        scroll.scrollId = result.getString("scrollId")
                    }

                    listener.onSuccess(SecurityDocumentList(profiles, result.getLong("total"), scroll))
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }

            }

            override fun onError(error: JSONObject) {
                listener.onError(error)
            }
        })
    }

    /**
     * [.searchProfiles]
     */
    @Throws(JSONException::class)
    fun searchProfiles(filters: JSONObject, listener: ResponseListener<SecurityDocumentList>) {
        searchProfiles(filters, null, listener)
    }

    /**
     * Create a new profile in Kuzzle.
     * Throws an error if the profile already exists, unless
     * "replaceIfExists" is set to true in request options
     *
     * @param id       ID of the new profile
     * @param policies List of policies attached to the new profile
     * @param options  Request optional arguments
     * @param listener Callback lisener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun createProfile(id: String, policies: Array<JSONObject>, options: Options? = null, listener: ResponseListener<Profile>? = null) {
        var action = "createProfile"

        if (id == null || policies == null) {
            throw IllegalArgumentException("Security.createProfile: cannot create a profile with null ID or policies")
        }

        val data = JSONObject()
                .put("_id", id)
                .put("body", JSONObject().put("policies", JSONArray(Arrays.asList(*policies))))

        if (options != null && options.isReplaceIfExist) {
            action = "createOrReplaceProfile"
        }

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs(action), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        listener.onSuccess(Profile(this@Security.kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs(action), data, options!!)
        }
    }

    /**
     * [.createProfile]
     */
    @Throws(JSONException::class)
    fun createProfile(id: String, policies: Array<JSONObject>, listener: ResponseListener<Profile>) {
        createProfile(id, policies, null, listener)
    }

    /**
     * Delete a profile from Kuzzle
     *
     * @param id       ID of the profile to delete
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun deleteProfile(id: String, options: Options? = null, listener: ResponseListener<String>? = null): Security {
        if (id == null) {
            throw IllegalArgumentException("Security.deleteProfile: cannot delete a profile with ID null")
        }

        val data = JSONObject().put("_id", id)

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs("deleteProfile"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result").getString("_id"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs("deleteProfile"), data, options!!)
        }

        return this
    }

    /**
     * [.deleteProfile]
     */
    @Throws(JSONException::class)
    fun deleteProfile(id: String, listener: ResponseListener<String>): Security {
        return deleteProfile(id, null, listener)
    }

    /**
     * Returns the next batch of searched profiles with scroll
     *
     * @param scroll   Scroll object obtained doing a scroll search
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun scrollProfiles(scroll: Scroll, options: Options, listener: ResponseListener<SecurityDocumentList>?) {
        val request: JSONObject

        try {
            request = JSONObject().put("body", JSONObject())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        if (listener == null) {
            throw IllegalArgumentException("listener cannot be null")
        }

        if (scroll.scrollId == null) {
            throw IllegalArgumentException("Security.scrollProfiles: scrollId is required")
        }

        options.scrollId = scroll.scrollId

        try {
            this.kuzzle.query(buildQueryArgs("scrollProfiles"), request, options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    try {
                        val hits = `object`.getJSONObject("result").getJSONArray("hits")
                        val profiles = ArrayList<AbstractSecurityDocument>()

                        for (i in 0 until hits.length()) {
                            val hit = hits.getJSONObject(i)
                            val profile = Profile(this@Security.kuzzle, hit.getString("_id"), hit.getJSONObject("_source"), hit.getJSONObject("_meta"))

                            profiles.add(profile)
                        }

                        val response = SecurityDocumentList(profiles, hits.length().toLong(), scroll)

                        listener.onSuccess(response)
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
     * [.scrollProfiles]
     */
    @Throws(JSONException::class)
    fun scrollProfiles(scroll: Scroll, listener: ResponseListener<SecurityDocumentList>) {
        this.scrollProfiles(scroll, Options(), listener)
    }

    /**
     * Update a profile's content
     *
     * @param id ID of the profile to update
     * @param policies List of policies to apply to this profile
     * @param options Request options
     * @param listener Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun updateProfile(id: String, policies: Array<JSONObject>, options: Options? = null, listener: ResponseListener<Profile>? = null): Security {
        if (id == null) {
            throw IllegalArgumentException("Security.updateProfile: cannot update a profile with ID null")
        }

        val data = JSONObject().put("_id", id)
        data.put("body", JSONObject().put("policies", JSONArray(Arrays.asList(*policies))))

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs("updateProfile"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(Profile(this@Security.kuzzle, response.getJSONObject("result").getString("_id"), response.getJSONObject("result").getJSONObject("_source"), response.getJSONObject("result").getJSONObject("_meta")))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs("updateProfile"), data, options!!)
        }

        return this
    }

    /**
     * [.updateProfile]
     */
    @Throws(JSONException::class)
    fun updateProfile(id: String, policies: Array<JSONObject>, listener: ResponseListener<Profile>): Security {
        return this.updateProfile(id, policies, null, listener)
    }

    /**
     * Instantiate a new Profile object.
     * Does not create it in Kuzzle.
     *
     * @param id Profile unique identifier
     * @param content Profile content
     * @param meta Profile metadata
     * @return new Profile object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun profile(id: String, content: JSONObject, meta: JSONObject): Profile {
        return Profile(this.kuzzle, id, content, meta)
    }

    /**
     * [.profile]
     */
    @Throws(JSONException::class)
    fun profile(id: String, content: JSONObject): Profile {
        return Profile(this.kuzzle, id, content, null)
    }

    /**
     * [.profile]
     */
    @Throws(JSONException::class)
    fun profile(id: String): Profile {
        return Profile(this.kuzzle, id, null, null)
    }

    /**
     * Get a specific user from kuzzle using its unique ID
     *
     * @param id       User ID to retrieve
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun fetchUser(id: String, options: Options?, listener: ResponseListener<User>) {
        if (id == null) {
            throw IllegalArgumentException("Security.fetchUser: cannot get user with ID null")
        }

        if (listener == null) {
            throw IllegalArgumentException("Security.fetchUser: a callback listener is required")
        }

        val data = JSONObject().put("_id", id)

        this.kuzzle.query(buildQueryArgs("getUser"), data, options, object : OnQueryDoneListener {
            override fun onSuccess(response: JSONObject) {
                try {
                    val result = response.getJSONObject("result")
                    listener.onSuccess(User(this@Security.kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }

            }

            override fun onError(error: JSONObject) {
                listener.onError(error)
            }
        })
    }

    /**
     * [.fetchUser]
     */
    @Throws(JSONException::class)
    fun fetchUser(id: String, listener: ResponseListener<User>) {
        fetchUser(id, null, listener)
    }

    /**
     * Replaces an existing user in Kuzzle.
     * The new content must contain a "profileIds" attribute, an array
     * listing the attached profiles for this user
     *
     * @param id       ID of the user to replace
     * @param content  New user content
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun replaceUser(id: String, content: JSONObject, options: Options? = null, listener: ResponseListener<User>? = null) {
        if (id == null) {
            throw IllegalArgumentException("Security.replaceUser: cannot replace user without an ID")
        }

        val action = "replaceUser"

        val data = JSONObject().put("_id", id).put("body", content)

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs(action), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        listener.onSuccess(User(this@Security.kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs(action), data, options!!)
        }
    }

    /**
     * [.replaceUser]
     */
    @Throws(JSONException::class)
    fun replaceUser(id: String, content: JSONObject, listener: ResponseListener<User>) {
        replaceUser(id, content, null, listener)
    }

    /**
     * Searches users using a set of filters
     *
     * @param filters  Search filters
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun searchUsers(filters: JSONObject, options: Options?, listener: ResponseListener<SecurityDocumentList>) {

        if (filters == null) {
            throw IllegalArgumentException("Security.searchUsers: cannot perform a search with null filters")
        }

        if (listener == null) {
            throw IllegalArgumentException("Security.searchUsers: a callback listener is required")
        }

        val data = JSONObject().put("body", filters)

        this.kuzzle.query(buildQueryArgs("searchUsers"), data, options, object : OnQueryDoneListener {
            override fun onSuccess(response: JSONObject) {
                try {
                    val result = response.getJSONObject("result")
                    val documents = result.getJSONArray("hits")
                    val documentsLength = documents.length()
                    val users = ArrayList<AbstractSecurityDocument>()

                    for (i in 0 until documentsLength) {
                        val document = documents.getJSONObject(i)
                        users.add(User(this@Security.kuzzle, document.getString("_id"), document.getJSONObject("_source"), document.getJSONObject("_meta")))
                    }

                    val scroll = Scroll()

                    if (result.has("scrollId")) {
                        scroll.scrollId = result.getString("scrollId")
                    }

                    listener.onSuccess(SecurityDocumentList(users, result.getLong("total"), scroll))
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }

            }

            override fun onError(error: JSONObject) {
                listener.onError(error)
            }
        })
    }

    /**
     * [.searchUsers]
     */
    @Throws(JSONException::class)
    fun searchUsers(filters: JSONObject, listener: ResponseListener<SecurityDocumentList>) {
        searchUsers(filters, null, listener)
    }

    /**
     * Create a new user in Kuzzle.
     *
     * @param id       ID of the user to create
     * @param content  User content
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun createUser(id: String, content: JSONObject, options: Options? = null, listener: ResponseListener<User>? = null) {
        val action = "createUser"
        if (id == null || content == null) {
            throw IllegalArgumentException("Security.createUser: cannot create a user with a null ID or content")
        }

        val data = JSONObject().put("_id", id).put("body", content)

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs(action), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        listener.onSuccess(User(this@Security.kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs(action), data, options!!)
        }
    }

    /**
     * [.createUser]
     */
    @Throws(JSONException::class)
    fun createUser(id: String, content: JSONObject, listener: ResponseListener<User>) {
        createUser(id, content, null, listener)
    }

    /**
     * Create a new restricted user in Kuzzle.
     *
     * This function will create a new user. It is not usable to update an existing user.
     * This function allows anonymous users to create a "restricted" user with predefined rights.
     *
     * @param id       ID of the user to create
     * @param content  New user content
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun createRestrictedUser(id: String, content: JSONObject, options: Options? = null, listener: ResponseListener<User>? = null) {
        if (id == null || content == null) {
            throw IllegalArgumentException("Security.createRestrictedUser: cannot create a user with a null ID or content")
        }

        if (content.has("profileIds")) {
            throw IllegalArgumentException("Security.createRestrictedUser: cannot provide profileIds")
        }

        val data = JSONObject().put("_id", id).put("body", content)

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs("createRestrictedUser"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val result = response.getJSONObject("result")
                        listener.onSuccess(User(this@Security.kuzzle, result.getString("_id"), result.getJSONObject("_source"), result.getJSONObject("_meta")))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs("createRestrictedUser"), data, options!!)
        }
    }

    /**
     * [.createRestrictedUser]
     */
    @Throws(JSONException::class)
    fun createRestrictedUser(id: String, content: JSONObject, listener: ResponseListener<User>) {
        createRestrictedUser(id, content, null, listener)
    }

    /**
     * Delete a user from Kuzzle
     *
     * @param id       ID of the user to delete
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun deleteUser(id: String, options: Options? = null, listener: ResponseListener<String>? = null): Security {
        if (id == null) {
            throw IllegalArgumentException("Security.deleteUser: cannot delete user with ID null")
        }

        val data = JSONObject().put("_id", id)

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs("deleteUser"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(response.getJSONObject("result").getString("_id"))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs("deleteUser"), data, options!!)
        }

        return this
    }

    /**
     * [.deleteUser]
     */
    @Throws(JSONException::class)
    fun deleteUser(id: String, listener: ResponseListener<String>): Security {
        return deleteUser(id, null, listener)
    }

    /**
     * Returns the next batch of searched users
     *
     * @param scroll   Scroll object, obtained using a scroll search
     * @param options  Request optional arguments
     * @param listener Response callback listener
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun scrollUsers(scroll: Scroll, options: Options, listener: ResponseListener<SecurityDocumentList>?) {
        val request: JSONObject

        try {
            request = JSONObject().put("body", JSONObject())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        if (listener == null) {
            throw IllegalArgumentException("listener cannot be null")
        }

        if (scroll.scrollId == null) {
            throw IllegalArgumentException("Security.scrollUsers: scrollId is required")
        }

        options.scrollId = scroll.scrollId

        try {
            this.kuzzle.query(buildQueryArgs("scrollUsers"), request, options, object : OnQueryDoneListener {
                override fun onSuccess(`object`: JSONObject) {
                    try {
                        val hits = `object`.getJSONObject("result").getJSONArray("hits")
                        val users = ArrayList<AbstractSecurityDocument>()

                        for (i in 0 until hits.length()) {
                            val hit = hits.getJSONObject(i)
                            val user = User(this@Security.kuzzle, hit.getString("_id"), hit.getJSONObject("_source"), hit.getJSONObject("_meta"))

                            users.add(user)
                        }

                        val response = SecurityDocumentList(users, hits.length().toLong(), scroll)

                        listener.onSuccess(response)
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
     * [.scrollUsers]
     */
    @Throws(JSONException::class)
    fun scrollUsers(scroll: Scroll, listener: ResponseListener<SecurityDocumentList>) {
        this.scrollUsers(scroll, Options(), listener)
    }

    /**
     * Update a user.
     *
     * @param id ID of the user to update
     * @param content User content to update
     * @param options Request options
     * @param listener Response callback listener
     * @return this
     * @throws JSONException
     */
    @Throws(JSONException::class)
    @JvmOverloads
    fun updateUser(id: String, content: JSONObject, options: Options? = null, listener: ResponseListener<User>? = null): Security {
        if (id == null) {
            throw IllegalArgumentException("Security.updateUser: cannot update user without an ID")
        }

        val data = JSONObject().put("_id", id)
        data.put("body", content)

        if (listener != null) {
            this.kuzzle.query(buildQueryArgs("updateUser"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        listener.onSuccess(User(this@Security.kuzzle, response.getJSONObject("result").getString("_id"), response.getJSONObject("result").getJSONObject("_source"), response.getJSONObject("result").getJSONObject("_meta")))
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                }

                override fun onError(error: JSONObject) {
                    listener.onError(error)
                }
            })
        } else {
            this.kuzzle.query(buildQueryArgs("updateUser"), data, options!!)
        }

        return this
    }

    /**
     * [.updateUser]
     */
    @Throws(JSONException::class)
    fun updateUser(id: String, content: JSONObject, listener: ResponseListener<User>): Security {
        return updateUser(id, content, null, listener)
    }

    /**
     * Instantiate a new User object
     * Does not create it in Kuzzle
     *
     * @param id      User unique identifier
     * @param content User content
     * @param meta    User metadata
     * @return new User object
     * @throws JSONException
     */
    @Throws(JSONException::class)
    fun user(id: String, content: JSONObject, meta: JSONObject): User {
        return User(this.kuzzle, id, content, meta)
    }

    /**
     * [.user]
     */
    @Throws(JSONException::class)
    fun user(id: String, content: JSONObject): User {
        return User(this.kuzzle, id, content, null)
    }

    /**
     * [.user]
     */
    @Throws(JSONException::class)
    fun user(id: String): User {
        return User(this.kuzzle, id, null, null)
    }

    /**
     * [.isActionAllowed]
     */
    fun isActionAllowed(policies: Array<JSONObject>, controller: String, action: String): Policies {
        return this.isActionAllowed(policies, controller, action, null, null)
    }

    /**
     * [.isActionAllowed]
     */
    fun isActionAllowed(policies: Array<JSONObject>, controller: String, action: String, index: String): Policies {
        return this.isActionAllowed(policies, controller, action, index, null)
    }

    /**
     * Tells whether an action is allowed, denied or conditional based on the rights
     * policies provided as the first argument. An action is defined as a couple of
     * action and controller (required), plus a data index and a collection (optional).
     *
     * @param policies List of policies containing the current authorizations
     * @param controller Kuzzle API Controller
     * @param action Controller action
     * @param index Data index
     * @param collection Data collection
     * @return action authorization status
     */
    fun isActionAllowed(policies: Array<JSONObject>, controller: String, action: String, index: String?, collection: String?): Policies {
        if (policies == null) {
            throw IllegalArgumentException("Security.isActionAllowed: policies are mandatory.")
        }
        if (controller == null || controller.isEmpty()) {
            throw IllegalArgumentException("Security.isActionAllowed: controller is mandatory.")
        }
        if (action == null || action.isEmpty()) {
            throw IllegalArgumentException("Security.isActionAllowed: action is mandatory.")
        }

        var filteredPolicies: Array<JSONObject>
        try {
            filteredPolicies = filterPolicy(policies, "controller", controller)
            filteredPolicies = filterPolicy(filteredPolicies, "action", action)
            filteredPolicies = filterPolicy(filteredPolicies, "index", index)
            filteredPolicies = filterPolicy(filteredPolicies, "collection", collection)

            for (filteredPolicy in filteredPolicies) {
                if (filteredPolicy.getString("value") == Policies.allowed.toString()) {
                    return Policies.allowed
                }
            }

            for (filteredPolicy in filteredPolicies) {
                if (filteredPolicy.getString("value") == Policies.conditional.toString()) {
                    return Policies.conditional
                }
            }
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        return Policies.denied
    }

    @Throws(JSONException::class)
    private fun filterPolicy(policies: Array<JSONObject>, attr: String, attrInput: String?): Array<JSONObject> {
        val filteredPolicies = ArrayList<JSONObject>()

        for (policy in policies) {
            val attrObject = policy.getString(attr)
            if (attrObject == attrInput || attrObject == "*") {
                filteredPolicies.add(policy)
            }
        }
        return filteredPolicies.toTypedArray()
    }

    /**
     * [.getUserRights]
     */
    fun getUserRights(id: String, listener: ResponseListener<Array<JSONObject>>): Security {
        return getUserRights(id, null, listener)
    }

    /**
     * Gets the rights array of a given user.
     *
     * @param id User ID to retrieve the rights from
     * @param options Request options
     * @param listener Response callback listener
     * @return this
     */
    fun getUserRights(id: String, options: Options?, listener: ResponseListener<Array<JSONObject>>): Security {
        if (id == null || id.isEmpty()) {
            throw IllegalArgumentException("Security.getUserRights: id is mandatory.")
        }
        if (listener == null) {
            throw IllegalArgumentException("Security.getUserRights: listener is mandatory.")
        }
        try {
            val data = JSONObject()
                    .put("_id", id)
            kuzzle.query(buildQueryArgs("getUserRights"), data, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val arr = response.getJSONObject("result").getJSONArray("hits")
                        val rights = arrayOfNulls<JSONObject>(arr.length())

                        for (i in 0 until arr.length()) {
                            rights[i] = arr.getJSONObject(i)
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

    /**
     * [.createCredentials]
     */
    fun createCredentials(strategy: String, kuid: String, credentials: JSONObject, listener: ResponseListener<JSONObject>): Security {
        return createCredentials(strategy, kuid, credentials, null, listener)
    }

    /**
     * Create credentials of the specified strategy for the provided user.
     *
     * @param strategy Strategy name to add to the user
     * @param kuid Kuzzle User unique Identifier for the user to update
     * @param credentials Credentials content
     * @param options Request options
     * @param listener Response callback listener
     * @return this
     */
    @JvmOverloads
    fun createCredentials(strategy: String, kuid: String, credentials: JSONObject, options: Options? = null, listener: ResponseListener<JSONObject>? = null): Security {
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("_id", kuid)
                    .put("body", credentials)
            kuzzle.query(buildQueryArgs("security", "createCredentials"), body, options, object : OnQueryDoneListener {
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
     * [.deleteCredentials]
     */
    fun deleteCredentials(strategy: String, kuid: String, listener: ResponseListener<JSONObject>): Security {
        return deleteCredentials(strategy, kuid, null, listener)
    }

    /**
     * Delete credentials of the specified strategy for the user kuid .
     *
     * @param strategy Strategy name to delete from the user
     * @param kuid Kuzzle User unique Identifier for the user to update
     * @param options Request options
     * @param listener Response callback listener
     * @return this
     */
    @JvmOverloads
    fun deleteCredentials(strategy: String, kuid: String, options: Options? = null, listener: ResponseListener<JSONObject>? = null): Security {
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("_id", kuid)
            kuzzle.query(buildQueryArgs("security", "deleteCredentials"), body, options, object : OnQueryDoneListener {
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
     * [.getAllCredentialFields]
     */
    fun getAllCredentialFields(listener: ResponseListener<JSONObject>) {
        getAllCredentialFields(null, listener)
    }

    /**
     * Gets a list of all accepted fields per authentication strategy.
     *
     * @param options - Request options
     * @param listener - Response callback listener
     */
    fun getAllCredentialFields(options: Options?, listener: ResponseListener<JSONObject>) {
        if (listener == null) {
            throw IllegalArgumentException("Security.getAllCredentialFields: listener is mandatory.")
        }
        try {
            val body = JSONObject()
            kuzzle.query(buildQueryArgs("security", "getAllCredentialFields"), body, options, object : OnQueryDoneListener {
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
     * [.getCredentialFields]
     */
    fun getCredentialFields(strategy: String, listener: ResponseListener<Array<String>>) {
        getCredentialFields(strategy, null, listener)
    }

    /**
     * Retrieve the list of accepted field names by the specified strategy.
     *
     * @param strategy Name of the strategy to get
     * @param options Request options
     * @param listener Response callback listener
     */
    fun getCredentialFields(strategy: String, options: Options?, listener: ResponseListener<Array<String>>) {
        if (listener == null) {
            throw IllegalArgumentException("Security.getAllCredentialFields: listener is mandatory.")
        }
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
            kuzzle.query(buildQueryArgs("security", "getCredentialFields"), body, options, object : OnQueryDoneListener {
                override fun onSuccess(response: JSONObject) {
                    try {
                        val array = response.getJSONObject("result").getJSONArray("hits")
                        val length = array.length()
                        val fields = arrayOfNulls<String>(length)
                        for (i in 0 until length) {
                            fields[i] = array.getString(i)
                        }
                        listener.onSuccess(fields)
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
     * [.getCredentials]
     */
    fun getCredentials(strategy: String, kuid: String, listener: ResponseListener<JSONObject>) {
        getCredentials(strategy, kuid, null, listener)
    }

    /**
     * Get credential information of the specified strategy for the user kuid.
     *
     * @param strategy Strategy name to get
     * @param kuid User unique identifier
     * @param options Request options
     * @param listener Response callback listener
     */
    fun getCredentials(strategy: String, kuid: String, options: Options?, listener: ResponseListener<JSONObject>) {
        if (listener == null) {
            throw IllegalArgumentException("Security.getCredentials: listener is mandatory.")
        }
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("_id", kuid)
            kuzzle.query(buildQueryArgs("security", "getCredentials"), body, options, object : OnQueryDoneListener {
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
     * [.hasCredentials]
     */
    fun hasCredentials(strategy: String, kuid: String, listener: ResponseListener<Boolean>) {
        hasCredentials(strategy, kuid, null, listener)
    }

    /**
     * Check the existence of the specified strategy’s credentials for the user kuid.
     *
     * @param strategy Strategy name to check
     * @param kuid User unique identifier
     * @param options Request options
     * @param listener Response callback listener
     */
    fun hasCredentials(strategy: String, kuid: String, options: Options?, listener: ResponseListener<Boolean>) {
        if (listener == null) {
            throw IllegalArgumentException("Security.hasCredentials: listener is mandatory.")
        }
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("_id", kuid)
            kuzzle.query(buildQueryArgs("security", "hasCredentials"), body, options, object : OnQueryDoneListener {
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

    /**
     * [.updateCredentials]
     */
    fun updateCredentials(strategy: String, kuid: String, credentials: JSONObject, listener: ResponseListener<JSONObject>): Security {
        return updateCredentials(strategy, kuid, credentials, null, listener)
    }

    /**
     * Updates credentials of the specified strategy for the user kuid.
     *
     * @param strategy Strategy name to update
     * @param kuid User unique identifier
     * @param credentials Credentials content to update
     * @param options Request options
     * @param listener Response callback listener
     * @return this
     */
    @JvmOverloads
    fun updateCredentials(strategy: String, kuid: String, credentials: JSONObject, options: Options? = null, listener: ResponseListener<JSONObject>? = null): Security {
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("_id", kuid)
                    .put("body", credentials)
            kuzzle.query(buildQueryArgs("security", "updateCredentials"), body, options, object : OnQueryDoneListener {
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
     * [.validateCredentials]
     */
    fun validateCredentials(strategy: String, kuid: String, credentials: JSONObject, listener: ResponseListener<Boolean>) {
        validateCredentials(strategy, kuid, credentials, null, listener)
    }

    /**
     * Validate credentials of the specified strategy for the user kuid.
     *
     * @param strategy Strategy name to validate
     * @param kuid User unique identifier
     * @param options Request options
     * @param listener Response callback listener
     */
    fun validateCredentials(strategy: String, kuid: String, credentials: JSONObject, options: Options?, listener: ResponseListener<Boolean>) {
        if (listener == null) {
            throw IllegalArgumentException("Security.getCredentials: listener is mandatory.")
        }
        try {
            val body = JSONObject()
                    .put("strategy", strategy)
                    .put("credentials", credentials)
                    .put("_id", kuid)
            kuzzle.query(buildQueryArgs("security", "validateCredentials"), body, options, object : OnQueryDoneListener {
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
 * [.createRole]
 */
/**
 * [.createRole]
 */
/**
 * [.deleteRole]
 */
/**
 * [.deleteRole]
 */
/**
 * [.updateRole]
 */
/**
 * [.updateRole]
 */
/**
 * [.createProfile]
 */
/**
 * [.createProfile]
 */
/**
 * [.deleteProfile]
 */
/**
 * [.deleteProfile]
 */
/**
 * [.updateProfile]
 */
/**
 * [.updateProfile]
 */
/**
 * [.replaceUser]
 */
/**
 * [.replaceUser]
 */
/**
 * [.createUser]
 */
/**
 * [.createUser]
 */
/**
 * [.createRestrictedUser]
 */
/**
 * [.createRestrictedUser]
 */
/**
 * [.deleteUser]
 */
/**
 * [.deleteUser]
 */
/**
 * [.updateUser]
 */
/**
 * [.updateUser]
 */
/**
 * [.createCredentials]
 */
/**
 * [.createCredentials]
 */
/**
 * [.deleteCredentials]
 */
/**
 * [.deleteCredentials]
 */
/**
 * [.updateCredentials]
 */
/**
 * [.updateCredentials]
 */
