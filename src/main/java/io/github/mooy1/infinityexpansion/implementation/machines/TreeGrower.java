package io.github.mooy1.infinityexpansion.implementation.machines;

import io.github.mooy1.infinityexpansion.lists.Categories;
import io.github.mooy1.infinityexpansion.lists.InfinityRecipes;
import io.github.mooy1.infinityexpansion.lists.Items;
import io.github.mooy1.infinityexpansion.lists.RecipeTypes;
import io.github.mooy1.infinityexpansion.utils.Utils;
import io.github.mooy1.infinitylib.math.RandomUtils;
import io.github.mooy1.infinitylib.objects.AbstractContainer;
import io.github.mooy1.infinitylib.presets.MenuPreset;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Grows trees in a virtual interface
 *
 * @author Mooy1
 */
public class TreeGrower extends AbstractContainer implements EnergyNetComponent, RecipeDisplayItem {

    public static final int ENERGY1 = 36;
    public static final int ENERGY2 = 180;
    public static final int ENERGY3 = 1800;
    public static final int SPEED1 = 1;
    public static final int SPEED2 = 5;
    public static final int SPEED3 = 25;
    public static final int TIME = 600;
    private final Type type;

    private static final int[] OUTPUT_SLOTS = Utils.largeOutput;
    private static final int[] INPUT_SLOTS = {
            MenuPreset.slot1 + 27
    };
    private static final int STATUS_SLOT = MenuPreset.slot1;

    public TreeGrower(Type type) {
        super(type.getCategory(), type.getItem(), type.getRecipeType(), type.getRecipe());
        this.type = type;

        registerBlockHandler(getId(), (p, b, stack, reason) -> {
            BlockMenu inv = BlockStorage.getInventory(b);

            if (inv != null) {
                Location l = b.getLocation();
                inv.dropItems(l, OUTPUT_SLOTS);
                inv.dropItems(l, INPUT_SLOTS);

                String progressType = getType(b);
                if (progressType != null) {
                    b.getWorld().dropItemNaturally(l, new ItemStack(Objects.requireNonNull(Material.getMaterial(progressType + "_SAPLING"))));
                }
            }

            setProgress(b, 0);
            setBlockData(b, "type", null);

            return true;
        });
    }

    public void setupInv(@Nonnull BlockMenuPreset blockMenuPreset) {
        for (int i : MenuPreset.slotChunk1) {
            blockMenuPreset.addItem(i, MenuPreset.borderItemStatus, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : MenuPreset.slotChunk1) {
            blockMenuPreset.addItem(i + 27, MenuPreset.borderItemInput, ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i : Utils.largeOutputBorder) {
            blockMenuPreset.addItem(i, MenuPreset.borderItemOutput, ChestMenuUtils.getEmptyClickHandler());
        }
        blockMenuPreset.addItem(STATUS_SLOT, MenuPreset.loadingItemRed, ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    public void onNewInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
        if (getProgress(b) == null) {
            setProgress(b, 0);
        }
    }

    @Override
    public int[] getTransportSlots(@Nonnull ItemTransportFlow flow) {
        if (flow == ItemTransportFlow.WITHDRAW) {
            return OUTPUT_SLOTS;
        }

        if (flow == ItemTransportFlow.INSERT) {
            return INPUT_SLOTS;
        }

        return new int[0];
    }

    @Override
    public int[] getTransportSlots(@Nonnull DirtyChestMenu menu, @Nonnull ItemTransportFlow flow, @Nonnull ItemStack item) {
        if (flow == ItemTransportFlow.WITHDRAW) {
            return OUTPUT_SLOTS;
        }

        if (flow == ItemTransportFlow.INSERT && SlimefunTag.SAPLINGS.isTagged(item.getType())) {
            return INPUT_SLOTS;
        }

        return new int[0];
    }

    @Override
    public void tick(@Nonnull Block b, @Nonnull BlockMenu inv) {
        int energy = this.type.getEnergy();
        int charge = getCharge(b.getLocation());
        boolean playerWatching = inv.toInventory() != null && !inv.toInventory().getViewers().isEmpty();

        if (charge < energy) { //not enough energy

            if (playerWatching) {
                inv.replaceExistingItem(STATUS_SLOT, MenuPreset.notEnoughEnergy);
            }

            return;

        }

        int progress = Integer.parseInt(getProgress(b));

        if (progress == 0) { //try to start
            ItemStack input = inv.getItemInSlot(INPUT_SLOTS[0]);

            if (input == null) {

                if (playerWatching) {
                    inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.BLUE_STAINED_GLASS_PANE, "&9Input a sapling"));
                }

            } else {

                String inputType = getInputType(input);

                if (inputType == null) {

                    if (playerWatching) {
                        inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.BARRIER, "&cInput a sapling!"));
                    }

                    for (int slot : OUTPUT_SLOTS) {
                        if (inv.getItemInSlot(slot) == null) {
                            inv.replaceExistingItem(slot, input);
                            inv.consumeItem(INPUT_SLOTS[0], input.getAmount());
                            break;
                        }
                    }

                } else { //start

                    setProgress(b, this.type.getSpeed());
                    setType(b, inputType);
                    inv.consumeItem(INPUT_SLOTS[0], 1);
                    setCharge(b.getLocation(), charge - energy);

                    if (playerWatching) {
                        inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.LIME_STAINED_GLASS_PANE,
                                "&aPlanting... (" + this.type.getSpeed() + "/" + TIME + ")"));
                    }

                }
            }
            return;
        }

        if (progress < TIME) { //progress

            setProgress(b, progress + this.type.getSpeed());
            setCharge(b.getLocation(), charge - energy);

            if (playerWatching) {
                inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.LIME_STAINED_GLASS_PANE,
                        "&aGrowing... (" + (progress + this.type.getSpeed()) + "/" + TIME + ")"));
            }
            return;
        }

        //done
        String type = getType(b);

        ItemStack output1 = new ItemStack(Objects.requireNonNull(Material.getMaterial(type + "_LOG")), RandomUtils.randomFromRange(6, 12));
        ItemStack output2 = new ItemStack(Objects.requireNonNull(Material.getMaterial(type + "_LEAVES")), RandomUtils.randomFromRange(8, 16));
        ItemStack output3 = new ItemStack(Objects.requireNonNull(Material.getMaterial(type + "_SAPLING")), RandomUtils.randomFromRange(1, 2));

        if (!inv.fits(output1, OUTPUT_SLOTS)) {

            if (playerWatching) {
                inv.replaceExistingItem(STATUS_SLOT, MenuPreset.notEnoughRoom);
            }

        } else {
            inv.pushItem(output1, OUTPUT_SLOTS);
            if (inv.fits(output2, OUTPUT_SLOTS)) inv.pushItem(output2, OUTPUT_SLOTS);
            if (inv.fits(output3, INPUT_SLOTS)) inv.pushItem(output3, INPUT_SLOTS);

            if (type.equals("OAK")) {
                ItemStack apple = new ItemStack(Material.APPLE);
                if (inv.fits(apple, OUTPUT_SLOTS)) inv.pushItem(apple, OUTPUT_SLOTS);
            }

            if (playerWatching) {
                inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.LIME_STAINED_GLASS_PANE, "&aHarvesting..."));
            }

            setProgress(b, 0);
            setType(b, null);
            setCharge(b.getLocation(), charge - energy);

        }
    }

    /**
     * This method gets the type of input
     *
     * @param input input item
     * @return type of input
     */
    @Nullable
    private String getInputType(@NonNull ItemStack input) {
        for (String recipe : INPUTS) {
            if (input.getType() == Material.getMaterial(recipe + "_SAPLING")) return recipe;
        }
        return null;
    }

    private void setType(Block b, String type) {
        setBlockData(b, "type", type);
    }

    private String getType(Block b) {
        return getBlockData(b.getLocation(), "type");
    }

    private void setProgress(Block b, int progress) {
        setBlockData(b, "progress", String.valueOf(progress));
    }

    private String getProgress(Block b) {
        return getBlockData(b.getLocation(), "progress");
    }

    private void setBlockData(Block b, String key, String data) {
        BlockStorage.addBlockInfo(b, key, data);
    }

    private String getBlockData(Location l, String key) {
        return BlockStorage.getLocationInfo(l, key);
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return this.type.getEnergy() * 2;
    }

    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> items = new ArrayList<>();

        for (String input : INPUTS) {
            items.add(new ItemStack(Objects.requireNonNull(Material.getMaterial(input + "_SAPLING"))));
            items.add(new ItemStack(Objects.requireNonNull(Material.getMaterial(input + "_LOG"))));
        }
        return items;
    }

    private static final String[] INPUTS = {
            "OAK",
            "DARK_OAK",
            "ACACIA",
            "SPRUCE",
            "BIRCH",
            "JUNGLE"
    };

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Type {
        BASIC(ENERGY1, SPEED1, Categories.BASIC_MACHINES, Items.BASIC_TREE_GROWER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[]{
                new ItemStack(Material.GLASS), new ItemStack(Material.GLASS), new ItemStack(Material.GLASS),
                Items.MAGSTEEL, new ItemStack(Material.PODZOL), Items.MAGSTEEL,
                Items.MACHINE_CIRCUIT, Items.BASIC_VIRTUAL_FARM, Items.MACHINE_CIRCUIT
        }),
        ADVANCED(ENERGY2, SPEED2, Categories.ADVANCED_MACHINES, Items.ADVANCED_TREE_GROWER, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[]{
                SlimefunItems.HARDENED_GLASS, SlimefunItems.HARDENED_GLASS, SlimefunItems.HARDENED_GLASS,
                Items.MAGNONIUM, Items.BASIC_TREE_GROWER, Items.MAGNONIUM,
                Items.MACHINE_CIRCUIT, Items.MACHINE_CORE, Items.MACHINE_CIRCUIT
        }),
        INFINITY(ENERGY3, SPEED3, Categories.INFINITY_CHEAT, Items.INFINITY_TREE_GROWER, RecipeTypes.INFINITY_WORKBENCH, InfinityRecipes.getRecipe(Items.INFINITY_TREE_GROWER));

        private final int energy;
        private final int speed;
        @Nonnull
        private final Category category;
        private final SlimefunItemStack item;
        private final RecipeType recipeType;
        private final ItemStack[] recipe;
    }
}
