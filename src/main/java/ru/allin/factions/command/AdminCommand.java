package ru.allin.factions.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.allin.factions.data.Database;
import ru.allin.factions.gui.GuiService;
import ru.allin.factions.model.*;
import ru.allin.factions.service.*;
import ru.allin.factions.util.Text;

public final class AdminCommand implements CommandExecutor {
    private final ru.allin.factions.ALLINFactionsPlugin plugin; private final Database db; private final FactionService factions; private final InquisitionService inq; private final GuiService gui;
    public AdminCommand(ru.allin.factions.ALLINFactionsPlugin plugin,Database db,FactionService factions,InquisitionService inq,GuiService gui){this.plugin=plugin;this.db=db;this.factions=factions;this.inq=inq;this.gui=gui;}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(!s.hasPermission("allinfactions.admin")){s.sendMessage("No permission");return true;}if(a.length==0){if(s instanceof Player p)gui.open(p);else help(s);return true;}try{
        switch(a[0].toLowerCase()){
            case "setleader"->{if(a.length<3)return usage(s,"/fadmin setleader <faction> <player>");FactionType f=FactionType.parse(a[1]);OfflinePlayer p=Bukkit.getOfflinePlayer(a[2]);if(f==null)return usage(s,"Неизвестная фракция");factions.setLeader(f,p);s.sendMessage("Глава назначен: "+p.getName()+" -> "+f.display());}
            case "join"->{if(a.length<3)return usage(s,"/fadmin join <player> <faction>");Player p=Bukkit.getPlayerExact(a[1]);FactionType f=FactionType.parse(a[2]);if(p==null||f==null)return usage(s,"Игрок должен быть онлайн / неверная фракция");PlayerData d=factions.data(p);if(d.faction()!=null)factions.leave(p,true);db.updatePlayer(p.getUniqueId(),"rejoin_until=0");factions.join(p,f);}
            case "leave"->{if(a.length<2)return usage(s,"/fadmin leave <player>");Player p=Bukkit.getPlayerExact(a[1]);if(p==null)return usage(s,"Игрок должен быть онлайн");factions.leave(p,true);}
            case "rank"->{if(a.length<3)return usage(s,"/fadmin rank <player> <rank>");OfflinePlayer p=Bukkit.getOfflinePlayer(a[1]);FactionRank r=FactionRank.parse(a[2]);if(r==null||r.leader())return usage(s,"Высший ранг назначается только setleader");s.sendMessage(factions.setRank(p,r)?"Ранг изменён":"Ранг не подходит фракции игрока");}
            case "cooldown"->{if(a.length>=3&&a[1].equalsIgnoreCase("clear")){OfflinePlayer p=Bukkit.getOfflinePlayer(a[2]);db.updatePlayer(p.getUniqueId(),"rejoin_until=0");s.sendMessage("Кулдаун снят.");}else return usage(s,"/fadmin cooldown clear <player>");}
            case "warns"->{if(a.length>=4&&a[1].equalsIgnoreCase("set")){OfflinePlayer p=Bukkit.getOfflinePlayer(a[2]);int n=Integer.parseInt(a[3]);db.updatePlayer(p.getUniqueId(),"warns=?",Math.max(0,n));s.sendMessage("Варны изменены.");}else return usage(s,"/fadmin warns set <player> <amount>");}
            case "wanted"->{if(a.length<3)return usage(s,"/fadmin wanted add|remove <player>");OfflinePlayer p=Bukkit.getOfflinePlayer(a[2]);boolean on=a[1].equalsIgnoreCase("add");db.updatePlayer(p.getUniqueId(),"wanted=?,wanted_since=?",on?1:0,on?System.currentTimeMillis():0);s.sendMessage("Розыск изменён.");}
            case "prison"->prison(s,a);
            case "thieves"->thieves(s,a);
            case "treasury"->treasury(s,a);
            case "salary"->{if(a.length<4)return usage(s,"/fadmin salary <faction> <rank> <amount>");FactionType f=FactionType.parse(a[1]);FactionRank r=FactionRank.parse(a[2]);double amount=Double.parseDouble(a[3]);if(f==null||r==null||r.faction()!=f)return usage(s,"Неверная фракция/ранг");factions.setSalary(f,r,amount);s.sendMessage("Ставка установлена: "+Text.money(amount)+"/ч");}
            case "blacklist"->blacklist(s,a);
            case "info"->{if(a.length<2)return usage(s,"/fadmin info <player>");OfflinePlayer p=Bukkit.getOfflinePlayer(a[1]);PlayerData d=factions.data(p);s.sendMessage("§6ALLINFactions info: §f"+d.name());s.sendMessage("§7Фракция: §f"+(d.faction()==null?"нет":d.faction().display()));s.sendMessage("§7Ранг: §f"+(d.rank()==null?"нет":d.rank().display()));s.sendMessage("§7Варны: §f"+d.warns()+" §7Розыск: §f"+d.wanted());s.sendMessage("§7Тюрьма: §f"+Text.time(d.prisonRemainingSeconds()));s.sendMessage("§7Долг: §f"+Text.money(d.salaryDebt()));}
            case "reload"->{plugin.reloadConfig();s.sendMessage("ALLINFactions config.yml перезагружен.");}
            default->help(s);
        }
    }catch(Exception e){s.sendMessage("§cОшибка: "+e.getMessage());plugin.getLogger().warning("Admin command: "+e);}
    return true;}
    private boolean prison(CommandSender s,String[] a){if(a.length<2)return usage(s,"/fadmin prison setspawn|toilet|jail|release ...");if(a[1].equalsIgnoreCase("setspawn")&&s instanceof Player p){replaceSingle(LocationType.PRISON_SPAWN,p.getLocation());s.sendMessage("Точка тюрьмы установлена.");return true;}if(a[1].equalsIgnoreCase("toilet")&&s instanceof Player p){if(a.length<3)return usage(s,"/fadmin prison toilet add|remove|list");if(a[2].equalsIgnoreCase("add")){db.addLocation(LocationType.TOILET,p.getLocation());s.sendMessage("Точка Котла добавлена.");}else if(a[2].equalsIgnoreCase("remove")){db.removeNearest(LocationType.TOILET,p.getLocation(),5);s.sendMessage("Ближайшая точка удалена.");}else s.sendMessage("Точек: "+db.locations(LocationType.TOILET).size());return true;}if(a[1].equalsIgnoreCase("jail")&&a.length>=4){Player p=Bukkit.getPlayerExact(a[2]);if(p==null)return usage(s,"Игрок должен быть онлайн");long min=Long.parseLong(a[3]);db.updatePlayer(p.getUniqueId(),"prison_remaining=?",min*60);var loc=db.locations(LocationType.PRISON_SPAWN);if(!loc.isEmpty())p.teleportAsync(loc.get(0));s.sendMessage("Игрок заключён на "+min+" мин.");return true;}if(a[1].equalsIgnoreCase("release")&&a.length>=3){Player p=Bukkit.getPlayerExact(a[2]);if(p==null)return usage(s,"Игрок должен быть онлайн");inq.release(p);s.sendMessage("Игрок освобождён.");return true;}return usage(s,"Неверная команда prison");}
    private boolean thieves(CommandSender s,String[] a){if(!(s instanceof Player p)||a.length<4||!a[1].equalsIgnoreCase("beacon"))return usage(s,"/fadmin thieves beacon start|finish add|remove|list");String side=a[2].toLowerCase(),op=a[3].toLowerCase();LocationType type=side.equals("start")?LocationType.CONTRABAND_START:side.equals("finish")?LocationType.CONTRABAND_FINISH:null;if(type==null)return usage(s,"start или finish");if(op.equals("add")){db.addLocation(type,p.getLocation());s.sendMessage("Точка добавлена.");}else if(op.equals("remove")){db.removeNearest(type,p.getLocation(),5);s.sendMessage("Ближайшая точка удалена.");}else if(op.equals("list")){s.sendMessage("Точек "+side+": "+db.locations(type).size());}return true;}
    private boolean treasury(CommandSender s,String[] a){if(a.length<4)return usage(s,"/fadmin treasury <faction> set|add|remove <amount>");FactionType f=FactionType.parse(a[1]);if(f==null)return usage(s,"Неверная фракция");double amount=Double.parseDouble(a[3]),now=db.treasury(f),next=switch(a[2].toLowerCase()){case "set"->amount;case "add"->now+amount;case "remove"->now-amount;default->Double.NaN;};if(Double.isNaN(next))return usage(s,"set|add|remove");db.setTreasury(f,Math.max(0,next));factions.tryPayDebts(f);s.sendMessage("Казна "+f.display()+": "+Text.money(db.treasury(f)));return true;}
    private boolean blacklist(CommandSender s,String[] a){if(a.length<4)return usage(s,"/fadmin blacklist <faction> add|remove <player>");FactionType f=FactionType.parse(a[1]);OfflinePlayer p=Bukkit.getOfflinePlayer(a[3]);if(f==null)return usage(s,"Неверная фракция");if(a[2].equalsIgnoreCase("add"))db.blacklist(f,p.getUniqueId(),p.getName(),s.getName());else if(a[2].equalsIgnoreCase("remove"))db.unblacklist(f,p.getUniqueId());else return usage(s,"add|remove");s.sendMessage("Blacklist обновлён.");return true;}
    private void replaceSingle(LocationType t,Location l){for(Location old:db.locations(t))db.removeNearest(t,old,0.5);db.addLocation(t,l);}
    private boolean usage(CommandSender s,String m){s.sendMessage("§e"+m);return true;}
    private void help(CommandSender s){s.sendMessage("§6ALLINFactions admin: setleader, join, leave, rank, cooldown, wanted, warns, prison, thieves, treasury, salary, blacklist, info, reload");}
}
