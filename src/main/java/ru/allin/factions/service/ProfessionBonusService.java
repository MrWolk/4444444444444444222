package ru.allin.factions.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.allin.factions.api.*;
import ru.allin.factions.data.Database;
import ru.allin.factions.model.*;

import java.util.concurrent.ThreadLocalRandom;

public final class ProfessionBonusService implements ALLINFactionsAPI {
    private final JavaPlugin plugin; private final Database db; private final FactionService factions;
    public ProfessionBonusService(JavaPlugin plugin,Database db,FactionService factions){this.plugin=plugin;this.db=db;this.factions=factions;}
    @Override public FactionType factionOf(Player p){return factions.data(p).faction();}
    @Override public FactionRank rankOf(Player p){return factions.data(p).rank();}
    @Override public LootBonusResult applyProfessionBonus(Player p, Profession profession, RewardType type, int amount){if(amount<=0)return new LootBonusResult(amount,amount,false,1);PlayerData d=factions.data(p);if(d.faction()!=FactionType.WORKERS||d.rank()==null||type==RewardType.SPECIAL&&!plugin.getConfig().getBoolean("workers.loot-bonus.double-special-rewards",false))return new LootBonusResult(amount,amount,false,1);String key=switch(d.rank()){case FACTORY_WORKER->"factory-worker";case WORKAHOLIC->"workaholic";case FATHER->"father";default->null;};if(key==null)return new LootBonusResult(amount,amount,false,1);double chance=plugin.getConfig().getDouble("workers.loot-bonus."+key,0);if(ThreadLocalRandom.current().nextDouble()>=chance)return new LootBonusResult(amount,amount,false,1);int out=amount*2;db.addStat(p.getUniqueId(),"worker_bonus_count",1);db.addStat(p.getUniqueId(),"worker_bonus_"+profession.name().toLowerCase(),1);db.addStat(p.getUniqueId(),"worker_extra_items",amount);if(plugin.getConfig().getBoolean("workers.loot-bonus.notification.actionbar",true))p.sendActionBar(Component.text("🍀 БОНУС РАБОТЯГ! "+amount+" → "+out,NamedTextColor.GREEN));if(plugin.getConfig().getBoolean("workers.loot-bonus.notification.sound",true))p.playSound(p.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,0.7f,1.2f);return new LootBonusResult(amount,out,true,2);}
}
