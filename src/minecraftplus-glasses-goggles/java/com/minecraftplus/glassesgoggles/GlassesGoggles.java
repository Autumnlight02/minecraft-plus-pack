package com.minecraftplus.glassesgoggles;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import de.srendi.advancedperipherals.common.items.SmartGlassesItem;
import de.srendi.advancedperipherals.common.smartglasses.SmartGlassesItemHandler;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

@Mod(GlassesGoggles.MOD_ID)
public final class GlassesGoggles {
    public static final String MOD_ID = "mcplus_glasses_goggles";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public GlassesGoggles(IEventBus modBus) {
        ITEMS.register("goggles_module", () -> new GogglesModuleItem(new Item.Properties()));
        ITEMS.register(modBus);
        GogglesItem.addIsWearingPredicate(GlassesGoggles::wearsGlassesWithGogglesModule);
    }

    // The Accessories compat layer maps the Curios "head" slot to its "hat" slot.
    static boolean wearsGlassesWithGogglesModule(Player player) {
        if (hasGogglesModule(player.getItemBySlot(EquipmentSlot.HEAD))) return true;
        ICuriosItemHandler curios = CuriosCapability.INVENTORY.getCapability(player, null);
        if (curios == null) return false;
        ICurioStacksHandler head = curios.getCurios().get("head");
        if (head == null) return false;
        IDynamicStackHandler stacks = head.getStacks();
        for (int slot = 0; slot < stacks.getSlots(); slot++) {
            if (hasGogglesModule(stacks.getStackInSlot(slot))) return true;
        }
        return false;
    }

    private static boolean hasGogglesModule(ItemStack glasses) {
        if (!(glasses.getItem() instanceof SmartGlassesItem)) return false;
        for (ItemStack installed : SmartGlassesItemHandler.loadItems(glasses).getAllUnsafe()) {
            if (installed.getItem() instanceof GogglesModuleItem) return true;
        }
        return false;
    }
}
