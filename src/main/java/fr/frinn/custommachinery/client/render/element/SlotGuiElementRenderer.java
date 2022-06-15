package fr.frinn.custommachinery.client.render.element;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.frinn.custommachinery.api.crafting.IMachineRecipe;
import fr.frinn.custommachinery.api.guielement.IMachineScreen;
import fr.frinn.custommachinery.api.integration.jei.IJEIElementRenderer;
import fr.frinn.custommachinery.client.ClientHandler;
import fr.frinn.custommachinery.client.screen.CustomMachineScreen;
import fr.frinn.custommachinery.common.config.CMConfig;
import fr.frinn.custommachinery.common.guielement.SlotGuiElement;
import fr.frinn.custommachinery.common.util.CycleTimer;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.List;

public class SlotGuiElementRenderer extends TexturedGuiElementRenderer<SlotGuiElement> implements IJEIElementRenderer<SlotGuiElement> {

    private static final CycleTimer timer = new CycleTimer(CMConfig.INSTANCE.itemSlotCycleTime.get());

    @Override
    public void renderElement(PoseStack matrix, SlotGuiElement element, IMachineScreen screen) {
        super.renderElement(matrix, element, screen);

        if(!element.getItems().isEmpty()) {
            timer.onDraw();
            List<Item> items = element.getItems().stream().flatMap(ingredient -> ingredient.getAll().stream()).toList();
            ((CustomMachineScreen)screen).renderTransparentItem(matrix, timer.getOrDefault(items, Items.AIR).getDefaultInstance(), element.getX() + 1, element.getY() + 1);
        }
    }

    @Override
    public List<Component> getTooltips(SlotGuiElement element, IMachineScreen screen) {
        return Collections.emptyList();
    }

    @Override
    public void renderElementInJEI(PoseStack matrix, SlotGuiElement element, IMachineRecipe recipe, int mouseX, int mouseY) {
        int posX = element.getX() - 1;
        int posY = element.getY() - 1;
        int width = element.getWidth();
        int height = element.getHeight();

        ClientHandler.bindTexture(element.getTexture());
        GuiComponent.blit(matrix, posX, posY, 0, 0, width, height, width, height);
    }

    @Override
    public List<Component> getJEITooltips(SlotGuiElement element, IMachineRecipe recipe) {
        return Collections.emptyList();
    }
}
