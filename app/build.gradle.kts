plugins {
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":kotlin-playground-app"))
    kover(project(":kotlin-playground-domain"))
    kover(project(":adapters:kotlin-playground-repositories"))
    kover(project(":adapters:kotlin-playground-http-services"))
}

kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}