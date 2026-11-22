plugins {
    alias(niaromix.plugins.android.library)
    alias(niaromix.plugins.compose)

    alias(niaromix.plugins.spotless)
    alias(libs.plugins.metro)
}

android {
    namespace = "niaromi.presentation.widget"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.metro)
    implementation(projects.domain)
    implementation(projects.presentationCore)
    implementation(projects.i18n)

    implementation(libs.androidx.glance.appWidget)
    implementation(libs.material)

    implementation(libs.kotlinx.datetime)

    implementation(libs.coil.core)

    implementation(libs.metro.runtime)
}
