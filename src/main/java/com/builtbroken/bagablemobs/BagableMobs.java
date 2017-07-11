package com.builtbroken.bagablemobs;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 7/11/2017.
 */
@Mod(modid = BagableMobs.DOMAIN, name = "Bagable Mobs", version = BagableMobs.VERSION)
public class BagableMobs
{
    public static final String DOMAIN = "bagablemobs";
    public static final String PREFIX = DOMAIN + ":";

    public static final String MAJOR_VERSION = "@MAJOR@";
    public static final String MINOR_VERSION = "@MINOR@";
    public static final String REVISION_VERSION = "@REVIS@";
    public static final String BUILD_VERSION = "@BUILD@";
    public static final String VERSION = MAJOR_VERSION + "." + MINOR_VERSION + "." + REVISION_VERSION + "." + BUILD_VERSION;

    public static Item itemBag;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        itemBag = new ItemBag();
        GameRegistry.registerItem(itemBag, "mobbag");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {

    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        GameRegistry.addShapedRecipe(new ItemStack(itemBag), "LSL", "LBL", " L ", 'L', Items.leather, 'B', Items.carrot, 'S', Items.string);
    }
}
