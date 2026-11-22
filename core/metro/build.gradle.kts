plugins {
    alias(niaromix.plugins.android.library)
    alias(niaromix.plugins.spotless)

    alias(libs.plugins.metro)
}

android {
    namespace = "niaromi.core.metro"
}

dependencies {
    implementation(libs.metro.runtime)
}
