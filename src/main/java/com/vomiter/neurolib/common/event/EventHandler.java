package com.vomiter.neurolib.common.event;

import com.vomiter.neurolib.common.command.ModCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class EventHandler {
    public static void init(){
        final IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(EventHandler::onRegisterCommands);
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommand.register(event.getDispatcher());
    }


}
