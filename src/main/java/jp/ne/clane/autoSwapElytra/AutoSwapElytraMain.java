package jp.ne.clane.autoSwapElytra;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jp.ne.clane.autoSwapElytra.commons.ClientUtils;
import jp.ne.clane.autoSwapElytra.commons.EnchantmentUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;

@Mod(AutoSwapElytraMain.MOD_ID)
public class AutoSwapElytraMain {
	public static final String MOD_ID = "autoswapelytra";
	public static final String MOD_NAME = "AutoSwapElytra";
	public static final String[] MOD_AUTHORS = {"ALFEECLARE@CLANE SOFTWARE"};
	private static final Logger log = LogManager.getLogger(MOD_ID);
	public static Minecraft mc;

	private static AutoSwapElytraMain instance;

	private AutoSwapElytraConfig config;

	public static void log(String message) {
		log.info("[{}] {}", log.getName(), message);
	}

	/**
	 * Reload modules
	 */
	public AutoSwapElytraMain modules() {
		try {
			mc = Minecraft.getInstance();
			if (mc.levelRenderer != null)
					mc.levelRenderer.allChanged();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		return this;
	}

	public AutoSwapElytraMain(IEventBus modEventBus, ModContainer modContainer) {
		instance = this;
		IEventBus bus = modEventBus;
		bus.addListener(this::setup);

		log(MOD_NAME + " Started");
	}

	/**
	 * get this mod
	 */
	public static AutoSwapElytraMain getMod() {
		return instance;
	}

	private void setup(final FMLCommonSetupEvent event) {
		setupCompatibility();
		config = new AutoSwapElytraConfig();
		try {
			config.loadConfig(config.getConfigFile());
		} catch (IllegalAccessException | IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		try {
			config.saveConfig(config.getConfigFile());
		} catch (IllegalAccessException | IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}
	
    private void setupCompatibility() {
    	LoadingModList modlist = FMLLoader.getLoadingModList();
    	ClientUtils.ipn = modlist.getModFileById("inventoryprofilesnext");
    }

    private Minecraft getMC() {
		if (mc == null) {	
			try {
				mc = Minecraft.getInstance();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
		return mc;
	}
	
	public static enum ItemSearchType {
    	CHESTARMOR,ELYTRA,FIREWORKS
    }
    
    public static boolean isSearchingItemType(Item item, ItemSearchType type) {
        return switch (item) {
	        case Item armor when type == ItemSearchType.CHESTARMOR                  -> EnchantmentUtils.isChestPlateItem(armor);
	        case Item armor when type == ItemSearchType.ELYTRA                      -> EnchantmentUtils.isElytraItem(armor);
	        case FireworkRocketItem fireworks when type == ItemSearchType.FIREWORKS -> true;
	        default                                                                 -> false;
        };
    }
}
