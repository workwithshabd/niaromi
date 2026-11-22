plugins {
    alias(niaromix.plugins.android.library)
    alias(niaromix.plugins.compose)

    alias(niaromix.plugins.spotless)

    alias(libs.plugins.valkyrie)
}

android {
    namespace = "niaromi.icons.custommaterialsymbols"
}

spotless {
    format("svg") {
        target("src/**/*.svg")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

valkyrie {
    packageName = "niaromi.icons.custommaterialsymbols"
    generateAtSync = true

    imageVector {
        suppressUnusedReceiverWarning = true
    }

    iconPack {
        name = "CustomMaterialSymbols"
        targetSourceSet = "main"

        nested {
            name = "Rounded"
            sourceFolder = "rounded"
            autoMirror = false
        }
    }
}

dependencies {
    api(libs.androidx.compose.ui)
}
