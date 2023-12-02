plugins {
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":app"))
    kover(project(":kotlin-playground-domain"))
    kover(project(":adapters:kotlin-playground-repositories"))
    kover(project(":adapters:kotlin-playground-http-services"))
}

koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}