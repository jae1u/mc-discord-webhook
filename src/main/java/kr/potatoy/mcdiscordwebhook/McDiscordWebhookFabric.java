package kr.potatoy.mcdiscordwebhook;

//? if fabric {
import net.fabricmc.api.ModInitializer;

import static kr.potatoy.mcdiscordwebhook.McDiscordWebhook.*;

public class McDiscordWebhookFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");

        //? if !release
        //LOGGER.warn("I'm still a template!");

        //? if fapi: <0.100
        //LOGGER.info("Fabric API is old on this version");
    }
}
//?}