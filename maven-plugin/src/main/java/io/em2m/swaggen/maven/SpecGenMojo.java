package io.em2m.swaggen.maven;

import io.em2m.swaggen.SpecBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Generate swagger files for your specifications
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class SpecGenMojo extends AbstractMojo {

    @Parameter(name = "verbose", required = false, defaultValue = "false")
    private boolean verbose;

    /**
     * Location of the output directory.
     */
    @Parameter(name = "outputDir", defaultValue = "${project.build.directory}/classes")
    private File outputDir;

    /**
     * The directory which contains scala/java source files
     */
    @Parameter(name = "sourceDir", property = "project.build.sourceDirectory")
    protected File sourceDir;


    @Parameter(property = "project.version", required = true)
    private String version;

    public void execute() throws MojoExecutionException {
        File specDir = new File(sourceDir.getParentFile(), "spec");
        new SpecBuilder(specDir, outputDir, version).build();
    }

}
