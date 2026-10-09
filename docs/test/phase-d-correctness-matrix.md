# Phase D: correctness matrix

Run on Windows with Direcium + DirectExp + Sodium (see `libs/README.md`). Nothing here has been run yet;
the Result column is for you to fill in. Reference points: **A** = vanilla DirectExp (no Sodium),
**B** = Sodium on OpenGL (same world, seed and position), **C** = Direcium (Sodium on DirectExp).

## Setup

1. `./gradlew runClient` with JVM arg `-Ddirecium.stats=true` (logs draw counts every 5 s).
2. Use one fixed world and one fixed spawn point + camera angle for every row. Render distance 6 unless the row says otherwise.
3. After each row: `python tools/scan-log.py run/logs/latest.log`. Exit code 1 means an error-class finding.
4. Screenshot at the same position for A/B/C when the row says "compare".

## Known and expected before testing

| Item | Expected | Why |
|------|----------|-----|
| No chunk fade-in | Chunks pop in | `u_SectionTimeInfo` unbound until DirectExp gets texel-buffer SRV (plan section 6, hazard 3) |
| Low frame rate | Much slower than B | Per-sub-draw constant-buffer restaging (hazard 4); Phase E fixes it, so do not file FPS as a bug yet |
| Memory grows with render distance | Yes | CPU-mirrored arenas (hazard 5) |
| `u_SectionTimeInfo` log line | Once per session | Same as fade-in |

## Matrix

Pass = matches reference **B** visually, no new log errors, no crash. Fill Result with PASS / FAIL / N/A + note.

### Startup and baseline

| # | Scenario | Check | Result |
|---|----------|-------|--------|
| 1 | Cold start, load world | Log: "Direcium loaded", "initialised", DirectExp "first native render pipeline"; no scan-log errors |Direcium initialised missing; still running|
| 2 | Opaque terrain | Correct positions, textures, no holes at region borders |passed|
| 3 | Cutout (leaves, grass, flowers) | Alpha cutout correct, no black fringes |passed|
| 4 | Translucent (water, stained glass, ice) | Draw order correct from above and below the surface |passed|
| 5 | Camera far from origin (travel to ~X 20000) | No jitter or swimming terrain (camera-relative floats) |passed|

### Window and device lifecycle

| # | Scenario | Check | Result |
|---|----------|-------|--------|
| 6 | Resize window (drag small and large) | Terrain stays correct, no stale frame |passed|
| 7 | Fullscreen toggle (F11) x5 | Terrain survives every toggle |passed|
| 8 | Minimise and restore | No crash, terrain intact |passed|
| 9 | F3+T resource reload x3 | Pipelines recompile ("shader source not available" warn is tolerated once), terrain returns |passed|
| 10 | Switch resource pack, then back | Textures update, no crash |passed|
| 11 | Dimension change (Overworld to Nether to End and back) | Terrain rebuilds, region ids and offsets correct |passed|
| 12 | Disconnect and rejoin the world | Second join draws correctly (draw context and batches are recreated) |passed|

### Sodium options

| # | Scenario | Check | Result |
|---|----------|-------|--------|
| 13 | Render distance sweep 2, 6, 12, 16, 24 | Correct at each; note memory and stats `max sub-draws in one region` (must stay <= 1793) |render distance >5 disapeared when see-through leaves toggle on (even if it turned off it still happen, restart the game fix it if see-through leaves never turned on in that session)|
| 14 | Fog on/off, fog distance changes | Matches B |havent tested required sodium extra|
| 15 | Anisotropic filtering / mipmap levels (RGSS where offered) | Matches B |passed|
| 16 | Vertex compression on/off | Matches B |sodium removed setting toggle, no flicked|
| 17 | Translucency sorting modes | Matches B in each mode |passed|
| 18 | Chunk update threads / always-defer toggles | No stalls or missing chunks |passed|

### Stress

| # | Scenario | Check | Result |
|---|----------|-------|--------|
| 19 | Fly fast in a straight line for 2 minutes | No missing regions, no growth in log errors |passed|
| 20 | Break and place many blocks near region borders | Updates show immediately, no flicker |passed|
| 21 | 30-minute idle + wander session | No memory growth trend, no crash. (Region time is clamped, so the 24.8-day int overflow cannot crash.) |passed|

## Reading the stats line

`Direcium stats: 5.0s: N passes, N region updates, N sub-draws (max M in one region)`

- `passes` is terrain passes (opaque, cutout, translucent), so about 3 per frame.
- `max M` above 1793 means Sodium's batch capacity assumption broke; the batch would throw first.
- Compare `sub-draws` per second against frame time to decide how soon Phase E is needed (plan section 9, third unknown).

## If a row fails

Capture: `latest.log`, a screenshot, row number, and whether B (OpenGL) shows the same thing, since some
Sodium behaviour differs from vanilla regardless of backend. A RenderDoc capture of one frame is
needed for any wrong-position or wrong-texture failure (compare the `DxPushConstants` buffer with `updateData`).
