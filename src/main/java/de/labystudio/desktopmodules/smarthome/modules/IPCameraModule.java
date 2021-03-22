package de.labystudio.desktopmodules.smarthome.modules;

import com.google.gson.JsonObject;
import de.labystudio.desktopmodules.core.addon.Addon;
import de.labystudio.desktopmodules.core.loader.TextureLoader;
import de.labystudio.desktopmodules.core.module.extension.FreeViewModule;
import de.labystudio.desktopmodules.core.renderer.IRenderContext;
import de.labystudio.desktopmodules.core.renderer.font.Font;
import de.labystudio.desktopmodules.core.renderer.font.FontStyle;
import de.labystudio.desktopmodules.core.renderer.font.StringAlignment;
import de.labystudio.desktopmodules.core.renderer.font.StringEffect;
import de.labystudio.desktopmodules.smarthome.SmartHomeAddon;
import de.labystudio.desktopmodules.smarthome.api.mjpeg.MotionPictureStream;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.EOFException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;

/**
 * Module to display a motion picture stream
 *
 * @author LabyStudio
 */
public class IPCameraModule extends FreeViewModule<SmartHomeAddon> {

    private static final Font FONT = new Font("Dialog", FontStyle.PLAIN, 12);

    private MotionPictureStream motionPictureStream;

    private URL motionUrl;
    private long lastMotionDetected;
    private long lastMotionChecked;
    private long motionInterval;
    private boolean motionDetectionEnabled;

    private int characterOffset;
    private byte characterByte;

    private BufferedImage textureLoading;

    public IPCameraModule() {
        super(200, 120);
    }

    @Override
    public void onLoadConfig(JsonObject config) {
        super.onLoadConfig(config);

        // Get stream configuration
        try {
            String streamUrl = Addon.getConfigValue(config, "mjpeg_url", "http://127.0.0.1/cgi-bin/CGIStream.cgi?cmd=GetMJStream");
            this.motionPictureStream = new MotionPictureStream(new URL(streamUrl));

            this.lastMotionDetected = System.currentTimeMillis();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Get motion configuration
        try {
            JsonObject motionDetection = Addon.getConfigObject(config, "motion_detection");
            this.motionDetectionEnabled = Addon.getConfigValue(motionDetection, "enabled", false);

            if (this.motionDetectionEnabled) {
                this.motionUrl = new URL(Addon.getConfigValue(motionDetection, "url", "http://127.0.0.1/cgi-bin/CGIProxy.fcgi?cmd=getDevState"));
                this.motionInterval = Addon.getConfigValue(motionDetection, "intervalInSeconds", 3) * 1000L;

                // Trigger condition of the document to detect the motion
                JsonObject triggerCondition = Addon.getConfigObject(motionDetection, "trigger_condition");

                // Offset of the response document
                this.characterOffset = Addon.getConfigValue(triggerCondition, "character_offset", 0);

                // Required character as byte to flag as motion
                this.characterByte = (byte) Addon.getConfigValue(triggerCondition, "character_byte", 0);
            } else if (this.motionPictureStream != null) {
                // Open stream directly
                this.lastMotionDetected = System.currentTimeMillis();
                this.motionPictureStream.openAsync();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTick() {
        // Check motion each x seconds
        if (!this.motionDetectionEnabled || this.lastMotionChecked + this.motionInterval > System.currentTimeMillis()) {
            return;
        }

        this.lastMotionChecked = System.currentTimeMillis();

        // Check motion
        if (this.motionPictureStream != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Check status file
                    DataInputStream dataInputStream = new DataInputStream(this.motionUrl.openStream());

                    // Skip to record state
                    dataInputStream.skipBytes(this.characterOffset);

                    try {
                        // Find byte at offset
                        byte targetByte = dataInputStream.readByte();

                        // Compare byte
                        if (targetByte == this.characterByte) {

                            // Start stream
                            if (!this.motionPictureStream.isAlive()) {
                                this.lastMotionDetected = System.currentTimeMillis();
                                this.motionPictureStream.openAsync();
                            }
                        } else if (this.motionPictureStream.isAlive() && this.lastMotionDetected + 1000 * 60 < System.currentTimeMillis() && !isMouseOver()) {

                            // Close the stream
                            this.motionPictureStream.close();
                        }
                    } catch (EOFException ignore) {
                        // File just changed
                    }

                    dataInputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void onRender(IRenderContext context, int width, int height) {
        // Error view
        if (this.motionPictureStream == null) {
            context.drawRect(0, 0, this.width, this.height, Color.DARK_GRAY);
            context.drawString("Wrong configuration", width / 2.0F, height / 2.0F + 6, StringAlignment.CENTERED, StringEffect.NONE, Color.RED, FONT);
        }

        // Hide the module if there is no video feed right now
        if (this.motionPictureStream == null || !this.motionPictureStream.isAlive()) {
            return;
        }

        // Animation values
        int timePassed = (int) (System.currentTimeMillis() - this.lastMotionDetected);
        float alpha = this.motionPictureStream.isLoading() ? 1.0F / 1000.0F * timePassed : 1.0F;

        // Update alpha value
        context.setAlpha(alpha);

        // No image
        context.drawRect(0, 0, this.width, this.height, Color.DARK_GRAY);

        // Draw video frame
        BufferedImage frame = this.motionPictureStream.getCurrentFrame();
        if (frame != null) {
            context.drawImage(frame, this.offsetX, this.offsetY,
                    this.width + this.zoom,
                    this.height + this.zoom);
        }

        // Loading animation
        if (this.motionPictureStream.isLoading()) {
            // Draw dimmed overlay
            context.drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 150));

            // Draw rotating loading image
            context.translate(this.width / 2D, this.height / 2D);
            context.scale(0.3, 0.3);
            context.rotate(timePassed / 300F);
            context.translate(-this.height / 2D, -this.height / 2D);
            context.drawImage(this.textureLoading, 0, 0, height, height);
        }
    }

    @Override
    public void loadTextures(TextureLoader textureLoader) {
        this.textureLoading = textureLoader.load("textures/smarthome/camera/loading.png");
    }

    @Override
    protected String getIconPath() {
        return "textures/smarthome/camera/cameramodule.png";
    }

    @Override
    public String getDisplayName() {
        return "IP Camera";
    }
}
