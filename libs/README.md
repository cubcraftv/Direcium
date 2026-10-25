Put the two jars this mod compiles against here (git-ignored). File names must match `gradle.properties`.

- **Sodium for Minecraft 26.3** (`sodium_jar`). Build it from the Sodium sources: `./gradlew build`, then take the
  Fabric jar from `fabric/build/libs`. Read its `fabric.mod.json` for the version and put it in
  `sodium_version_range`. Sodium's config API (`net.caffeinemc.mods.sodium.api.config`) is inside that jar.
- **DirectExp for Minecraft 26.3** (`directexp_jar`, default `directexp-0.1.0-mc26.3.jar`). **Build it on Windows**
  (`./gradlew build` in DirectExp): the native DLL is only bundled into the jar there. Direcium uses it only for
  `config` classes and the settings state; it needs no draw-path classes from it.
