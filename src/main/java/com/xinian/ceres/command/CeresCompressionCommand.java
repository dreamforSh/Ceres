package com.xinian.ceres.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xinian.ceres.Ceres;
import com.xinian.ceres.CeresConfig;
import com.xinian.ceres.CeresConfig.CommonConfig.*;
import com.xinian.ceres.common.compression.CeresCompressionManager;
import com.xinian.ceres.common.compression.CompressionBenchmark;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


public class CeresCompressionCommand {


    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ceres")
                        .requires(source -> source.hasPermission(2)) // 需要OP权限
                        .then(Commands.literal("compression")
                                .then(Commands.literal("stats")
                                        .executes(CeresCompressionCommand::showStats))
                                .then(Commands.literal("reset")
                                        .executes(CeresCompressionCommand::resetStats))
                                .then(Commands.literal("engine")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("JAVA");
                                                    builder.suggest("LIBDEFLATE");
                                                    builder.suggest("AUTO");
                                                    return builder.buildFuture();
                                                })
                                                .executes(CeresCompressionCommand::setEngine)))
                                .then(Commands.literal("benchmark")
                                        .executes(CeresCompressionCommand::runBenchmark))
                        )
        );
    }


    private static int showStats(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                Component.literal("Compression stats: " + CeresCompressionManager.getCompressionStats()),
                false
        );
        return 1;
    }


    private static int resetStats(CommandContext<CommandSourceStack> context) {
        CeresCompressionManager.resetStats();
        context.getSource().sendSuccess(
                Component.literal("Compression stats reset"),
                false
        );
        return 1;
    }


    private static int setEngine(CommandContext<CommandSourceStack> context) {
        String engineName = StringArgumentType.getString(context, "type");
        try {
            CeresConfig.CompressionEngine engine = CeresConfig.CompressionEngine.valueOf(engineName.toUpperCase());

            // TODO: 实现CeresCompressionManager.setEngine方法

            Ceres.LOGGER.info("Requested to change compression engine to: {}", engine);

            context.getSource().sendSuccess(
                    Component.literal("Compression engine change requested: " + engine + " (not implemented yet)"),
                    true
            );
            return 1;
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(
                    Component.literal("Invalid compression engine: " + engineName)
            );
            return 0;
        }
    }

    /**
     * 运行压缩基准测试
     */
    private static int runBenchmark(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                Component.literal("Running compression benchmark..."),
                false
        );

        // 如果是玩家执行命令，向玩家发送结果
        if (context.getSource().getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            CompressionBenchmark.runBenchmarkForPlayer(player);
        } else {
            // 否则，记录到控制台
            CompressionBenchmark.runBenchmark();
        }

        return 1;
    }
}

