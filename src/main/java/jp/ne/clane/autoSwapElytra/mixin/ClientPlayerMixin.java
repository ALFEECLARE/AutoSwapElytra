package jp.ne.clane.autoSwapElytra.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;

import jp.ne.clane.autoSwapElytra.AutoSwapElytraConfig;
import jp.ne.clane.autoSwapElytra.AutoSwapElytraConfig.SwapMode;
import jp.ne.clane.autoSwapElytra.AutoSwapElytraMain;
import jp.ne.clane.autoSwapElytra.AutoSwapElytraMain.ItemSearchType;
import jp.ne.clane.autoSwapElytra.commons.ClientUtils;
import jp.ne.clane.autoSwapElytra.commons.ClientUtils.InventoryType;
import jp.ne.clane.autoSwapElytra.commons.EnchantmentUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;

@Mixin(value = LocalPlayer.class)
public class ClientPlayerMixin extends AbstractClientPlayer {
	private static final int HEAD_SLOT = EquipmentSlot.HEAD.getIndex();
	private static final int CHEST_SLOT = EquipmentSlot.CHEST.getIndex();
	private static final int LEGS_SLOT = EquipmentSlot.LEGS.getIndex();
	private static final int FEET_SLOT = EquipmentSlot.FEET.getIndex();
	private static final int OFFHAND = EquipmentSlot.OFFHAND.getIndex();
	private Pair<InventoryType,Integer> previousSwappedArmor = null;
	private Pair<InventoryType,Integer> previousSwappedFireworksPair = null;
	private Pair<InventoryType,Integer> previousSwappedHotbarPair = null;
	private boolean isFlyAfter = false;

    public ClientPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onClimbable()Z"))
    private boolean allowFlightFromLava(LocalPlayer localPlayer) {
        return onClimbable() || isInLava();
    }

    private boolean isFlightStarttable() {
        return !this.onGround() && !this.isFallFlying() && !this.isInWater() && !this.hasEffect(MobEffects.LEVITATION);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z"))
    private void tryEquipElytra(CallbackInfo callbackinfo) {
        if (isFlightStarttable()) {
            Inventory inventory = this.getInventory();

            // エリトラ装備済みなら何もしない
            if (isFlyAfter || EnchantmentUtils.isElytraItem(inventory.getItem(CHEST_SLOT).getItem()))
            	return;

            // 交換対象のエリトラを選択
            Pair<InventoryType, Integer> elytraSlotPair = findItemInAllSlot(inventory, ItemSearchType.ELYTRA);
            if (elytraSlotPair == null)
            	return;
            ClientUtils.swapPlayerInventorySlot(this, ClientUtils.convertSlotIdFromEquipmentId(EquipmentSlot.CHEST), ClientUtils.convertSlotIdFromInventoryPair(elytraSlotPair));
            previousSwappedArmor = elytraSlotPair;
            isFlyAfter = true;
            if (AutoSwapElytraConfig.isSwapFireworks) {
            	// メインハンドかオフハンドに花火を持っているなら前回履歴をクリアして終了
            	if (inventory.getSelectedItem().getItem() instanceof FireworkRocketItem || ClientUtils.getInventoryFromInventoryType(InventoryType.OFFHAND, inventory).getFirst().getItem() instanceof FireworkRocketItem) {
            		previousSwappedFireworksPair = null;
            		previousSwappedHotbarPair = null;
					return;
            	}
            	// 交換対象の花火を探す
                Pair<InventoryType, Integer> fireworksSlotPair = findItemInAllSlot(inventory, ItemSearchType.FIREWORKS);
                if (fireworksSlotPair == null)
                	return;
                Pair<InventoryType, Integer> fireworksSwappedHotbarPair = getForeworksSwapTargetSlot(inventory.getSelectedSlot());
                ClientUtils.swapPlayerInventorySlot(this, ClientUtils.convertSlotIdFromInventoryPair(fireworksSwappedHotbarPair), ClientUtils.convertSlotIdFromInventoryPair(fireworksSlotPair));
                previousSwappedFireworksPair = fireworksSlotPair;
                previousSwappedHotbarPair = fireworksSwappedHotbarPair;
            }
        }
    }
    

    @Inject(method = "aiStep", at = @At(value = "TAIL"))
    private void tryUnequipElytra(CallbackInfo callbackinfo) {
    	Inventory inventory = this.getInventory();

        // 飛行直後でない、降下中、及び既にエリトラを外している(何もなしと鎧装備済みの両方)、このMod以外の方法でエリトラを外した場合、何もしない
        if (!isFlyAfter || this.isFallFlying() || !EnchantmentUtils.isElytraItem(ClientUtils.getInventoryFromInventoryType(InventoryType.ARMOR, inventory).get(CHEST_SLOT).getItem()) || previousSwappedArmor == null)
        	return;

        // 交換対象の鎧を選択
        Pair<InventoryType, Integer> armorSlotPair = findItemInAllSlot(inventory, ItemSearchType.CHESTARMOR);
        if (armorSlotPair == null)
        	return;
        ClientUtils.swapPlayerInventorySlot(this, ClientUtils.convertSlotIdFromEquipmentId(EquipmentSlot.CHEST), ClientUtils.convertSlotIdFromInventoryPair(armorSlotPair));
        previousSwappedArmor = armorSlotPair;
        isFlyAfter = false;
        if (AutoSwapElytraConfig.isSwapFireworks) {
        	if (previousSwappedFireworksPair == null || previousSwappedHotbarPair == null) {
        		previousSwappedFireworksPair = null;
        		previousSwappedHotbarPair = null;
        		return;
        	}
        	ClientUtils.swapPlayerInventorySlot(this, ClientUtils.convertSlotIdFromInventoryPair(previousSwappedHotbarPair), ClientUtils.convertSlotIdFromInventoryPair(previousSwappedFireworksPair));
            previousSwappedFireworksPair = null;
            previousSwappedHotbarPair = null;
        }
    }

    private Pair<InventoryType, Integer> findItemInAllSlot(Inventory inventory, ItemSearchType type) {
		Pair<InventoryType, Integer> answer = null;
		int maxScore = 0;
		switch (AutoSwapElytraConfig.armorSwapMode) {
			case SwapMode.VALUE:
				break;
			case SwapMode.PREUSED:
				if (previousSwappedArmor != null && AutoSwapElytraMain.isSearchingItemType(ClientUtils.getItemStackFromInventoryPair(previousSwappedArmor, inventory).getItem(), type))
					return previousSwappedArmor;
				break;
			case SwapMode.OFFHAND:
				ItemStack offhandItem = ClientUtils.getInventoryFromInventoryType(InventoryType.OFFHAND, inventory).getFirst();
				if (offhandItem != ItemStack.EMPTY && AutoSwapElytraMain.isSearchingItemType(offhandItem.getItem(), type))
					return new Pair<InventoryType, Integer>(InventoryType.OFFHAND,0);
				break;
			case SwapMode.SLOT:
				ItemStack slotItem = ClientUtils.getInventoryFromInventoryType(InventoryType.INVENTORY, inventory).get(AutoSwapElytraConfig.swapSlot);
				if (slotItem != ItemStack.EMPTY && AutoSwapElytraMain.isSearchingItemType(slotItem.getItem(), type))
					return new Pair<InventoryType, Integer>(InventoryType.INVENTORY,AutoSwapElytraConfig.swapSlot);
				break;
		}
    	for ( InventoryType inventoryType : List.of(InventoryType.INVENTORY, InventoryType.OFFHAND)) {
    		NonNullList<ItemStack> searchSlots = ClientUtils.getInventoryFromInventoryType(inventoryType, inventory);
	        for (int slot = 0; slot < searchSlots.size(); slot++) {
	    		int currentScore = 0;
	            ItemStack stack = searchSlots.get(slot);
	            Item item = stack.getItem();
	            if (!AutoSwapElytraMain.isSearchingItemType(item, type))
	            	continue;
	            if (item.components().has(DataComponents.EQUIPPABLE) && !AutoSwapElytraConfig.ignoreArmorTier)
	            	currentScore += getArmorMaterialPoint(inventory, EnchantmentUtils.getMaterial(item));
	            if (item.components().has(DataComponents.EQUIPPABLE)) {
		            currentScore += EnchantmentUtils.getEnchantmentLevel(clientLevel, stack, Enchantments.MENDING) * 500;
		            currentScore += EnchantmentUtils.getEnchantmentLevel(clientLevel, stack, Enchantments.UNBREAKING) * 50;
		            currentScore += EnchantmentUtils.getEnchantmentLevel(clientLevel, stack, Enchantments.PROTECTION) * 100;
		            currentScore += EnchantmentUtils.getEnchantmentLevel(clientLevel, stack, Enchantments.BLAST_PROTECTION) * 100;
		            currentScore += EnchantmentUtils.getEnchantmentLevel(clientLevel, stack, Enchantments.FIRE_PROTECTION) * 100;
		            currentScore += EnchantmentUtils.getEnchantmentLevel(clientLevel, stack, Enchantments.PROJECTILE_PROTECTION) * 100;
		            currentScore += EnchantmentUtils.getEnchantmentLevel(clientLevel, stack, Enchantments.BINDING_CURSE) * -999999;
		            currentScore += EnchantmentUtils.getEnchantmentLevel(clientLevel, stack, Enchantments.VANISHING_CURSE) * -1000;
		            currentScore += (stack.getMaxDamage() - stack.getDamageValue()) + stack.getMaxDamage() / 100;
		            if (stack.getMaxDamage() - stack.getDamageValue() < 10)
		            	currentScore = stack.getMaxDamage() - stack.getDamageValue();
	            }
	            if (stack.getMaxStackSize() > 1) {
	            	if (item instanceof FireworkRocketItem) {
		            	currentScore += (AutoSwapElytraConfig.isUseSmallFireworksStack ? stack.getMaxStackSize() - stack.getCount() : stack.getCount()) * 100;
	            		Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
	            		if (fireworks != null) {
	            			currentScore += fireworks.flightDuration() * 10;
	            			currentScore += fireworks.explosions().size() * -30;
	            		}
	            	} else {
		            	currentScore += stack.getCount() * 100;
	            	}
	            }
	            if (currentScore < 0)
	            	continue;
	            if (answer == null || maxScore < currentScore) {
	            	answer = new Pair<InventoryType, Integer>(inventoryType, slot);
	            	maxScore = currentScore;
	            }
	        }
    	}
    	return answer;
    }

	private int getArmorMaterialPoint(Inventory inventory, ResourceKey<EquipmentAsset> material) {
		if (material == EquipmentAssets.NETHERITE) { return 3000; } else
		if (material == EquipmentAssets.DIAMOND)   { return 2000; } else
		if (material == EquipmentAssets.GOLD)      {
				if (this.clientLevel.dimension() == Level.NETHER && isWearNoGoldArmor(inventory)) {
					return 3000;
				} else {
					return 0;
				}
		} else
		if (material == EquipmentAssets.IRON)      { return 1000; } else 
		if (material == EquipmentAssets.CHAINMAIL) { return 500;  } else 
		if (material == EquipmentAssets.LEATHER)   { return 0;  } 
		else                                       { return 2500; } //mod素材
	}
	
	private final boolean isWearNoGoldArmor(Inventory inventory) {
		for (EquipmentSlot armorSlot : EquipmentSlot.values()) {
			Item item = ClientUtils.getInventoryFromInventoryType(InventoryType.ARMOR, inventory).get(armorSlot.getIndex()).getItem(); 
			if (!(item.components().has(DataComponents.EQUIPPABLE)))
				continue;
			if (EnchantmentUtils.getMaterial(item) == EquipmentAssets.GOLD)
				return false;
		}
		return true;
	}

	private Pair<InventoryType, Integer> getForeworksSwapTargetSlot(int mainhandSelected) {
		return AutoSwapElytraConfig.isFireworksOffhand ? new Pair<InventoryType, Integer>(InventoryType.OFFHAND,0) : new Pair<InventoryType, Integer>(InventoryType.INVENTORY, mainhandSelected);		
	}
}
