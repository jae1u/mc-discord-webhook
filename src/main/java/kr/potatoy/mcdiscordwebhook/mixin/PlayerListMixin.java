package kr.potatoy.mcdiscordwebhook.mixin;

import kr.potatoy.mcdiscordwebhook.DiscordWebhook;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"))
	private void onSystemMessage(Component message, boolean bypassHiddenChat, CallbackInfo info) {
		DiscordWebhook.send(message.getString());
	}
}
