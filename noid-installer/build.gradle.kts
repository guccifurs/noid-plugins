plugins {
    id("java")
}

group = "com.tonic.installer"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.tonic.installer.NoidInstaller",
            "Implementation-Title" to "Noid Installer",
            "Implementation-Version" to version
        )
    }
    
    archiveBaseName.set("NoidInstaller")
    archiveVersion.set("")
    
    // Make it a fat jar (include all dependencies if any)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
