package de.labystudio.desktopmodules.smarthome;

import com.google.gson.JsonObject;
import de.labystudio.desktopmodules.core.addon.Addon;
import de.labystudio.desktopmodules.smarthome.api.fritzbox.AsyncFritzBoxAPI;
import de.labystudio.desktopmodules.smarthome.api.hue.HueAPI;
import de.labystudio.desktopmodules.smarthome.modules.HomeIndicatorModule;
import de.labystudio.desktopmodules.smarthome.modules.HueModule;
import de.labystudio.desktopmodules.smarthome.modules.IPCameraModule;

public class SmartHomeAddon extends Addon {

    private final AsyncFritzBoxAPI fritzBox = new AsyncFritzBoxAPI();
    private final HueAPI hueAPI = new HueAPI();

    @Override
    public void onInitialize() throws Exception {
        // Register modules
        registerModule(HomeIndicatorModule.class);
        registerModule(IPCameraModule.class);
        registerModule(HueModule.class);

        // Save config to create default values
        saveConfig();
    }

    @Override
    public void onEnable() {
        JsonObject objectFritzBox = getConfigObject(this.config, "fritzbox");
        String address = getConfigValue(objectFritzBox, "address", "fritz.box");
        String password = getConfigValue(objectFritzBox, "password", "admin");
        int updateInterval = getConfigValue(objectFritzBox, "update_interval", 30);

        // Change address
        this.fritzBox.setAddress(address);

        // Connect
        try {
            this.fritzBox.connect(password, updateInterval);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObject objectHueBridge = getConfigObject(this.config, "hue_bridge");
        String addressBridge = getConfigValue(objectHueBridge, "address", "");
        String apiKey = getConfigValue(objectHueBridge, "api_key", "");

        if (addressBridge.isEmpty() || apiKey.isEmpty()) {
            // Setup bridge connection
            this.hueAPI.setup((authAddress, authApiKey) -> {
                // Store address and api key of bridge
                objectHueBridge.addProperty("address", authAddress);
                objectHueBridge.addProperty("api_key", authApiKey);
                saveConfig();

                // Connect to newly created connection
                this.hueAPI.connect(authAddress, authApiKey);
            });
        } else {
            this.hueAPI.connect(addressBridge, apiKey);
        }
    }

    @Override
    public void onDisable() {
        this.fritzBox.disconnect();
    }

    public AsyncFritzBoxAPI getFritzBox() {
        return this.fritzBox;
    }

    public HueAPI getHueAPI() {
        return hueAPI;
    }
}
