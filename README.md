# Ladder Sense

Climbing speed that follows where you look. On any climbable surface, glance upward to ascend
faster and look downward to descend faster. Look roughly level and movement stays exactly vanilla.
No new blocks, controls, or progression — just more responsive vertical travel.

## How it works

While climbing (ladders, vines, cave/twisting/weeping vines, and any block tagged climbable):

- **Looking up** past the dead zone accelerates your ascent, up to the max ascent multiplier when
  looking nearly straight up. Ascent only speeds up while you're actually climbing (pressing into
  the ladder or jumping), so simply glancing up never launches you.
- **Looking down** past the dead zone accelerates your descent, up to the max descent multiplier.
- **Looking level** (within the dead zone) keeps vanilla speed, so you can pause without fiddling.
- **Sneaking** still parks you on the ladder, untouched.

## Installation & multiplayer

The mechanic runs in common (client + server) code, but Minecraft player movement is
**client-authoritative** — your client computes your own climbing and the server accepts it. So:

- **Client is required** for your own climbing to change. Single-player works out of the box.
- **Install on the server too** for the best multiplayer experience: the server then expects the
  faster movement (no anti-cheat rubber-banding) and applies the mechanic to climbing mobs.
- **Server-only with vanilla clients has no effect on players** — the vanilla client still computes
  normal climbing speed and the server has nothing to override.
- **Client-only on a server** works, but a strict vanilla/anti-cheat server may rubber-band high
  multipliers since it sees the player moving faster than expected.

## Configuration

Settings live in `config/ladder-sense.json`, created with defaults on first launch:

| Key | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Master switch; `false` restores vanilla climbing. |
| `maxAscentMultiplier` | `3.0` | Top upward speed multiplier when looking nearly straight up. |
| `maxDescentMultiplier` | `4.0` | Top downward speed multiplier when looking nearly straight down. |
| `deadZoneDegrees` | `15.0` | Pitch half-width (±) where movement stays vanilla. |
| `rampCurve` | `SMOOTH` | How speed ramps with pitch: `LINEAR`, `SMOOTH`, or `AGGRESSIVE`. |
| `affectScaffolding` | `false` | Whether scaffolding is affected (it has its own fast movement). |
| `disableAccelerationCurves` | `false` | Motion comfort: jump straight to max past the dead zone (fixed tiers). |
| `maxVerticalSpeedCap` | `0.0` | Absolute ceiling on vertical climb speed (blocks/tick); `0` disables it. |

## Setup

For setup instructions, see the [Fabric Documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up)
for your IDE. Build with `./gradlew build`; the mod jar lands in `build/libs/`.

## License

Available under the CC0 license.
