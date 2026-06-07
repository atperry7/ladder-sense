package com.cadaewen.laddersense.mixin;

import com.cadaewen.laddersense.ClimbSpeed;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks the two points in {@link LivingEntity} that govern climbing speed and reroutes the vertical
 * velocity through {@link ClimbSpeed}:
 *
 * <ul>
 *   <li>{@code handleOnClimbable} produces the actual per-tick velocity (and clamps the downward slide),
 *       so it drives descent speed and the instantaneous ascent.</li>
 *   <li>{@code handleRelativeFrictionAndCalculateMovement} refreshes the persistent upward boost
 *       (vanilla {@code 0.2}), so it sustains an accelerated ascent across ticks.</li>
 * </ul>
 *
 * Both run on client and server (common code), matching the design's server-authoritative goal while
 * still working in single-player.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Shadow
	protected boolean jumping;

	@Inject(method = "handleOnClimbable", at = @At("RETURN"), cancellable = true)
	private void laddersense$adjustClimbMovement(Vec3 delta, CallbackInfoReturnable<Vec3> cir) {
		applyLadderSense(cir);
	}

	@Inject(method = "handleRelativeFrictionAndCalculateMovement", at = @At("RETURN"), cancellable = true)
	private void laddersense$adjustClimbBoost(Vec3 input, float friction, CallbackInfoReturnable<Vec3> cir) {
		applyLadderSense(cir);
	}

	private void applyLadderSense(CallbackInfoReturnable<Vec3> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.onClimbable()) {
			return;
		}
		Vec3 current = cir.getReturnValue();
		// horizontalCollision is a public field inherited from Entity; read it directly rather than
		// @Shadow-ing it, since the shadow attach only searches the declared target class.
		boolean pressingIn = self.horizontalCollision || this.jumping;
		double newY = ClimbSpeed.adjustClimbY(self, current.y, pressingIn);
		if (newY != current.y) {
			cir.setReturnValue(new Vec3(current.x, newY, current.z));
		}
	}
}
