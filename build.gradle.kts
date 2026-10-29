plugins {
    id("java")
}

group = "gipf"
version = "1.0"

java {
    //force java21 during build
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")

    testCompileOnly("org.projectlombok:lombok:1.18.48")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.48")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/*
tasks.named<Jar>("jar") {
    archiveFileName.set("gipf.jar")
    manifest {
        attributes["Main-Class"] = "gipf.Entrypoint"
    }
}
 */

tasks.register<Jar>("serverJar") {
    description = "builds a jar that directly runs the server"
    //TODO: it would be nice to use proguard to remove client code

    from(sourceSets.main.get().output)
    archiveFileName.set("gipf-server.jar")

    manifest {
        attributes["Main-Class"] = "gipf.server.ServerMain"
    }
}

tasks.test {
    useJUnitPlatform()
}
