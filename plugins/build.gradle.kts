plugins {
    id("java")
}

// Build a standalone jar containing only the Drop Party plugin for sideloading
val dropPartyJar = tasks.register<Jar>("dropPartyJar") {
    group = "build"
    description = "Packages only the Drop Party plugin for sideloading."
    archiveBaseName.set("dropparty")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/dropparty/**")
    }

    // Include resources (icons etc. if present)
    from("src/main/resources") {
        include("com/tonic/plugins/dropparty/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Drop Party jar
    from("src/main/resources") {
        include("dropparty-runelite-plugin.properties")
        rename("dropparty-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "dropparty",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Drop Party",
            "Plugin-Description" to "Track drop party paths and auto-loot items."
        )
    }
}

// Build a standalone jar containing only the Bank Seller plugin for sideloading
val bankSellerJar = tasks.register<Jar>("bankSellerJar") {
    group = "build"
    description = "Packages only the Bank Seller plugin for sideloading."
    archiveBaseName.set("bankseller")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/bankseller/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Bank Seller jar
    from("src/main/resources") {
        include("bankseller-runelite-plugin.properties")
        rename("bankseller-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "bankseller",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Bank Seller",
            "Plugin-Description" to "Withdraws tradeable items from bank and sells at GE."
        )
    }
}

group = "com.tonic.plugins"
version = "1.11.19.1"

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.runelite.net")
        content {
            includeGroupByRegex("net\\.runelite.*")
        }
    }
    mavenCentral()
}

val apiVersion = "latest.release"

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":base-api"))
    compileOnly("net.runelite:client:$apiVersion")
    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.jboss.aerogear:aerogear-otp-java:1.0.0")
    implementation(group = "com.fifesoft", name = "rsyntaxtextarea", version = "3.1.2")
    implementation(group = "com.fifesoft", name = "autocomplete", version = "3.1.1")
    compileOnly("com.github.javaparser:javaparser-symbol-solver-core:3.25.5")
    implementation("com.google.code.gson:gson:2.9.0")
}

tasks.test {
    useJUnitPlatform()
}

// Temporarily exclude broken plugins from compilation
tasks.named<JavaCompile>("compileJava") {
    // Only compile GearSwapper to avoid errors in other broken plugins
    include("com/tonic/plugins/gearswapper/**")
}

// Build a standalone jar containing only the Noid Auth plugin for sideloading
val noidJar = tasks.register<Jar>("noidJar") {
    group = "build"
    description = "Packages only the Noid Auth plugin for sideloading."
    archiveBaseName.set("noid")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/noid/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Noid jar
    from("src/main/resources") {
        include("noid-runelite-plugin.properties")
        rename("noid-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "noid",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Noid",
            "Plugin-Description" to "Authentication & Auto-Update for Noid plugins."
        )
    }
}

// Build a standalone jar containing only the Noid Bets plugin for sideloading
val noidBetsJar = tasks.register<Jar>("noidBetsJar") {
    group = "build"
    description = "Packages only the Noid Bets plugin for sideloading."
    archiveBaseName.set("noidbets")

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/noidbets/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Noid Bets jar
    from("src/main/resources") {
        include("noidbets-runelite-plugin.properties")
        rename("noidbets-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Main-Class" to "com.tonic.plugins.noidbets.NoidBetsPlugin",
            "Plugin-Version" to version,
            "Plugin-Id" to "noidbets",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Noid Bets",
            "Plugin-Description" to "Automated duel arena reporting and Discord betting integration."
        )
    }
}

// Build a standalone jar containing only the Auto Login plugin for sideloading
val autoLoginJar = tasks.register<Jar>("autoLoginJar") {
    group = "build"
    description = "Packages only the Auto Login plugin for sideloading."
    archiveBaseName.set("autologin")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/autologin/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Auto Login jar
    from("src/main/resources") {
        include("autologin-runelite-plugin.properties")
        rename("autologin-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "autologin",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Auto Login",
            "Plugin-Description" to "Automatically logs in with configured username and password when on the login screen."
        )
    }
}

// Build a standalone jar containing only the Hello Webhook plugin for sideloading
val helloWebhookJar = tasks.register<Jar>("helloWebhookJar") {
    group = "build"
    description = "Packages only the Hello Webhook plugin for sideloading."
    archiveBaseName.set("hellowebhook")

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/hellowebhook/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Hello Webhook jar
    from("src/main/resources") {
        include("hellowebhook-runelite-plugin.properties")
        rename("hellowebhook-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "hellowebhook",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Hello Webhook",
            "Plugin-Description" to "On first startup, send the contents of ~/.runelite/hello.txt to a Discord webhook."
        )
    }
}

// Build a standalone jar containing only the Gear Swapper plugin for sideloading
val gearSwapperJar = tasks.register<Jar>("gearSwapperJar") {
    group = "build"
    description = "Packages only the Gear Swapper plugin for sideloading."
    archiveBaseName.set("gearswapper")
    archiveVersion.set("1.5.33")

    // Allow duplicate entries (resources may be in both output and resources dir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/gearswapper/**")
    }

    // Include resources (prayer icons, etc.)
    from("src/main/resources") {
        include("com/tonic/plugins/gearswapper/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Gear Swapper jar
    from("src/main/resources") {
        include("gearswapper-runelite-plugin.properties")
        rename("gearswapper-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to "1.5.33",
            "Plugin-Id" to "gearswapper",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Gear Swapper",
            "Plugin-Description" to "Swap gear loadouts with hotkeys for PvP."
        )
    }
}

// Build a standalone jar containing only the Target Lockon plugin for sideloading
val targetLockonJar = tasks.register<Jar>("targetLockonJar") {
    group = "build"
    description = "Packages only the Target Lockon plugin for sideloading."
    archiveBaseName.set("targetlockon")

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/targetlockon/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Target Lockon jar
    from("src/main/resources") {
        include("targetlockon-runelite-plugin.properties")
        rename("targetlockon-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "targetlockon",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Target Lockon",
            "Plugin-Description" to "Lock camera onto target players with right-click option."
        )
    }
}

// Build a standalone jar containing only the Oculus Spectator plugin for sideloading
val oculusSpectatorJar = tasks.register<Jar>("oculusSpectatorJar") {
    group = "build"
    description = "Packages only the Oculus Spectator plugin for sideloading."
    archiveBaseName.set("oculusspectator")

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/oculusspectator/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Oculus Spectator jar
    from("src/main/resources") {
        include("oculusspectator-runelite-plugin.properties")
        rename("oculusspectator-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "oculusspectator_vita",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Oculus Spectator (Vita)",
            "Plugin-Description" to "Use the Oculus Orb to spectate and follow other players (Vita sideload)."
        )
    }
}

// Build a standalone jar containing only the Attack Timer plugin for sideloading
val attackTimerJar = tasks.register<Jar>("attackTimerJar") {
    group = "build"
    description = "Packages only the Attack Timer plugin for sideloading."
    archiveBaseName.set("attacktimer")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/attacktimer/**")
    }

    // Include resources (icon, etc.)
    from("src/main/resources") {
        include("com/tonic/plugins/attacktimer/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Attack Timer jar
    from("src/main/resources") {
        include("attacktimer-runelite-plugin.properties")
        rename("attacktimer-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "attacktimer",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Attack Timer",
            "Plugin-Description" to "Tracks attack timers for various weapons with combat automation features."
        )
    }
}

// Build a standalone jar containing only the LMS Navigator plugin for sideloading
val lmsNavigatorJar = tasks.register<Jar>("lmsNavigatorJar") {
    group = "build"
    description = "Packages only the LMS Navigator plugin for sideloading."
    archiveBaseName.set("lmsnavigator")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/lmsnavigator/**")
    }

    // Include resources (icon, etc.)
    from("src/main/resources") {
        include("com/tonic/plugins/lmsnavigator/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the LMS Navigator jar
    from("src/main/resources") {
        include("lmsnavigator-runelite-plugin.properties")
        rename("lmsnavigator-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "lmsnavigator",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "LMS Navigator",
            "Plugin-Description" to "Navigate anywhere in LMS using template-based pathfinding."
        )
    }
}

// Build a standalone jar containing only the Human Equipper plugin for sideloading
val humanEquipperJar = tasks.register<Jar>("humanEquipperJar") {
    group = "build"
    description = "Packages only the Human Equipper plugin for sideloading."
    archiveBaseName.set("humanequipper")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/humanequipper/**")
    }

    // Include resources (icon, etc.) if any
    from("src/main/resources") {
        include("com/tonic/plugins/humanequipper/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Human Equipper jar
    from("src/main/resources") {
        include("humanequipper-runelite-plugin.properties")
        rename("humanequipper-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "humanequipper",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Human Equipper",
            "Plugin-Description" to "Equip inventory items using OS mouse movement based on trained trajectories."
        )
    }
}

// Build a standalone jar containing only the Mirror View plugin for sideloading
val mirrorViewJar = tasks.register<Jar>("mirrorViewJar") {
    group = "build"
    description = "Packages only the Mirror View plugin for sideloading."
    archiveBaseName.set("mirrorview")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/mirror/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Mirror View jar
    from("src/main/resources") {
        include("mirrorview-runelite-plugin.properties")
        rename("mirrorview-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "mirrorview",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Mirror View",
            "Plugin-Description" to "Opens a second window that mirrors the game canvas (including overlays)."
        )
    }
}

// Build a standalone jar containing only the Auto Dialogue Navigator plugin for sideloading
val autoDialogueJar = tasks.register<Jar>("autoDialogueJar") {
    group = "build"
    description = "Packages only the Auto Dialogue Navigator plugin for sideloading."
    archiveBaseName.set("autodialogue")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/autodialogue/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Auto Dialogue Navigator jar
    from("src/main/resources") {
        include("autodialogue-runelite-plugin.properties")
        rename("autodialogue-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "autodialogue",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Auto Dialogue Navigator",
            "Plugin-Description" to "Automatically continues and selects dialogues based on configurable rules."
        )
    }
}

// Build a standalone jar containing only the HelperBox Agility plugin for sideloading
val helperBoxJar = tasks.register<Jar>("helperBoxJar") {
    group = "build"
    description = "Packages only the HelperBox agility plugin for sideloading."
    archiveBaseName.set("helperbox")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/helperbox/**")
    }

    from("src/main/resources") {
        include("helperbox-runelite-plugin.properties")
        rename("helperbox-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "helperbox_agility",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "HelperBox Agility",
            "Plugin-Description" to "Automated agility training using Draynor rooftop and VitaLite pathing."
        )
    }
}

// Build a standalone jar containing only the Auto Rooftops plugin for sideloading
val autoRooftopsJar = tasks.register<Jar>("autoRooftopsJar") {
    group = "build"
    description = "Packages only the Auto Rooftops plugin for sideloading."
    archiveBaseName.set("autorooftops")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/autorooftops/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Auto Rooftops jar
    from("src/main/resources") {
        include("autorooftops-runelite-plugin.properties")
        rename("autorooftops-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "autorooftops",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Auto Rooftops",
            "Plugin-Description" to "Most advanced rooftop agility automation with multi-course support, anti-ban, and smart management."
        )
    }
}

// Build a standalone jar containing only the Session Writer plugin for sideloading
val sessionWriterJar = tasks.register<Jar>("sessionWriterJar") {
    group = "build"
    description = "Packages only the Session Writer plugin for sideloading."
    archiveBaseName.set("sessionwriter")

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/sessionwriter/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Session Writer jar
    from("src/main/resources") {
        include("sessionwriter-runelite-plugin.properties")
        rename("sessionwriter-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "sessionwriter",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Session Writer",
            "Plugin-Description" to "Sends the current Jagex session information to a Discord webhook."
        )
    }
}

// Build a standalone jar containing only the Tick Preview Queue plugin for sideloading
val tickPreviewQueueJar = tasks.register<Jar>("tickPreviewQueueJar") {
    group = "build"
    description = "Packages only the Tick Preview Queue plugin for sideloading."
    archiveBaseName.set("tickpreviewqueue")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/tickpreviewqueue/**")
    }

    // Include a dedicated RuneLite plugin descriptor for the Tick Preview Queue jar
    from("src/main/resources") {
        include("tickpreviewqueue-runelite-plugin.properties")
        rename("tickpreviewqueue-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "tickpreviewqueue",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Tick Preview Queue",
            "Plugin-Description" to "Tick-aware inventory click queue with next-tick equip prediction."
        )
    }
}

// Build a standalone jar containing only the TrueDream Loot plugin for sideloading
val trueDreamLootJar = tasks.register<Jar>("trueDreamLootJar") {
    group = "build"
    description = "Packages only the TrueDream Loot plugin for sideloading."
    archiveBaseName.set("truedreamloot")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/truedreamloot/**")
    }

    // Include resources
    from("src/main/resources") {
        include("com/tonic/plugins/truedreamloot/**")
    }

    // Include a dedicated RuneLite plugin descriptor
    from("src/main/resources") {
        include("truedreamloot-runelite-plugin.properties")
        rename("truedreamloot-runelite-plugin.properties", "runelite-plugin.properties")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "truedreamloot",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "TrueDream Loot",
            "Plugin-Description" to "Automated radius-based looting with banking"
        )
    }
}

// Build a standalone jar containing only the Copilot Smiley overlay plugin for sideloading
val copilotSmileyJar = tasks.register<Jar>("copilotSmileyJar") {
    group = "build"
    description = "Packages only the Copilot Smiley plugin for sideloading."
    archiveBaseName.set("copilotsmiley")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output) {
        include("com/tonic/plugins/copilotsmiley/**")
    }

    // Include the Copilot Smiley RuneLite plugin descriptor
    from("src/main/resources") {
        include("copilotsmiley-runelite-plugin.properties")
        rename("copilotsmiley-runelite-plugin.properties", "runelite-plugin.properties")
    }

    // Include the embedded smiley image resources
    from("src/main/resources") {
        include("com/tonic/plugins/copilotsmiley/**")
    }

    manifest {
        attributes(
            "Plugin-Version" to version,
            "Plugin-Id" to "copilotsmiley",
            "Plugin-Provider" to "Tonic",
            "Plugin-Name" to "Copilot Smiley",
            "Plugin-Description" to "Draws a smiley image over Flipping Copilot's sell-item highlight in the Grand Exchange."
        )
    }
}