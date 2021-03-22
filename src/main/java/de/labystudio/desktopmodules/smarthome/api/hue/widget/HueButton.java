package de.labystudio.desktopmodules.smarthome.api.hue.widget;

import de.labystudio.desktopmodules.core.renderer.IRenderContext;
import de.labystudio.desktopmodules.core.renderer.font.Font;
import de.labystudio.desktopmodules.core.renderer.font.FontStyle;
import de.labystudio.desktopmodules.core.renderer.font.StringAlignment;
import de.labystudio.desktopmodules.core.renderer.font.StringEffect;
import io.github.zeroone3010.yahueapi.Light;
import io.github.zeroone3010.yahueapi.Room;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class HueButton {

    private static final Font FONT = new Font("Dialog", FontStyle.PLAIN, 12);
    public static final int SIZE = 30;

    private final String name;
    private final BufferedImage icon;

    public int x;
    public int y;

    public HueButton(String name, BufferedImage icon) {
        this.name = name;
        this.icon = icon;
    }

    public HueButton(Room room, BufferedImage icon) {
        this(room.getName(), icon);
    }

    public HueButton(Light light, BufferedImage icon) {
        this(light.getName(), icon);
    }

    public void render(IRenderContext context, int mouseX, int mouseY) {
        int padding = isMouseOver(mouseX, mouseY) ? 2 : 0;
        String name = this.name.length() > 8 ? this.name.substring(0, 8) : this.name;

        context.drawImage(this.icon, this.x - SIZE / 2.0 - padding, this.y - SIZE / 2.0 - padding, SIZE + padding * 2, SIZE + padding * 2);
        context.drawString(name, this.x, this.y + SIZE / 2.0 + 5, StringAlignment.CENTERED, StringEffect.SHADOW, Color.WHITE, FONT);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= this.x - SIZE / 2 && mouseX <= this.x + SIZE / 2
                && mouseY >= this.y - SIZE / 2 && mouseY <= this.y + SIZE / 2;
    }

    public String getName() {
        return name;
    }
}
