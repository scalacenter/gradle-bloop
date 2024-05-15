package bloop.integrations.gradle

import bloop.integrations.gradle.syntax._
import bloop.integrations.gradle.tasks.BloopInstallTask
import bloop.integrations.gradle.tasks.ConfigureBloopInstallTask

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Main entry point of the gradle bloop plugin.
 *
 * The bloop plugin defines two tasks:
 *
 * 1. `configureBloopInstall`: responsible to set up the environment and
 * force artifact resolution.
 * 2. `bloopInstall`: responsible of generating the bloop config files
 * from the configured data.
 *
 * The second task depends on the first one so that this data dependency is
 * always met.
 */
final class BloopPlugin extends Plugin[Project] {
  override def apply(project: Project): Unit = {
    project.getLogger.info(
      s"Applying bloop plugin to project ${project.getName}",
      Seq.empty: _*
    )
    project.createExtension[BloopParametersExtension]("bloop", project)

    val bloopConfig = project.getConfigurations().create("bloopConfig")
    bloopConfig.setDescription(
      "A configuration for Bloop to be able to export artifacts in all other configurations."
    )

    // Make this configuration not visbile in dependencyInsight reports
    bloopConfig.setVisible(false)
    // Allow this configuration to be resolved
    bloopConfig.setCanBeResolved(true)
    // This configuration is not meant to be consumed by other projects
    bloopConfig.setCanBeConsumed(false)

    extendCompatibleConfigurationAfterEvaluate(project, bloopConfig)

    // Creates two tasks: one to configure the plugin and the other one to generate the config files
    val configureBloopInstall =
      project.createTask[ConfigureBloopInstallTask]("configureBloopInstall")
    val bloopInstall = project.createTask[BloopInstallTask]("bloopInstall")
    configureBloopInstall.installTask = Some(bloopInstall)
    bloopInstall.dependsOn(configureBloopInstall)
    ()
  }

  private[this] val incompatibleConfigurations = Set[String](
    "incrementalScalaAnalysisElements",
    "incrementalScalaAnalysisFormain",
    "incrementalScalaAnalysisFortest",
    "incrementalScalaAnalysisForintegTest",
    "incrementalScalaAnalysisForjmh",
    "zinc"
  )

  def extendCompatibleConfigurationAfterEvaluate(
      project: Project,
      bloopConfig: Configuration
  ): Unit = {
    // Use consumer instead of Scala closure because of Scala 2.11 compat
    val extendConfigurationIfCompatible = new java.util.function.Consumer[Configuration] {
      def accept(config: Configuration): Unit = {
        if (
          config != bloopConfig && config.isCanBeResolved &&
          !incompatibleConfigurations.contains(config.getName())
        ) {
          bloopConfig.extendsFrom(config)
        }
      }
    }

    // Important to do this after evaluation so Gradle can add all the necessary project configuration before we extend them
    project.afterEvaluate(
      // Use Action instead of Scala closure because of Scala 2.11 compat
      new org.gradle.api.Action[Project] {
        def execute(project: Project): Unit = {
          project.getConfigurations.forEach(extendConfigurationIfCompatible)
        }
      }
    )
  }
}
