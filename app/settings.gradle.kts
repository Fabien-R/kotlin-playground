rootProject.name = "kotlin-playground"

include(
    "app",
    "kotlin-playground-domain",
    "adapters:kotlin-playground-repositories",
    "adapters:kotlin-playground-http-services",
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}
