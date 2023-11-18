rootProject.name = "kotlin-playground"

include(
    "app",
    "kotlin-playground-domain",
    "adapters:kotlin-playground-repositories",
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}
