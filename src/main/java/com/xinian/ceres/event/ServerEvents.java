package com.xinian.ceres.event;

import com.xinian.ceres.Ceres;
import com.xinian.ceres.command.CeresCompressionCommand;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Ceres.MOD_ID)
public class ServerEvents {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        Ceres.LOGGER.info("Registering Ceres commands");
        CeresCompressionCommand.register(event.getServer().getCommands().getDispatcher());
    }
}

