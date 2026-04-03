package com.vomiter.neurolib;

import com.mojang.logging.LogUtils;
import com.vomiter.neurolib.common.event.EventHandler;
import com.vomiter.neurolib.common.registry.ModRegistries;
import com.vomiter.neurolib.data.ModDataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NeuroLib.MOD_ID)
public class NeuroLib
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "neurolib";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean DEBUG_MODE = false;


    public static ResourceLocation modLoc(String path){
        return Helpers.id(NeuroLib.MOD_ID, path);
    }

    public NeuroLib(FMLJavaModLoadingContext context) {
        EventHandler.init();
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::commonSetup);
        modBus.addListener(ModDataGenerator::generateData);
        ModRegistries.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

}
