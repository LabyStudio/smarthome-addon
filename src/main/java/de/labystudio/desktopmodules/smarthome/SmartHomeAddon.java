package de.labystudio.desktopmodules.smarthome;

import com.google.gson.JsonObject;
import de.labystudio.desktopmodules.core.addon.Addon;
import de.labystudio.desktopmodules.smarthome.api.fritzbox.AsyncFritzBoxAPI;
import de.labystudio.desktopmodules.smarthome.modules.HomeIndicatorModule;
import de.labystudio.desktopmodules.smarthome.modules.IPCameraModule;

public class SmartHomeAddon extends Addon {

    private final AsyncFritzBoxAPI fritzBox = new AsyncFritzBoxAPI();

    @Override
    public void onInitialize() throws Exception {
        // Register modules
        registerModule(HomeIndicatorModule.class);
        registerModule(IPCameraModule.class);

        // Save config to create default values
        saveConfig();
    }

    @Override
    public void onEnable() {
        JsonObject object = getConfigObject(this.config, "fritzbox");
        String address = getConfigValue(object, "address", "fritz.box");
        String username = getConfigValue(object, "username", "admin");
        String password = getConfigValue(object, "password", "admin");
        int updateInterval = getConfigValue(object, "update_interval", 30);

        // Change address
        this.fritzBox.setAddress(address);

        // Connect
        this.fritzBox.connect(username, password, updateInterval);
    }

    @Override
    public void onDisable() {
        this.fritzBox.disconnect();
    }

    public AsyncFritzBoxAPI getFritzBox() {
        return this.fritzBox;
    }
}
