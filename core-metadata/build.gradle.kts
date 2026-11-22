plugins {
    alias(niaromix.plugins.android.library)
    alias(niaromix.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "niaromi.core.metadata"
}

dependencies {
    implementation(projects.sourceApi)

    implementation(libs.bundles.serialization)
}
