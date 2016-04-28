package org.spongepowered.common.mixin.optimization.world.gen.structure;

import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.interfaces.world.IMixinWorld;

@Mixin(MapGenStructure.class)
public abstract class MixinMapGenStructure_Structure_Saving extends MapGenBase {

    @Redirect(method = "initializeStructureData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;loadItemData(Ljava/lang/Class;Ljava/lang/String;)Lnet/minecraft/world/WorldSavedData;"))
    private WorldSavedData checkLoadOrDefault(World world, Class<? extends WorldSavedData> structureClass, String structureName) {
        if (world instanceof IMixinWorld) {
            if (((IMixinWorld) world).getActiveConfig().getConfig().getStructures().structureMap.get(structureName)) {
                return world.loadItemData(structureClass, structureName);
            } else {
                return new MapGenStructureData(structureName);
            }
        } else {
            return world.loadItemData(structureClass, structureName);
        }

    }

}
