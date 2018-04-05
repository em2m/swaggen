package io.em2m.swaggen

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import java.io.File
import java.io.FileWriter


class SpecBuilder(
        private val sourceDir: File,
        private val outputDir: File,
        private val version: String,
        private val services: Set<String> = emptySet(),
        private val profiles: Set<String> = emptySet()) {

    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule().enable(SerializationFeature.INDENT_OUTPUT)

    private fun writeSwagger(spec: Specification, genRoot: File, name: String) {
        val ymlWriter = FileWriter(File(genRoot, "$name.yml"))
        val jsonWriter = FileWriter(File(genRoot, "$name.json"))
        val swagger = SwaggerFormatter().toSwagger(spec)
        mapper.writeValue(jsonWriter, swagger)
        YAMLFactory(mapper).createGenerator(ymlWriter).writeTree(swagger)
        ymlWriter.close()
        jsonWriter.close()
    }

    private fun writeSpec(spec: Specification, genRoot: File, name: String) {
        val ymlWriter = FileWriter(File(genRoot, "$name.yml"))
        val jsonWriter = FileWriter(File(genRoot, "$name.json"))
        mapper.writeValue(jsonWriter, spec)
        YAMLFactory(mapper).createGenerator(ymlWriter).writeTree(mapper.valueToTree(spec))
        ymlWriter.close()
        jsonWriter.close()
    }

    fun build() {

        println("sourceDir: $sourceDir")
        println("outputDir: $outputDir")
        println("version: $version")

        outputDir.mkdirs()

        val loader = ServiceLoader()

        val info = loader.loadInfo(sourceDir)
        info.version = version

        require(sourceDir.exists(), { "Source directory is missing" })

        val files = sourceDir.listFiles().filter { it.isDirectory }
        val spec = files.map { loader.loadSpec(it) }
                .fold(Specification(info, mutableListOf())) { total, next ->
                    next.services
                            .filter { services.isEmpty() || services.contains(it.name) }
                            .forEach { service ->
                                total.services.add(service)
                                total.tags.add(mapOf("name" to service.name))
                            }
                    total
                }
                .let { loader.filterProfiles(it, profiles) }
                .let { loader.filterServices(it, services) }

        println("Loaded spec")

        writeSwagger(spec, outputDir, "swagger_docs")
        loader.relocateActions(spec)
        writeSwagger(spec, outputDir, "swagger_code")
        writeSpec(spec, outputDir, "specification")
    }

}

fun main(args: Array<String>) {

    val options = Options()

    val profilesOpt = Option.builder("p").longOpt("profiles")
            .desc("The comma separated list of profiles to include in the specification.")
            .valueSeparator(',').build()

    val servicesOpt = Option.builder("S").longOpt("services")
            .desc("The comma separated list of services to include in the specification.")
            .valueSeparator(',').build()

    options.addOption("s", "sourceDir", true, "The source directory for service specifications")
    options.addOption("t", "outputDir", true, "The target directory for the generated swagger files")
    options.addOption("v", "version", true, "The version information for the generated swagger specification")
    options.addOption(profilesOpt)
    options.addOption(servicesOpt)

    val cmd = DefaultParser().parse(options, args)

    val sourceDir = File(cmd.getOptionValue("sourceDir").trim())
    val outputDir = File(cmd.getOptionValue("outputDir").trim())
    val version = cmd.getOptionValue("version").trim()
    val services = cmd.getOptionValues("profiles").toSet()
    val profiles = cmd.getOptionValues("profiles").toSet()

    SpecBuilder(sourceDir, outputDir, version, services, profiles).build()
}
