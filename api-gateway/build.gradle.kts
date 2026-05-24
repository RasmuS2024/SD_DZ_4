plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.0"))

    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}