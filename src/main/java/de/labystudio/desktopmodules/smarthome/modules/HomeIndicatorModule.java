package de.labystudio.desktopmodules.smarthome.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.labystudio.desktopmodules.core.loader.TextureLoader;
import de.labystudio.desktopmodules.core.module.Module;
import de.labystudio.desktopmodules.core.renderer.IRenderContext;
import de.labystudio.desktopmodules.core.renderer.font.Font;
import de.labystudio.desktopmodules.core.renderer.font.FontStyle;
import de.labystudio.desktopmodules.core.renderer.font.StringAlignment;
import de.labystudio.desktopmodules.core.renderer.font.StringEffect;
import de.labystudio.desktopmodules.smarthome.SmartHomeAddon;
import de.labystudio.desktopmodules.smarthome.api.fritzbox.AsyncFritzBoxAPI;
import de.labystudio.desktopmodules.smarthome.api.fritzbox.model.Client;
import de.labystudio.desktopmodules.smarthome.api.fritzbox.model.Network;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Module to display a list of devices in the home network
 *
 * @author LabyStudio
 */
public class HomeIndicatorModule extends Module<SmartHomeAddon> implements Consumer<Network> {

    private static final Font FONT = new Font("Dialog", FontStyle.PLAIN, 12);

    private BufferedImage textureFritz;
    private BufferedImage textureOnline;
    private BufferedImage textureOffline;

    private final List<Filter> filter = new CopyOnWriteArrayList<>();
    private Map<String, Boolean> visibleClients = new HashMap<>();
    private int lastFilterMatchedResult = -1;

    private boolean networkValid = false;

    public HomeIndicatorModule() {
        super(230, 24); // Changed dynamically
    }

    @Override
    public void onInitialize(SmartHomeAddon addon, JsonObject config) {
        super.onInitialize(addon, config);

        addon.getFritzBox().registerNetworkListener(this);
    }

    @Override
    public void onLoadConfig(JsonObject config) {
        super.onLoadConfig(config);

        if (config.has("filter")) {
            // Load filter list
            JsonArray array = config.get("filter").getAsJsonArray();

            this.filter.clear();
            for (JsonElement entry : array) {
                this.filter.add(new Filter(entry.getAsJsonObject()));
            }
        } else {
            // Create default list
            JsonArray array = new JsonArray();
            JsonObject entry = new JsonObject();
            JsonArray deviceNames = new JsonArray();
            deviceNames.add("MobilePhone123");
            deviceNames.add("MyComputer");
            entry.add("deviceNames", deviceNames);
            entry.addProperty("nickname", "Peter");
            array.add(entry);
            config.add("filter", array);

            // Save default values
            try {
                this.addon.saveConfig();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void accept(Network network) {
        Map<String, Boolean> visibleClients = new HashMap<>();

        // Render all clients
        for (Client client : network.network) {
            // Filter clients
            Filter filter = getByDeviceName(client.name);
            if (filter == null)
                continue;

            Boolean alreadyRegisteredAsOnline = visibleClients.get(filter.nickname);
            if (alreadyRegisteredAsOnline == null || !alreadyRegisteredAsOnline) {
                visibleClients.put(filter.nickname, client.isActive());
            }
        }

        // Update list
        this.visibleClients = visibleClients;

        // Update module size
        int size = this.visibleClients.size();
        if (this.lastFilterMatchedResult != size) {
            this.lastFilterMatchedResult = size;

            // Change size
            updateSize(230, Math.max(24, 12 * (size + 1)));
        }

        this.networkValid = true;
    }

    @Override
    public void onTick() {
    }

    @Override
    public void onRender(IRenderContext context, int width, int height) {
        AsyncFritzBoxAPI fritzBoxAPI = this.addon.getFritzBox();

        // Not connected to fritz box
        if (!this.networkValid || this.addon.getFritzBox() == null || fritzBoxAPI.isAuthenticationError() || this.filter.isEmpty()) {
            // Get error message
            String message = fritzBoxAPI.isAuthenticationError() ? "Wrong FritzBox password in config" :
                    this.addon.getFritzBox() == null ? "Connecting..." : "Reading clients...";

            // Draw error message
            context.drawImage(this.textureFritz, this.rightBound ? this.width - this.height : 0, 0, this.height, this.height);
            context.drawString(message, this.rightBound ? this.width - this.height - 2 : this.height + 2, 15, StringAlignment.from(this.rightBound), StringEffect.SHADOW, Color.RED, FONT);
            return;
        }

        // Render client
        int y = 10;
        for (Map.Entry<String, Boolean> client : this.visibleClients.entrySet()) {
            context.drawImage(client.getValue() ? this.textureOnline : this.textureOffline, this.rightBound ? this.width - 12 : 0, y - 10);
            context.drawString(client.getKey(), this.rightBound ? this.width - 14 : 14, y, StringAlignment.from(this.rightBound), StringEffect.SHADOW, Color.WHITE, FONT);
            y += 16;
        }

        // Wrong filter message
        if (this.visibleClients.isEmpty() || this.filter.isEmpty()) {
            String message = this.filter.isEmpty() ? "No filters defined" : "Filter didn't match to anyone";

            context.drawImage(this.textureFritz, this.rightBound ? this.width - this.height : 0, 0, this.height, this.height);
            context.drawString(message, this.rightBound ? this.width - this.height - 2 : this.height + 2, 15, StringAlignment.from(this.rightBound), StringEffect.SHADOW, Color.YELLOW, FONT);
        }
    }

    /**
     * Find a matching filter to the device name
     *
     * @param deviceName The device name to filter
     * @return Matching filter
     */
    private Filter getByDeviceName(String deviceName) {
        for (Filter filter : this.filter) {
            for (String filterDeviceName : filter.deviceNames) {
                if (filterDeviceName.equalsIgnoreCase(deviceName)) {
                    return filter;
                }
            }
        }
        return null;
    }

    @Override
    public void loadTextures(TextureLoader textureLoader) {
        this.textureFritz = textureLoader.loadTexture("textures/smarthome/homeindicator/fritz.png");
        this.textureOnline = textureLoader.loadTexture("textures/smarthome/homeindicator/online.png", 12, 12);
        this.textureOffline = textureLoader.loadTexture("textures/smarthome/homeindicator/offline.png", 12, 12);
    }

    @Override
    protected String getIconPath() {
        return "textures/smarthome/homeindicator/homeindicatormodule.png";
    }

    @Override
    public String getDisplayName() {
        return "Home Indicator";
    }


    /**
     * A list of devices names for a client to filter and a replacement nickname
     *
     * @author LabyStudio
     */
    private static class Filter {
        protected List<String> deviceNames = new ArrayList<>();
        protected String nickname = "Unknown";

        /**
         * Create filter directly from a list and a string
         *
         * @param deviceNames List of device names to filter
         * @param nickname    The replacement nickname
         */
        public Filter(List<String> deviceNames, String nickname) {
            this.deviceNames = deviceNames;
            this.nickname = nickname;
        }

        /**
         * Create filter from json object
         *
         * @param entry Serialized filter object
         */
        public Filter(JsonObject entry) {
            if (entry.has("deviceNames")) {
                JsonArray deviceNames = entry.get("deviceNames").getAsJsonArray();

                if (deviceNames.size() != 0) {
                    // Get device names
                    for (JsonElement deviceName : deviceNames) {
                        this.deviceNames.add(deviceName.getAsString());
                    }

                    // Get nickname
                    if (entry.has("nickname")) {
                        this.nickname = entry.get("nickname").getAsString();
                    } else {
                        this.nickname = this.deviceNames.get(0);
                    }
                }
            }
        }
    }
}
