package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSIWorldAccess;

import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

@Mixin(RedstoneWireBlock.class)
public class GSRedstoneWireBlockMixin {

	@Inject(
		method = "getReceivedRedstonePower",
		allow = 1,
		at = @At(
			value = "INVOKE",
			shift = Shift.BEFORE,
			target =
				"Lnet/minecraft/world/World;getReceivedRedstonePower(" +
					"Lnet/minecraft/util/math/BlockPos;" +
				")I"
		)
	)
	private void onGetReceivedRedstonePowerBeforePowerCheck(World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		((GSIWorldAccess)world).gcp_requestPlaybackPower(1);
	}
}
