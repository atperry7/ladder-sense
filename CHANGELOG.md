# Changelog

All notable changes to Ladder Sense are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-06-17

### Changed
- **Updated to Minecraft 26.2.** A single jar now covers all of 26.2.x. The build pins to the
  26.2 floor and the released jar declares `~26.2`, so it loads on every 26.2 patch.
- Bumped Fabric API to `0.152.1+26.2` and Fabric Loom to `1.17-SNAPSHOT`.
- Raised the Gradle wrapper to `9.5.0`, which Loom 1.17 requires.

### Notes
- 26.1.x users should stay on 1.1.0; 1.2.0 targets the 26.2 series.
- All gametests pass against the official 26.2 release on a headless server.

## [1.1.0] - 2026-06-11

### Added
- Fabric gametest suite that boots a real headless server with mixins applied, so climbing
  behaviour is verified end to end on every build and release.
- Full mod artwork replacing the placeholder icon.

### Changed
- Widened Minecraft compatibility so one jar covers all of 26.1.x.
- Hyphenated version tags (e.g. `v1.2.0-rc.1`) now publish as GitHub pre-releases.

## [1.0.0] - 2026-06-07

### Added
- Initial release: look-driven climbing speed — glance up to ascend faster, look down to descend
  faster, stay level for vanilla movement.
- Chains are climbable and participate in the look-driven speed mechanic.
- Configurable acceleration curves and an optional vertical speed cap, with scaffolding support
  enabled by default.
- GitHub release workflow that attaches the built jar to each tagged release.

[1.2.0]: https://github.com/atperry7/ladder-sense/releases/tag/v1.2.0
[1.1.0]: https://github.com/atperry7/ladder-sense/releases/tag/v1.1.0
[1.0.0]: https://github.com/atperry7/ladder-sense/releases/tag/v1.0.0
