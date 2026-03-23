package com.vomiter.neurolib;

import com.mojang.logging.LogUtils;
import com.vomiter.neurolib.common.event.EventHandler;
import com.vomiter.neurolib.data.ModDataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(NeuroLib.MOD_ID)
public class NeuroLib
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "neurolib";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean DEBUG_MODE = true;


    public static ResourceLocation modLoc(String path){
        return Helpers.id(NeuroLib.MOD_ID, path);
    }

    public NeuroLib(ModContainer mod, IEventBus modBus) {
        EventHandler.init();
        modBus.addListener(this::commonSetup);
        modBus.addListener(ModDataGenerator::generateData);
        mod.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

}
