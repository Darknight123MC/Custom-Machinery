package fr.frinn.custommachinery.api.guielement;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Used to handle rendering for all IGuiElement instances of a specific GuiElementType.
 * Register an IGuiElementRenderer using RegisterGuiElementRendererEvent.
 * All registered GuiElementType must have an IGuiElementRenderer or nothing will show in the machine gui.
 * @param <E> The IGuiElement to render.
 */
public interface IGuiElementRenderer<E extends IGuiElement> {

    /**
     * Called each frame for each gui element of the corresponding type.
     * Render your element (text, texture, item, etc..) here.
     * The MatrixStack is translated to the top left of the machine gui, consider it the 0,0 point for the rendering.
     */
    void renderElement(PoseStack matrix, E element, IMachineScreen screen);

    /**
     * Called by the default implementation of {@link IGuiElementRenderer#renderTooltip} when the player's mouse cursor hover the element.
     * @param element The element being hovered by the player mouse cursor.
     * @return A list of tooltips to display when the player's mouse cursor hover the element.
     */
    List<Component> getTooltips(E element, IMachineScreen screen);

    /**
     * Called each frame for each gui element of the corresponding type that return true to isHovered.
     * Render a tooltip here.
     * The MatrixStack is translated to the top left of the machine gui, consider it the 0,0 point for the rendering.
     */
    default void renderTooltip(PoseStack pose, E element, IMachineScreen screen, int mouseX, int mouseY) {
        List<Component> tooltips = getTooltips(element, screen);
        if(!tooltips.isEmpty())
            screen.drawTooltips(pose, tooltips, mouseX, mouseY);
    };

    /**
     * Calculate if the mouse cursor is hovering the element and return true if so.
     */
    default boolean isHovered(E element, IMachineScreen screen, int mouseX, int mouseY) {
        return mouseX >= element.getX() && mouseX <= element.getX() + element.getWidth() && mouseY >= element.getY() && mouseY <= element.getY() + element.getHeight();
    }

    /**
     * Called client side only when the player click the element.
     * @param element The element clicked.
     * @param screen The machine screen currently opened.
     * @param button The mouse button that was clicked.
     *               0 : left
     *               1 : right
     *               2 : middle
     */
    default void handleClick(E element, IMachineScreen screen, int mouseX, int mouseY, int button) {

    }
}
