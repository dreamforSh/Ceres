package com.xinian.ceres;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;
import com.xinian.ceres.common.compression.CeresCompressionType;

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
        public final ForgeConfigSpec.BooleanValue showDetailedStats;
        public final ForgeConfigSpec.IntValue statsUpdateFrequency;

        public ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("client");

            enableClientOptimizations = builder
                    .comment("Enable client-side network optimizations")
                    .define("enableClientOptimizations", true);

            clientPacketBatchDelay = builder
                    .comment("Delay in milliseconds before sending batched packets")
                    .defineInRange("clientPacketBatchDelay", 50, 0, 1000);

            clientPacketBatchSize = builder
                    .comment("Maximum number of packets to batch together")
                    .defineInRange("clientPacketBatchSize", 64, 1, 1024);

            showNetworkStats = builder
                    .comment("Show network statistics in F3 debug screen")
                    .define("showNetworkStats", false);

            showDetailedStats = builder
                    .comment("Show detailed compression statistics in F3 debug screen")
                    .define("showDetailedStats", false);

            statsUpdateFrequency = builder
                    .comment("How often to update statistics in ticks (20 ticks = 1 second)")
                    .defineInRange("statsUpdateFrequency", 20, 1, 100);

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
                    .comment("Enable server-side network optimizations")
                    .define("enableServerOptimizations", true);

            serverPacketBatchDelay = builder
                    .comment("Delay in milliseconds before sending batched packets")
                    .defineInRange("serverPacketBatchDelay", 20, 0, 1000);

            serverPacketBatchSize = builder
                    .comment("Maximum number of packets to batch together")
                    .defineInRange("serverPacketBatchSize", 128, 1, 2048);

            prioritizePlayerPositionPackets = builder
                    .comment("Prioritize player position packets over other types")
                    .define("prioritizePlayerPositionPackets", true);

            useChunkDeltaCompression = builder
                    .comment("Use delta compression for chunk updates")
                    .define("useChunkDeltaCompression", true);

            chunkUpdatePriority = builder
                    .comment("Priority for chunk update packets (higher values = higher priority)")
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


        public final ForgeConfigSpec.EnumValue<CompressionEngine> compressionEngine;
        public final ForgeConfigSpec.BooleanValue useNativeCompression;
        public final ForgeConfigSpec.IntValue advancedCompressionLevel;
        public final ForgeConfigSpec.EnumValue<CeresCompressionType> compressionFormat;
        public final ForgeConfigSpec.BooleanValue enableAdaptiveCompression;
        public final ForgeConfigSpec.IntValue adaptiveThreshold;
        public final ForgeConfigSpec.IntValue minPacketSizeToCompress;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("common");

            optimizationMode = builder
                    .comment("Network optimization mode: VANILLA (Forge network system) or MODERN (direct Netty)")
                    .defineEnum("optimizationMode", OptimizationMode.VANILLA);

            enableCompression = builder
                    .comment("Enable packet compression")
                    .define("enableCompression", true);

            compressionThreshold = builder
                    .comment("Minimum packet size in bytes before compression is applied")
                    .defineInRange("compressionThreshold", 256, 64, 8192);

            compressionLevel = builder
                    .comment("Compression level for standard Java compression (0-9)")
                    .defineInRange("compressionLevel", 3, 0, 9);

            enableLogging = builder
                    .comment("Enable detailed logging for debugging")
                    .define("enableLogging", false);

            enableCompatibilityMode = builder
                    .comment("Enable compatibility mode for problematic mods")
                    .define("enableCompatibilityMode", true);

            enableNettyOptimization = builder
                    .comment("Enable direct Netty optimizations (only used in MODERN mode)")
                    .define("enableNettyOptimization", true);

            enableDuplicateFiltering = builder
                    .comment("Filter duplicate packets to reduce network traffic")
                    .define("enableDuplicateFiltering", true);

            duplicateTimeoutMs = builder
                    .comment("Time in milliseconds before a duplicate packet can be sent again")
                    .defineInRange("duplicateTimeoutMs", 500, 100, 5000);

            maxConsecutiveDuplicates = builder
                    .comment("Maximum number of consecutive duplicate packets to filter")
                    .defineInRange("maxConsecutiveDuplicates", 3, 1, 20);

            filterPositionPackets = builder
                    .comment("Filter duplicate position packets")
                    .define("filterPositionPackets", true);

            filterChunkPackets = builder
                    .comment("Filter duplicate chunk packets")
                    .define("filterChunkPackets", true);

            filterEntityPackets = builder
                    .comment("Filter duplicate entity packets")
                    .define("filterEntityPackets", true);

            builder.push("advanced_compression");

            compressionEngine = builder
                    .comment("Compression engine to use: AUTO (select best), JAVA (built-in), LIBDEFLATE (high performance)")
                    .defineEnum("compressionEngine", CompressionEngine.AUTO);

            useNativeCompression = builder
                    .comment("Use native compression library if available (faster but may cause issues on some systems)")
                    .define("useNativeCompression", true);

            advancedCompressionLevel = builder
                    .comment("Compression level for LIBDEFLATE engine (0-12, higher = better compression but slower)")
                    .defineInRange("advancedCompressionLevel", 6, 0, 12);

            compressionFormat = builder
                    .comment("Compression format to use: DEFLATE, ZLIB, or GZIP")
                    .defineEnum("compressionFormat", CeresCompressionType.ZLIB);

            enableAdaptiveCompression = builder
                    .comment("Dynamically adjust compression based on network conditions")
                    .define("enableAdaptiveCompression", true);

            adaptiveThreshold = builder
                    .comment("Network congestion threshold to trigger more aggressive compression (ms)")
                    .defineInRange("adaptiveThreshold", 100, 10, 1000);

            minPacketSizeToCompress = builder
                    .comment("Minimum packet size to apply compression (bytes)")
                    .defineInRange("minPacketSizeToCompress", 256, 64, 8192);

            builder.pop(); // advanced_compression

            builder.pop(); // common
        }
    }

    public enum OptimizationMode {
        VANILLA,
        MODERN
    }

    public enum CompressionEngine {
        AUTO,       // 自动选择最佳引擎
        JAVA,       // 使用Java内置压缩
        LIBDEFLATE  // 使用libdeflate高性能压缩
    }
}
