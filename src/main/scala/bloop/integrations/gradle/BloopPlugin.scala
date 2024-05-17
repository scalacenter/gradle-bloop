package bloop.integrations.gradle

import scala.collection.JavaConverters._

import bloop.integrations.gradle.syntax._
import bloop.integrations.gradle.tasks.BloopInstallTask
import bloop.integrations.gradle.tasks.ConfigureBloopInstallTask
import bloop.integrations.gradle.tasks.PluginUtils

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet

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

    project.afterEvaluate(
      new org.gradle.api.Action[Project] {
        def execute(project: Project): Unit = {

          val bloopParams = project.getExtension[BloopParametersExtension].createParameters

          if (PluginUtils.hasJavaScalaPlugin(project)) {
            project.allSourceSets.foreach { sourceSet =>
              val bloopConfigName = generateBloopConfigName(sourceSet)
              val bloopConfig = createBloopConfigForSourceSet(bloopConfigName, project)
              val compatibleConfigNames =
                findCompatibleConfigNamesFromSourceSet(sourceSet) ++
                  bloopParams.extendUserConfigurations

              extendCompatibleConfigurationAfterEvaluate(
                project,
                bloopConfig,
                compatibleConfigNames,
                Set.empty
              )
            }
          }

          if (PluginUtils.hasAndroidPlugin(project)) {
            // In the Android world, we don't have source sets for each variant, so instead
            // we create an Android-specific configuration that we can use to resolve all
            // relevant artifacts in all variants (the empty extend configs)
            val bloopAndroidConfig = createBloopConfigForSourceSet("bloopAndroidConfig", project)

            extendCompatibleConfigurationAfterEvaluate(
              project,
              bloopAndroidConfig,
              Set.empty,
              incompatibleAndroidConfigurations
            )
          }
        }
      }
    )

    // Creates two tasks: one to configure the plugin and the other one to generate the config files
    val configureBloopInstall =
      project.createTask[ConfigureBloopInstallTask]("configureBloopInstall")
    val bloopInstall = project.createTask[BloopInstallTask]("bloopInstall")
    configureBloopInstall.installTask = Some(bloopInstall)
    bloopInstall.dependsOn(configureBloopInstall)
    ()
  }

  private def findCompatibleConfigNamesFromSourceSet(sourceSet: SourceSet): Set[String] = Set(
    sourceSet.getApiConfigurationName(),
    sourceSet.getImplementationConfigurationName(),
    sourceSet.getCompileOnlyConfigurationName(),
    // sourceSet.getCompileOnlyApiConfigurationName(),
    sourceSet.getCompileClasspathConfigurationName(),
    sourceSet.getRuntimeOnlyConfigurationName(),
    sourceSet.getRuntimeClasspathConfigurationName(),
    sourceSet.getRuntimeElementsConfigurationName(),
    "scalaCompilerPlugins"
  )

  private[this] val incompatibleAndroidConfigurations = Set[String](
    "incrementalScalaAnalysisElements",
    "incrementalScalaAnalysisFormain",
    "incrementalScalaAnalysisFortest",
    "incrementalScalaAnalysisForintegTest",
    "incrementalScalaAnalysisForjmh",
    "zinc"
  )

  private def createBloopConfigForSourceSet(
      bloopConfigName: String,
      project: Project
  ): Configuration = {
    val bloopConfig = project.getConfigurations().create(bloopConfigName)
    bloopConfig.setDescription(
      "A configuration for Bloop to be able to export artifacts in all other configurations."
    )

    // Make this configuration not visbile in dependencyInsight reports
    bloopConfig.setVisible(false)
    // Allow this configuration to be resolved
    bloopConfig.setCanBeResolved(true)
    // This configuration is not meant to be consumed by other projects
    bloopConfig.setCanBeConsumed(false)

    bloopConfig
  }

  /**
   * Makes the input configuration extend valid compatible configurations.
   * Note that if extendConfigNames is empty, all resolvable and non-whitelisted
   * configurations will be extended automatically.
   */
  private def extendCompatibleConfigurationAfterEvaluate(
      project: Project,
      bloopSourceSetConfig: Configuration,
      compatibleConfigNames: Set[String],
      incompatibleConfigNames: Set[String]
  ): Unit = {
    // Use consumer instead of Scala closure because of Scala 2.11 compat
    val extendConfigurationIfCompatible = new java.util.function.Consumer[Configuration] {
      def accept(config: Configuration): Unit = {
        if (
          config != bloopSourceSetConfig &&
          config.isCanBeResolved &&
          (compatibleConfigNames.isEmpty || compatibleConfigNames.contains(config.getName())) &&
          !incompatibleConfigNames.contains(config.getName())
        ) {
          bloopSourceSetConfig.extendsFrom(config)
        }
      }
    }

    project.getConfigurations.forEach(extendConfigurationIfCompatible)
  }
}
