java {
    sourceSets {
        main {
            java.setSrcDirs(listOf("src/main"))
        }
    }
}

dependencies {
    implementation(project(":messages"))
    implementation(project(":overallConfiguration"))
}
