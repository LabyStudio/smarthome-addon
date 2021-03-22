package de.labystudio.desktopmodules.smarthome.api.hue.widget;

import de.labystudio.desktopmodules.core.renderer.IRenderContext;
import io.github.zeroone3010.yahueapi.Light;
import io.github.zeroone3010.yahueapi.Room;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HueMenu {

    private List<HueButton> buttons;
    private long timestamp;
    private boolean lamp;

    public void openRooms(Collection<Room> rooms, BufferedImage icon) {
        List<HueButton> buttons = new ArrayList<>();
        for (Room room : rooms) {
            buttons.add(new HueButton(room, icon));
        }
        open(buttons);
        this.lamp = false;
    }

    public void openLights(Collection<Light> lights, BufferedImage icon) {
        List<HueButton> buttons = new ArrayList<>();
        for (Light light : lights) {
            buttons.add(new HueButton(light, icon));
        }
        open(buttons);
        this.lamp = true;
    }

    public void open(List<HueButton> buttons) {
        this.buttons = buttons;
        this.timestamp = System.currentTimeMillis();
    }

    public void render(IRenderContext context, int x, int y, boolean rightBound, int mouseX, int mouseY) {
        if (this.buttons == null) {
            return;
        }

        tickButtons(x, y, rightBound);

        for (HueButton button : this.buttons) {
            button.render(context, mouseX, mouseY);
        }
    }

    public HueButton onClick(int mouseX, int mouseY) {
        for (HueButton button : this.buttons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }

    private void tickButtons(int x, int y, boolean rightBound) {
        for (HueButton button : this.buttons) {
            button.x = x;
            button.y = y;

            x -= (HueButton.SIZE + 5) * (rightBound ? 1 : -1);
        }
    }

    public boolean isLamp() {
        return lamp;
    }
}
