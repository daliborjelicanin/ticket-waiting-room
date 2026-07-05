plugins {
    id("java")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":common"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}"))
    implementation(libs.spring.boot.starter.web)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.bundles.testing)
}
