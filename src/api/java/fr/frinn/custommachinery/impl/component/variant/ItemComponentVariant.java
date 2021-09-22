package fr.frinn.custommachinery.impl.component.variant;

import fr.frinn.custommachinery.api.components.IMachineComponentManager;
import fr.frinn.custommachinery.api.components.variant.IComponentVariant;
import net.minecraft.item.ItemStack;

public abstract class ItemComponentVariant implements IComponentVariant {

    public abstract void tick(IMachineComponentManager manager);

    public abstract boolean isItemValid(IMachineComponentManager manager, ItemStack stack);
}