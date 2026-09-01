package ru.allin.factions.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Text {
    private Text(){}
    public static Component prefix(){return Component.text("ALLIN ", NamedTextColor.GOLD).append(Component.text("• ",NamedTextColor.DARK_GRAY));}
    public static Component msg(String s){return prefix().append(Component.text(s,NamedTextColor.WHITE));}
    public static String money(double d){return String.format(java.util.Locale.US,"$%,.0f",d);}
    public static String time(long sec){long h=sec/3600,m=(sec%3600)/60,s=sec%60;if(h>0)return h+"ч "+m+"м";if(m>0)return m+"м "+s+"с";return s+"с";}
}
