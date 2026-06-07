package com.cadaewen.laddersense;

import com.cadaewen.laddersense.config.LadderSenseConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

/**
 * Core Ladder Sense rule: turn the player's look pitch into a vertical climbing speed.
 *
 * <p>Vanilla climbing has two relevant constants in {@code LivingEntity}: an upward boost of
 * {@code 0.2} blocks/tick when engaging a ladder, and a downward slide clamped to {@code 0.15}
 * blocks/tick. We scale those baselines by a pitch-driven multiplier so that looking up speeds
 * the ascent and looking down speeds the descent, while a configurable dead zone preserves the
 * vanilla feel when looking roughly level.
 */
public final class ClimbSpeed {

	private ClimbSpeed() {
	}

	/** Vanilla upward boost applied while engaging a climbable (LivingEntity#handleRelativeFrictionAndCalculateMovement). */
	private static final double VANILLA_ASCENT = 0.2;
	/** Vanilla downward slide clamp on a climbable (LivingEntity#handleOnClimbable). */
	private static final double VANILLA_DESCENT = 0.15;

	/**
	 * Returns the vertical velocity that climbing should use this tick.
	 *
	 * @param entity     the climbing entity
	 * @param vanillaY   the vertical velocity vanilla computed (returned unchanged when the mod stays out of the way)
	 * @param pressingIn whether the entity is actively engaging the climbable (colliding into it or jumping). This is
	 *                   the player's climb-up intent: while engaging, they always ascend (looking up speeds it).
	 */
	public static double adjustClimbY(LivingEntity entity, double vanillaY, boolean pressingIn) {
		LadderSenseConfig cfg = LadderSenseConfig.get();
		if (!cfg.enabled) return vanillaY;

		// Only the player drives the look-to-speed mechanic; mobs keep vanilla climbing.
		if (!(entity instanceof Player)) return vanillaY;

		// Scaffolding moves differently from ladders (sneak descends instead of parking), so it has its own path.
		if (entity.getInBlockState().is(Blocks.SCAFFOLDING)) {
			return cfg.affectScaffolding ? adjustScaffolding(entity, vanillaY, pressingIn, cfg) : vanillaY;
		}

		return adjustLadder(entity, vanillaY, pressingIn, cfg);
	}

	/** Ladders, vines, chains, etc.: hold into them (or jump) to climb; release and slide to descend; sneak parks. */
	private static double adjustLadder(LivingEntity entity, double vanillaY, boolean pressingIn, LadderSenseConfig cfg) {
		// Sneaking parks the player on a ladder; honour that intent and stay vanilla.
		if (entity.isSuppressingSlidingDownLadder()) return vanillaY;

		float pitch = entity.getXRot(); // -90 = straight up, +90 = straight down
		if (pressingIn) {
			// Climb intent: holding "up" always climbs, and never gets blocked by looking down.
			return accelerateAscent(entity, vanillaY, pitch, cfg);
		}
		// Sliding: looking down accelerates the descent.
		return accelerateDescent(vanillaY, pitch, cfg);
	}

	/**
	 * Scaffolding: jump (or press in) to climb up, sneak to descend — unlike ladders, where sneaking parks you.
	 * When neither key is held the player is standing on or crossing the top, so vertical movement is left untouched.
	 */
	private static double adjustScaffolding(LivingEntity entity, double vanillaY, boolean pressingIn, LadderSenseConfig cfg) {
		float pitch = entity.getXRot();
		if (pressingIn) {
			// Going up: looking up speeds the climb (same trigger vanilla uses for the scaffolding boost).
			return accelerateAscent(entity, vanillaY, pitch, cfg);
		}
		if (entity.isSuppressingSlidingDownLadder()) {
			// Going down: on scaffolding, sneak is the descent control, so looking down speeds it.
			return accelerateDescent(vanillaY, pitch, cfg);
		}
		// Neither climbing nor descending (standing on / walking across the top): leave movement alone.
		return vanillaY;
	}

	/** Scales upward speed by how far above the dead zone the player is looking, tapering near the top of a climb. */
	private static double accelerateAscent(LivingEntity entity, double vanillaY, float pitch, LadderSenseConfig cfg) {
		double deadZone = cfg.deadZoneDegrees;
		if (pitch < -deadZone) {
			double t = progress(-pitch, deadZone);
			double speed = cap(VANILLA_ASCENT * multiplier(t, cfg.maxAscentMultiplier, cfg), cfg);
			return taperNearTop(entity, speed);
		}
		return vanillaY;
	}

	/** Scales downward speed by how far below the dead zone the player is looking. */
	private static double accelerateDescent(double vanillaY, float pitch, LadderSenseConfig cfg) {
		double deadZone = cfg.deadZoneDegrees;
		if (pitch > deadZone) {
			double t = progress(pitch, deadZone);
			return -cap(VANILLA_DESCENT * multiplier(t, cfg.maxDescentMultiplier, cfg), cfg);
		}
		return vanillaY;
	}

	/**
	 * Prevents the accelerated ascent from flinging the player off the top of a ladder. Once the climbable no longer
	 * continues above the player's head, the extra speed has nowhere to go, so we fall back to the gentle vanilla
	 * ascent for a clean exit. Speeds at or below vanilla are left untouched.
	 */
	private static double taperNearTop(LivingEntity entity, double ascentSpeed) {
		if (ascentSpeed <= VANILLA_ASCENT) return ascentSpeed;
		boolean continuesAbove = entity.level()
			.getBlockState(entity.blockPosition().above(2))
			.is(BlockTags.CLIMBABLE);
		return continuesAbove ? ascentSpeed : VANILLA_ASCENT;
	}

	/** Normalises how far past the dead zone a pitch is, to [0, 1]. */
	private static double progress(double absPitch, double deadZone) {
		double t = (absPitch - deadZone) / (90.0 - deadZone);
		return Math.max(0.0, Math.min(1.0, t));
	}

	/** Maps ramp progress [0,1] to a speed multiplier in [1, max] using the configured curve. */
	private static double multiplier(double t, double max, LadderSenseConfig cfg) {
		if (cfg.disableAccelerationCurves) {
			// Fixed tier: full multiplier the moment pitch leaves the dead zone.
			return max;
		}
		double curved = switch (cfg.rampCurve) {
			case LINEAR -> t;
			case SMOOTH -> t * t * (3.0 - 2.0 * t);             // smoothstep
			case AGGRESSIVE -> 1.0 - (1.0 - t) * (1.0 - t);     // ease-out: rises quickly
		};
		return 1.0 + (max - 1.0) * curved;
	}

	/** Applies the optional absolute speed cap (operating on the magnitude). */
	private static double cap(double speed, LadderSenseConfig cfg) {
		if (cfg.maxVerticalSpeedCap > 0.0) {
			return Math.min(speed, cfg.maxVerticalSpeedCap);
		}
		return speed;
	}
}
