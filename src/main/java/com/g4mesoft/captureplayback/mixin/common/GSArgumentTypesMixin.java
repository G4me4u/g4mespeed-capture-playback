package com.g4mesoft.captureplayback.mixin.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.g4mesoft.captureplayback.module.server.GSAssetHandleArgumentType;
import com.mojang.brigadier.arguments.ArgumentType;

import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.command.argument.serialize.ArgumentSerializer.ArgumentTypeProperties;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.registry.Registry;

@Mixin(ArgumentTypes.class)
public class GSArgumentTypesMixin {

	@Shadow protected static native <A extends ArgumentType<?>, T extends ArgumentTypeProperties<A>> ArgumentSerializer<A, T> register(
			Registry<ArgumentSerializer<?, ?>> registry, String id, Class<? extends A> clazz, ArgumentSerializer<A, T> serializer);
	
	@Inject(
		method =
			"Lnet/minecraft/command/argument/ArgumentTypes;register(" +
				"Lnet/minecraft/registry/Registry;" +
			")Lnet/minecraft/command/argument/serialize/ArgumentSerializer;",
		at = @At("HEAD")
	)
	private static void onRegister(Registry<ArgumentSerializer<?, ?>> registry, CallbackInfoReturnable<ArgumentSerializer<?, ?>> cir) {
		register(registry, "g4mespeed-capture-playback:assethandle", GSAssetHandleArgumentType.class, ConstantArgumentSerializer.of(GSAssetHandleArgumentType::handle));
	}
}
