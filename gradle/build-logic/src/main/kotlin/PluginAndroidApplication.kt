import niaromi.gradle.extensions.alias
import niaromi.gradle.extensions.libs
import niaromi.gradle.extensions.mihonx
import niaromi.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidApplication : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.application)
            alias(mihonx.plugins.android.base)
        }
    }
}
