plugins {
    alias(niaromix.plugins.android.library)
    alias(niaromix.plugins.compose)
    alias(niaromix.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "niaromi.core.navigation"
}

dependencies {
    implementation(projects.presentationCore)
    implementation(libs.bundles.androidx.navigation3)

    api(libs.composeMaterialMotion)
    api(libs.androidx.compose.ui)
}
