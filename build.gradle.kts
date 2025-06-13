plugins {
    java
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    `maven-publish`
}

group = "com.github.beothorn"
version = "8.0.0"
extra["springAiVersion"] = "1.0.0-RC1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()

    maven("https://repo.spring.io/milestone")
    maven("https://repo.spring.io/snapshot")
    maven {
        name = "Central Portal Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
}

dependencies {

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.telegram:telegrambots-longpolling:8.0.0")
    implementation("org.telegram:telegrambots-client:8.0.0")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")
    implementation("ai.fal.client:fal-client:0.7.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("com.github.beothorn.telegramAIConnector.Main")
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            groupId = "com.github.beothorn"
            artifactId = "telegramAIConnector"
            version = project.version.toString()

            pom {
                name.set("Telegram AI Connector")
                description.set("A connector for using AI with Telegram bots.")
                url.set("https://github.com/beothorn/telegramAIConnector")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("beothorn")
                        name.set("Beothorn")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/beothorn/telegramAIConnector.git")
                    developerConnection.set("scm:git:ssh://github.com/beothorn/telegramAIConnector.git")
                    url.set("https://github.com/beothorn/telegramAIConnector")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/beothorn/telegramAIConnector")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
