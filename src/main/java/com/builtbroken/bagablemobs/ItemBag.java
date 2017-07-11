package com.builtbroken.bagablemobs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import java.util.List;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 7/11/2017.
 */
public class ItemBag extends Item
{
    public static int mobsPerBag = 100;

    @SideOnly(Side.CLIENT)
    public IIcon filledIcon;

    public ItemBag()
    {
        setUnlocalizedName(BagableMobs.PREFIX + "bag");
        setTextureName(BagableMobs.PREFIX + "bag");
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack bag, EntityPlayer player, List list, boolean b)
    {
        final boolean isEmpty = bag.getTagCompound() == null || !bag.getTagCompound().hasKey("data");
        if (!isEmpty)
        {
            final String saveID = bag.getTagCompound().getCompoundTag("data").getString("saveID");
            list.add(I18n.format(getUnlocalizedName() + ".info.entity", saveID));

            list.add(I18n.format(getUnlocalizedName() + ".info.count", "" + bag.getTagCompound().getCompoundTag("data").getInteger("count")));
        }
        else
        {
            list.add(I18n.format(getUnlocalizedName() + ".info.empty"));
        }
    }

    @Override
    public void onUpdate(ItemStack bag, World world, Entity entity, int slot, boolean held)
    {
        //TODO add optional feature to have mobs escape the bag
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack bag, EntityPlayer player, EntityLivingBase entity)
    {
        final boolean isEmpty = bag.getTagCompound() == null || !bag.getTagCompound().hasKey("data");
        if (canCapture(entity))
        {
            if (isEmpty)
            {
                if (!player.worldObj.isRemote)
                {
                    try
                    {
                        //Copy
                        ItemStack stack = bag.copy();
                        stack.stackSize = 1;

                        //Encode item
                        encode(stack, entity);

                        //Add to inventory
                        if (!player.inventory.addItemStackToInventory(stack))
                        {
                            player.dropPlayerItemWithRandomChoice(stack, false);
                        }

                        //Update inventory
                        bag.stackSize--;
                        player.inventoryContainer.detectAndSendChanges();

                        player.worldObj.removeEntity(entity);
                    }
                    catch (Exception e)
                    {
                        player.addChatComponentMessage(new ChatComponentTranslation(getUnlocalizedName() + ".error.unexpected", e.getMessage()));
                        e.printStackTrace();
                    }
                }
                return true;
            }
            else
            {
                int count = bag.getTagCompound().getCompoundTag("data").getInteger("count");
                if (count < mobsPerBag)
                {
                    NBTTagCompound newEntity = toNBT(entity);
                    NBTTagCompound oldEntity = bag.getTagCompound().getCompoundTag("data");

                    if (newEntity.getCompoundTag("nbt").equals(oldEntity.getCompoundTag("nbt")))
                    {
                        bag.getTagCompound().getCompoundTag("data").setInteger("count", count + 1);
                        player.inventoryContainer.detectAndSendChanges();
                        player.worldObj.removeEntity(entity);
                    }
                    return true;
                }
                else
                {
                    player.addChatComponentMessage(new ChatComponentTranslation(getUnlocalizedName() + ".error.full"));
                }
            }
        }
        return false;
    }

    public boolean canCapture(EntityLivingBase entity)
    {
        //TODO do blacklist check to prevent some mobs from being capture
        if (entity instanceof EntityPlayer || entity instanceof IBossDisplayData)
        {
            return false;
        }
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack p_77659_1_, World p_77659_2_, EntityPlayer p_77659_3_)
    {
        return p_77659_1_;
    }

    @Override
    public boolean onItemUse(ItemStack bag, EntityPlayer player, World world, int x, int y, int z, int side, float xx, float yy, float zz)
    {
        //Only do logic server side
        final boolean isEmpty = bag.getTagCompound() == null || !bag.getTagCompound().hasKey("data");
        if (!isEmpty && player.isSneaking())
        {
            if (world.isRemote)
            {
                return true;
            }

            final String saveID = bag.getTagCompound().getCompoundTag("data").getString("saveID");

            //Translates based on side clicked
            //      Prevents mob placement in wall
            if (side == 0)
            {
                --y;
            }
            else if (side == 1)
            {
                ++y;
            }
            else if (side == 2)
            {
                --z;
            }
            else if (side == 3)
            {
                ++z;
            }
            else if (side == 4)
            {
                --x;
            }
            else if (side == 5)
            {
                ++x;
            }

            try
            {
                Entity entity = decode(bag, world);
                if (entity != null)
                {
                    entity.setPosition(x, y, z);
                    entity.onGround = true;
                    //TODO figure out how to handle unique IDs being the same if creative spawned
                    world.spawnEntityInWorld(entity);

                    if (!player.capabilities.isCreativeMode)
                    {
                        //Consume bag
                        int count = bag.getTagCompound().getCompoundTag("data").getInteger("count");
                        count--;
                        if (count <= 0)
                        {
                            bag.stackSize--;

                            //Drop bag
                            ItemStack stack = new ItemStack(this);
                            if (!player.inventory.addItemStackToInventory(stack))
                            {
                                player.dropPlayerItemWithRandomChoice(stack, false);
                            }
                        }
                        else
                        {
                            bag.getTagCompound().getCompoundTag("data").setInteger("count", count);
                        }

                        //Update player client
                        player.inventoryContainer.detectAndSendChanges();
                    }
                }
                else
                {
                    player.addChatComponentMessage(new ChatComponentTranslation(getUnlocalizedName() + ".error.failedToCreateEntity", saveID));
                }
            }
            catch (Exception e)
            {
                player.addChatComponentMessage(new ChatComponentTranslation(getUnlocalizedName() + ".error.unexpected", e.getMessage()));
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    /**
     * Encodes the data for the bag to store
     *
     * @param bag
     */
    public static void encode(ItemStack bag, Entity entity)
    {
        if (bag.getTagCompound() == null)
        {
            bag.setTagCompound(new NBTTagCompound());
        }

        bag.getTagCompound().setTag("data", toNBT(entity));
        bag.getTagCompound().getCompoundTag("data").setInteger("count", 1);
        bag.setItemDamage(1); //Meta is used to set the icon
    }

    public static NBTTagCompound toNBT(Entity entity)
    {
        NBTTagCompound tag = new NBTTagCompound();

        //Save NBT
        NBTTagCompound entitySave = new NBTTagCompound();
        entity.writeToNBT(entitySave);

        //Remove unused junk from NBT
        cleanNBT(entitySave);

        entitySave.setString("id", EntityList.getEntityString(entity));
        tag.setTag("nbt", entitySave);

        //Save load ID
        tag.setString("saveID", EntityList.getEntityString(entity));
        //TODO save unique ID as list

        return tag;
    }

    public static void cleanNBT(NBTTagCompound tag)
    {
        tag.removeTag("Pos");
        tag.removeTag("Motion");
        tag.removeTag("Rotation");
        tag.removeTag("Rotation");
        tag.removeTag("FallDistance");
        tag.removeTag("Fire");
        tag.removeTag("Air");
        tag.removeTag("OnGround");
        tag.removeTag("Dimension");
        tag.removeTag("PortalCooldown");

        //TODO keep if possible
        tag.removeTag("UUIDMost");
        tag.removeTag("UUIDLeast");
    }

    /**
     * Gets the ItemStack that represents the stored block
     *
     * @param bag
     * @return
     */
    public static Entity decode(ItemStack bag, World world)
    {
        if (bag.getTagCompound() == null || !bag.getTagCompound().hasKey("data"))
        {
            return null;
        }
        //String saveID = bag.getTagCompound().getCompoundTag("data").getString("saveID");
        NBTTagCompound nbt = bag.getTagCompound().getCompoundTag("data").getCompoundTag("nbt");

        Entity entity = EntityList.createEntityFromNBT(nbt, world);
        if (entity != null)
        {
            return entity;
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg)
    {
        this.itemIcon = reg.registerIcon(this.getIconString());
        this.filledIcon = reg.registerIcon(this.getIconString() + ".filled");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int meta)
    {
        if (meta == 1)
        {
            return this.filledIcon;
        }
        return this.itemIcon;
    }

    @Override
    public int getItemStackLimit(ItemStack stack)
    {
        if (stack.getTagCompound() != null)
        {
            return 1;
        }
        return 64;
    }
}
