package jp.ne.clane.autoSwapElytra.commons;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;

public class EnchantmentUtils {
	public static int getEnchantmentLevel(ClientLevel clientLevel, ItemStack itemStack, ResourceKey<Enchantment> enchant) {
		return itemStack.getEnchantmentLevel(getEnchantmentHolder(clientLevel, enchant));
	}
	
	private static Holder<Enchantment> getEnchantmentHolder(ClientLevel clientLevel, ResourceKey<Enchantment> enchant) {
		return clientLevel.registryAccess().holderOrThrow(enchant);
	}
	
	public static boolean isElytraItem(Item armor) {
    	DataComponentMap itemComponents = armor.components();
		if (!(itemComponents.has(DataComponents.EQUIPPABLE)))
			return false;
    	return itemComponents.get(DataComponents.EQUIPPABLE).slot().getId() == ArmorType.CHESTPLATE.getSlot().getId() && (itemComponents.has(DataComponents.GLIDER));
	}

	public static boolean isChestPlateItem(Item armor) {
    	DataComponentMap itemComponents = armor.components();
		if (!(itemComponents.has(DataComponents.EQUIPPABLE)))
			return false;
    	return itemComponents.get(DataComponents.EQUIPPABLE).slot().getId() == ArmorType.CHESTPLATE.getSlot().getId() && !(itemComponents.has(DataComponents.GLIDER));
	}
	
	public static ResourceKey<EquipmentAsset> getMaterial(Item item) {
    	return item.components().get(DataComponents.EQUIPPABLE).assetId().get();
	}
}
