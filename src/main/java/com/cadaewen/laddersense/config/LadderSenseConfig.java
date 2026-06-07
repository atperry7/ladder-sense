package com.cadaewen.laddersense.config;

import com.cadaewen.laddersense.LadderSense;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * User-tunable settings for Ladder Sense, persisted as JSON in the Fabric config directory
 * ({@code config/ladder-sense.json}). The file is created with defaults on first launch.
 */
public class LadderSenseConfig {

	/** How quickly speed ramps from vanilla (at the dead-zone edge) to the maximum (at +/-90 degrees). */
	public enum RampCurve {
		/** Constant rate of increase across the pitch range. */
		LINEAR,
		/** Smoothstep: eases in and out for a gentle feel. */
		SMOOTH,
		/** Ease-out: speed climbs quickly with a little look, then tapers. */
		AGGRESSIVE
	}

	/** Master switch. When false the mod is dormant and vanilla climbing is untouched. */
	public boolean enabled = true;

	/** Maximum upward speed multiplier, reached when looking nearly straight up. */
	public double maxAscentMultiplier = 3.0;

	/** Maximum downward speed multiplier, reached when looking nearly straight down. */
	public double maxDescentMultiplier = 4.0;

	/** Half-width of the neutral zone (degrees). Within +/- this pitch, movement stays vanilla. */
	public double deadZoneDegrees = 15.0;

	/** Shape of the speed ramp outside the dead zone. */
	public RampCurve rampCurve = RampCurve.SMOOTH;

	/** Whether scaffolding is affected. On by default; scaffolding climbs with jump (up) and sneak (down). */
	public boolean affectScaffolding = true;

	// --- Accessibility ---

	/** Motion comfort: when true, any pitch past the dead zone jumps straight to the max multiplier (fixed tiers). */
	public boolean disableAccelerationCurves = false;

	/** Hard ceiling on vertical climb speed in blocks/tick. 0 disables the cap. */
	public double maxVerticalSpeedCap = 0.0;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static LadderSenseConfig instance = new LadderSenseConfig();

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("ladder-sense.json");
	}

	/** The active config. Always non-null; returns defaults until {@link #load()} runs. */
	public static LadderSenseConfig get() {
		return instance;
	}

	/** Loads the config from disk (creating it with defaults if missing) and writes back any sanitized values. */
	public static void load() {
		Path path = configPath();
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					LadderSenseConfig loaded = GSON.fromJson(reader, LadderSenseConfig.class);
					if (loaded != null) {
						instance = loaded;
					}
				}
			}
		} catch (Exception e) {
			LadderSense.LOGGER.warn("Failed to read {}, falling back to defaults", path, e);
			instance = new LadderSenseConfig();
		}
		instance.sanitize();
		save();
	}

	/** Writes the current config to disk. */
	public static void save() {
		Path path = configPath();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			LadderSense.LOGGER.warn("Failed to write {}", path, e);
		}
	}

	/** Clamps values into sane ranges so a hand-edited file can't break movement. */
	private void sanitize() {
		if (maxAscentMultiplier < 1.0) maxAscentMultiplier = 1.0;
		if (maxDescentMultiplier < 1.0) maxDescentMultiplier = 1.0;
		if (deadZoneDegrees < 0.0) deadZoneDegrees = 0.0;
		if (deadZoneDegrees > 89.0) deadZoneDegrees = 89.0;
		if (maxVerticalSpeedCap < 0.0) maxVerticalSpeedCap = 0.0;
		if (rampCurve == null) rampCurve = RampCurve.SMOOTH;
	}
}
