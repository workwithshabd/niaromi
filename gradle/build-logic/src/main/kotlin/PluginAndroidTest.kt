import niaromi.gradle.extensions.alias
import niaromi.gradle.extensions.libs
import niaromi.gradle.extensions.niaromix
import niaromi.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidTest : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.test)
            alias(niaromix.plugins.android.base)
        }
    }
}
