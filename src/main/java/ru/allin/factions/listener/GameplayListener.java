package ru.allin.factions.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.allin.factions.command.FactionChatCommand;
import ru.allin.factions.data.Database;
import ru.allin.factions.gui.GuiService;
import ru.allin.factions.model.*;
import ru.allin.factions.service.*;
import ru.allin.factions.util.Text;

import java.util.List;

public final class GameplayListener implements Listener {
    private final JavaPlugin plugin; private final Database db; private final FactionService factions; private final InquisitionService inq; private final ThiefService thieves; private final GuiService gui; private final FactionChatCommand fc;
    public GameplayListener(JavaPlugin plugin,Database db,FactionService factions,InquisitionService inq,ThiefService thieves,GuiService gui,FactionChatCommand fc){this.plugin=plugin;this.db=db;this.factions=factions;this.inq=inq;this.thieves=thieves;this.gui=gui;this.fc=fc;}

    @EventHandler public void onGuiClick(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p))return;String action=gui.action(e.getCurrentItem());if(action!=null){e.setCancelled(true);gui.click(p,action);return;}if(inq.isReadOnlySearcher(p)){e.setCancelled(true);}}
    @EventHandler public void onGuiDrag(InventoryDragEvent e){if(e.getWhoClicked() instanceof Player p&&inq.isReadOnlySearcher(p))e.setCancelled(true);}
    @EventHandler public void onInventoryClose(InventoryCloseEvent e){if(e.getPlayer() instanceof Player p)inq.endReadOnlySearch(p);}

    @EventHandler(ignoreCancelled=true) public void onHit(EntityDamageByEntityEvent e){if(!(e.getDamager() instanceof Player officer)||!(e.getEntity() instanceof Player target))return;ItemStack hand=officer.getInventory().getItemInMainHand();if(inq.isBaton(hand,officer))inq.batonHit(officer,target);}

    @EventHandler(ignoreCancelled=true) public void onInteract(PlayerInteractEvent e){Player p=e.getPlayer();ItemStack item=e.getItem();if(item!=null&&thieves.isContraband(item)&&item.getType()==Material.ENDER_PEARL){e.setCancelled(true);return;}if(item!=null&&item.getType()==Material.ENDER_PEARL&&db.contrabandAmount(p.getUniqueId())>0){e.setCancelled(true);p.sendMessage(Text.msg("С контрабандой нельзя использовать жемчуг Эндера!"));return;}if(e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getClickedBlock()==null)return;Location loc=e.getClickedBlock().getLocation();if(e.getClickedBlock().getType()==Material.CAULDRON||e.getClickedBlock().getType()==Material.WATER_CAULDRON){if(near(LocationType.TOILET,loc,2)){e.setCancelled(true);inq.toilet(p,loc);return;}}
        if(e.getClickedBlock().getType()==Material.BEACON){if(near(LocationType.CONTRABAND_START,loc,2)){e.setCancelled(true);thieves.startContraband(p);return;}if(near(LocationType.CONTRABAND_FINISH,loc,2)){e.setCancelled(true);thieves.deliverContraband(p);}}
    }
    private boolean near(LocationType type,Location loc,double radius){double r2=radius*radius;for(Location l:db.locations(type))if(l.getWorld().equals(loc.getWorld())&&l.distanceSquared(loc)<=r2)return true;return false;}

    @EventHandler(ignoreCancelled=true) public void onDrop(PlayerDropItemEvent e){ItemStack i=e.getItemDrop().getItemStack();if(inq.isAnyBaton(i)||thieves.isContraband(i)){e.setCancelled(true);e.getPlayer().sendMessage(Text.msg("Этот специальный предмет нельзя выбрасывать."));}}
    @EventHandler(ignoreCancelled=true) public void onPickup(EntityPickupItemEvent e){if(!(e.getEntity() instanceof Player p))return;ItemStack i=e.getItem().getItemStack();if(thieves.isContraband(i)&&!thieves.isOwnedContraband(i,p)){e.setCancelled(true);return;}if(inq.isAnyBaton(i)&&!inq.isBaton(i,p))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void onSpecialInventory(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p))return;ItemStack cursor=e.getCursor(),current=e.getCurrentItem();boolean special=(thieves.isContraband(cursor)||inq.isAnyBaton(cursor)||thieves.isContraband(current)||inq.isAnyBaton(current));if(!special)return;InventoryType top=e.getView().getTopInventory().getType();if(top!=InventoryType.CRAFTING&&top!=InventoryType.PLAYER){e.setCancelled(true);p.sendMessage(Text.msg("Специальный предмет нельзя помещать в хранилище."));}}

    @EventHandler public void onDeath(PlayerDeathEvent e){Player p=e.getPlayer();boolean hadContraband=db.contrabandAmount(p.getUniqueId())>0;if(hadContraband){e.getDrops().removeIf(thieves::isContraband);thieves.loseContraband(p);}e.getDrops().removeIf(inq::isAnyBaton);}
    @EventHandler public void onJoin(PlayerJoinEvent e){Player p=e.getPlayer();PlayerData d=factions.data(p);if(d.faction()!=null&&plugin.getConfig().getBoolean("faction-chat.join-notifications",true))factions.broadcastFaction(d.faction(),net.kyori.adventure.text.Component.text("➕ "+p.getName()+" вошёл на сервер.",d.faction().color()));if(db.contrabandAmount(p.getUniqueId())>0)thieves.applyCarryEffects(p);if(d.faction()!=FactionType.INQUISITION||d.rank()==null||d.rank().level()<2)inq.removeBatons(p);}
    @EventHandler public void onQuit(PlayerQuitEvent e){Player p=e.getPlayer();PlayerData d=factions.data(p);if(d.faction()!=null&&plugin.getConfig().getBoolean("faction-chat.quit-notifications",true))factions.broadcastFaction(d.faction(),net.kyori.adventure.text.Component.text("➖ "+p.getName()+" покинул сервер.",d.faction().color()));}

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onChat(AsyncChatEvent e){Player p=e.getPlayer();PlayerData d=factions.data(p);if(d.faction()==null||!d.factionChat())return;e.setCancelled(true);String text=PlainTextComponentSerializer.plainText().serialize(e.message());fc.send(p,d,text);}
}
