package com.g4mesoft.captureplayback.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.access.GSIServerWorldAccess;

import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(PistonBlock.class)
public abstract class GSPistonBlockMixin {

	@Shadow protected abstract boolean shouldExtend(World world, BlockPos pos, Direction pistonFace);
	
	@Inject(method = "tryMove", cancellable = true, at = @At("HEAD"))
	private void onTryMoveHead(World world, BlockPos pos, BlockState state, CallbackInfo ci) {
		if (!world.isClient && ((GSIServerWorldAccess)world).isPlaybackPosition(pos))
			ci.cancel();
	}
	
	@Inject(method = "shouldExtend", cancellable = true, at = @At("HEAD"))
	private void onShouldExtendHead(World world, BlockPos pos, Direction pistonFace, CallbackInfoReturnable<Boolean> cir) {
		if (!world.isClient && ((GSIServerWorldAccess)world).isPlaybackPosition(pos)) {
			cir.setReturnValue(world.getBlockState(pos).get(PistonBlock.EXTENDED));
			cir.cancel();
		}
	}
	
	@Redirect(method = "onSyncedBlockEvent", at = @At(value = "INVOKE", 
			target = "Lnet/minecraft/block/PistonBlock;shouldExtend(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"))
	public boolean onShouldExtendRedirect(PistonBlock block, World world, BlockPos pos, Direction pistonFace,
			BlockState state, World world2, BlockPos pos2, int type, int data) {
		
		if (!world.isClient && ((GSIServerWorldAccess)world).isPlaybackPosition(pos))
			return (type == 0);
		
		return shouldExtend(world, pos, pistonFace);
	}
}
