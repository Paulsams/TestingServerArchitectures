plugins {
    application
}

java {
    sourceSets {
        main {
            java.setSrcDirs(listOf("src/main"))
        }
    }
}

dependencies {
    implementation(project(":overallConfiguration"))
    implementation(project(":serverArchitectures"))

    implementation("org.jfree:jfreechart:1.5.4")
}

application {
    mainClass = "CombinerEntryPoint"
}
