package dev.beecube31.crazyae2.common.items.cells;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.exceptions.MissingDefinitionException;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.data.IAEStack;
import appeng.core.AEConfig;
import appeng.core.features.AEFeature;
import appeng.items.AEBaseItem;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import dev.beecube31.crazyae2.common.registration.definitions.Materials;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public abstract class BaseCell<T extends IAEStack<T>> extends AEBaseItem implements IStorageCell<T>, IItemGroup {
	protected final Materials.MaterialType component;
	protected final int capacity;
	protected final double idleDrain;
	protected final int bytesPerType;

	public BaseCell(Materials.MaterialType whichCell, int bytes) {
		this(whichCell, bytes, 1, 16);
	}

	public BaseCell(Materials.MaterialType whichCell, int bytes, int bytesPerType) {
		this(whichCell, bytes, bytesPerType, 16);
	}

	public BaseCell(Materials.MaterialType whichCell, int bytes, double idleDrain) {
		this(whichCell, bytes, 1, idleDrain);
	}

	public BaseCell(Materials.MaterialType whichCell, int bytes, int bytesPerType, double idleDrain) {
		this.setMaxStackSize(1);
		this.component = whichCell;
		this.capacity = bytes;
		this.idleDrain = idleDrain;
		this.bytesPerType = bytesPerType;
	}

	public abstract int getTotalTypes(@NotNull ItemStack cellItem);

	public abstract boolean isBlackListed(@NotNull ItemStack cellItem, @NotNull T requestedAddition);

	public abstract String getUnlocalizedGroupName(Set<ItemStack> others, ItemStack is);

	public abstract boolean isEditable(ItemStack is);

	@SideOnly(Side.CLIENT)
	public void addCheckedInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag advancedTooltips) {
		AEApi.instance().client().addCellInformation(AEApi.instance().registries().cell().getCellInventory(stack,
			null, this.getChannel()), lines);
	}

	public int getBytes(@NotNull ItemStack cellItem) {
		return this.capacity;
	}

	public boolean storableInStorageCell() {
		return false;
	}

	public boolean isStorageCell(@NotNull ItemStack i) {
		return true;
	}

	public abstract IItemHandler getUpgradesInventory(ItemStack is);

	public abstract IItemHandler getConfigInventory(ItemStack is);

	public FuzzyMode getFuzzyMode(ItemStack is) {
		try {
			return FuzzyMode.valueOf(Platform.openNbtData(is).getString("FuzzyMode"));
		} catch (Throwable var4) {
			return FuzzyMode.IGNORE_ALL;
		}
	}

	public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
		Platform.openNbtData(is).setString("FuzzyMode", fzMode.name());
	}

	public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, @NotNull EntityPlayer player, @NotNull EnumHand hand) {
		this.disassembleDrive(player.getHeldItem(hand), player);
		return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
	}

	protected boolean disassembleDrive(ItemStack stack, EntityPlayer player) {
		if (player.isSneaking()) {
			if (Platform.isClient()) {
				return false;
			}

			var playerInventory = player.inventory;
			IMEInventoryHandler<T> inv = AEApi.instance().registries().cell().getCellInventory(stack, null, this.getChannel());
			if (inv != null && playerInventory.getCurrentItem() == stack) {
				var ia = InventoryAdaptor.getAdaptor(player);
				var list = inv.getAvailableItems(this.getChannel().createList());
				if (list.isEmpty()) {
					playerInventory.setInventorySlotContents(playerInventory.currentItem, ItemStack.EMPTY);
					var extraB = ia.addItems(this.component.stack(1));
					if (!extraB.isEmpty()) {
						player.dropItem(extraB, false);
					}

					var upgradesInventory = this.getUpgradesInventory(stack);

					for (var upgradeIndex = 0; upgradeIndex < upgradesInventory.getSlots(); ++upgradeIndex) {
						var upgradeStack = upgradesInventory.getStackInSlot(upgradeIndex);
						var leftStack = ia.addItems(upgradeStack);
						if (!leftStack.isEmpty() && upgradeStack.getItem() instanceof IUpgradeModule) {
							player.dropItem(upgradeStack, false);
						}
					}

					this.dropEmptyStorageCellCase(ia, player);
					if (player.inventoryContainer != null) {
						player.inventoryContainer.detectAndSendChanges();
					}

					return true;
				}
			}
		}

		return false;
	}

	protected void dropEmptyStorageCellCase(final InventoryAdaptor ia, final EntityPlayer player) {
		AEApi.instance().definitions().materials().emptyStorageCell().maybeStack(1).ifPresent(is ->
		{
			final var extraA = ia.addItems(is);
			if (!extraA.isEmpty()) {
				player.dropItem(extraA, false);
			}
		});
	}

	public @NotNull EnumActionResult onItemUseFirst(@NotNull EntityPlayer player, @NotNull World world,
	                                                @NotNull BlockPos pos, @NotNull EnumFacing side, float hitX,
													float hitY, float hitZ, @NotNull EnumHand hand) {
		return this.disassembleDrive(player.getHeldItem(hand), player) ?
				EnumActionResult.SUCCESS : EnumActionResult.PASS;
	}

	public @NotNull ItemStack getContainerItem(@NotNull ItemStack itemStack) {
		return AEApi.instance().definitions().materials().emptyStorageCell().maybeStack(1)
			.orElseThrow(() -> new MissingDefinitionException(
				"Tried to use empty storage cells while basic storage cells are defined."));
	}

	public boolean hasContainerItem(@NotNull ItemStack stack) {
		return AEConfig.instance().isFeatureEnabled(AEFeature.ENABLE_DISASSEMBLY_CRAFTING);
	}

	@Override
	public int getBytesPerType(@NotNull ItemStack itemStack) {
		return this.bytesPerType;
	}

	@Override
	public double getIdleDrain() {
		return this.idleDrain;
	}
}
