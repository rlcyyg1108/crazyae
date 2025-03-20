package dev.beecube31.crazyae2.common.parts.implementations;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.definitions.IItemDefinition;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.core.settings.TickRates;
import appeng.helpers.Reflected;
import appeng.items.parts.PartModels;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.parts.PartModel;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import dev.beecube31.crazyae2.Tags;
import dev.beecube31.crazyae2.common.interfaces.mana.IManaLinkableDevice;
import dev.beecube31.crazyae2.common.sync.CrazyAEGuiBridge;
import dev.beecube31.crazyae2.common.sync.CrazyAEGuiHandler;
import dev.beecube31.crazyae2.core.CrazyAE;
import dev.beecube31.crazyae2.core.api.storage.IManaStorageChannel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import vazkii.botania.api.mana.*;

public class PartManaExportBus extends CrazyAEPartSharedBus implements IManaLinkableDevice {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Tags.MODID, "part/mana/export_bus_base");
    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, new ResourceLocation(Tags.MODID, "part/mana/export_bus_off"));
    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, new ResourceLocation(Tags.MODID, "part/mana/export_bus_on"));
    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, new ResourceLocation(Tags.MODID, "part/mana/export_bus_has_channel"));

    private final IActionSource source;

    private int connectionX = Integer.MIN_VALUE;
    private int connectionY = Integer.MIN_VALUE;
    private int connectionZ = Integer.MIN_VALUE;
    private boolean hasConnection;

    @Reflected
    public PartManaExportBus(final ItemStack is) {
        super(is);

        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        this.getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.source = new MachineSource(this);
    }

    @Override
    public void getBoxes(final IPartCollisionHelper bch) {
        bch.addBox(6, 6, 11, 10, 10, 13);
    }

    @Override
    public void readFromNBT(final NBTTagCompound extra) {
        super.readFromNBT(extra);
        this.hasConnection = extra.getBoolean("hasConnection");
        if (extra.hasKey("connection")) {
            int[] connection = extra.getIntArray("connection");
            if (connection.length > 0) {
                this.connectionX = connection[0];
                this.connectionY = connection[1];
                this.connectionZ = connection[2];

            }
        }
    }

    @Override
    public void writeToNBT(final NBTTagCompound extra) {
        super.writeToNBT(extra);
        extra.setBoolean("hasConnection", this.hasConnection);
        if (this.hasConnection) {
            int[] poz = new int[3];
            poz[0] = this.connectionX;
            poz[1] = this.connectionY;
            poz[2] = this.connectionZ;
            extra.setIntArray("connection", poz);
        }
    }


    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 5;
    }

    @Override
    public boolean onPartActivate(final EntityPlayer player, final EnumHand hand, final Vec3d pos) {
        if (Platform.isServer()) {
            CrazyAEGuiHandler.openGUI(player, this.getHost().getTile(), this.getSide(), CrazyAEGuiBridge.IMPROVED_BUS);
        }
        return true;
    }

    @Override
    @NotNull
    public TickingRequest getTickingRequest(final @NotNull IGridNode node) {
        return new TickingRequest(1, TickRates.ExportBus.getMax(), false, false);
    }

    @Override
    @NotNull
    public TickRateModulation tickingRequest(final @NotNull IGridNode node, final int ticksSinceLastCall) {
        return this.doBusWork();
    }

    @Override
    protected TickRateModulation doBusWork() {
        if (!this.canDoBusWork()) {
            return TickRateModulation.IDLE;
        }

        if (this.hasConnection && !this.getTile().getWorld().isRemote) {
            BlockPos pos = new BlockPos(this.connectionX, this.connectionY, this.connectionZ);
            final TileEntity te = this.getTile().getWorld().getTileEntity(pos);
            if (!(te instanceof IManaPool poolReceiver)) {
                this.hasConnection = false;
                return TickRateModulation.SLOWER;
            }
            if (this.getTile().getWorld().isBlockLoaded(pos)) {
                try {
                    final IEnergyGrid energyGrid = this.getProxy().getEnergy();

                    final IMEMonitor<IAEItemStack> inv = this.getProxy()
                            .getStorage()
                            .getInventory(
                                    AEApi.instance().storage().getStorageChannel(IManaStorageChannel.class));

                    IAEItemStack item = AEItemStack.fromItemStack(CrazyAE.definitions().items().manaAsAEStack()
                            .maybeStack(Math.min(this.calculateManaToSend(), te.getUpdateTag().getInteger("manaCap")
                                    - ((IManaPool) te).getCurrentMana())).orElse(ItemStack.EMPTY));

                    if (item != null && item.getStackSize() > 0) {
                        IAEItemStack simulate = Platform.poweredExtraction(energyGrid, inv, item, this.source, Actionable.SIMULATE);
                        if (simulate != null && simulate.getStackSize() > 0) {
                            item.setStackSize(Math.min(item.getStackSize(), simulate.getStackSize()));
                            poolReceiver.recieveMana((int) item.getStackSize());
                            Platform.poweredExtraction(energyGrid, inv, item, this.source, Actionable.MODULATE);
                            return TickRateModulation.URGENT;
                        }
                    }
                } catch (GridAccessException e) {
                    // :c
                }
            }
        }

        return TickRateModulation.SLOWER;
    }

    @Override
    protected boolean isSleeping() {
        return false;
    }

    @Override
    public RedstoneMode getRSMode() {
        return (RedstoneMode) this.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
    }

    @Override
    @NotNull
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }

    @Override
    public void link(int x, int y, int z) {
        this.hasConnection = true;
        this.connectionX = x;
        this.connectionY = y;
        this.connectionZ = z;
    }

    @Override
    public int getLinkedPoolPosX() {
        return this.connectionX;
    }

    @Override
    public int getLinkedPoolPosY() {
        return this.connectionY;
    }

    @Override
    public int getLinkedPoolPosZ() {
        return this.connectionZ;
    }

    @Override
    public boolean hasLinkedPool() {
        return this.hasConnection;
    }

    @Override
    public IItemDefinition getBlock() {
        return CrazyAE.definitions().parts().manaExportBus();
    }
}
