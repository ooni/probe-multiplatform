plugins {
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("commonConfiguration") {
            id = "ooni.common"
            implementationClass = "ConfigurationPlugin"
        }
    }
}
