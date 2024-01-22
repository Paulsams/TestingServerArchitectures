plugins {
    id("java")
}

group = "org.paulsams"
version = "1.0"

subprojects {
    apply {
        plugin("java")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("com.google.protobuf:protobuf-java:3.25.1")
    }
}
