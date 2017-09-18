package io.em2m.swaggen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class SwaggerFormatter {

    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule().enable(SerializationFeature.INDENT_OUTPUT)

    fun toSwagger(spec: Specification): ObjectNode {
        val result = mapper.createObjectNode()
        result.put("swagger", "2.0")
        result.putPOJO("tags", spec.tags)
        result.set("info", toSwagger(spec.info))
        val paths = result.with("paths")
        val definitions = result.with("definitions")
        spec.services.forEach {
            service ->
            service.actions.forEach {
                action ->
                val path = "/" + service.name + "/actions/" + action.name
                paths.set(path, toSwagger(service, action))
            }
            service.models.forEach {
                model ->
                definitions.set(model.name, toSwagger(model))
            }
        }
        return result
    }

    fun toSwagger(info: Info): ObjectNode {
        return mapper.valueToTree(info)
    }

    fun toSwagger(model: Model): ObjectNode {
        return model.schema
    }

    fun toSwagger(service: Service, action: Action): ObjectNode {
        val result = mapper.createObjectNode()
        val post = result.with("post")
        post.withArray("tags").add(service.name)
        post.put("description", action.description)
        post.put("operationId", action.name)
        post.put("summary", action.name)
        post.withArray("consumes").add("application/json")
        post.withArray("produces").add("application/json")

        // Request Parameters
        val parameters = post.withArray("parameters")
        val body = mapper.createObjectNode()
        body.put("in", "body")
        body.put("name", action.name + "Request")
        body.put("required", true)
        if (action.request.schema != null) {
            body.set("schema", action.request.schema)
        }
        if (action.request.model != null) {
            body.with("schema").put("\$ref", "#/definitions/" + action.request.model)
        }
        parameters.add(body)

        // Response Parameters
        val responses = post.with("responses")
        for ((key, response) in action.responses) {
            responses.set(key, toSwagger(response))
        }
        return result
    }

    fun toSwagger(response: Response): ObjectNode {
        val result = mapper.createObjectNode()
        result.put("description", response.description)
        if (response.name != null) {
            result.put("name", response.name)
        }
        if (response.schema != null) {
            result.set("schema", response.schema)
        }
        if (response.model != null) {
            println("response.model.name: ${response.model}")
            result.with("schema").put("\$ref", "#/definitions/" + response.model)
        }
        return result
    }

}