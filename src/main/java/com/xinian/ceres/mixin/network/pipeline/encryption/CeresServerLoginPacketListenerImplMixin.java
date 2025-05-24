package com.xinian.ceres.mixin.network.pipeline.encryption;

import com.xinian.ceres.common.network.CeresClientConnectionEncryptionExtension;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Key;

@Mixin(ServerLoginPacketListenerImpl.class)
public class CeresServerLoginPacketListenerImplMixin {
    @Shadow
    @Final
    public Connection connection;

    @Redirect(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Crypt;getCipher(ILjava/security/Key;)Ljavax/crypto/Cipher;"))
    private Cipher onKey$initializeVelocityCipher(int ignored1, Key secretKey) throws GeneralSecurityException {

        ((CeresClientConnectionEncryptionExtension) this.connection).setupEncryption((SecretKey) secretKey);


        return null;
    }

    @Redirect(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V"))
    public void onKey$ignoreMinecraftEncryptionPipelineInjection(Connection connection, Cipher ignored1, Cipher ignored2) {

    }
}


