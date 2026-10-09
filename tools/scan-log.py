#!/usr/bin/env python3
"""Scan a Minecraft latest.log from a Direcium + DirectExp run for the problems Phase D looks for.

Usage: python tools/scan-log.py path/to/latest.log
Exit code 1 if any error-class finding is present, so it can gate a scripted run.
"""
import re
import sys

# (severity, label, regex)
CHECKS = [
    ("error", "Direcium reports Sodium surface mismatch", r"Direcium: Sodium's RenderRegion no longer matches"),
    ("error", "Direcium wrong render pass backend", r"Direcium expected DirectExp's render pass backend"),
    ("error", "Mixin injection failed (Sodium drift?)", r"(InjectionError|MixinApplyError|InvalidInjectionException).*direcium|direcium.*(InjectionError|MixinApplyError|InvalidInjectionException)"),
    ("error", "DirectExp pipeline invalid", r"DirectExp: pipeline .* is invalid"),
    ("error", "multiDrawIndexed rejected (D1 regression)", r"multiDrawIndexed.*(UnsupportedOperationException|IllegalArgumentException)|UnsupportedOperationException.*multiDraw"),
    ("error", "Unexpected GL call on DirectX", r"(GlStateManager|GL11|GL15|GL30|GL43)\b.*(Exception|Error)|OpenGL.*not.*(current|available)"),
    ("error", "Render thread crash", r"Exception in thread \"Render thread\"|---- Minecraft Crash Report ----"),
    ("warn", "Shader source not available (retries after reload)", r"shader source is not available yet"),
    ("warn", "Unbound u_SectionTimeInfo (no fade-in until texel-buffer SRV exists)", r"u_SectionTimeInfo"),
    ("warn", "DirectExp draw problem", r"drawProblem|draw problem"),
]
INFO = [
    ("Direcium loaded", r"Direcium loaded"),
    ("Direcium initialised", r"Direcium \(Sodium to DirectExp bridge\) initialised"),
    ("DirectExp first pipeline", r"first native render pipeline assembled"),
]

def main(path):
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = f.read().splitlines()

    findings = {}
    for sev, label, rx in CHECKS:
        pat = re.compile(rx)
        hits = [(i + 1, l) for i, l in enumerate(lines) if pat.search(l)]
        if hits:
            findings[(sev, label)] = hits

    stats = [l for l in lines if "Direcium stats:" in l]
    print(f"Scanned {len(lines)} lines from {path}\n")
    for label, rx in INFO:
        found = any(re.search(rx, l) for l in lines)
        print(f"  [{'ok' if found else 'MISSING'}] {label}")
    print()

    errors = 0
    for (sev, label), hits in sorted(findings.items()):
        tag = "ERROR" if sev == "error" else "warn "
        if sev == "error":
            errors += 1
        print(f"[{tag}] {label}: {len(hits)} hit(s)")
        for n, l in hits[:3]:
            print(f"         line {n}: {l.strip()[:160]}")
    if not findings:
        print("No known problem markers found.")

    if stats:
        print(f"\nDirecium stats lines: {len(stats)} (last: {stats[-1].split('Direcium stats:')[1].strip()})")
    else:
        print("\nNo 'Direcium stats' lines (run with -Ddirecium.stats=true to record draw counts).")
    return 1 if errors else 0

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
