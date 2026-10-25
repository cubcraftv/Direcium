# Direcium

Client-side Fabric mod that lets **Sodium**'s terrain renderer draw through **DirectExp**'s Direct3D 11
backend. DirectExp provides the backend; Direcium is the bridge between the two. It requires both,
ships no Sodium code, and patches Sodium at run time with one Mixin injection.

Also see [DirectExp](https://github.com/cubcraftv/DirectExp).

## Layout

| Path | Purpose |
|------|---------|
| `client/mixin/DrawBackendMixin` | Head-injects Sodium's `DrawBackend.chooseBackend()`; returns `VK_MULTIDRAW` when DirectExp is active. Sodium's own multi-draw batch and `RenderPass.pushConstants` path then run on DirectExp unchanged |
| `client/bridge/DirectExpBackend` | Detects DirectExp from `DeviceInfo.backendName()` |
| `client/DireciumConfig` | Sodium config entry point: DirectExp's Graphics API option on Sodium's options screen (Sodium replaces the vanilla video screen that normally holds it) |
| `client/DireciumClient` | Startup check that Sodium still has `DrawBackend.VK_MULTIDRAW` / `chooseBackend` |
| `bridge/DirectExpBackendName` (in `src/main`) | Pure Java name rule, unit tested |

Targets Minecraft 26.3 and Sodium 26.3. See `docs/plan/2026-10-25-sodium-26.3-port-plan.md`.

## Testing

Work through `docs/test/phase-d-correctness-matrix.md` (not run yet for 26.3). `./gradlew test` runs the pure unit tests.

## Status

26.3 port implemented, not yet compiled or run.

## License

See `LICENSE`.
