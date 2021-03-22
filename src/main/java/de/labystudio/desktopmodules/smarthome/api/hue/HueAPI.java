package de.labystudio.desktopmodules.smarthome.api.hue;

import io.github.zeroone3010.yahueapi.Hue;
import io.github.zeroone3010.yahueapi.HueBridge;
import io.github.zeroone3010.yahueapi.discovery.HueBridgeDiscoveryService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * API to find a bridge and to connect to it
 *
 * @author LabyStudio
 */
public class HueAPI implements Consumer<HueBridge> {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final HueBridgeDiscoveryService SERVICE = new HueBridgeDiscoveryService();

    private List<HueBridge> bridges;
    private Hue hue;

    private boolean couldNotConnect = false;

    /**
     * Create Hue connection using the given address and api key
     *
     * @param address The address of the bridge
     * @param apiKey  The API key for the bridge
     */
    public void connect(String address, String apiKey) {
        this.hue = new Hue(address, apiKey);
    }

    /**
     * Find a bridge and request an API key
     */
    public void setup(HueSetupCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                Future<List<HueBridge>> bridgesFuture = SERVICE.discoverBridges(this);
                this.bridges = bridgesFuture.get();

                // Bridges found
                if (!this.bridges.isEmpty()) {
                    String address = this.bridges.get(0).getIp();
                    String key = Hue.hueBridgeConnectionBuilder(address).initializeApiConnection("DesktopModules").get();

                    callback.onAuthenticated(address, key);
                }
            } catch (Exception e) {
                e.printStackTrace();

               this.couldNotConnect = true;
            }
        });
    }

    @Override
    public void accept(HueBridge hueBridge) {

    }

    /**
     * Is connected to the bridge
     *
     * @return returns false when waiting for button press
     */
    public boolean isConnected() {
        return this.hue != null;
    }

    /**
     * Get the hue api connection
     *
     * @return Hue connection
     */
    public Hue getHue() {
        return hue;
    }

    public boolean isCouldNotConnect() {
        return couldNotConnect;
    }
}
