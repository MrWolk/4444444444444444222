package ru.allin.factions.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class Items {
    private Items(){}
    public static ItemStack item(Material m, Component name, List<Component> lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.displayName(name);meta.lore(lore);i.setItemMeta(meta);return i;}
}
