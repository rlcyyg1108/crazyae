package dev.beecube31.crazyae2.mixins.features.cellinformation;

import appeng.api.config.IncludeExclude;
import appeng.api.storage.ICellInventory;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.IClientHelper;
import appeng.core.api.ApiClientHelper;
import appeng.core.localization.GuiText;
import appeng.fluids.items.FluidDummyItem;
import appeng.fluids.util.AEFluidStack;
import appeng.util.ReadableNumberConverter;
import appeng.util.item.AEItemStack;
import dev.beecube31.crazyae2.common.sync.CrazyAEGuiText;
import dev.beecube31.crazyae2.common.sync.CrazyAEGuiTooltip;
import dev.beecube31.crazyae2.common.sync.CrazyAETooltip;
import dev.beecube31.crazyae2.core.api.storage.IManaStorageChannel;
import dev.beecube31.crazyae2.core.api.storage.energy.IEnergyStorageChannel;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(value = ApiClientHelper.class, remap = false)
public abstract class MixinApiClientHelper implements IClientHelper {
    @Shadow protected abstract String fluidStackSize(long size);

    @Inject(method = "addCellInformation", at = @At(value = "HEAD"), remap = false, cancellable = true)
    private void patchCellView(ICellInventoryHandler handler, List<String> lines, CallbackInfo ci) {
        if (handler == null) {
            return;
        }

        final ICellInventory<?> cellInventory = handler.getCellInv();

        if (cellInventory != null) {
            lines.add(cellInventory.getUsedBytes() + " " + GuiText.Of.getLocal() + ' ' + cellInventory.getTotalBytes() + ' ' + GuiText.BytesUsed.getLocal());

            lines.add(
                        (cellInventory.getChannel() instanceof IFluidStorageChannel ? cellInventory.getStoredItemCount() / 1000 : cellInventory.getStoredItemCount())
                        + " "
                        + GuiText.Of.getLocal()
                        + ' '
                        + (cellInventory.getTotalBytes() - cellInventory.getBytesPerType() - (Math.max(cellInventory.getStoredItemTypes() - 1, 0))) * 8
                        + (cellInventory.getChannel() instanceof IFluidStorageChannel ? "B " : ' ')
                        + (
                                cellInventory.getChannel() instanceof IEnergyStorageChannel ? CrazyAEGuiText.ENERGY_USED.getLocal()
                                : cellInventory.getChannel() instanceof IFluidStorageChannel ? CrazyAEGuiText.FLUID_USED.getLocal()
                                : cellInventory.getChannel() instanceof IEnergyStorageChannel ? CrazyAEGuiText.ENERGY_USED.getLocal()
                                : cellInventory.getChannel() instanceof IManaStorageChannel ? CrazyAEGuiText.MANA_USED.getLocal()
                                : CrazyAEGuiText.ITEMS_USED.getLocal()
                        )
            );

            if (!(cellInventory.getChannel() instanceof IManaStorageChannel)) {
                lines.add(
                        cellInventory.getStoredItemTypes()
                        + " "
                        + GuiText.Of.getLocal()
                        + ' '
                        + cellInventory.getTotalItemTypes()
                        + ' '
                        + GuiText.Types.getLocal()
                        + ' '
                        + CrazyAEGuiText.USED.getLocal()
                );
            }

            if (cellInventory.getStoredItemTypes() > 0) {
                lines.add(CrazyAETooltip.SHIFT_FOR_DETAILS.getLocal());
                if (!(cellInventory.getChannel() instanceof IFluidStorageChannel)) {
                    lines.add(CrazyAETooltip.CTRL_FOR_PRECISE_DETAILS.getLocal());
                }
            }
        }

        IItemList<?> itemList = cellInventory.getChannel().createList();

        if (cellInventory.getChannel() instanceof IEnergyStorageChannel) {
            lines.add(String.format(
                    CrazyAEGuiTooltip.ENERGY_CELL_FORMATTING_HINT.getLocal()
            ));
        }

        if (handler.isPreformatted()) {
            final String list = (handler.getIncludeExcludeMode() == IncludeExclude.WHITELIST ? GuiText.Included : GuiText.Excluded).getLocal();

            if (handler.isFuzzy()) {
                lines.add("[" + GuiText.Partitioned.getLocal() + "]" + " - " + list + ' ' + GuiText.Fuzzy.getLocal());
            } else {
                lines.add("[" + GuiText.Partitioned.getLocal() + "]" + " - " + list + ' ' + GuiText.Precise.getLocal());
            }

            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                IItemHandler inv = cellInventory.getConfigInventory();
                cellInventory.getAvailableItems((IItemList) itemList);
                for (int i = 0; i < inv.getSlots(); i++) {
                    final ItemStack is = inv.getStackInSlot(i);
                    if (!is.isEmpty()) {
                        if (cellInventory.getChannel() instanceof IItemStorageChannel || cellInventory.getChannel() instanceof IEnergyStorageChannel) {
                            lines.remove(CrazyAETooltip.SHIFT_FOR_DETAILS.getLocal());
                            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                                lines.remove(CrazyAETooltip.CTRL_FOR_PRECISE_DETAILS.getLocal());
                            }
                            if (!handler.isFuzzy()) {
                                final IAEItemStack ais = AEItemStack.fromItemStack(is);
                                IAEItemStack stocked = ((IItemList<IAEItemStack>) itemList).findPrecise(ais);
                                lines.add("[" + is.getDisplayName() + "]" + ": " + (stocked == null ? "0" :
                                        (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) ? stocked.getStackSize() : ReadableNumberConverter.INSTANCE.toWideReadableForm(stocked.getStackSize()))
                                ));
                            } else {
                                final IAEItemStack ais = AEItemStack.fromItemStack(is);
                                Collection<IAEItemStack> stocked = ((IItemList<IAEItemStack>) itemList).findFuzzy(ais, handler.getCellInv().getFuzzyMode());

                                int[] ids = OreDictionary.getOreIDs(is);
                                long size = 0;
                                for (IAEItemStack ist : stocked) {
                                    size += ist.getStackSize();
                                }

                                if (is.getItem().isDamageable()) {
                                    lines.add("[" + is.getDisplayName() + "]" + ": " + size);
                                } else if (ids.length > 0) {
                                    StringBuilder sb = new StringBuilder();
                                    for (int j : ids) {
                                        sb.append(OreDictionary.getOreName(j)).append(", ");
                                    }
                                    lines.add("[{" + sb.substring(0, sb.length() - 2) + "}]" + ": " +
                                            (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) ? size : ReadableNumberConverter.INSTANCE.toWideReadableForm(size))
                                    );
                                }
                            }
                        } else if (cellInventory.getChannel() instanceof IFluidStorageChannel) {
                            lines.remove(CrazyAETooltip.SHIFT_FOR_DETAILS.getLocal());
                            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                                lines.remove(CrazyAETooltip.CTRL_FOR_PRECISE_DETAILS.getLocal());
                            }
                            final AEFluidStack ais;
                            if (is.getItem() instanceof FluidDummyItem) {
                                ais = AEFluidStack.fromFluidStack(((FluidDummyItem) is.getItem()).getFluidStack(is));
                            } else {
                                ais = AEFluidStack.fromFluidStack(FluidUtil.getFluidContained(is));
                            }
                            IAEFluidStack stocked = ((IItemList<IAEFluidStack>) itemList).findPrecise(ais);
                            lines.add("[" + is.getDisplayName() + "]" + ": " + (stocked == null ? "0" : fluidStackSize(stocked.getStackSize())));
                        }
                    }
                }
            }
        } else {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                lines.remove(CrazyAETooltip.SHIFT_FOR_DETAILS.getLocal());
                if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                    lines.remove(CrazyAETooltip.CTRL_FOR_PRECISE_DETAILS.getLocal());
                }
                cellInventory.getAvailableItems((IItemList) itemList);
                boolean hasItems = false;
                for (IAEStack<?> s : itemList) {
                    if (!hasItems) {
                        lines.add("—————————————————");
                        hasItems = true;
                    }
                    if (s instanceof IAEItemStack) {
                        lines.add(((IAEItemStack) s).getDefinition().getDisplayName() + ": " +
                                (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) ? s.getStackSize() : ReadableNumberConverter.INSTANCE.toWideReadableForm(s.getStackSize()))
                        );
                    } else if (s instanceof IAEFluidStack) {
                        lines.add(((IAEFluidStack) s).getFluidStack().getLocalizedName() + ": " + fluidStackSize(s.getStackSize()));
                    }
                }
            }
        }
        ci.cancel();
    }
}
