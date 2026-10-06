package kr.potatoy.mcdiscordwebhook.mixin;

import kr.potatoy.mcdiscordwebhook.DiscordWebhook;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "getSignedMessage", at = @At("RETURN"))
	private void onChatMessage(ServerboundChatPacket packet, LastSeenMessages lastSeenMessages, CallbackInfoReturnable<PlayerChatMessage> info) {
		String avatarUrl = "https://mc-heads.net/avatar/" + player.getUUID();
		DiscordWebhook.send(packet.message(), player.getScoreboardName(), avatarUrl);
	}
}
