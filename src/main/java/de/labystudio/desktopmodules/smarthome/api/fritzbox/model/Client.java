package de.labystudio.desktopmodules.smarthome.api.fritzbox.model;


/**
 * Registered fritz box client
 *
 * @author LabyStudio
 */
public class Client {

    /**
     * Device name
     */
    public String name;

    /**
     * Device is active
     */
    private String active;

    /**
     * Device is active in the network
     *
     * @return Device is active
     */
    public boolean isActive() {
        return this.active != null && this.active.equals("1");
    }
}
