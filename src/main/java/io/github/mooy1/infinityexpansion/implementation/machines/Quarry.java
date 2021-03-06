package io.github.mooy1.infinityexpansion.implementation.machines;

import io.github.mooy1.infinityexpansion.InfinityExpansion;
import io.github.mooy1.infinityexpansion.lists.Categories;
import io.github.mooy1.infinityexpansion.lists.InfinityRecipes;
import io.github.mooy1.infinityexpansion.lists.Items;
import io.github.mooy1.infinityexpansion.lists.RecipeTypes;
import io.github.mooy1.infinitylib.math.RandomUtils;
import io.github.mooy1.infinitylib.objects.AbstractContainer;
import io.github.mooy1.infinitylib.presets.MenuPreset;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Mines stuff
 *
 * @author Mooy1
 */
public class Quarry extends AbstractContainer implements EnergyNetComponent, RecipeDisplayItem {

    public static final int BASIC_SPEED = 1;
    public static final int ADVANCED_SPEED = 2;
    public static final int VOID_SPEED = 4;
    public static final int INFINITY_SPEED = 16;

    public static final int BASIC_ENERGY = 360;
    public static final int ADVANCED_ENERGY = 1440;
    public static final int VOID_ENERGY = 5760;
    public static final int INFINITY_ENERGY = 23040;

    private final Type type;

    private static final int[] OUTPUT_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private static final int STATUS_SLOT = 4;

    public Quarry(Type type) {
        super(type.category, type.item, type.recipeType, type.recipe);
        this.type = type;

        registerBlockHandler(getId(), (p, b, stack, reason) -> {
            BlockMenu inv = BlockStorage.getInventory(b);

            if (inv != null) {
                inv.dropItems(b.getLocation(), OUTPUT_SLOTS);
            }

            return true;
        });
    }

    @Override
    public int[] getTransportSlots(@Nonnull ItemTransportFlow flow) {
        if (flow == ItemTransportFlow.WITHDRAW) {
            return OUTPUT_SLOTS;
        }
        return new int[0];
    }

    @Override
    public void tick(@Nonnull Block b, @Nonnull BlockMenu inv) {
        
        if (getCharge(b.getLocation()) < this.type.energy) {
            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS_SLOT, MenuPreset.notEnoughEnergy);
            }
            return;
        }
        
        ItemStack outputItem; 
        
        if (RandomUtils.chanceIn(this.type.chance)) {
            outputItem = RandomUtils.randomOutput(this.type.output);
            Material outputType = outputItem.getType();
            if (b.getWorld().getEnvironment() != World.Environment.NETHER && (outputType == Material.QUARTZ || outputType == Material.NETHERITE_INGOT || outputType == Material.NETHERRACK)) {
                outputItem = this.type.cobble;
            }
        } else {
            outputItem = this.type.cobble;
        }
        
        if (!inv.fits(outputItem, OUTPUT_SLOTS)) {
            if (inv.hasViewer()) {
                inv.replaceExistingItem(STATUS_SLOT, MenuPreset.notEnoughRoom);
            }
            return;
        }

        if (inv.hasViewer()) {
            inv.replaceExistingItem(STATUS_SLOT, new CustomItem(Material.LIME_STAINED_GLASS_PANE, "&aMining..."));
        }
        removeCharge(b.getLocation(), this.type.energy);
        inv.pushItem(outputItem, OUTPUT_SLOTS);
            
    }

    @Override
    public void setupInv(@Nonnull BlockMenuPreset blockMenuPreset) {
        for (int i = 0 ; i < 4 ; i++) {
            blockMenuPreset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i = 5 ; i < 9 ; i++) {
            blockMenuPreset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int i = 45 ; i < 54 ; i++) {
            blockMenuPreset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
        blockMenuPreset.addItem(STATUS_SLOT, MenuPreset.loadingItemRed, ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    public int getCapacity() {
        return this.type.energy * 2;
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Nonnull
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> items = new ArrayList<>();

        for (ItemStack item : this.type.output) {
            if (!items.contains(item)) {
                items.add(null);
                items.add(item);
            }
        }

        return items;
    }

    @Nonnull
    @Override
    public String getRecipeSectionLabel(@Nonnull Player p) {
        return "&7Mines:";
    }
    
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Type {
        BASIC(Categories.ADVANCED_MACHINES, BASIC_ENERGY, Items.BASIC_QUARRY, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.MAGSTEEL_PLATE, SlimefunItems.CARBONADO_EDGED_CAPACITOR, Items.MAGSTEEL_PLATE,
                new ItemStack(Material.IRON_PICKAXE), SlimefunItems.GEO_MINER, new ItemStack(Material.IRON_PICKAXE),
                Items.MACHINE_CIRCUIT, Items.MACHINE_CORE, Items.MACHINE_CIRCUIT}, BASIC_OUTPUTS, 5, makeOutput(Material.COBBLESTONE, 1)
        ),
        ADVANCED(Categories.ADVANCED_MACHINES, ADVANCED_ENERGY, Items.ADVANCED_QUARRY, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.MACHINE_PLATE, SlimefunItems.ENERGIZED_CAPACITOR, Items.MACHINE_PLATE,
                new ItemStack(Material.DIAMOND_PICKAXE), Items.BASIC_QUARRY, new ItemStack(Material.DIAMOND_PICKAXE),
                Items.MACHINE_CIRCUIT, Items.MACHINE_CORE, Items.MACHINE_CIRCUIT}, ADVANCED_OUTPUTS, 4, makeOutput(Material.COBBLESTONE, 12)
        ),
        VOID(Categories.ADVANCED_MACHINES, VOID_ENERGY, Items.VOID_QUARRY, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                Items.VOID_INGOT, Items.VOID_CAPACITOR, Items.VOID_INGOT,
                new ItemStack(Material.NETHERITE_PICKAXE), Items.ADVANCED_QUARRY, new ItemStack(Material.NETHERITE_PICKAXE),
                Items.MACHINE_CIRCUIT, Items.MACHINE_CORE, Items.MACHINE_CIRCUIT}, VOID_OUTPUTS, 3, makeOutput(Material.COBBLESTONE, 4)
        ),
        INFINITY(Categories.INFINITY_CHEAT, INFINITY_ENERGY, Items.INFINITY_QUARRY, RecipeTypes.INFINITY_WORKBENCH,
                InfinityRecipes.getRecipe(Items.INFINITY_QUARRY), INFINITY_OUTPUTS, 2, makeOutput(Material.COBBLESTONE, 16)
        );
        
        private final Category category;
        private final int energy;
        private final SlimefunItemStack item;
        private final RecipeType recipeType;
        private final ItemStack[] recipe;
        private final ItemStack[] output;
        private final int chance;
        private final ItemStack cobble;

    }

    private static final ItemStack[] BASIC_OUTPUTS = {
            makeOutput(Material.COAL, 4),
            makeOutput(Material.IRON_ORE, 1),
            makeOutput(Material.COAL, 4),
            makeOutput(Material.GOLD_ORE, 1),
            makeOutput(Material.LAPIS_LAZULI, 4),
            makeOutput(Material.EMERALD, 1),
            makeOutput(Material.DIAMOND, 1),
            makeOutput(Material.REDSTONE, 4),
    };

    private static final ItemStack[] ADVANCED_OUTPUTS = {
            makeOutput(Material.COAL, 8),
            makeOutput(Material.IRON_INGOT, 2),
            makeOutput(Material.NETHERRACK, 2),
            makeOutput(Material.NETHERRACK, 2),
            makeOutput(Material.QUARTZ, 8),
            makeOutput(Material.GOLD_INGOT, 2),
            makeOutput(Material.COAL, 8),
            makeOutput(Material.LAPIS_LAZULI, 8),
            makeOutput(Material.EMERALD, 2),
            makeOutput(Material.DIAMOND, 2),
            makeOutput(Material.REDSTONE, 8),
            makeOutput(Material.NETHERITE_INGOT, 1)
    };

    private static final ItemStack[] VOID_OUTPUTS = {
            makeOutput(Material.COAL, 16),
            makeOutput(SlimefunItems.SIFTED_ORE, 6),
            makeOutput(Material.COBBLESTONE, 4),
            makeOutput(Material.IRON_INGOT, 4),
            makeOutput(Material.NETHERRACK, 4),
            makeOutput(Material.NETHERRACK, 4),
            makeOutput(Material.QUARTZ, 16),
            makeOutput(Material.COAL, 16),
            makeOutput(Material.GOLD_INGOT, 4),
            makeOutput(Material.COAL, 16),
            makeOutput(SlimefunItems.SIFTED_ORE, 6),
            makeOutput(Material.LAPIS_LAZULI, 16),
            makeOutput(Material.EMERALD, 4),
            makeOutput(Material.DIAMOND, 4),
            makeOutput(Material.REDSTONE, 16),
            makeOutput(SlimefunItems.GOLD_24K, 4),
            makeOutput(Material.NETHERITE_INGOT, 2),
    };

    private static final ItemStack[] INFINITY_OUTPUTS = {
            makeOutput(Material.COAL, 64),
            makeOutput(Material.IRON_INGOT, 16),
            makeOutput(Material.NETHERRACK, 16),
            makeOutput(Material.NETHERRACK, 16),
            makeOutput(Material.QUARTZ, 64),
            makeOutput(Material.COAL, 64),
            makeOutput(Material.GOLD_INGOT, 16),
            makeOutput(SlimefunItems.MAGNESIUM_INGOT, 16),
            makeOutput(SlimefunItems.COPPER_INGOT, 16),
            makeOutput(SlimefunItems.ZINC_INGOT, 16),
            makeOutput(SlimefunItems.TIN_INGOT, 16),
            makeOutput(SlimefunItems.ALUMINUM_INGOT, 16),
            makeOutput(SlimefunItems.SILVER_INGOT, 16),
            makeOutput(SlimefunItems.LEAD_INGOT, 16),
            makeOutput(Material.LAPIS_LAZULI, 64),
            makeOutput(Material.EMERALD, 16),
            makeOutput(Material.COAL, 64),
            makeOutput(Material.DIAMOND, 16),
            makeOutput(Material.REDSTONE, 64),
            makeOutput(SlimefunItems.GOLD_24K, 16),
            makeOutput(Material.NETHERITE_INGOT, 8),
    };

    private static ItemStack makeOutput(Material material, int amount) {
        return new ItemStack(material, (int) (InfinityExpansion.getVanillaScale() * Math.ceil((float) amount / 2)));
    }

    private static ItemStack makeOutput(SlimefunItemStack stack, int amount) {
        return new SlimefunItemStack(stack, amount);
    }

}
