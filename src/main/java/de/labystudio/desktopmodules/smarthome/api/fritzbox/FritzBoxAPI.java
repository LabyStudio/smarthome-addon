package de.labystudio.desktopmodules.smarthome.api.fritzbox;

import com.google.gson.Gson;
import de.labystudio.desktopmodules.smarthome.api.fritzbox.model.Network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * FritzBox API
 *
 * @author LabyStudio
 */
public class FritzBoxAPI {

    private static final String URL_API_CHALLENGE = "http://%s/login_sid.lua";
    private static final String URL_API_LOGIN = "http://%s/login_sid.lua?user=&response=%s";
    private static final String URL_API_NETWORK_DEVICES = "http://%s/query.lua?sid=%s&network=landevice:settings/landevice/list(name, active)";

    private static final Gson GSON = new Gson();

    protected String address;
    protected String sid = "";

    /**
     * Fritz box API access using the given router address
     *
     * @param address Address of the fritz box (Could be fritz.box)
     */
    public FritzBoxAPI(String address) {
        this.address = address;
    }

    /**
     * Fritz box API access using the default fritz.box address.
     */
    public FritzBoxAPI() {
        this("fritz.box");
    }

    /**
     * Connect to the fritz box
     *
     * @param password The password of the fritz box
     * @throws Exception Can't authenticate
     */
    public void connect(String password) throws Exception {
        String challenge = getChallenge();
        this.sid = login(challenge, password);
    }

    /**
     * Get a list of all registered fritz box client network devices
     *
     * @return Network response
     * @throws Exception
     */
    public Network getNetworkDevices() throws Exception {
        String json = request(String.format(URL_API_NETWORK_DEVICES, this.address, this.sid));
        return GSON.fromJson(json, Network.class);
    }

    /**
     * Generate a challenge code for the login
     *
     * @return The challenge code as string
     * @throws Exception
     */
    private String getChallenge() throws Exception {
        String result = request(String.format(URL_API_CHALLENGE, this.address));
        return result.substring(result.indexOf("<Challenge>") + 11, result.indexOf("<Challenge>") + 19);
    }

    /**
     * Login to the fritz box using the challenge code and the fritz box password
     *
     * @param challenge Challenge code as string
     * @param password  Fritz box password
     * @return SID (Access token)
     * @throws Exception
     */
    private String login(String challenge, String password) throws Exception {
        String stringToHash = challenge + "-" + password;
        String stringToHashUTF16 = new String(stringToHash.getBytes(StandardCharsets.UTF_16LE), StandardCharsets.UTF_8);

        String md5 = md5(stringToHashUTF16);
        String response = challenge + "-" + md5;

        String result = request(String.format(URL_API_LOGIN, this.address, response));
        return result.substring(result.indexOf("<SID>") + 5, result.indexOf("<SID>") + 21);
    }

    /**
     * Make GET request to the given URL
     *
     * @param url The URL as string
     * @return The response from the server as string
     * @throws Exception
     */
    private String request(String url) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        String line;
        StringBuilder content = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            content.append(line).append("\n");
        }
        return content.toString();
    }

    /**
     * Generate md5 hash of given string
     *
     * @return The md5 hashed string
     */
    private static String md5(String string) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hashInBytes = md.digest(string.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
