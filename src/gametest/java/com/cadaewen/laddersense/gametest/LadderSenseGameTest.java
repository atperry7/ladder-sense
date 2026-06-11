package com.cadaewen.laddersense.gametest;

import com.cadaewen.laddersense.ClimbSpeed;
import com.cadaewen.laddersense.config.LadderSenseConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Behaviour tests for the paths a Minecraft version port could silently break:
 * the climbable tag merge (one bad required entry nukes the whole merged tag),
 * the pitch-to-speed rule for ladders and scaffolding, and config gating.
 *
 * <p>All tests run synchronously on the server thread, so config mutations are
 * restored before any other test can observe them.
 */
public class LadderSenseGameTest {

	/** Vanilla upward boost while engaging a climbable (mirrors ClimbSpeed.VANILLA_ASCENT). */
	private static final double VANILLA_ASCENT = 0.2;
	/** Vanilla downward slide clamp on a climbable (mirrors ClimbSpeed.VANILLA_DESCENT). */
	private static final double VANILLA_DESCENT = 0.15;

	@GameTest
	public void chainsAreClimbableAndVanillaTagSurvives(GameTestHelper helper) {
		// The vanilla entries surviving proves our merged tag file didn't nuke the whole tag.
		helper.assertTrue(Blocks.LADDER.defaultBlockState().is(BlockTags.CLIMBABLE),
			"ladder lost the climbable tag - the merged climbable.json likely has a bad entry");
		helper.assertTrue(Blocks.VINE.defaultBlockState().is(BlockTags.CLIMBABLE),
			"vine lost the climbable tag - the merged climbable.json likely has a bad entry");
		// Every chain (iron + the copper family) should be climbable via our #minecraft:chains entry.
		int chains = 0;
		for (Holder<Block> chain : BuiltInRegistries.BLOCK.getTagOrEmpty(BlockTags.CHAINS)) {
			chains++;
			helper.assertTrue(chain.value().defaultBlockState().is(BlockTags.CLIMBABLE),
				chain.getRegisteredName() + " is in #minecraft:chains but not climbable");
		}
		helper.assertTrue(chains > 0, "#minecraft:chains is empty - did the tag get renamed again?");
		helper.succeed();
	}

	@GameTest
	public void lookingUpAcceleratesAscent(GameTestHelper helper) {
		BlockPos base = new BlockPos(2, 1, 2);
		buildLadderColumn(helper, base, 5);
		ServerPlayer player = climbingPlayer(helper, base, -80.0F);
		double y = ClimbSpeed.adjustClimbY(player, VANILLA_ASCENT, true);
		helper.assertTrue(y > VANILLA_ASCENT,
			"looking up while engaging a ladder should ascend faster than vanilla, got " + y);
		helper.succeed();
	}

	@GameTest
	public void ascentTapersAtTopOfClimb(GameTestHelper helper) {
		// Only two ladders: nothing climbable two blocks above the player's head,
		// so the accelerated ascent must ease back to vanilla for a clean exit.
		BlockPos base = new BlockPos(2, 1, 2);
		buildLadderColumn(helper, base, 2);
		ServerPlayer player = climbingPlayer(helper, base, -80.0F);
		double y = ClimbSpeed.adjustClimbY(player, VANILLA_ASCENT, true);
		helper.assertValueEqual(y, VANILLA_ASCENT, "ascent speed at the top of a climb");
		helper.succeed();
	}

	@GameTest
	public void deadZoneKeepsVanillaSpeed(GameTestHelper helper) {
		BlockPos base = new BlockPos(2, 1, 2);
		buildLadderColumn(helper, base, 5);
		ServerPlayer player = climbingPlayer(helper, base, 0.0F);
		double sentinel = 0.05;
		helper.assertValueEqual(ClimbSpeed.adjustClimbY(player, sentinel, true), sentinel,
			"climb speed while looking level (inside the dead zone)");
		helper.assertValueEqual(ClimbSpeed.adjustClimbY(player, sentinel, false), sentinel,
			"slide speed while looking level (inside the dead zone)");
		helper.succeed();
	}

	@GameTest
	public void lookingDownAcceleratesDescent(GameTestHelper helper) {
		BlockPos base = new BlockPos(2, 1, 2);
		buildLadderColumn(helper, base, 5);
		ServerPlayer player = climbingPlayer(helper, base, 80.0F);
		double y = ClimbSpeed.adjustClimbY(player, -VANILLA_DESCENT, false);
		helper.assertTrue(y < -VANILLA_DESCENT,
			"looking down while sliding should descend faster than vanilla, got " + y);
		helper.succeed();
	}

	@GameTest
	public void sneakParksOnLadder(GameTestHelper helper) {
		BlockPos base = new BlockPos(2, 1, 2);
		buildLadderColumn(helper, base, 5);
		ServerPlayer player = climbingPlayer(helper, base, 80.0F);
		player.setShiftKeyDown(true);
		helper.assertValueEqual(ClimbSpeed.adjustClimbY(player, 0.0, false), 0.0,
			"vertical speed while sneak-parked on a ladder");
		helper.succeed();
	}

	@GameTest
	public void disabledModKeepsEverythingVanilla(GameTestHelper helper) {
		BlockPos base = new BlockPos(2, 1, 2);
		buildLadderColumn(helper, base, 5);
		ServerPlayer player = climbingPlayer(helper, base, -80.0F);
		LadderSenseConfig cfg = LadderSenseConfig.get();
		boolean wasEnabled = cfg.enabled;
		try {
			cfg.enabled = false;
			helper.assertValueEqual(ClimbSpeed.adjustClimbY(player, VANILLA_ASCENT, true), VANILLA_ASCENT,
				"climb speed with the mod disabled");
		} finally {
			cfg.enabled = wasEnabled;
		}
		helper.succeed();
	}

	@GameTest
	public void speedCapClampsAscent(GameTestHelper helper) {
		BlockPos base = new BlockPos(2, 1, 2);
		buildLadderColumn(helper, base, 5);
		ServerPlayer player = climbingPlayer(helper, base, -80.0F);
		LadderSenseConfig cfg = LadderSenseConfig.get();
		double oldCap = cfg.maxVerticalSpeedCap;
		try {
			cfg.maxVerticalSpeedCap = 0.25;
			double y = ClimbSpeed.adjustClimbY(player, VANILLA_ASCENT, true);
			helper.assertTrue(y > VANILLA_ASCENT && y <= 0.25,
				"capped ascent should sit between vanilla and the cap, got " + y);
		} finally {
			cfg.maxVerticalSpeedCap = oldCap;
		}
		helper.succeed();
	}

	@GameTest
	public void scaffoldingFollowsItsOwnControls(GameTestHelper helper) {
		// A column, not a single block: the top-of-climb taper eases ascent back to
		// vanilla unless the climbable continues above the player's head.
		BlockPos pos = new BlockPos(4, 2, 4);
		helper.setBlock(pos.below(), Blocks.STONE);
		for (int i = 0; i < 4; i++) {
			helper.setBlock(pos.above(i), Blocks.SCAFFOLDING);
		}
		ServerPlayer player = climbingPlayer(helper, pos, 80.0F);

		// On scaffolding, sneak descends (it does not park as on a ladder).
		player.setShiftKeyDown(true);
		double down = ClimbSpeed.adjustClimbY(player, -VANILLA_DESCENT, false);
		helper.assertTrue(down < -VANILLA_DESCENT,
			"sneaking on scaffolding while looking down should descend faster than vanilla, got " + down);

		// Jumping (pressingIn) while looking up climbs faster.
		player.setShiftKeyDown(false);
		player.setXRot(-80.0F);
		double up = ClimbSpeed.adjustClimbY(player, VANILLA_ASCENT, true);
		helper.assertTrue(up > VANILLA_ASCENT,
			"jumping in scaffolding while looking up should ascend faster than vanilla, got " + up);

		// affectScaffolding=false hands scaffolding back to vanilla entirely.
		LadderSenseConfig cfg = LadderSenseConfig.get();
		boolean oldAffect = cfg.affectScaffolding;
		try {
			cfg.affectScaffolding = false;
			helper.assertValueEqual(ClimbSpeed.adjustClimbY(player, VANILLA_ASCENT, true), VANILLA_ASCENT,
				"scaffolding climb speed with affectScaffolding=false");
		} finally {
			cfg.affectScaffolding = oldAffect;
		}
		helper.succeed();
	}

	/** A mock player standing at the given structure-relative position, looking at the given pitch. */
	@SuppressWarnings("removal") // makeMockServerPlayerInLevel is deprecated-for-removal but is currently the only ServerPlayer mock.
	private static ServerPlayer climbingPlayer(GameTestHelper helper, BlockPos rel, float pitch) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		Vec3 pos = helper.absoluteVec(new Vec3(rel.getX() + 0.5, rel.getY(), rel.getZ() + 0.5));
		player.setPos(pos.x, pos.y, pos.z);
		player.setXRot(pitch);
		return player;
	}

	/** A ladder column of the given height, with a stone wall behind it for support. */
	private static void buildLadderColumn(GameTestHelper helper, BlockPos base, int height) {
		for (int i = 0; i < height; i++) {
			helper.setBlock(base.above(i).south(), Blocks.STONE);
			helper.setBlock(base.above(i), Blocks.LADDER);
		}
	}
}
