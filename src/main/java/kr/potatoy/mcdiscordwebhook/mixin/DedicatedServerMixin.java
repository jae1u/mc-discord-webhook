package kr.potatoy.mcdiscordwebhook.mixin;

import kr.potatoy.mcdiscordwebhook.DiscordWebhook;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
	@Inject(method = "initServer", at = @At("RETURN"))
	private void onStarted(CallbackInfoReturnable<Boolean> info) {
		if (info.getReturnValueZ()) DiscordWebhook.send("Server started");
	}

	@Inject(method = "stopServer", at = @At("HEAD"))
	private void onStopping(CallbackInfo info) {
		DiscordWebhook.send("Server closed");
		DiscordWebhook.close();
	}
}
