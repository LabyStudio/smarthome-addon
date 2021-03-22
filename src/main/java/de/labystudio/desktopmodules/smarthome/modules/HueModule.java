package de.labystudio.desktopmodules.smarthome.modules;

import de.labystudio.desktopmodules.core.loader.TextureLoader;
import de.labystudio.desktopmodules.core.module.Module;
import de.labystudio.desktopmodules.core.renderer.IRenderContext;
import de.labystudio.desktopmodules.core.renderer.font.StringEffect;
import de.labystudio.desktopmodules.smarthome.SmartHomeAddon;
import de.labystudio.desktopmodules.smarthome.api.hue.HueAPI;
import de.labystudio.desktopmodules.smarthome.api.hue.widget.HueButton;
import de.labystudio.desktopmodules.smarthome.api.hue.widget.HueMenu;
import io.github.zeroone3010.yahueapi.Hue;
import io.github.zeroone3010.yahueapi.Light;
import io.github.zeroone3010.yahueapi.Room;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class HueModule extends Module<SmartHomeAddon> {

    private BufferedImage textureHue;
    private BufferedImage textureLamp;

    private final HueMenu menu = new HueMenu();
    private String room = null;

    public HueModule() {
        super(200, 40);
    }

    @Override
    public void onRender(IRenderContext context, int width, int height, int mouseX, int mouseY) {
        context.drawImage(this.textureHue, this.rightBound ? width - HueButton.SIZE : 0, 0, HueButton.SIZE, HueButton.SIZE);

        HueAPI api = this.addon.getHueAPI();
        if (api.isConnected()) {
            Hue hue = api.getHue();

            this.menu.render(context, this.rightBound ? width - HueButton.SIZE - HueButton.SIZE / 2 - 5 : height + HueButton.SIZE / 2 + 5, HueButton.SIZE / 2,
                    this.rightBound, mouseX, mouseY);
        } else {
            String message = api.isCouldNotConnect() ? "Could not connect to bridge" : "Press the button of the bridge...";
            context.drawString(message, width, HueButton.SIZE, 17, this.rightBound, StringEffect.SHADOW,
                    api.isCouldNotConnect() ? Color.RED : Color.WHITE, DEFAULT_FONT);
        }
    }

    @Override
    public void onMousePressed(int mouseX, int mouseY, int mouseButton) {
        super.onMousePressed(mouseX, mouseY, mouseButton);

        HueAPI api = this.addon.getHueAPI();
        Hue hue = api.getHue();

        if (hue != null) {
            int x = this.rightBound ? width - height : 0;
            if (mouseX >= x && mouseX <= x + this.height) {
                this.menu.openRooms(hue.getRooms(), this.textureLamp);
            }

            HueButton button = this.menu.onClick(mouseX, mouseY);
            if (button != null) {
                if (this.room == null) {
                    this.room = button.getName();
                    open();
                } else {
                    Room room = hue.getRoomByName(this.room).get();
                    Light light = room.getLightByName(button.getName()).get();

                    if (light.isOn()) {
                        light.turnOff();
                    } else {
                        light.turnOn();
                    }
                }
            }
        }
    }

    private void open() {
        HueAPI api = this.addon.getHueAPI();
        Hue hue = api.getHue();

        if (hue != null) {
            if (this.room == null) {
                this.menu.openRooms(hue.getRooms(), this.textureLamp);
            } else {
                Room room = hue.getRoomByName(this.room).get();
                this.menu.openLights(room.getLights(), this.textureLamp);
            }
        }
    }

    @Override
    public void loadTextures(TextureLoader textureLoader) {
        this.textureHue = textureLoader.load("textures/smarthome/hue/hue.png");
        this.textureLamp = textureLoader.load("textures/smarthome/hue/lamp.png");
    }

    @Override
    protected String getIconPath() {
        return "textures/smarthome/hue/hue.png";
    }

    @Override
    public String getDisplayName() {
        return "Phillips Hue";
    }

}
