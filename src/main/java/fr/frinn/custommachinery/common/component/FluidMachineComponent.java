package fr.frinn.custommachinery.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.frinn.custommachinery.api.component.ComponentIOMode;
import fr.frinn.custommachinery.api.component.IComparatorInputComponent;
import fr.frinn.custommachinery.api.component.IMachineComponentManager;
import fr.frinn.custommachinery.api.component.IMachineComponentTemplate;
import fr.frinn.custommachinery.api.component.ISerializableComponent;
import fr.frinn.custommachinery.api.component.ISideConfigComponent;
import fr.frinn.custommachinery.api.component.MachineComponentType;
import fr.frinn.custommachinery.api.network.ISyncable;
import fr.frinn.custommachinery.api.network.ISyncableStuff;
import fr.frinn.custommachinery.apiimpl.codec.CodecLogger;
import fr.frinn.custommachinery.apiimpl.component.AbstractMachineComponent;
import fr.frinn.custommachinery.apiimpl.component.config.RelativeSide;
import fr.frinn.custommachinery.apiimpl.component.config.SideConfig;
import fr.frinn.custommachinery.apiimpl.component.config.SideMode;
import fr.frinn.custommachinery.common.init.Registration;
import fr.frinn.custommachinery.common.network.syncable.FluidStackSyncable;
import fr.frinn.custommachinery.common.network.syncable.SideConfigSyncable;
import fr.frinn.custommachinery.common.util.Codecs;
import fr.frinn.custommachinery.common.util.ingredient.IIngredient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class FluidMachineComponent extends AbstractMachineComponent implements ISerializableComponent, ISyncableStuff, IComparatorInputComponent, ISideConfigComponent {

    private final String id;
    private final int capacity;
    private final int maxInput;
    private final int maxOutput;
    private final List<IIngredient<Fluid>> filter;
    private final boolean whitelist;
    private final SideConfig config;

    private long actualTick;
    private int actualTickInput;
    private int actualTickOutput;
    private FluidStack fluidStack = FluidStack.EMPTY;

    public FluidMachineComponent(IMachineComponentManager manager, ComponentIOMode mode, String id, int capacity, int maxInput, int maxOutput, List<IIngredient<Fluid>> filter, boolean whitelist, Map<RelativeSide, SideMode> defaultConfig) {
        super(manager, mode);
        this.id = id;
        this.capacity = capacity;
        this.maxInput = maxInput;
        this.maxOutput = maxOutput;
        this.filter = filter;
        this.whitelist = whitelist;
        this.config = new SideConfig(this, defaultConfig);
    }

    @Override
    public MachineComponentType<FluidMachineComponent> getType() {
        return Registration.FLUID_MACHINE_COMPONENT.get();
    }

    public String getId() {
        return this.id;
    }

    public int getMaxInput() {
        return this.maxInput;
    }

    public int getMaxOutput() {
        return this.maxOutput;
    }

    @Override
    public SideConfig getConfig() {
        return this.config;
    }

    @Override
    public void serialize(CompoundTag nbt) {
        fluidStack.writeToNBT(nbt);
        nbt.put("config", this.config.serialize());
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        this.fluidStack = FluidStack.loadFluidStackFromNBT(nbt);
        if(nbt.contains("config"))
            this.config.deserialize(nbt.get("config"));
    }

    @Override
    public void getStuffToSync(Consumer<ISyncable<?, ?>> container) {
        container.accept(FluidStackSyncable.create(() -> this.fluidStack, fluidStack -> this.fluidStack = fluidStack));
        container.accept(SideConfigSyncable.create(this::getConfig, this.config::set));
    }

    @Override
    public int getComparatorInput() {
        return (int) (15 * ((double)this.fluidStack.getAmount() / (double)this.capacity));
    }

    /** FLUID HANDLER STUFF **/

    public FluidStack getFluidStack() {
        return this.fluidStack.copy();
    }

    public void setFluidStack(FluidStack fluidStack) {
        this.fluidStack = fluidStack.copy();
        getManager().markDirty();
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getRemainingSpace() {
        if(this.actualTick != this.getManager().getLevel().getGameTime()) {
            this.actualTick = this.getManager().getLevel().getGameTime();
            this.actualTickInput = 0;
            this.actualTickOutput = 0;
        }

        int maxInput = this.maxInput - this.actualTickInput;

        if(!this.fluidStack.isEmpty())
            return Math.min(this.capacity - this.fluidStack.getAmount(), maxInput);
        return Math.min(this.capacity, maxInput);
    }

    public boolean isFluidValid(@Nonnull FluidStack stack) {
        return this.filter.stream().anyMatch(ingredient -> ingredient.test(stack.getFluid())) == this.whitelist && (this.fluidStack.isEmpty() || stack.isFluidEqual(this.fluidStack));
    }

    public int insert(Fluid fluid, int amount, CompoundTag nbt, FluidAction action) {
        if (amount <= 0)
            return 0;

        if(this.actualTick != this.getManager().getLevel().getGameTime()) {
            this.actualTick = this.getManager().getLevel().getGameTime();
            this.actualTickInput = 0;
            this.actualTickOutput = 0;
        }

        int maxInsert = this.maxInput - this.actualTickInput;
        if(this.fluidStack.isEmpty()) {
            amount = Math.min(amount, maxInsert);
            if(action.execute()) {
                this.fluidStack = new FluidStack(fluid, amount, nbt);
                this.actualTickInput += amount;
                getManager().markDirty();
            }
        }
        else {
            amount = Math.min(Math.min(getRemainingSpace(), maxInsert), amount);
            if(action.execute()) {
                this.fluidStack.grow(amount);
                this.actualTickInput += amount;
                getManager().markDirty();
            }
        }
        return amount;
    }

    public FluidStack extract(int amount, FluidAction action) {
        if(amount <= 0)
            return FluidStack.EMPTY;

        if(this.actualTick != this.getManager().getLevel().getGameTime()) {
            this.actualTick = this.getManager().getLevel().getGameTime();
            this.actualTickInput = 0;
            this.actualTickOutput = 0;
        }

        int maxExtract = this.maxOutput - this.actualTickOutput;
        amount = Mth.clamp(amount, 0, Math.min(this.fluidStack.getAmount(), maxExtract));
        if(action.execute()) {
            this.fluidStack.shrink(amount);
            this.actualTickOutput += amount;
            getManager().markDirty();
        }
        return new FluidStack(this.fluidStack.getFluid(), amount, this.fluidStack.getTag());
    }

    /** Recipe Stuff **/

    public int getRecipeRemainingSpace() {
        if(!this.fluidStack.isEmpty())
            return this.capacity - this.fluidStack.getAmount();
        return this.capacity;
    }

    public void recipeInsert(Fluid fluid, int amount, CompoundTag nbt) {
        if(amount <= 0)
            return;

        if(this.fluidStack.isEmpty())
            this.fluidStack = new FluidStack(fluid, amount, nbt);
        else {
            amount = Mth.clamp(amount, 0, getRecipeRemainingSpace());
            this.fluidStack.grow(amount);
        }
        getManager().markDirty();
    }

    public void recipeExtract(int amount) {
        if(amount <= 0)
            return;

        amount = Mth.clamp(amount, 0, this.fluidStack.getAmount());
        this.fluidStack.shrink(amount);
        getManager().markDirty();
    }

    public static class Template implements IMachineComponentTemplate<FluidMachineComponent> {

        public static final Codec<FluidMachineComponent.Template> CODEC = RecordCodecBuilder.create(fluidMachineComponentTemplate ->
                fluidMachineComponentTemplate.group(
                        Codec.STRING.fieldOf("id").forGetter(template -> template.id),
                        Codec.INT.fieldOf("capacity").forGetter(template -> template.capacity),
                        CodecLogger.loggedOptional(Codec.INT,"maxInput").forGetter(template -> Optional.of(template.maxInput)),
                        CodecLogger.loggedOptional(Codec.INT,"maxOutput").forGetter(template -> Optional.of(template.maxOutput)),
                        CodecLogger.loggedOptional(Codecs.list(IIngredient.FLUID),"filter", Collections.emptyList()).forGetter(template -> template.filter),
                        CodecLogger.loggedOptional(Codec.BOOL,"whitelist", false).forGetter(template -> template.whitelist),
                        CodecLogger.loggedOptional(Codecs.COMPONENT_MODE_CODEC,"mode", ComponentIOMode.BOTH).forGetter(template -> template.mode),
                        CodecLogger.loggedOptional(Codecs.SIDE_CONFIG_CODEC, "config", SideConfig.DEFAULT_ALL_BOTH).forGetter(template -> template.defaultConfig)
                ).apply(fluidMachineComponentTemplate, (id, capacity, maxInput, maxOutput, filter, whitelist, mode, config) ->
                        new Template(id, capacity, maxInput.orElse(capacity), maxOutput.orElse(capacity), filter, whitelist, mode, config)
                )
        );

        private final String id;
        private final int capacity;
        private final int maxInput;
        private final int maxOutput;
        private final List<IIngredient<Fluid>> filter;
        private final boolean whitelist;
        private final ComponentIOMode mode;
        private final Map<RelativeSide, SideMode> defaultConfig;

        public Template(String id, int capacity, int maxInput, int maxOutput, List<IIngredient<Fluid>> filter, boolean whitelist, ComponentIOMode mode, Map<RelativeSide, SideMode> defaultConfig) {
            this.id = id;
            this.capacity = capacity;
            this.maxInput = maxInput;
            this.maxOutput = maxOutput;
            this.filter = filter;
            this.whitelist = whitelist;
            this.mode = mode;
            this.defaultConfig = defaultConfig;
        }

        @Override
        public MachineComponentType<FluidMachineComponent> getType() {
            return Registration.FLUID_MACHINE_COMPONENT.get();
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public boolean canAccept(Object ingredient, boolean isInput, IMachineComponentManager manager) {
            if(isInput != this.mode.isInput())
                return false;
            if(ingredient instanceof FluidStack stack) {
                return this.filter.stream().flatMap(f -> f.getAll().stream()).anyMatch(f -> f == stack.getFluid()) == this.whitelist;
            } else if(ingredient instanceof List<?> list) {
                return list.stream().allMatch(object -> {
                    if(object instanceof FluidStack stack)
                        return this.filter.stream().flatMap(f -> f.getAll().stream()).anyMatch(f -> f == stack.getFluid()) == this.whitelist;
                    return false;
                });
            }
            return false;
        }

        @Override
        public FluidMachineComponent build(IMachineComponentManager manager) {
            return new FluidMachineComponent(manager, this.mode, this.id, this.capacity, this.maxInput, this.maxOutput, this.filter, this.whitelist, this.defaultConfig);
        }
    }
}
