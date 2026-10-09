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

## Testing

Run with `-Ddirecium.stats=true`, work through `docs/test/phase-d-correctness-matrix.md`, then
`python tools/scan-log.py run/logs/latest.log`.

## Status

Beta testing.

## License

See `LICENSE`.
