package de.labystudio.desktopmodules.smarthome.api.hue;

import java.io.IOException;

public interface HueSetupCallback {
    void onAuthenticated(String address, String apiKey) throws IOException;
}
