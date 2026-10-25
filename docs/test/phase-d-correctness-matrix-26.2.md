# Phase D: correctness matrix (HISTORICAL, Minecraft 26.2 / Sodium 0.9.2 / old bridge)

> Results below were recorded against the pre-26.3 bridge (`DireciumDrawContext` / `DireciumMultiDrawBatch`), which no longer exists. Kept for the findings (F2 especially). The current matrix is `phase-d-correctness-matrix.md`.

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
| 1 | Cold start, load world | Log: "Direcium loaded", "initialised", DirectExp "first native render pipeline"; no scan-log errors |Direcium initialised missing (cause: no `main` entrypoint registered; fixed, see Findings F1); otherwise OK|
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
| 13 | Render distance sweep 2, 6, 12, 16, 24 | Correct at each; note memory and stats `max sub-draws in one region` (must stay <= 1793) |PASSED after rebuild. An earlier run lost terrain above render distance 5 after toggling see-through leaves; no longer reproduces (see F2, failed to reproduce)|
| 14 | Fog on/off, fog distance changes | Matches B |passed|
| 15 | Anisotropic filtering / mipmap levels (RGSS where offered) | Matches B |passed|
| 16 | Vertex compression on/off | Matches B |N/A: this Sodium build has no such toggle|
| 17 | Translucency sorting modes | Matches B in each mode |passed|
| 18 | Chunk update threads / always-defer toggles | No stalls or missing chunks |passed|

### Stress

| # | Scenario | Check | Result |
|---|----------|-------|--------|
| 19 | Fly fast in a straight line for 2 minutes | No missing regions, no growth in log errors |passed|
| 20 | Break and place many blocks near region borders | Updates show immediately, no flicker |passed|
| 21 | 30-minute idle + wander session | No memory growth trend, no crash. (Region time is clamped, so the 24.8-day int overflow cannot crash.) |passed|

## Findings

**F1: "Direcium initialised" missing (row 1), resolved in code.** `fabric.mod.json` only registered the
`client` entrypoint, so `Direcium.onInitialize` never ran. Not a rendering fault. A `main` entrypoint was added;
the line will appear after rebuilding.

**F2: terrain disappears at render distance > 5 after toggling see-through leaves (row 13). Status: FAILED TO REPRODUCE after rebuild; row 13 marked passed, bug kept on record.**
Observed once, then not again after rebuilding with the Phase D follow-up changes (main entrypoint, scanner, docs).
None of those changes touch rendering or buffers, so the cause is unknown and the failure may be intermittent
(it depended on a native allocation succeeding). Reopen if it returns; the notes below still apply.
Evidence from the log: DirectExp's native `ID3D11Device::CreateBuffer` fails ("unsupported bind-flag
combination, or out of memory"), DirectExp falls back to a CPU-only buffer with no native handle, and then skips every
draw that uses it. A buffer that fell back stays at handle 0 for its lifetime, which matches "stays broken until restart".

What the evidence points toward (not confirmed):
- Vertex buffers map only to the vertex bind flag in DirectExp, so a bad flag combination looks unlikely; out of memory (or the
  D3D11 per-resource size cap) fits better.
- Steady-state high render distance works without the toggle, so it is probably not plain capacity. A full chunk rebuild
  has old and new region buffers alive together, and every DirectExp buffer also keeps a CPU mirror. D3D11 can also defer freeing a released
  buffer until it is unbound and the context flushed, so VRAM may stay occupied across the rebuild.
- Sodium creates these buffers through Blaze3D; Direcium does not create or size any buffer, so this is most likely on the DirectExp side.

Discriminating tests (optional now; run them if the failure returns, row 13b to 13d):

| # | Scenario | Why | Result |
|---|----------|-----|--------|
| 13b | Render distance 12, never toggle leaves, press F3+A (reload chunks) | If terrain disappears, the cause is the generic chunk-reload path and leaves are incidental |passed|
| 13c | Render distance 12, watch dedicated GPU memory (Task Manager, Performance, GPU) before, during and after the toggle | Shows whether VRAM fills and stays full after the old buffers should be gone |test env using integrated gpu, unable to verify|
| 13d | Same toggle at render distance 5, 6, 8; note the first distance that fails and your GPU's VRAM | A fixed threshold suggests a size/memory limit; a threshold that moves with VRAM suggests the churn peak |passed|

When reporting to DirectExp, include the first `native GPU buffer creation unavailable` line, the 13b-13d results, the GPU model
and VRAM. `tools/scan-log.py` now flags these two log lines as errors (it previously passed this run).

**Not run:** row 16 (setting does not exist in this Sodium build). Row 14 (fog) has since been run and passed.
The `-Ddirecium.stats=true` flag was not used, so no stats lines were recorded; add it for 13b-13d.

## Reading the stats line

`Direcium stats: 5.0s: N passes, N region updates, N sub-draws (max M in one region)`

- `passes` is terrain passes (opaque, cutout, translucent), so about 3 per frame.
- `max M` above 1793 means Sodium's batch capacity assumption broke; the batch would throw first.
- Compare `sub-draws` per second against frame time to decide how soon Phase E is needed (plan section 9, third unknown).

## If a row fails

Capture: `latest.log`, a screenshot, row number, and whether B (OpenGL) shows the same thing, since some
Sodium behaviour differs from vanilla regardless of backend. A RenderDoc capture of one frame is
needed for any wrong-position or wrong-texture failure (compare the `DxPushConstants` buffer with `updateData`).