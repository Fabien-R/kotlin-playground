rootProject.name = "kotlin-playground"

include(
    "kotlin-playground-domain",
    "adapaters:kotlin-playground-repositories",
    "app",
)

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}
