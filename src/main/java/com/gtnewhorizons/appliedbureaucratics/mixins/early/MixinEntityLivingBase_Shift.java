package com.gtnewhorizons.appliedbureaucratics.mixins.early;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.appliedbureaucratics.workforce.Employment;
import com.gtnewhorizons.appliedbureaucratics.workforce.IEmployable;

/// Suspends a contracted mob's AI while it is on shift. See Note [Contracted mobs are held by mixins].
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase_Shift extends Entity implements IEmployable {

    @Unique
    private @Nullable Employment ab$employment;
    @Unique
    private boolean ab$employmentLoaded;

    private MixinEntityLivingBase_Shift(World world) {
        super(world);
    }

    @Shadow
    protected abstract void updateAITasks();

    @Shadow
    protected abstract void updateEntityActionState();

    @Override
    public @Nullable Employment ab$getEmployment() {
        if (!this.ab$employmentLoaded) {
            this.ab$employmentLoaded = true;
            this.ab$employment = Employment.load(this);
        }
        return this.ab$employment;
    }

    @Override
    public void ab$setEmployment(@Nullable Employment employment) {
        this.ab$employment = employment;
        this.ab$employmentLoaded = true;
        Employment.save(this, employment);
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void ab$reloadEmployment(NBTTagCompound tag, CallbackInfo ci) {
        this.ab$employmentLoaded = false;
    }

    @Redirect(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateAITasks()V"))
    private void ab$updateAITasks(EntityLivingBase self) {
        if (!Employment.isOnShift(self)) {
            this.updateAITasks();
        }
    }

    @Redirect(
        method = "onLivingUpdate",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;updateEntityActionState()V"))
    private void ab$updateEntityActionState(EntityLivingBase self) {
        if (!Employment.isOnShift(self)) {
            this.updateEntityActionState();
        }
    }
}
