package de.labystudio.desktopmodules.smarthome.api.fritzbox;

import de.labystudio.desktopmodules.smarthome.api.fritzbox.model.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


/**
 * Asynchronous FritzBox API
 *
 * @author LabyStudio
 */
public class AsyncFritzBoxAPI extends FritzBoxAPI {

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> fritzBoxTask;

    private boolean authenticationError = false;
    private int retry = 10;

    private final List<Consumer<Network>> networkListeners = new ArrayList<>();

    /**
     * Fritz box API access using the given router address
     *
     * @param address Address of the fritz box (Could be fritz.box)
     */
    public AsyncFritzBoxAPI(String address) {
        super(address);
    }

    /**
     * Fritz box API access using the default fritz.box address.
     */
    public AsyncFritzBoxAPI() {
        super();
    }

    /**
     * Login asynchronously to the fritz box and start a repeating task to update the network devices
     *
     * @param password       The password of the fritz box
     * @param updateInterval Update the network device list each x seconds
     * @throws Exception Login exception
     */
    public void connect(String password, int updateInterval) {
        // Connect to fritz box async
        this.executorService.execute(() -> {
            try {
                // Login
                super.connect(password);

                // Start repeating task
                if (this.fritzBoxTask == null || this.fritzBoxTask.isDone() || this.fritzBoxTask.isCancelled()) {
                    this.fritzBoxTask = this.executorService.scheduleAtFixedRate(this::updateNetworkDevices, 0, updateInterval, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                e.printStackTrace();

                this.authenticationError = true;

                // Check for retry
                if (this.retry > 0) {
                    this.retry--;

                    // Wait 2 seconds
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }

                    // Reconnect
                    connect(password, updateInterval);
                }
            }
        });
    }

    /**
     * Stop the repeating task
     */
    public void disconnect() {
        if (this.fritzBoxTask != null) {
            this.fritzBoxTask.cancel(true);
        }
    }

    /**
     * Update the network devices list and trigger all network listeners
     */
    private void updateNetworkDevices() {
        if (this.authenticationError) {
            return;
        }

        try {
            // Get current network devices
            Network network = getNetworkDevices();

            // Call listeners
            if (network != null) {
                for (Consumer<Network> listener : this.networkListeners) {
                    listener.accept(network);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            this.authenticationError = true;
        }
    }

    /**
     * Register a network device update listener
     *
     * @param listener The callback
     */
    public void registerNetworkListener(Consumer<Network> listener) {
        this.networkListeners.add(listener);
    }

    /**
     * Authentication status
     *
     * @return Could not login to the fritz box
     */
    public boolean isAuthenticationError() {
        return authenticationError;
    }

    /**
     * Change the fritz box address
     *
     * @param address New address of the fritz box
     */
    public void setAddress(String address) {
        this.address = address;
    }
}
