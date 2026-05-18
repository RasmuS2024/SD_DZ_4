plugins {
    java
    id("org.springframework.boot") version "3.2.5" apply false
}

allprojects {
    group = "sysdesign"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}