# Correctness matrix: Direcium on Minecraft 26.3

Nothing here has been run. Fill the Result column on Windows with Direcium + DirectExp + Sodium 26.3
(see `libs/README.md`). References: **A** = vanilla DirectExp (no Sodium), **B** = Sodium on OpenGL,
**C** = Direcium (Sodium on DirectExp). Use one world, spawn and camera angle; render distance 6 unless stated.
Pass = matches B visually, no new log errors, no crash.

## Smoke signals (check first)

| # | Check | Expected |
|---|-------|----------|
| S1 | Log | `Direcium loaded`, `Direcium (Sodium to DirectExp bridge) initialised`, `DirectExp is the active backend; Sodium terrain uses its VK_MULTIDRAW path` |
| S2 | F3 screen | `Terrain Rendering: ext_multidraw (sodium)` |
| S3 | Log | DirectExp `first native render pipeline assembled`; no `HLSL emission/compilation failed` for `sodium:*`; no `NoClassDefFoundError` for `org.lwjgl.vulkan.*` (risk R1) |
| S4 | Sodium options | A "DirectExp" page with a Graphics API option; changing it asks for a restart and is written to `config/directexp.properties` |

## Rendering

| # | Scenario | Check | Result |
|---|----------|-------|--------|
| 1 | Opaque terrain | Positions, textures, no holes at region borders | |
| 2 | Cutout (leaves, grass) | Alpha cutout correct | |
| 3 | Translucent (water, glass, ice) | Order correct above and below surface | |
| 4 | Far from origin (~X 20000) | No jitter (camera-relative offset via push constants) | |
| 5 | Chunk fade-in | Chunks fade, not pop (`u_SectionTimeInfo` texel buffer, R3) | |
| 6 | OIT / transparency option on and off | Correct, or documented limitation (R5) | |

## Lifecycle

| # | Scenario | Check | Result |
|---|----------|-------|--------|
| 7 | Resize, F11 x5, minimise/restore | Terrain intact | |
| 8 | F3+T x3, resource pack switch | Pipelines recompile, terrain returns | |
| 9 | Dimension change, disconnect/rejoin | Terrain rebuilds | |

## Sodium options and stress

| # | Scenario | Check | Result |
|---|----------|-------|--------|
| 10 | Render distance 2, 6, 12, 16, 24 | Correct at each; watch for native `CreateBuffer` failures (see F2 in the 26.2 matrix; risk R4) | |
| 11 | Fog, anisotropy / RGSS, translucency sorting modes | Matches B | |
| 12 | Fast flight 2 min; block edits at region borders | No missing regions or flicker | |
| 13 | 30 min wander | No memory growth trend, no crash | |
