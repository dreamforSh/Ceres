package com.xinian.ceres.mixin.network.pipeline.encryption;

import com.xinian.ceres.common.network.CeresClientConnectionEncryptionExtension;
import com.xinian.ceres.common.network.pipeline.CeresMinecraftCipherDecoder;
import com.xinian.ceres.common.network.pipeline.CeresMinecraftCipherEncoder;
import com.xinian.ceres.common.network.util.CeresNatives;
import com.xinian.ceres.common.network.util.CeresNatives.CeresCipher;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;

@Mixin(Connection.class)
public class CeresConnectionMixin implements CeresClientConnectionEncryptionExtension {
    @Shadow
    private boolean encrypted;
    @Shadow
    private Channel channel;

    @Override
    public void setupEncryption(SecretKey key) throws GeneralSecurityException {
        if (!this.encrypted) {
            CeresCipher decryption = CeresNatives.cipher.forDecryption(key);
            CeresCipher encryption = CeresNatives.cipher.forEncryption(key);

            this.encrypted = true;
            this.channel.pipeline().addBefore("splitter", "decrypt", new CeresMinecraftCipherDecoder(decryption));
            this.channel.pipeline().addBefore("prepender", "encrypt", new CeresMinecraftCipherEncoder(encryption));
        }
    }
}
