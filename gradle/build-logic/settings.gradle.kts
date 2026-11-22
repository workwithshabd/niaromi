dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
        create("niaromix") {
            from(files("../niaromi.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
