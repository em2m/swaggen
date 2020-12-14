package io.em2m.swaggen

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.node.ObjectNode

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class Info(
    var title: String? = null,
    var description: String = "",
    var version: String? = null,
    var profiles: List<String> = emptyList()
) {

    fun hasProfile(profiles: Set<String>): Boolean {
        var matches = false
        profiles.forEach {
            if (this.profiles.contains(it)) {
                matches = true
                return@forEach
            }
        }
        return matches
    }

}

data class Specification(
    var info: Info,
    var services: MutableList<Service> = mutableListOf(),
    val tags: MutableList<Map<String, Any>> = mutableListOf()
)

data class Service(
    var name: String,
    var info: Info = Info(),
    val actions: MutableList<Action>,
    val models: MutableList<Model>
)

data class Action(
    var name: String?,
    var description: String = "",
    var consumes: String?,
    var produces: String?,
    var request: Request,
    var responses: Map<String, Response> = emptyMap(),
    val response: Response? = null
)

data class Model(var name: String, var schema: ObjectNode)

data class Request(var schema: ObjectNode?, var model: String?, var headers: ObjectNode?)

data class Response(
    var name: String? = "",
    var description: String? = "",
    var schema: ObjectNode? = null,
    var model: String? = null,
    var headers: ObjectNode?
)
