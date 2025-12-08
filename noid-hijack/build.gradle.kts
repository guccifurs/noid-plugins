plugins {
    id("java")
}

group = "com.tonic.noid"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
    }
    mavenCentral()
}

dependencies {
    // Include VitaLite API classes
    implementation(project(":api"))
    implementation(project(":base-api"))
    
    // RuneLite dependencies (compile only - provided at runtime)
    compileOnly("net.runelite:client:latest.release")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.tonic.noid.NoidHijack",
            "Implementation-Title" to "Noid Hijack",
            "Implementation-Version" to version
        )
    }
    
    archiveBaseName.set("NoidHijack")
    archiveVersion.set("")
    
    // Create a fat JAR with VitaLite API bundled
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
