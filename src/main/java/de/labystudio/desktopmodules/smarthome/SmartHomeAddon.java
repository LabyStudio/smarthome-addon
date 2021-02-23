package de.labystudio.desktopmodules.smarthome;

import com.google.gson.JsonObject;
import de.labystudio.desktopmodules.core.addon.Addon;
import de.labystudio.desktopmodules.smarthome.api.fritzbox.AsyncFritzBoxAPI;
import de.labystudio.desktopmodules.smarthome.modules.HomeIndicatorModule;
import de.labystudio.desktopmodules.smarthome.modules.SurveillanceCameraModule;

public class SmartHomeAddon extends Addon {

    private final AsyncFritzBoxAPI fritzBox = new AsyncFritzBoxAPI();

    @Override
    public void onInitialize() throws Exception {
        // Register modules
        registerModule(HomeIndicatorModule.class);
        registerModule(SurveillanceCameraModule.class);

        // Save config to create default values
        saveConfig();
    }

    @Override
    public void onEnable() {
        JsonObject object = getConfigObject(this.config, "fritzbox");
        String address = getConfigValue(object, "address", "fritz.box");
        String password = getConfigValue(object, "password", "admin");

        // Change address
        this.fritzBox.setAddress(address);

        // Connect
        try {
            this.fritzBox.connect(password, 30);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        this.fritzBox.disconnect();
    }

    public AsyncFritzBoxAPI getFritzBox() {
        return this.fritzBox;
    }
}
