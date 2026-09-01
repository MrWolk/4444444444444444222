package ru.allin.factions.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.allin.factions.data.Database;
import ru.allin.factions.economy.EconomyHook;
import ru.allin.factions.model.*;
import ru.allin.factions.util.Text;

import java.util.*;

public final class FactionService {
    private final JavaPlugin plugin; private final Database db; private final EconomyHook economy;
    public FactionService(JavaPlugin plugin, Database db, EconomyHook economy){this.plugin=plugin;this.db=db;this.economy=economy;}
    public PlayerData data(Player p){return db.getPlayer(p.getUniqueId(),p.getName());}
    public PlayerData data(OfflinePlayer p){return db.getPlayer(p.getUniqueId(),p.getName()==null?p.getUniqueId().toString():p.getName());}
    public boolean join(Player p,FactionType f){PlayerData d=data(p); long now=System.currentTimeMillis();if(d.faction()!=null){p.sendMessage(Text.msg("Вы уже состоите во фракции."));return false;}if(d.rejoinBlockedUntil()>now){p.sendMessage(Text.msg("Вступление доступно через "+Text.time((d.rejoinBlockedUntil()-now)/1000)+"."));return false;}if(db.blacklisted(f,p.getUniqueId())){p.sendMessage(Text.msg("Вы внесены в чёрный список этой фракции."));return false;}FactionRank rank=FactionRank.starter(f);db.updatePlayer(p.getUniqueId(),"faction=?,rank=?,joined_at=?,faction_seconds=0,salary_seconds=0",f.name(),rank.name(),now);db.log(p.getName(),"FACTION_JOIN",f.name());p.sendMessage(Text.msg("Вы вступили во фракцию «"+f.display()+"» и получили ранг «"+rank.display()+"»."));return true;}
    public void leave(Player p, boolean admin){PlayerData d=data(p);if(d.faction()==null)return;long until=admin?0:System.currentTimeMillis()+plugin.getConfig().getLong("factions.leave.rejoin-cooldown-hours",24)*3600000L;db.updatePlayer(p.getUniqueId(),"faction=NULL,rank=NULL,joined_at=0,salary_seconds=0,rejoin_until=?,faction_chat=0",until);if(d.faction()==FactionType.WORKERS && plugin.getConfig().getBoolean("workers.promotion.reset-progress-on-leave",true))db.updatePlayer(p.getUniqueId(),"faction_seconds=0");db.log(p.getName(),admin?"ADMIN_FACTION_LEAVE":"FACTION_LEAVE",d.faction().name());p.sendMessage(Text.msg(admin?"Вы были удалены из фракции администратором.":"Вы покинули фракцию. Новое вступление будет доступно через 24 часа."));}
    public boolean setRank(OfflinePlayer p,FactionRank rank){PlayerData d=data(p);if(d.faction()==null||rank.faction()!=d.faction())return false;db.updatePlayer(p.getUniqueId(),"rank=?",rank.name());if(p.isOnline())p.getPlayer().sendMessage(Text.msg("Ваш новый ранг: "+rank.display()));db.log("SYSTEM","RANK_CHANGE",p.getUniqueId()+" -> "+rank.name());return true;}
    public void setLeader(FactionType f,OfflinePlayer p){UUID old=db.leader(f);if(old!=null&&!old.equals(p.getUniqueId())){OfflinePlayer oldP=Bukkit.getOfflinePlayer(old);setRank(oldP,FactionRank.second(f));}PlayerData d=data(p);if(d.faction()!=f){db.updatePlayer(p.getUniqueId(),"faction=?,joined_at=?,faction_seconds=0,salary_seconds=0",f.name(),System.currentTimeMillis());}setRank(p,FactionRank.leader(f));db.setLeader(f,p.getUniqueId());db.log("ADMIN","SET_LEADER",f.name()+" -> "+p.getUniqueId());if(p.isOnline())p.getPlayer().sendMessage(Text.msg("Вы назначены главой фракции «"+f.display()+"»."));}
    public boolean isLeader(Player p){PlayerData d=data(p);return d.rank()!=null&&d.rank().leader()&&Objects.equals(db.leader(d.faction()),p.getUniqueId());}
    public double treasury(FactionType f){return db.treasury(f);} public double salary(FactionType f,FactionRank r){return db.salary(f,r);} public void setSalary(FactionType f,FactionRank r,double amount){db.setSalary(f,r,amount);}
    public boolean treasuryDeposit(Player p,double amount){PlayerData d=data(p);if(d.faction()==null||!isLeader(p)||amount<=0||!economy.withdraw(p,amount))return false;db.setTreasury(d.faction(),db.treasury(d.faction())+amount);db.log(p.getName(),"TREASURY_DEPOSIT",d.faction()+" "+amount);tryPayDebts(d.faction());return true;}
    public boolean treasuryWithdraw(Player p,double amount){PlayerData d=data(p);if(d.faction()==null||!isLeader(p)||amount<=0||db.treasury(d.faction())<amount)return false;db.setTreasury(d.faction(),db.treasury(d.faction())-amount);if(!economy.deposit(p,amount)){db.setTreasury(d.faction(),db.treasury(d.faction())+amount);return false;}db.log(p.getName(),"TREASURY_WITHDRAW",d.faction()+" "+amount);return true;}
    public void tickOnline(){for(Player p:Bukkit.getOnlinePlayers()){PlayerData d=data(p);if(d.faction()==null)continue;long factionSeconds=d.factionSeconds()+1;long salarySeconds=d.salarySeconds()+1;db.updatePlayer(p.getUniqueId(),"faction_seconds=?,salary_seconds=?",factionSeconds,salarySeconds);if(d.faction()==FactionType.WORKERS&&d.rank()==FactionRank.FACTORY_WORKER){long need=plugin.getConfig().getLong("workers.promotion.workaholic-hours",100)*3600;if(factionSeconds>=need){setRank(p,FactionRank.WORKAHOLIC);Bukkit.broadcast(Component.text("🎉 "+p.getName()+" получил ранг Трудоголик!",NamedTextColor.GREEN));}}
            if(salarySeconds>=plugin.getConfig().getLong("economy.salaries.interval-minutes",60)*60){paySalary(p,data(p));}
        }}
    private void paySalary(Player p,PlayerData d){long interval=plugin.getConfig().getLong("economy.salaries.interval-minutes",60)*60;long cycles=Math.max(1,d.salarySeconds()/interval);double rate=db.salary(d.faction(),d.rank());double due=rate*cycles;long remain=d.salarySeconds()%interval;double treasury=db.treasury(d.faction());double debt=d.salaryDebt();double total=debt+due;if(treasury>=total&&economy.available()){db.setTreasury(d.faction(),treasury-total);if(economy.deposit(p,total)){db.updatePlayer(p.getUniqueId(),"salary_seconds=?,salary_debt=0",remain);db.addStat(p.getUniqueId(),"salary_received",total);p.sendMessage(Text.msg("Зарплата: "+Text.money(total)+"."));db.log("SALARY","PAID",p.getUniqueId()+" "+total);}else{db.setTreasury(d.faction(),treasury);db.updatePlayer(p.getUniqueId(),"salary_seconds=?,salary_debt=?",remain,total);}}else{db.updatePlayer(p.getUniqueId(),"salary_seconds=?,salary_debt=?",remain,total);p.sendMessage(Text.msg("Зарплата не выплачена. Долг: "+Text.money(total)+"."));}}
    public void tryPayDebts(FactionType f){for(PlayerData d:db.factionMembers(f)){if(d.salaryDebt()<=0)continue;double t=db.treasury(f);if(t<d.salaryDebt())break;OfflinePlayer p=Bukkit.getOfflinePlayer(d.uuid());if(!economy.available())break;db.setTreasury(f,t-d.salaryDebt());if(economy.deposit(p,d.salaryDebt()))db.updatePlayer(d.uuid(),"salary_debt=0");else db.setTreasury(f,t);}}
    public void broadcastFaction(FactionType f, Component message){for(Player p:Bukkit.getOnlinePlayers()){PlayerData d=data(p);if(d.faction()==f)p.sendMessage(message);}}
}
