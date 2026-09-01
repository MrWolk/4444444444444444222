package ru.allin.factions;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.allin.factions.api.ALLINFactionsAPI;
import ru.allin.factions.command.*;
import ru.allin.factions.data.Database;
import ru.allin.factions.economy.EconomyHook;
import ru.allin.factions.gui.GuiService;
import ru.allin.factions.listener.GameplayListener;
import ru.allin.factions.service.*;

public final class ALLINFactionsPlugin extends JavaPlugin {
    private Database database;
    @Override public void onEnable(){
        saveDefaultConfig();
        try{database=new Database(this);database.open();}catch(Exception e){getLogger().severe("Cannot open SQLite: "+e.getMessage());getServer().getPluginManager().disablePlugin(this);return;}
        EconomyHook economy=new EconomyHook(this);if(!economy.setup())getLogger().warning("Vault economy provider not found. Economy features will wait for a provider.");
        FactionService factions=new FactionService(this,database,economy);
        InquisitionService inq=new InquisitionService(this,database,factions,economy);
        ThiefService thieves=new ThiefService(this,database,factions,economy);
        ProfessionBonusService api=new ProfessionBonusService(this,database,factions);
        GuiService gui=new GuiService(this,database,factions,inq);
        FactionChatCommand fc=new FactionChatCommand(database,factions);
        register("faction",new FactionCommand(gui,factions));register("fc",fc);
        InquisitionCommands inqCmd=new InquisitionCommands(factions,inq);register("warn",inqCmd);register("warns",inqCmd);register("search",inqCmd);register("jailtime",inqCmd);
        register("steal",new StealCommand(thieves));register("fadmin",new AdminCommand(this,database,factions,inq,gui));
        getServer().getPluginManager().registerEvents(new GameplayListener(this,database,factions,inq,thieves,gui,fc),this);
        Bukkit.getServicesManager().register(ALLINFactionsAPI.class,api,this,ServicePriority.Normal);
        Bukkit.getScheduler().runTaskTimer(this,factions::tickOnline,20L,20L);
        Bukkit.getScheduler().runTaskTimer(this,inq::tickPrison,20L,20L);
        BackupService backups=new BackupService(this);long hours=Math.max(1,getConfig().getLong("database.backup.interval-hours",6));Bukkit.getScheduler().runTaskTimerAsynchronously(this,backups::backup,20L*60,20L*60*60*hours);
        getLogger().info("ALLINFactions v"+getPluginMeta().getVersion()+" enabled.");
    }
    private void register(String name,org.bukkit.command.CommandExecutor executor){PluginCommand c=getCommand(name);if(c!=null)c.setExecutor(executor);}
    @Override public void onDisable(){if(database!=null)database.close();}
}
