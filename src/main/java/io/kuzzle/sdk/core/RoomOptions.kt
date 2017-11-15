package io.kuzzle.sdk.core

import org.json.JSONObject

import io.kuzzle.sdk.enums.Scope
import io.kuzzle.sdk.enums.State
import io.kuzzle.sdk.enums.Users

class RoomOptions {

    private var subscribeToSelf = true
    private var _volatile = JSONObject()
    private var scope = Scope.ALL
    private var state = State.DONE
    private var users = Users.NONE

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
     * @return  this
     */
    fun setSubscribeToSelf(subscribeToSelf: Boolean): RoomOptions {
        this.subscribeToSelf = subscribeToSelf

        return this
    }

    /**
     * volatile property getter
     * @return volatile property value
     */
    fun getVolatile(): JSONObject {
        return _volatile
    }

    /**
     * volatile property setter
     * @param  _volatile New volatile value
     * @return this
     */
    fun setVolatile(_volatile: JSONObject): RoomOptions {
        this._volatile = _volatile

        return this
    }

    /**
     * scope property getter
     * @return scope property value
     */
    fun getScope(): Scope {
        return scope
    }

    /**
     * scope property setter
     * @param  scope New scope value
     * @return this
     */
    fun setScope(scope: Scope): RoomOptions {
        this.scope = scope

        return this
    }

    /**
     * state property getter
     * @return state property value
     */
    fun getState(): State {
        return state
    }

    /**
     * state property setter
     * @param  state New state value
     * @return this
     */
    fun setState(state: State): RoomOptions {
        this.state = state

        return this
    }

    /**
     * users property getter
     * @return users property value
     */
    fun getUsers(): Users {
        return users
    }

    /**
     * users property setter
     * @param  users New users value
     * @return this
     */
    fun setUsers(users: Users): RoomOptions {
        this.users = users

        return this
    }

}
