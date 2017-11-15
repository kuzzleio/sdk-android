package io.kuzzle.sdk.responses


import io.kuzzle.sdk.security.AbstractSecurityDocument
import io.kuzzle.sdk.util.Scroll

class SecurityDocumentList @JvmOverloads constructor(override val documents: List<AbstractSecurityDocument>, override val total: Long, val scroll: Scroll = Scroll()) : KuzzleList<AbstractSecurityDocument>
