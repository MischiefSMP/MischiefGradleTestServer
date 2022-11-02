import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.mischiefsmp"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20220924")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

gradlePlugin {
    plugins {
        create(project.name) {
            id = project.name
            implementationClass = "com.mischiefsmp.mgts.GradlePlugin"
        }
    }
}