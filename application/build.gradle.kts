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
    implementation(project(":messages"))

    implementation(project(":serverArchitectures"))
    implementation(project(":serverArchitectures:blocking"))
    implementation(project(":serverArchitectures:non-blocking"))
    implementation(project(":serverArchitectures:asynchronous"))

    implementation(project(":client"))

    implementation("com.diogonunes:JColor:5.5.1")
    implementation("org.jfree:jfreechart:1.5.4")
}

application {
    mainClass = "charts.EntryPoint"
}
