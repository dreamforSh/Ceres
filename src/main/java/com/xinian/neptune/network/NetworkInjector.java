package com.xinian.neptune.network;

import com.xinian.neptune.Neptune;
import com.xinian.neptune.NeptuneConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

@Mod.EventBusSubscriber(modid = Neptune.MOD_ID)
public class NetworkInjector {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            // 检查是否使用现代模式并启用了Netty优化
            if (!isNettyOptimizationEnabled()) {
                return;
            }

            // 获取服务器端的网络连接
            if (event.getEntity() instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer) event.getEntity();
                ServerGamePacketListenerImpl packetListener = player.connection;
                Connection networkManager = packetListener.getConnection();

                // 注入优化处理器
                NettyOptimizer.injectOptimizer(networkManager, NetworkDirection.PLAY_TO_CLIENT);

                Neptune.LOGGER.info("Injected packet optimizer for player: {}", player.getName().getString());
            }
        } catch (Exception e) {
            Neptune.LOGGER.error("Failed to inject packet optimizer on player login: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 可以在这里清理资源
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientConnected(ClientPlayerNetworkEvent.LoggingIn event) {
        try {
            // 检查是否使用现代模式并启用了Netty优化
            if (!isNettyOptimizationEnabled()) {
                return;
            }

            // 获取客户端的网络连接
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() != null) {
                Connection networkManager = minecraft.getConnection().getConnection();

                // 注入优化处理器
                NettyOptimizer.injectOptimizer(networkManager, NetworkDirection.PLAY_TO_SERVER);

                Neptune.LOGGER.info("Injected packet optimizer for client connection");
            }
        } catch (Exception e) {
            Neptune.LOGGER.error("Failed to inject packet optimizer on client connection: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientDisconnected(ClientPlayerNetworkEvent.LoggingOut event) {

    }


    private static boolean isNettyOptimizationEnabled() {
        return NeptuneConfig.COMMON.optimizationMode.get() == NeptuneConfig.OptimizationMode.MODERN &&
                NeptuneConfig.COMMON.enableNettyOptimization.get();
    }
}

