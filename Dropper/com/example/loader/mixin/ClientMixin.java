package com.example.loader.mixin;

import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({class_310.class})
public class ClientMixin {
  @Inject(at = {@At("HEAD")}, method = {"tick()V"})
  private void onTick(CallbackInfo info) {}
}
