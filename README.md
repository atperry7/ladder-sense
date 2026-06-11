# Ladder Sense

Climbing speed that follows where you look. On any climbable surface, glance upward to ascend
faster and look downward to descend faster. Look roughly level and movement stays exactly vanilla.
No new blocks, controls, or progression — just more responsive vertical travel.

## Features

- **Look up to climb faster, look down to descend faster** — speed scales with your view pitch.
- **Dead zone** keeps movement vanilla when you're looking roughly level, so you can pause and aim
  on a ladder without changing speed.
- **Clean exits** — accelerated ascent eases back to vanilla speed at the top of a climb, so you're
  never flung off the top of a ladder.
- **Sneak to park** still works exactly like vanilla.
- **Chains are climbable** — Ladder Sense adds chains to the climbable set, so they work like ladders
  (with look-driven speed too).
- **Accessibility options** — disable the acceleration curve for fixed speed tiers, or cap the
  maximum climb speed.
- **Fully configurable** — tune the multipliers, dead zone, and ramp curve, or turn the mod off
  entirely.

## How it works

While climbing:

- **Looking up** past the dead zone accelerates your ascent, up to the max ascent multiplier when
  looking nearly straight up. Ascent only speeds up while you're actually climbing (holding into the
  ladder or jumping), so simply glancing up never launches a standing player, and holding "up" always
  climbs regardless of where you look.
- **Looking down** past the dead zone accelerates your descent (while sliding), up to the max descent
  multiplier when looking nearly straight down.
- **Looking level** (within the dead zone) keeps vanilla speed.
- **Sneaking** parks you on the ladder, untouched.

### Supported blocks

Anything tagged `#minecraft:climbable` works automatically, including modded climbables:

- Ladders
- Chains (iron and all copper variants) — *added by this mod*
- Vines, cave vines, twisting vines, weeping vines
- Scaffolding — *on by default*; disable with `affectScaffolding`. Scaffolding keeps its own controls
  (jump to go up, sneak to go down), and Ladder Sense scales those by your look pitch the same way.

## Configuration

Settings live in `config/ladder-sense.json`, created with defaults on first launch.

> The config is read **once at startup**. After editing the file, restart the game (or server) for
> changes to take effect.

| Key | Default | Valid values | What it does |
| --- | --- | --- | --- |
| `enabled` | `true` | `true` / `false` | Master switch. `false` restores fully vanilla climbing. |
| `maxAscentMultiplier` | `3.0` | `≥ 1.0` | Top upward speed when looking nearly straight up (× the vanilla 0.2 blocks/tick climb). |
| `maxDescentMultiplier` | `4.0` | `≥ 1.0` | Top downward speed when looking nearly straight down while sliding (× the vanilla 0.15 blocks/tick slide). |
| `deadZoneDegrees` | `15.0` | `0.0`–`89.0` | Half-width (±) of the neutral look zone where movement stays vanilla. |
| `rampCurve` | `"SMOOTH"` | `"LINEAR"`, `"SMOOTH"`, `"AGGRESSIVE"` | How speed ramps from vanilla (at the dead-zone edge) to max (at ±90°). `LINEAR` = constant rate; `SMOOTH` = eases in and out; `AGGRESSIVE` = rises quickly then tapers. Case-sensitive. |
| `affectScaffolding` | `true` | `true` / `false` | Whether scaffolding is affected (jump to climb, sneak to descend). Set `false` to leave scaffolding fully vanilla. |
| `disableAccelerationCurves` | `false` | `true` / `false` | Accessibility: skip the ramp and jump straight to the max multiplier past the dead zone (fixed tiers). |
| `maxVerticalSpeedCap` | `0.0` | `≥ 0.0` | Accessibility: absolute ceiling on climb speed in blocks/tick. `0` disables the cap. |

Values are sanitized on load, so an out-of-range edit is clamped rather than breaking movement.

### Resetting to defaults

Delete `config/ladder-sense.json` and it will be regenerated with defaults on next launch, or paste
this back into the file:

```json
{
  "enabled": true,
  "maxAscentMultiplier": 3.0,
  "maxDescentMultiplier": 4.0,
  "deadZoneDegrees": 15.0,
  "rampCurve": "SMOOTH",
  "affectScaffolding": true,
  "disableAccelerationCurves": false,
  "maxVerticalSpeedCap": 0.0
}
```

## Installation & multiplayer

One jar covers Minecraft **26.1 – 26.1.2** (Fabric Loader ≥ 0.19.3, Fabric API, Java 25).

The mechanic runs in common (client + server) code, but Minecraft player movement is
**client-authoritative** — your client computes your own climbing and the server accepts it. So:

- **Client is required** for your own climbing to change. Single-player works out of the box.
- **Install on the server too** for the best multiplayer experience: the server then expects the
  faster movement (no anti-cheat rubber-banding) and applies the mechanic to climbing mobs.
- **Server-only with vanilla clients has no effect on players** — the vanilla client still computes
  normal climbing speed and the server has nothing to override.
- **Client-only on a server** works, but a strict vanilla/anti-cheat server may rubber-band high
  multipliers since it sees the player moving faster than expected.

## Building

Build with `./gradlew build`; the mod jar lands in `build/libs/`. For development environment setup,
see the [Fabric Documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up).

## License

Available under the CC0 license.
