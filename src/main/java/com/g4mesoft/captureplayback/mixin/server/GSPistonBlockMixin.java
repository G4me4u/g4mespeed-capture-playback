package com.g4mesoft.captureplayback.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;

import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(PistonBlock.class)
public abstract class GSPistonBlockMixin {

	@Inject(method = "shouldExtend", cancellable = true, at = @At("HEAD"))
	private void onShouldExtendHead(World world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
		// TODO: handle this in the world class instead.
		if (!world.isClient && ((GSIServerWorldAccess)world).isPlaybackPowering(pos)) {
			cir.setReturnValue(true);
			cir.cancel();
		}
	}
}
