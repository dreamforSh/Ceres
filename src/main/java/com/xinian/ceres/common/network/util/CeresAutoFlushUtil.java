package com.xinian.ceres.common.network.util;

import com.xinian.ceres.common.network.CeresConfigurableAutoFlush;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;

/**
 * 自动刷新工具类
 * 用于控制网络连接的自动刷新行为
 */
public class CeresAutoFlushUtil {
    /**
     * 为指定玩家设置自动刷新状态
     *
     * @param player 服务器玩家
     * @param val 是否启用自动刷新
     */
    public static void setAutoFlush(ServerPlayer player, boolean val) {
        if (player.getClass() == ServerPlayer.class) {
            CeresConfigurableAutoFlush configurableAutoFlusher = ((CeresConfigurableAutoFlush) player.connection.getConnection());
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    /**
     * 为指定连接设置自动刷新状态
     *
     * @param conn 网络连接
     * @param val 是否启用自动刷新
     */
    public static void setAutoFlush(Connection conn, boolean val) {
        if (conn.getClass() == Connection.class) {
            CeresConfigurableAutoFlush configurableAutoFlusher = ((CeresConfigurableAutoFlush) conn);
            configurableAutoFlusher.setShouldAutoFlush(val);
        }
    }

    /**
     * 手动刷新玩家的网络连接队列
     *
     * @param player 服务器玩家
     */
    public static void flushQueue(ServerPlayer player) {
        if (player.getClass() == ServerPlayer.class) {
            CeresConfigurableAutoFlush configurableAutoFlusher = ((CeresConfigurableAutoFlush) player.connection.getConnection());
            configurableAutoFlusher.flushQueue();
        }
    }

    /**
     * 手动刷新网络连接队列
     *
     * @param conn 网络连接
     */
    public static void flushQueue(Connection conn) {
        if (conn.getClass() == Connection.class) {
            CeresConfigurableAutoFlush configurableAutoFlusher = ((CeresConfigurableAutoFlush) conn);
            configurableAutoFlusher.flushQueue();
        }
    }


    private CeresAutoFlushUtil() {
    }
}
