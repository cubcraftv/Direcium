> **SUPERSEDED (2026-10-25):** this plan targets MC 26.2 / Sodium 0.9.2. For 26.3 see `2026-10-25-sodium-26.3-port-plan.md`.

# Direcium (DirectExp + Sodium bridge): Implementation Plan

**Created:** 2026-10-07. **Status:** proposal; supersedes "Phase 3" of
`2026-10-06-directexp-sodium-support-plan.md` (which stays the overall roadmap).
**Basis:** static reading of `sodium-26.2-stable.zip` and the current DirectExp tree
(Phases 0 to 2 done and built on Windows). Nothing in the add-on exists yet and none of it has been
compiled; "verified" below means read in source, not run.

> **Naming (2026-10-08):** two separate mods. **DirectExp** (`dev.cubcraftv.directexp`) is the Direct3D 11
> backend. **Direcium** (`dev.cubcraftv.direcium`, mod id `direcium`) is the client-only bridge described
> here, depending on `sodium` and `directexp`. Pure classes live in `src/main` (`bridge/`), Minecraft-facing
> ones in `src/client` (`client/bridge/`, `client/mixin/`). Class names in the tables below were updated to match.

## 1. Decisions already made

- **Add-on mod, not a fork.** Separate Fabric mod that requires real Sodium and patches it at run
  time with Mixin. It ships no Sodium code or resources, and does not edit Sodium's shaders.
- **Open source, no license blocker** (owner decision). Keep it clean anyway: write the two new
  classes from scratch (do not paste `VK*` classes), and name it as a bridge, not "Sodium for
  DirectX".
- **Option B** for per-region values: DirectExp already turns Sodium's `layout(push_constant)` block
  into the `DxPushConstants` constant buffer and exposes `DxPushConstants.setPushConstants`.

## 2. How Sodium picks its draw path (what we are hooking)

`DefaultChunkRenderer` creates `DrawContext.create()` once (field initialiser), then per frame, per
terrain pass:

```
pass.setPipeline(...); drawContext.setContext(pass, pipeline);
pass.setIndexBuffer(shared 32-bit index buffer) ; pass.setUniform("u_Globals"/"u_SectionTimeInfo"); bindTexture(...)
for each region: pass.setVertexBuffer(0, slice); drawContext.updateData(region, camera); batch.draw(drawContext)
drawContext.endDraw()
```

`DrawContext.create()` and `MultiDrawBatch.newBatch(int)` both `switch` on the enum
`DrawBackend.BACKEND`, which resolves to `OPENGL` for anything that is not `VulkanDevice`. That
switch is the only seam. Everything else (arenas, staging, shaders, sorting) is backend-neutral
Blaze3D calls, which DirectExp already implements.

## 3. DirectExp changes required first (small, one real blocker)

| # | Change | Why |
|---|--------|-----|
| **D1** | In `DxDeviceBackend`, report `DeviceFeatures.multiDrawDirectInterleaved = true` and set `DeviceLimits.maxMultiDrawDirectInterleavedDrawCount` to e.g. `65536` (currently `0`, 5th argument of `DEFAULT_LIMITS`). | **Blocker.** Blaze3D's `RenderPass.multiDrawIndexed(IntBuffer, ...)` throws `UnsupportedOperationException` when the feature is false and `IllegalArgumentException` when `drawCount` exceeds the limit. Sodium's batch calls exactly that method. It is honest to report it: `DxRenderPassBackend.multiDrawIndexed(IntBuffer...)` is implemented (loop of `drawIndexed`, parameters read as `firstIndex, indexCount, baseVertex`). The only consumers of the flag are `RenderPass` validation and Sodium's `DrawBackend` (grepped across `reference/` and Sodium), so vanilla is unaffected. Needed limit is at least `ModelQuadFacing.COUNT (7) * RenderRegion.REGION_SIZE (8*4*8 = 256) + 1 = 1793`. |
| D2 | Keep `persistentMapping = false`. | Sodium then uses `MojangStagingBuffer` -> `writeToBuffer`, which DirectExp supports. `true` would route to `MappedStagingBuffer`, which casts to GL classes. |
| D3 | Add a test that pins D1 (features/limits record values). | Prevents a silent regression that would only show as a Sodium crash. |

Not blockers but they decide quality (Phase 1 leftovers, see section 8): texel-buffer SRV binding
(chunk fade-in), native buffer-to-buffer copy, GPU-only buffers.

## 4. What the add-on contains

Id `direcium`, package `dev.cubcraftv.direcium`, Java 25, Minecraft 26.2,
same Loom / loader as DirectExp (`fabric-loom 1.18.2`, loader `0.19.5`). 26.x is unobfuscated, so no
remapping or refmap is expected, but confirm with the first build.

### 4.1 Mixins (the complete list)

| Mixin | Target | Injection | Behaviour |
|-------|--------|-----------|-----------|
| `DrawContextMixin` | `net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext` | `@Inject(method = "create", at = @At("HEAD"), cancellable = true)` on the **static** factory (handler is `private static`) | If `DirectExpBackend.isActive()`, `cir.setReturnValue(new DireciumDrawContext())`. Otherwise do nothing. |
| `MultiDrawBatchMixin` | `...gpu.device.batch.MultiDrawBatch` | `@Inject(method = "newBatch", at = @At("HEAD"), cancellable = true)` | If DirectExp, return `new DireciumMultiDrawBatch(capacity)`. |
| `RenderPassBackendAccessor` | `com.mojang.blaze3d.systems.RenderPass` | `@Accessor("backend")` returning `RenderPassBackend` | Our own accessor, instead of reusing Sodium's `RenderPassAccessor`, so we do not depend on Sodium's internal mixin interface. |

`DrawBackend` is deliberately **not** mixed in: it is an enum (cannot gain a constant), and it
already returns `OPENGL` without throwing on DirectX. Because `DrawContext.create` and `newBatch` are
intercepted at `HEAD`, nothing reads `DrawBackend.BACKEND` on the terrain path.

Mixin config: `direcium.client.mixins.json`, `"client"` environment, `required: true`, `injectors.defaultRequire: 1`
so a Sodium update that renames either method fails at startup with a clear message instead of
silently falling back to the OpenGL path.

### 4.2 New classes

| Class | Responsibility |
|-------|----------------|
| `DirectExpBackend` | `static boolean isActive()`: `RenderSystem.getDevice().getDeviceInfo().backendName().startsWith("Direct3D 11")`, cached. Uses only Mojang public API, so no compile dependency on DirectExp internals. (Same trick Sodium itself uses with `contains("OpenGL")`.) |
| `DireciumDrawContext extends DrawContext` | `setContext(pass, pipeline)`: store `pass`; fetch backend via `RenderPassBackendAccessor`; require `instanceof DxPushConstants`, else throw `IllegalStateException` naming both classes. `updateData(region, camera)`: compute the three camera-relative floats with `getCameraTranslation(...)` (protected static in `DrawContext`), time = `Math.toIntExact(System.currentTimeMillis() - region.getCreationTime())`, id = `region.getId()`, encode via `DireciumPushBlock`, call `setPushConstants`. `rotate`, `delete`, `endDraw`: no-ops. |
| `DireciumPushBlock` (pure Java) | `encode(ByteBuffer out, float x, float y, float z, int time, int regionId)`: 20 bytes, native order: floats at 0/4/8, `int` at 12, `uint` at 16 (matches `DrawContext.PUSH_CONSTANT_RANGE = 20` and the shader block). Pure so it is unit-testable without Minecraft. |
| `DireciumMultiDrawBatch extends MultiDrawBatch` | A direct `IntBuffer` of `capacity * 3` ints. `put(size, elementCount, baseVertex, elementOffset)` writes `[firstIndex = (int) elementOffset, indexCount = elementCount, baseVertex]` at `size * 3` and calls `updateMaxElementCount`. **`elementOffset` is in index units, not bytes** (the OpenGL batch shifts it `<< 2`, the Vulkan batch does not; DirectExp wants indices). `draw(ctx)`: set position 0 / limit `size * 3`, `ctx.getPass().multiDrawIndexed(buffer, 1, 0, size)`. `delete()`: nothing to free (or release the buffer). |
| `DireciumDrawParameters` (pure Java, optional) | The packing logic of the batch, split out for tests. |
| `DireciumClient` (client entrypoint) | Logs whether the bridge is active; on startup warns if Sodium's version is outside the tested range; reflectively checks `RenderRegion#getCreationTime` / `getId` exist for a readable error. |

### 4.3 Metadata

`fabric.mod.json`: `environment: client`, `depends`: `minecraft 26.2`, `java >=25`, `fabricloader`,
`sodium` (range pinned to the tested 26.2 build), `directexp` (>= the build that has D1 and
`DxPushConstants`). Build: `modCompileOnly` Sodium and DirectExp jars (local `libs/` or Modrinth
maven), `modLocalRuntime` both for the dev client.

## 5. What needs no work (and why)

- **Sodium's shaders and pipeline definitions**: unchanged. `VULKAN` is predefined by shaderc, the
  push-constant block becomes `DxPushConstants`, the texel buffer becomes an HLSL `Buffer<int4>`
  (verified offline in Phase 2).
- **`GlDeviceMixin`, `GlSurfaceMixin`, `GlStateManagerMixin`, `WindowMixin` (targets `GlBackend`),
  `VulkanPipelineMixin`, `*RenderPassAccessor`**: target classes that are never instantiated on
  DirectX, so their handlers never run.
- **Startup compatibility checks**: GL-specific ones are guarded by `backendName().contains("OpenGL")`;
  DirectExp's name is `"Direct3D 11 (feature level 10.1 floor)"`.
- **`MojangStagingBuffer`, arenas, shared index buffer, `UniformBufferManager`**: plain
  `createBuffer` / `writeToBuffer` / `copyToBuffer` / `map` calls with `USAGE_VERTEX`, `INDEX`,
  `COPY_SRC/DST`, `MAP_WRITE`, `UNIFORM_TEXEL_BUFFER`; all already supported (slowly, see section 8).
- **`QUADS` topology and 32-bit indices**: `DxPipelineStateMapping` maps `QUADS` to a triangle list
  (Sodium supplies its own 6-indices-per-quad buffer) and `bindIndexBuffer` supports `R32_UINT`.

## 6. Known hazards to check on the first runs

1. **`DrawBackend.BACKEND == OPENGL` is a stand-in.** Only `MappedStagingBuffer.flush()` branches on
   it in runtime code, and it is unreachable while `persistentMapping` is false. Re-grep
   `DrawBackend.` and `DrawBackend.BACKEND` on every Sodium update.
2. **Namespaced shader imports.** Sodium uses `#moj_import <sodium:...>`. DirectExp's preprocessing
   uses Mojang's resolver, so it should work, but confirm the first pipeline compiles (watch for the
   "missing shader source" log).
3. **Unbound `u_SectionTimeInfo`** until texel-buffer binding exists: logged once, reads as zero,
   so no fade-in.
4. **Per-sub-draw restaging.** The binder re-stages every constant buffer for each of the up to
   ~1.8k sub-draws of a region's batch. Correct, slow; first frame rates will understate the result.
5. **CPU-mirrored arenas.** Memory scales with render distance; test small distances first.
6. **Sodium update drift.** Two injection points plus two overridden abstract classes is the entire
   surface; `defaultRequire` makes drift loud.

## 7. Phases

### Phase A: DirectExp prerequisites (D1 to D3)
Exit: `multiDrawIndexed` on a DirectExp `RenderPass` no longer throws; test pins the values.

### Phase B: Add-on scaffold and pure logic
New Gradle project (own repo recommended, consuming DirectExp as a jar, so release cadences stay
independent), mixin config, entrypoint, `DirectExpBackend`, `DireciumPushBlock`,
`DireciumDrawParameters`, JUnit tests for the pure classes (byte layout, `elementOffset` stays in
indices, capacity edge, `size` smaller than capacity).
Exit: `build` passes; game launches with both mods and logs "bridge active".

### Phase C: First terrain frame
Wire `DireciumDrawContext` and `DireciumMultiDrawBatch`, run with a small render distance.
Exit: Sodium terrain draws (opaque, cutout, translucent) with correct textures and positions; log shows
no GL calls and no "pipeline invalid" entries; one RenderDoc capture checks the
`DxPushConstants` buffer contents against `updateData`.

### Phase D: Correctness matrix
Status (2026-10-08): hardening, instrumentation and the test matrix are in the repo (`docs/test/phase-d-correctness-matrix.md`,
`tools/scan-log.py`, `-Ddirecium.stats=true`); the matrix itself has not been run.

Resize, fullscreen toggle, F3+T resource reload, dimension change, long session, render distance
sweep, Sodium options (fog, RGSS/anisotropy, vertex compression), translucency sorting modes, with and
without the Phase 1 leftovers. Compare with vanilla DirectExp and, if possible, Sodium on OpenGL.

### Phase E: Performance
In DirectExp, not the add-on: skip restaging unchanged constant buffers within one `multiDrawIndexed`,
a native batched multi-draw (one JNI crossing per region), then the Phase 1 leftovers (GPU-only
buffers, native `CopySubresourceRegion` for `copyToBuffer`, texel-buffer SRV). Profile before and
after each.

## 8. Order of work and ownership

1. Phase A (DirectExp, tiny).
2. Phase B and C (add-on; the bulk of new code is under 300 lines).
3. In parallel, Phase 1 leftovers in DirectExp: **texel-buffer SRV first** (fixes fade-in and a
   one-time diagnostic), then native buffer copy, then GPU-only buffers.
4. Phases D and E.

## 9. Unknowns (not resolvable from source)

- Exact Sodium build string and Modrinth coordinates to pin in `depends` and the Gradle dependency.
- Whether Loom 1.18.2 resolves a Modrinth Sodium jar for 26.x without remapping (expected yes since
  26.x is unobfuscated).
- Real draw counts and frame cost with per-sub-draw restaging, which decides how soon Phase E is
  needed.
- Whether `RenderSystem.getDevice()` is guaranteed non-null when `DrawContext.create()` first runs
  (Sodium's own `DrawBackend` assumes it, so the add-on does too; the first run will show it).
