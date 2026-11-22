plugins {
    alias(niaromix.plugins.android.library)
    alias(niaromix.plugins.compose)

    alias(niaromix.plugins.spotless)

    alias(libs.plugins.valkyrie)
}

android {
    namespace = "niaromi.icons.simpleicons"
}

spotless {
    format("svg") {
        target("src/**/*.svg")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

valkyrie {
    packageName = "niaromi.icons.simpleicons"
    generateAtSync = true

    imageVector {
        suppressUnusedReceiverWarning = true
    }

    iconPack {
        name = "SimpleIcons"
        targetSourceSet = "main"
    }
}

dependencies {
    api(libs.androidx.compose.ui)
}
