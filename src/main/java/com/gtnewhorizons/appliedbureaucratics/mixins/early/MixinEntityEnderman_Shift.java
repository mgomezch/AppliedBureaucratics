package com.gtnewhorizons.appliedbureaucratics.mixins.early;

import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.world.GameRules;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;

/// An Enderman on shift stays put, whatever damages, wets or looks at it. See Note [Contracted mobs are held by
/// mixins].
@Mixin(EntityEnderman.class)
public abstract class MixinEntityEnderman_Shift {

    @Inject(method = "teleportTo(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void ab$stayOnShift(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Employment.isOnShift((EntityEnderman) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    /// A manager does not rearrange the office.
    @Redirect(
        method = "onLivingUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/GameRules;getGameRuleBooleanValue(Ljava/lang/String;)Z"))
    private boolean ab$noGriefingOnShift(GameRules rules, String rule) {
        return !Employment.isOnShift((EntityEnderman) (Object) this) && rules.getGameRuleBooleanValue(rule);
    }
}
