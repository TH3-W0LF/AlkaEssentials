plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.alkacode"
version = "1.3.4"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    // depend hard no plugin.yml - o AlkaEssentials usa o AlkaCore de verdade:
    // AlkaPlugin (classe base + AlkaAPI), MessageProvider (mensagens), BaseGui
    // (menus), AbstractRepository/DatabaseProvider (dados no banco, pro modulo de
    // punicoes/InvRestore que vem depois). Locais (spawn/warps/homes) ficam em YAML
    // proprio (decisao do projeto), mas a infraestrutura vem toda do Core.
    compileOnly("com.alkacode:AlkaCore:1.0.3")
    // Limite de homes via grupo LuckPerms (ex: VIPs tem mais homes). O AlkaCore ja
    // tem um LuckPermsHook, mas expor a conta aqui via compileOnly e mais direto
    // quando for ler nodes de limite (essentials.homes.<n>).
    compileOnly("net.luckperms:api:5.4")
    // Cobranca opcional pra teleportar num warp de jogador (mesmo padrao de import
    // direto + compileOnly + getPlugin()!=null do AlkaEconomyHook do AlkaVips - AlkaEconomy
    // continua softdepend em runtime, so classe carregada de verdade se instalada).
    compileOnly("com.alkacode:AlkaEconomy:1.0.6")
    // API do TAB (NEZNAMY) - para aplicar o nick no tab list / registrar placeholder.
    compileOnly(files("libs/TAB.jar"))
    // API do PlaceholderAPI - para registrar %alkaessentials_nick% e integrar com nChat/TAB.
    compileOnly(files("libs/PlaceholderAPI.jar"))
    // API do ProtocolLib - vanish profundo (ocultar no TAB + baú silencioso).
    compileOnly(files("libs/ProtocolLib.jar"))
    // WorldGuard - scoreboard.regions no scoreboards.yml (ver WorldGuardHook). WorldEdit
    // e dependencia transitiva do worldguard-bukkit (BlockVector3/BukkitAdapter).
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.15")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    // sem isso, o Gradle nao percebe que so `version` mudou e reusa o plugin.yml
    // antigo do cache (processResources fica UP-TO-DATE incorretamente).
    inputs.property("version", project.version)
    // expand() SO no plugin.yml - e o unico arquivo que usa ${version}. Aplicar em
    // todos os resources (como estava antes) processa backslashes como template
    // Groovy e colapsa escapes reais (`\\d` -> `\d`) em qualquer outro YAML com
    // regex, ex: config.yml's blocked-patterns - bug real, corrompia o jar mesmo
    // com o config.yml fonte 100% correto.
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
