rootProject.name = "drpc4k"
//include("drpc4k-pure")
enableFeaturePreview("STABLE_PUBLISHING")

val kotlin_version: String by settings
val undercouch_dl_version: String by settings
val bintray_version: String by settings
val artifactory_version: String by settings

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            println("Requested: " + requested.id.id)
            when (requested.id.id) {
                "de.undercouch.download" -> useVersion(undercouch_dl_version)
                "com.jfrog.bintray" -> useVersion(bintray_version)
                "com.jfrog.artifactory" -> useVersion(artifactory_version)
                else -> when (requested.id.namespace) {
                    "org.jetbrains.kotlin" -> useVersion(kotlin_version)
                }
            }
            if (requested.id.id == "kotlin2js") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
        }
    }
}