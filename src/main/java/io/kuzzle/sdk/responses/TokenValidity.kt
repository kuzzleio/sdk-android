package io.kuzzle.sdk.responses

import java.util.Date

class TokenValidity {
    var isValid: Boolean = false
    var state: String? = null
    var expiresAt: Date? = null
}
