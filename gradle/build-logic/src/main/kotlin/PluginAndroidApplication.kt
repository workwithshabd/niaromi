import niaromi.gradle.extensions.alias
import niaromi.gradle.extensions.libs
import niaromi.gradle.extensions.niaromix
import niaromi.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidApplication : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.application)
            alias(niaromix.plugins.android.base)
        }
    }
}
