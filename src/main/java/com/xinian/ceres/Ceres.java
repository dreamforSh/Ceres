package com.xinian.ceres;

import com.xinian.ceres.network.CompressedDataPacket;
import com.xinian.ceres.network.DuplicatePacketFilter;
import com.xinian.ceres.network.NetworkOptimizer;
import com.xinian.ceres.network.NettyOptimizer;
import com.xinian.ceres.network.PacketCompressor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Ceres.MOD_ID)
public class Ceres {
    public static final String MOD_ID = "ceres";
    public static final String VERSION = "0.2.1";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static final String PROTOCOL_VERSION = "0.2.1";

    public static final SimpleChannel NETWORK;
    static {
        ResourceLocation channelName = makeResourceLocation();
        NETWORK = NetworkRegistry.newSimpleChannel(
                channelName,
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
    }

    private static ResourceLocation makeResourceLocation() {
        return new ResourceLocation(MOD_ID, "main");
    }

    private boolean isModernMode;

    public Ceres() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::serverSetup);

        MinecraftForge.EVENT_BUS.register(this);

        CeresConfig.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down Ceres network optimizations");
            if (isModernMode) {
                NettyOptimizer.shutdown();
            }
        }));

        LOGGER.info("Ceres network optimization mod initialized - Version: {}", VERSION);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Ceres common setup");

        isModernMode = CeresConfig.COMMON.optimizationMode.get() == CeresConfig.OptimizationMode.MODERN;
        LOGGER.info("Using {} optimization mode", isModernMode ? "MODERN (Netty)" : "VANILLA (Forge)");

        event.enqueueWork(() -> {
            registerNetworkMessages();
            initNetworkOptimizations();

            if (isModernMode) {
                if (CeresConfig.COMMON.enableNettyOptimization.get()) {
                    NettyOptimizer.init();
                    LOGGER.info("Modern mode: Netty optimizer initialized");
                }
            } else {
                NetworkOptimizer.init();
                LOGGER.info("Vanilla mode: Network optimizer initialized");
            }
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Ceres client setup");

        if (CeresConfig.CLIENT.enableClientOptimizations.get()) {
            event.enqueueWork(this::initClientNetworkOptimizations);
        } else {
            LOGGER.info("Client-side network optimizations are disabled in config");
        }
    }

    private void serverSetup(final FMLDedicatedServerSetupEvent event) {
        LOGGER.info("Ceres server setup");

        if (CeresConfig.SERVER.enableServerOptimizations.get()) {
            event.enqueueWork(this::initServerNetworkOptimizations);
        } else {
            LOGGER.info("Server-side network optimizations are disabled in config");
        }
    }

    private void registerNetworkMessages() {
        LOGGER.info("Registering Ceres network messages");
        int id = 0;

        NETWORK.registerMessage(id++,
                CompressedDataPacket.class,
                CompressedDataPacket::encode,
                CompressedDataPacket::decode,
                CompressedDataPacket::handle);

        LOGGER.info("Registered {} network messages", id);
    }

    private void initNetworkOptimizations() {
        LOGGER.info("Initializing common network optimizations");

        boolean enableCompression = CeresConfig.COMMON.enableCompression.get();
        int compressionThreshold = CeresConfig.COMMON.compressionThreshold.get();
        int compressionLevel = CeresConfig.COMMON.compressionLevel.get();
        boolean enableNettyOptimization = CeresConfig.COMMON.enableNettyOptimization.get();
        boolean enableDuplicateFiltering = CeresConfig.COMMON.enableDuplicateFiltering.get();

        LOGGER.info("Compression enabled: {}, threshold: {} bytes, level: {}",
                enableCompression, compressionThreshold, compressionLevel);

        if (isModernMode) {
            LOGGER.info("Modern mode: Netty optimization enabled: {}", enableNettyOptimization);
        }

        LOGGER.info("Duplicate packet filtering enabled: {}", enableDuplicateFiltering);

        PacketCompressor.resetStats();
        if (isModernMode) {
            NettyOptimizer.resetStats();
        }
        DuplicatePacketFilter.resetStats();

        if (enableCompression) {
            LOGGER.info("Packet compression initialized");
        }

        if (enableDuplicateFiltering) {
            LOGGER.info("Duplicate packet filtering initialized");
        }
    }

    private void initClientNetworkOptimizations() {
        LOGGER.info("Initializing client-side network optimizations");

        int batchDelay = CeresConfig.CLIENT.clientPacketBatchDelay.get();
        int batchSize = CeresConfig.CLIENT.clientPacketBatchSize.get();
        boolean showStats = CeresConfig.CLIENT.showNetworkStats.get();

        LOGGER.info("Client packet batch delay: {}ms, batch size: {}, show stats: {}",
                batchDelay, batchSize, showStats);
    }

    private void initServerNetworkOptimizations() {
        LOGGER.info("Initializing server-side network optimizations");

        int batchDelay = CeresConfig.SERVER.serverPacketBatchDelay.get();
        int batchSize = CeresConfig.SERVER.serverPacketBatchSize.get();
        boolean prioritizePosition = CeresConfig.SERVER.prioritizePlayerPositionPackets.get();
        boolean useDeltaCompression = CeresConfig.SERVER.useChunkDeltaCompression.get();

        LOGGER.info("Server packet batch delay: {}ms, batch size: {}", batchDelay, batchSize);
        LOGGER.info("Prioritize player position: {}, use chunk delta compression: {}",
                prioritizePosition, useDeltaCompression);
    }
}
