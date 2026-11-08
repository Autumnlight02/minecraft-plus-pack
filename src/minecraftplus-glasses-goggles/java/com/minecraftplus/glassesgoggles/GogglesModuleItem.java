package com.minecraftplus.glassesgoggles;

import de.srendi.advancedperipherals.common.smartglasses.SmartGlassesSideAccess;
import de.srendi.advancedperipherals.common.smartglasses.modules.IModule;
import de.srendi.advancedperipherals.common.smartglasses.modules.IModuleFunctions;
import de.srendi.advancedperipherals.common.smartglasses.modules.IModuleItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class GogglesModuleItem extends Item implements IModuleItem<GogglesModuleItem.Module> {
    private static final ResourceLocation MODULE_ID = ResourceLocation.fromNamespaceAndPath(GlassesGoggles.MOD_ID, "goggles");
    private static final Component TOOLTIP =
        Component.translatable("item.mcplus_glasses_goggles.goggles_module.tooltip").withStyle(ChatFormatting.GRAY);

    public GogglesModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ResourceLocation moduleId() {
        return MODULE_ID;
    }

    @Override
    public Module createModule(SmartGlassesSideAccess access) {
        return Module.INSTANCE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(TOOLTIP);
    }

    public static final class Module implements IModule {
        private static final Module INSTANCE = new Module();

        @Override
        public ResourceLocation getId() {
            return MODULE_ID;
        }

        @Override
        public IModuleFunctions getFunctions(SmartGlassesSideAccess access) {
            return null;
        }
    }
}
