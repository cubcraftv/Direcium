# Direcium

Client-side Fabric mod that lets **Sodium**'s terrain renderer draw through **DirectExp**'s Direct3D 11
backend. DirectExp provides the backend; Direcium is the bridge between the two. It requires both,
ships no Sodium code, and patches Sodium at run time with two Mixin injections.

## Layout

| Path | Purpose |
|------|---------|
| `client/mixin/DrawContextMixin`, `MultiDrawBatchMixin` | Head-inject Sodium's two backend factories; return our classes when DirectExp is active |
| `client/mixin/RenderPassBackendAccessor` | Reads `RenderPass.backend` |
| `client/bridge/DireciumDrawContext` | Per-region `updateData` -> `DxPushConstants.setPushConstants` |
| `client/bridge/DireciumMultiDrawBatch` | Packs draws for `RenderPass.multiDrawIndexed(IntBuffer, ...)` |
| `client/bridge/DirectExpBackend` | Detects DirectExp from `DeviceInfo.backendName()` |
| `bridge/*` (in `src/main`) | Pure Java (no Minecraft types), unit tested |

## Before it can run

1. DirectExp must include its Phase 2 + D1 changes (`DxPushConstants`, and `multiDrawDirectInterleaved`
   reported with a draw-count limit of at least 1793).
2. Put the Sodium and DirectExp jars in `libs/` (see `libs/README.md`).
3. `./gradlew build`, then `./gradlew runClient`.

## Status

Scaffold only. The pure logic is unit tested; the Minecraft-facing classes were only compiled against
hand-written stubs, never against the real Sodium/Minecraft jars. Expect a round of compile fixes.

## License

See `LICENSE`.
