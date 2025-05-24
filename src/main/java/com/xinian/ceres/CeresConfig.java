package com.xinian.ceres;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = Ceres.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CeresConfig {
    public static final ClientConfig CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final CommonConfig COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<ClientConfig, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder()
                .configure(ClientConfig::new);
        CLIENT = clientSpecPair.getLeft();
        CLIENT_SPEC = clientSpecPair.getRight();

        final Pair<ServerConfig, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder()
                .configure(ServerConfig::new);
        SERVER = serverSpecPair.getLeft();
        SERVER_SPEC = serverSpecPair.getRight();

        final Pair<CommonConfig, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder()
                .configure(CommonConfig::new);
        COMMON = commonSpecPair.getLeft();
        COMMON_SPEC = commonSpecPair.getRight();
    }

    public static void init() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);

        Ceres.LOGGER.info("Ceres config initialized");
    }

    @SubscribeEvent
    public static void onClientConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.CLIENT) {
            Ceres.LOGGER.debug("Loaded Ceres client config");
        }
    }

    @SubscribeEvent
    public static void onServerConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getType() == ModConfig.Type.SERVER) {
            Ceres.LOGGER.debug("Loaded Ceres server config");
        }
    }

    @SubscribeEvent
    public static void onConfigReload(final ModConfigEvent.Reloading event) {
        Ceres.LOGGER.debug("Ceres config reloaded");
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.BooleanValue enableClientOptimizations;
        public final ForgeConfigSpec.IntValue clientPacketBatchDelay;
        public final ForgeConfigSpec.IntValue clientPacketBatchSize;
        public final ForgeConfigSpec.BooleanValue showNetworkStats;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("client");

            enableClientOptimizations = builder
                    .define("enableClientOptimizations", true);

            clientPacketBatchDelay = builder
                    .defineInRange("clientPacketBatchDelay", 50, 0, 1000);

            clientPacketBatchSize = builder
                    .defineInRange("clientPacketBatchSize", 64, 1, 1024);

            showNetworkStats = builder
                    .define("showNetworkStats", false);

            builder.pop();
        }
    }

    public static class ServerConfig {
        public final ForgeConfigSpec.BooleanValue enableServerOptimizations;
        public final ForgeConfigSpec.IntValue serverPacketBatchDelay;
        public final ForgeConfigSpec.IntValue serverPacketBatchSize;
        public final ForgeConfigSpec.BooleanValue prioritizePlayerPositionPackets;
        public final ForgeConfigSpec.BooleanValue useChunkDeltaCompression;
        public final ForgeConfigSpec.IntValue chunkUpdatePriority;

        public ServerConfig(ForgeConfigSpec.Builder builder) {
            builder.push("server");

            enableServerOptimizations = builder
                    .define("enableServerOptimizations", true);

            serverPacketBatchDelay = builder
                    .defineInRange("serverPacketBatchDelay", 20, 0, 1000);

            serverPacketBatchSize = builder
                    .defineInRange("serverPacketBatchSize", 128, 1, 2048);

            prioritizePlayerPositionPackets = builder
                    .define("prioritizePlayerPositionPackets", true);

            useChunkDeltaCompression = builder
                    .define("useChunkDeltaCompression", true);

            chunkUpdatePriority = builder
                    .defineInRange("chunkUpdatePriority", 5, 0, 10);

            builder.pop();
        }
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.BooleanValue enableCompression;
        public final ForgeConfigSpec.IntValue compressionThreshold;
        public final ForgeConfigSpec.IntValue compressionLevel;
        public final ForgeConfigSpec.BooleanValue enableLogging;
        public final ForgeConfigSpec.BooleanValue enableCompatibilityMode;
        public final ForgeConfigSpec.BooleanValue enableNettyOptimization;
        public final ForgeConfigSpec.BooleanValue enableDuplicateFiltering;
        public final ForgeConfigSpec.IntValue duplicateTimeoutMs;
        public final ForgeConfigSpec.IntValue maxConsecutiveDuplicates;
        public final ForgeConfigSpec.BooleanValue filterPositionPackets;
        public final ForgeConfigSpec.BooleanValue filterChunkPackets;
        public final ForgeConfigSpec.BooleanValue filterEntityPackets;
        public final ForgeConfigSpec.EnumValue<OptimizationMode> optimizationMode;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("common");

            optimizationMode = builder
                    .comment("Network optimization mode: VANILLA (Forge network system) or MODERN (direct Netty)")
                    .defineEnum("optimizationMode", OptimizationMode.VANILLA);

            enableCompression = builder
                    .define("enableCompression", true);

            compressionThreshold = builder
                    .defineInRange("compressionThreshold", 256, 64, 8192);

            compressionLevel = builder
                    .defineInRange("compressionLevel", 3, 0, 9);

            enableLogging = builder
                    .define("enableLogging", false);

            enableCompatibilityMode = builder
                    .define("enableCompatibilityMode", true);

            enableNettyOptimization = builder
                    .comment("Only used in MODERN mode")
                    .define("enableNettyOptimization", true);

            enableDuplicateFiltering = builder
                    .define("enableDuplicateFiltering", true);

            duplicateTimeoutMs = builder
                    .defineInRange("duplicateTimeoutMs", 500, 100, 5000);

            maxConsecutiveDuplicates = builder
                    .defineInRange("maxConsecutiveDuplicates", 3, 1, 20);

            filterPositionPackets = builder
                    .define("filterPositionPackets", true);

            filterChunkPackets = builder
                    .define("filterChunkPackets", true);

            filterEntityPackets = builder
                    .define("filterEntityPackets", true);

            builder.pop();
        }
    }

    public enum OptimizationMode {
        VANILLA,
        MODERN
    }
}