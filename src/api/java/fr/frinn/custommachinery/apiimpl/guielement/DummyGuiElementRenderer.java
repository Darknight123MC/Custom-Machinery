package fr.frinn.custommachinery.apiimpl.guielement;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.frinn.custommachinery.api.guielement.IGuiElement;
import fr.frinn.custommachinery.api.guielement.IGuiElementRenderer;
import fr.frinn.custommachinery.api.guielement.IMachineScreen;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public class DummyGuiElementRenderer implements IGuiElementRenderer<IGuiElement> {

    @Override
    public void renderElement(PoseStack matrix, IGuiElement element, IMachineScreen screen) {

    }

    @Override
    public List<Component> getTooltips(IGuiElement element, IMachineScreen screen) {
        return Collections.emptyList();
    }

    @Override
    public void renderTooltip(PoseStack matrix, IGuiElement element, IMachineScreen screen, int mouseX, int mouseY) {

    }

    @Override
    public boolean isHovered(IGuiElement element, IMachineScreen screen, int mouseX, int mouseY) {
        return false;
    }
}
