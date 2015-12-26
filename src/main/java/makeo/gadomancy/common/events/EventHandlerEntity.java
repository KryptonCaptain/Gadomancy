package makeo.gadomancy.common.events;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import makeo.gadomancy.common.aura.AuraEffects;
import makeo.gadomancy.common.blocks.tiles.TileAuraPylon;
import makeo.gadomancy.common.blocks.tiles.TileBlockProtector;
import makeo.gadomancy.common.data.config.ModConfig;
import makeo.gadomancy.common.entities.EntityPermNoClipItem;
import makeo.gadomancy.common.familiar.FamiliarAIController;
import makeo.gadomancy.common.registration.RegisteredItems;
import makeo.gadomancy.common.utils.ItemUtils;
import makeo.gadomancy.common.utils.MiscUtils;
import makeo.gadomancy.common.utils.NBTHelper;
import makeo.gadomancy.common.utils.Vector3;
import makeo.gadomancy.common.utils.world.TCMazeHandler;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.items.armor.Hover;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is part of the Gadomancy Mod
 * Gadomancy is Open Source and distributed under the
 * GNU LESSER GENERAL PUBLIC LICENSE
 * for more read the LICENSE file
 * <p/>
 * Created by HellFirePvP @ 31.10.2015 15:28
 */
public class EventHandlerEntity {

    public static List<ChunkCoordinates> registeredLuxPylons = new ArrayList<ChunkCoordinates>();

    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public void on(EntityEvent.EntityConstructing e) {
        if (e.entity instanceof EntityPlayer) {
            //e.entity.registerExtendedProperties(Gadomancy.MODID, new ExtendedPlayerProperties((EntityPlayer) e.entity));
        }
    }*/

    @SubscribeEvent
    public void on(LivingSpawnEvent.CheckSpawn event) {
        if(event.entityLiving.isCreatureType(EnumCreatureType.monster, false)) {
            double rangeSq = AuraEffects.LUX.getRange() * AuraEffects.LUX.getRange();
            Vector3 entityPos = MiscUtils.getPositionVector(event.entity);
            for(ChunkCoordinates luxPylons : registeredLuxPylons) {
                Vector3 pylon = Vector3.fromCC(luxPylons);
                if(entityPos.distanceSquared(pylon) <= rangeSq) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public void on(EnderTeleportEvent e) {
        if (!(e.entityLiving instanceof EntityPlayer) && !(e.entityLiving instanceof IBossDisplayData)) {
            if (TileBlockProtector.isSpotProtected(e.entityLiving.worldObj, e.entityLiving)) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void on(LivingSetAttackTargetEvent targetEvent) {
        if (targetEvent.target instanceof EntityPlayer) {
            FamiliarAIController.notifyTargetEvent(targetEvent.entityLiving, (EntityPlayer) targetEvent.target);
        }
    }

    @SubscribeEvent
    public void on(LivingDeathEvent event) {
        if (!event.entity.worldObj.isRemote && event.entityLiving instanceof EntityPlayer) {
            TCMazeHandler.closeSession((EntityPlayer) event.entityLiving, false);
        }
    }

    @SubscribeEvent
    public void on(EntityItemPickupEvent event) {
        if (!event.entityPlayer.worldObj.isRemote) {
            if (event.item != null && event.item instanceof EntityPermNoClipItem) {
                EntityPermNoClipItem item = (EntityPermNoClipItem) event.item;
                ChunkCoordinates master = (ChunkCoordinates) item.getDataWatcher().getWatchedObject(ModConfig.entityNoClipItemDatawatcherMasterId).getObject();
                TileEntity te = event.entityPlayer.worldObj.getTileEntity(master.posX, master.posY, master.posZ);
                if (te == null || !(te instanceof TileAuraPylon)) return;
                ((TileAuraPylon) te).informItemPickup();
            }
        }
    }

    @SubscribeEvent
    public void on(LivingEvent.LivingUpdateEvent event) {
        if (event.entityLiving == null || !(event.entityLiving instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.entity;
        if ((event.entity.worldObj.provider.dimensionId == ModConfig.dimOuterId) && ((player.ticksExisted & 7) == 0) && ((player.capabilities.isFlying) || (Hover.getHover(player.getEntityId())))) {
            player.capabilities.isFlying = false;
            Hover.setHover(player.getEntityId(), false);
            if (!((EntityPlayer) event.entityLiving).worldObj.isRemote) {
                String msg = StatCollector.translateToLocal("tc.break.fly");
                if (player.capabilities.isCreativeMode) {
                    msg += " " + StatCollector.translateToLocal("gadomancy.eldritch.noflyCreative");
                }
                player.addChatMessage(new ChatComponentText(EnumChatFormatting.ITALIC + "" + EnumChatFormatting.GRAY + msg));
            }
        }
    }

}
