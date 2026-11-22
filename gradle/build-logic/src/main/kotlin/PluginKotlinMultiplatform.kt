import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import niaromi.gradle.configurations.configureKotlin
import niaromi.gradle.extensions.alias
import niaromi.gradle.extensions.configureTest
import niaromi.gradle.extensions.coreLibraryDesugaring
import niaromi.gradle.extensions.libs
import niaromi.gradle.extensions.niaromix
import niaromi.gradle.extensions.plugins
import niaromi.gradle.extensions.release
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("UNUSED")
class PluginKotlinMultiplatform : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.kmp.library)
            alias(libs.plugins.kotlin.multiplatform)
        }

        configureKotlin()
        configureTest()

        kotlin {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            applyDefaultHierarchyTemplate()

            android {
                minSdk = niaromix.versions.android.sdk.min.get().toInt()
                compileSdk {
                    version = release(niaromix.versions.android.sdk.compile)
                }
                enableCoreLibraryDesugaring = true
            }
        }

        dependencies {
            coreLibraryDesugaring(libs.android.desugar)
        }
    }
}

private fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(block)
}

private fun KotlinMultiplatformExtension.android(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach(block)
}
