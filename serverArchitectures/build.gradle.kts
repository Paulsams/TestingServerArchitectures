java {
    sourceSets {
        main {
            java.setSrcDirs(listOf("src/main"))
        }
    }
}

allprojects {
    dependencies {
        implementation(project(":messages"))
        implementation(project(":overallConfiguration"))
    }
}

subprojects {
    dependencies {
        implementation(project(":serverArchitectures"))
    }
}
