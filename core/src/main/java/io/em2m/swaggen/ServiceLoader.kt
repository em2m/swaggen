package io.em2m.swaggen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

fun nameFromFile(file: File): String {
    val name = file.name
    return name.substring(0, name.lastIndexOf('.'))
}

class ServiceLoader() {

    val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    fun loadSpec(root: File): Specification {
        val info: Info = loadInfo(File(root, "info.yml"))
        val spec: Specification = Specification(info)

        spec.services.add(loadService(root))
        return spec
    }

    fun loadInfo(root: File): Info {
        val file = File(root, "info.yml")
        if (!file.exists()) return Info()
        return try {
            mapper.readValue(file)
        } catch (e: Exception) {
            println("Error loading info block (${file.name}): ${e.message}")
            throw e
        }
    }

    fun loadService(src: File): Service {

        val service = Service(src.name, Info(), mutableListOf(), mutableListOf())

        val actions = File(src, "actions")
        val models = File(src, "models")
        val infoYml = File(src, "info.yml")
        val infoJson = File(src, "info.json")

        if (infoYml.isFile) {
            service.info = loadInfo(infoYml)
        } else if (infoJson.isFile) {
            service.info = loadInfo(infoJson)
        }

        if (actions.isDirectory) {
            actions.listFiles().forEach {
                service.actions.add(try {
                    loadAction(it)
                } catch (e: Exception) {
                    println("Error processing action (${it.name}): ${e.message}")
                    throw e
                })
            }
        }

        if (models.isDirectory) {
            models.listFiles().forEach { file ->
                val model = try {
                    loadModel(file)
                } catch (e: Exception) {
                    println("Error loading model (${file.name}): ${e.message}")
                    throw e
                }
                service.models.add(model)
            }
        }
        return service
    }

    fun loadAction(src: File): Action {
        val result: Action = mapper.readValue(src)
        if (result.name == null) {
            result.name = nameFromFile(src)
        }
        if (result.response != null) {
            result.responses += "200" to result.response
        }
        val okay = result.responses["200"]
        if (okay != null && okay.name.isNullOrEmpty()) {
            okay.name = "${result.name}Result"
        }
        return result
    }

    fun loadModel(src: File): Model {
        val schema: ObjectNode = mapper.readValue(src)
        val name = nameFromFile(src)
        return Model(name, schema)
    }

    fun filterProfiles(spec: Specification, profiles: Set<String>): Specification {
        return if (profiles.isNotEmpty()) {
            val services = spec.services.filter { it.info.hasProfile(profiles) }.map { service ->
                val info = service.info
                val actions = service.actions
                val models = service.models
                Service(service.name, info, actions, models)
            }.toMutableList()
            Specification(spec.info, services, spec.tags)
        } else spec
    }

    fun filterServices(spec: Specification, services: Set<String>): Specification {
        return if (services.isNotEmpty()) {
            val services = spec.services.filter { services.contains(it.name) }.map { service ->
                val info = service.info
                val actions = service.actions
                val models = service.models
                Service(service.name, info, actions, models)
            }.toMutableList()
            Specification(spec.info, services, spec.tags)
        } else spec
    }

    fun relocateActions(spec: Specification) {
        spec.services.forEach { service ->
            service.actions.forEach { action ->
                for (response in action.responses.values) {
                    val name = response.name
                    val schema = response.schema
                    if (name != null && schema != null) {
                        val model = Model(name, schema)
                        response.schema = null
                        response.model = model.name
                        service.models.add(model)
                    }
                }
            }
        }
    }
}
