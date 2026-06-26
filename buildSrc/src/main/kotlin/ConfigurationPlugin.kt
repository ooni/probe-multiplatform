import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Common configuration plugin for projects.
 */
class ConfigurationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            // Common configuration that applies to all projects
            group = "org.ooni.probe"
            version = project.findProperty("version") ?: "1.0.0"

            val organization = project.findProperty("organization") as? String
            val config = Organization.fromKey(organization).config

            // The desktop packaging shell (:desktopApp) owns compose.desktop and the
            // packaging post-processing; everything else (shared library + desktop
            // resources/native/branding) is configured on :composeApp.
            registerDesktopAppTasks()
            registerTasks(config)
            configureTasks()
        }
    }
}
