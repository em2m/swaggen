package io.em2m.swaggen

import com.fasterxml.jackson.databind.node.ObjectNode

data class Info(var title: String? = null, var description: String? = null, var version: String? = null)

data class Specification(var info: Info, var services: MutableList<Service> = mutableListOf(), val tags: MutableList<Map<String, Any>> = mutableListOf())

data class Service(var name: String, val actions: MutableList<Action>, val models: MutableList<Model>)

data class Action(var name: String?, var description: String = "", var consumes: String?, var produces: String?, var request: Request, var responses: Map<String, Response>)

data class Model(var name: String, var schema: ObjectNode)

data class Request(var schema: ObjectNode?, var model: String?, var headers: ObjectNode?)

data class Response(var name: String? = "", var description: String? = "", var schema: ObjectNode? = null, var model: String? = null, var headers: ObjectNode?)
