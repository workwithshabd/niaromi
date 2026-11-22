plugins {
    alias(niaromix.plugins.android.library)
    alias(niaromix.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "niaromi.core.archive"
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.archive)
    implementation(libs.unifile)
}
