package ru.allin.factions.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyHook {
    private final JavaPlugin plugin; private Economy economy;
    public EconomyHook(JavaPlugin plugin){this.plugin=plugin;}
    public boolean setup(){
        if(plugin.getServer().getPluginManager().getPlugin("Vault")==null) return false;
        RegisteredServiceProvider<Economy> rsp=plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp==null)return false; economy=rsp.getProvider(); return economy!=null;
    }
    public boolean available(){return economy!=null;}
    public double balance(OfflinePlayer p){return economy==null?0:economy.getBalance(p);}
    public boolean withdraw(OfflinePlayer p,double amount){return economy!=null && amount>=0 && economy.withdrawPlayer(p,amount).transactionSuccess();}
    public boolean deposit(OfflinePlayer p,double amount){return economy!=null && amount>=0 && economy.depositPlayer(p,amount).transactionSuccess();}
}
