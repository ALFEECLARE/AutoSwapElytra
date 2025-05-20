package jp.ne.clane.autoSwapElytra.commons;

import org.anti_ad.mc.ipn.api.access.IPN;

import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;

public class ClientUtils {
    // 互換性維持
    public static ModFileInfo ipn = null;

    public static int convertSlotIdFromInventoryPair(Pair<InventoryType, Integer> inventoryPair) {
		return convertSlotIdFromInventoryPair(inventoryPair.getFirst(), inventoryPair.getSecond());
	}
	
	//ホットバー コンテナ:0-8 -> メニュー:36-44
	//インベントリ コンテナ:9-35 -> メニュー 9-35 ※つまり変更不要
	//アーマー コンテナ:0-3 -> メニュー 5-8 ただし順番が逆
	//オフハンド コンテナ 40 -> メニュー 45
	public static int convertSlotIdFromInventoryPair(InventoryType inventoryType, int inventoryIndex) {
		return switch (inventoryType) {
			case InventoryType.OFFHAND   -> InventoryMenu.SHIELD_SLOT;
			case InventoryType.ARMOR     -> InventoryMenu.ARMOR_SLOT_START+ (InventoryMenu.ARMOR_SLOT_COUNT - 1 - inventoryIndex);
			case InventoryType.INVENTORY -> inventoryIndex < Inventory.getSelectionSize() ? InventoryMenu.USE_ROW_SLOT_START + inventoryIndex : inventoryIndex ;
		};
	}

	public static int convertSlotIdFromEquipmentId(EquipmentSlot equipmentSlot) {
		return InventoryMenu.ARMOR_SLOT_START + (InventoryMenu.ARMOR_SLOT_COUNT - 1 - equipmentSlot.getIndex());
	}
		
	public static int convertSlotIdFromHotbarId(int inventoryIndex) {
		return InventoryMenu.USE_ROW_SLOT_START + inventoryIndex;
	}

	public static void swapPlayerInventorySlot(AbstractClientPlayer localPlayer,int fromSlotId, int toSlotId) {
		Minecraft mc = Minecraft.getInstance();
		//IPNのswapが装備品スロットの入れ替えに対応してないくさい？
		//暫定対応。装備スロットとの入れ替えの場合は問答無用でバニラ方式。どっちかしかない場合は装備したままで問題ない可能性が高いので、大きな問題はない
    	if (ipn == null || fromSlotId < 9 || toSlotId < 9) {
    	//if (ipn == null) {
			mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, toSlotId,   0, ClickType.PICKUP, localPlayer);
			mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, fromSlotId, 0, ClickType.PICKUP, localPlayer);
			mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, toSlotId,   0, ClickType.PICKUP, localPlayer);
    	} else {
    		IPN.getInstance().getContainerClicker().swap(toSlotId, fromSlotId);
    		//IPN.getInstance().getContainerClicker().leftClick(toSlotId);
    		//IPN.getInstance().getContainerClicker().leftClick(fromSlotId);
    		//IPN.getInstance().getContainerClicker().leftClick(toSlotId);
    	}
	}
	
    public static ItemStack getItemStackFromInventoryPair(Pair<InventoryType, Integer> inventoryPair, Inventory inventory) {
    	return getItemStackFromInventoryPair(inventoryPair.getFirst(), inventoryPair.getSecond(), inventory);
	}

    public static ItemStack getItemStackFromInventoryPair(InventoryType inventoryType, int inventoryIndex, Inventory inventory) {
    	return getInventoryFromInventoryType(inventoryType, inventory).get(inventoryIndex);
	}

    public static final NonNullList<ItemStack> getInventoryFromInventoryType(InventoryType inventoryType, Inventory inventory) {
		return switch (inventoryType) {
			case InventoryType.OFFHAND -> inventory.offhand;
			case InventoryType.ARMOR   -> inventory.armor;
			case InventoryType.INVENTORY -> inventory.items;
		};
	}

    public static enum InventoryType {
    	OFFHAND,ARMOR,INVENTORY
    }

}
