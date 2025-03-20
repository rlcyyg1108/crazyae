package dev.beecube31.crazyae2.client.gui.implementations;

import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.client.gui.widgets.ISortSource;
import appeng.core.localization.GuiText;
import appeng.util.IConfigManagerHost;
import dev.beecube31.crazyae2.client.gui.CrazyAEBaseGui;
import dev.beecube31.crazyae2.client.gui.sprites.Sprite;
import dev.beecube31.crazyae2.client.gui.widgets.ProgressBar;
import dev.beecube31.crazyae2.client.me.EnergyRepo;
import dev.beecube31.crazyae2.common.containers.ContainerEnergyTerminal;
import dev.beecube31.crazyae2.common.containers.base.slot.InternalEnergySlotME;
import dev.beecube31.crazyae2.common.containers.base.slot.SlotEnergyME;
import dev.beecube31.crazyae2.common.interfaces.gui.ICrazyAEProgressProvider;
import dev.beecube31.crazyae2.common.interfaces.gui.IGuiElementsCallbackHandler;
import dev.beecube31.crazyae2.common.sync.CrazyAEGuiText;
import dev.beecube31.crazyae2.common.sync.CrazyAEGuiTooltip;
import dev.beecube31.crazyae2.common.util.Utils;
import dev.beecube31.crazyae2.core.CrazyAE;
import dev.beecube31.crazyae2.core.client.CrazyAEClientConfig;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Keyboard;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class GuiEnergyTerminal extends CrazyAEBaseGui implements ISortSource, IConfigManagerHost, IGuiElementsCallbackHandler, ICrazyAEProgressProvider {
    private final List<SlotEnergyME> slots = new LinkedList<>();
    private final EnergyRepo repo;
    private final ContainerEnergyTerminal container;

    private ProgressBar efBar;
    private ProgressBar euBar;
    private ProgressBar feBar;
    private ProgressBar qeBar;
    private ProgressBar seBar;


    protected ITerminalHost terminal;

    public GuiEnergyTerminal(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        this(inventoryPlayer, te, new ContainerEnergyTerminal(inventoryPlayer, te));
    }

    public GuiEnergyTerminal(InventoryPlayer inventoryPlayer, final ITerminalHost te, final ContainerEnergyTerminal c) {
        super(c);
        this.terminal = te;
        this.xSize = 176;
        this.ySize = 190;
        this.repo = new EnergyRepo();
        (this.container = (ContainerEnergyTerminal) this.inventorySlots).setGui(this);
    }

    @Override
    public void initGui() {
        this.mc.player.openContainer = this.inventorySlots;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;

        for (int x = 0; x < 6; x++) {
            SlotEnergyME slot = new SlotEnergyME(new InternalEnergySlotME(this.repo, x, Integer.MIN_VALUE, Integer.MIN_VALUE));
            this.getSlots().add(slot);
            this.inventorySlots.inventorySlots.add(slot);
        }

        this.buttonList.add(this.efBar = new ProgressBar(
                this,
                this.guiLeft + 10,
                this.guiTop + 18,
                72,
                18,
                ProgressBar.Direction.HORIZONTAL,
                CrazyAEGuiText.EF_ENERGY.getLocal(),
                1,
                this.getGuiHue(),
                Sprite.ENERGY_BAR_EF,
                Sprite.ENERGY_BAR_EMPTY,
                true
        ).disableHue());

        this.buttonList.add(this.feBar = new ProgressBar(
                this,
                this.guiLeft + 10,
                this.guiTop + 51,
                72,
                18,
                ProgressBar.Direction.HORIZONTAL,
                CrazyAEGuiText.FE_ENERGY.getLocal(),
                2,
                this.getGuiHue(),
                Sprite.ENERGY_BAR_NE,
                Sprite.ENERGY_BAR_EMPTY,
                true
        ).disableHue());

        this.buttonList.add(this.seBar = new ProgressBar(
                this,
                this.guiLeft + 94,
                this.guiTop + 18,
                72,
                18,
                ProgressBar.Direction.HORIZONTAL,
                CrazyAEGuiText.SE_ENERGY.getLocal(),
                3,
                this.getGuiHue(),
                Sprite.ENERGY_BAR_SE,
                Sprite.ENERGY_BAR_EMPTY,
                true
        ).disableHue());

        this.buttonList.add(this.qeBar = new ProgressBar(
                this,
                this.guiLeft + 94,
                this.guiTop + 51,
                72,
                18,
                ProgressBar.Direction.HORIZONTAL,
                CrazyAEGuiText.QE_ENERGY.getLocal(),
                4,
                this.getGuiHue(),
                Sprite.ENERGY_BAR_QE,
                Sprite.ENERGY_BAR_EMPTY,
                true
        ).disableHue());

        this.buttonList.add(this.euBar = new ProgressBar(
                this,
                this.guiLeft + 50,
                this.guiTop + 83,
                72,
                18,
                ProgressBar.Direction.HORIZONTAL,
                CrazyAEGuiText.EU_ENERGY.getLocal(),
                5,
                this.getGuiHue(),
                Sprite.ENERGY_BAR_EU,
                Sprite.ENERGY_BAR_EMPTY,
                true
        ).disableHue());

        this.getGuiHue().setParams(
                (float) CrazyAEClientConfig.getColorizerColorRed() / 255,
                (float) CrazyAEClientConfig.getColorizerColorGreen() / 255,
                (float) CrazyAEClientConfig.getColorizerColorBlue() / 255,
                1.0F
        );

        this.getTextHue().setParams(
                (float) CrazyAEClientConfig.getColorizerTextColorRed() / 255,
                (float) CrazyAEClientConfig.getColorizerTextColorGreen() / 255,
                (float) CrazyAEClientConfig.getColorizerTextColorBlue() / 255,
                1.0F
        );
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.drawString(CrazyAEGuiText.ENERGY_TERMINAL.getLocal(), 8, 6);
        this.drawString(GuiText.inventory.getLocal(), 8, this.ySize - 96 + 3);
        this.getTextHue().endDrawHue();
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.bindTexture(this.getBackground());
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    public void updateScreen() {
        this.repo.setPower(this.container.isPowered());
        super.updateScreen();
    }

    public void postUpdate(final List<IAEItemStack> list) {
        for (final IAEItemStack is : list) {
            this.repo.postUpdate(is);
        }

        this.repo.updateView();
    }

    @Override
    public Enum<SortOrder> getSortBy() {
        return SortOrder.NAME;
    }

    @Override
    public Enum<SortDir> getSortDir() {
        return SortDir.ASCENDING;
    }

    @Override
    public Enum<ViewItems> getSortDisplay() {
        return ViewItems.ALL;
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        this.repo.updateView();
    }

    protected List<SlotEnergyME> getSlots() {
        return this.slots;
    }

    @Override
    protected boolean isPowered() {
        return this.repo.hasPower();
    }

    protected String getBackground() {
        return "guis/energy_terminal.png";
    }

    @Override
    public void onInteractionStart() {}

    @Override
    public void onInteractionUpdate() {}

    @Override
    public void onInteractionEnd() {}

    @Override
    public CrazyAEBaseGui getCallbackHandler() {
        return this;
    }

    @Override
    public double getCurrentProgress() {
        return -404;
    }

    @Override
    public double getCurrentProgress(int index) {
        AtomicLong ret = new AtomicLong();

        switch (index) {
            case 1 -> this.getSlots().forEach(is -> {
                if (CrazyAE.definitions().items().EFEnergyAsAeStack().isSameAs(is.getDefinition())) {
                    ret.set(is.getAEStack().getStackSize());
                }
            });

            case 2 -> this.getSlots().forEach(is -> {
                if (CrazyAE.definitions().items().FEEnergyAsAeStack().isSameAs(is.getDefinition())) {
                    ret.set(is.getAEStack().getStackSize());
                }
            });

            case 3 -> this.getSlots().forEach(is -> {
                if (CrazyAE.definitions().items().SEEnergyAsAeStack().isSameAs(is.getDefinition())) {
                    ret.set(is.getAEStack().getStackSize());
                }
            });

            case 4 -> this.getSlots().forEach(is -> {
                if (CrazyAE.definitions().items().QEEnergyAsAeStack().isSameAs(is.getDefinition())) {
                    ret.set(is.getAEStack().getStackSize());
                }
            });

            case 5 -> this.getSlots().forEach(is -> {
                if (CrazyAE.definitions().items().EUEnergyAsAeStack().isSameAs(is.getDefinition())) {
                    ret.set(is.getAEStack().getStackSize());
                }
            });
        }

        return ret.get();

    }

    @Override
    public double getMaxProgress() {
        return 1;
    }

    @Override
    public double getMaxProgress(int index) {
        return 1;
    }

    @Override
    public String getTooltip(String title, boolean disableMaxProgress, int tooltipID) {
        return title +
                "\n" +
                CrazyAEGuiTooltip.STORED.getLocalWithSpaceAtEnd() +
                (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT) ? this.getCurrentProgress(tooltipID)
                    : Utils.energyToFormattedString(this.getCurrentProgress(tooltipID)));
    }
}
