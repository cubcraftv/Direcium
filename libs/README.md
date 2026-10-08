Put the two jars this mod compiles against here (git-ignored):

- The Sodium Fabric jar for Minecraft 26.2 (download it, or build it from the Sodium sources).
- The DirectExp jar that contains `DxPushConstants` and the multi-draw feature flag (DirectExp's Phase 2 + D1 patch).

Then set `sodium_jar` / `directexp_jar` in `gradle.properties` to match the file names.
