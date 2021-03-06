package net.earthcomputer.multiconnect.protocols.v1_10.mixin;

import net.earthcomputer.multiconnect.impl.MixinHelper;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.HorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HorseEntity.class)
public interface HorseEntityAccessor {
    @Accessor("VARIANT")
    static TrackedData<Integer> getVariant() {
        return MixinHelper.fakeInstance();
    }
}
