plugins {
    java
    id("org.springframework.boot") version "3.5.13" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("checkstyle")
    id("jacoco")
}

allprojects {
    group = "cosmoscan"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    apply(plugin = "checkstyle")

    configure<org.gradle.api.plugins.quality.CheckstyleExtension> {
        toolVersion = "13.4.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
        maxWarnings = 0
    }

    tasks.withType<Checkstyle>().configureEach {
        reports {
            xml.required.set(false)
            html.required.set(true)
        }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        implementation("org.springframework.boot:spring-boot-starter-actuator")

        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("org.springframework.boot:spring-boot-starter-test")

    }

    tasks.test {
        useJUnitPlatform()
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required = true
            html.required = true
        }
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.named("test") })

    subprojects.forEach { sub ->
        sub.plugins.withId("java") {
            sourceDirectories.from(sub.sourceSets.main.get().allSource.srcDirs)
            classDirectories.from(sub.sourceSets.main.get().output)
            executionData.from(sub.fileTree("build/jacoco") { include("*.exec") })
        }
    }

    reports {
        xml.required = true
        html.required = true
    }
}
