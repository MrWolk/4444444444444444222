package ru.allin.factions.command;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.allin.factions.model.PlayerData;
import ru.allin.factions.service.*;
import ru.allin.factions.util.Text;

public final class InquisitionCommands implements CommandExecutor {
    private final FactionService factions; private final InquisitionService inq;
    public InquisitionCommands(FactionService factions,InquisitionService inq){this.factions=factions;this.inq=inq;}
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){if(!(sender instanceof Player p))return true;String name=cmd.getName().toLowerCase();if(name.equals("jailtime")){PlayerData d=factions.data(p);p.sendMessage(Text.msg(d.jailed()?"До освобождения: "+Text.time(d.prisonRemainingSeconds()):"Вы не находитесь в тюрьме."));return true;}if(args.length<1){p.sendMessage(Text.msg("Укажите ник игрока."));return true;}Player t=Bukkit.getPlayerExact(args[0]);if(t==null){p.sendMessage(Text.msg("Игрок должен быть онлайн."));return true;}switch(name){case "warn"->inq.warn(p,t);case "warns"->{PlayerData d=factions.data(t);p.sendMessage(Text.msg(t.getName()+": варнов "+d.warns()+", розыск: "+(d.wanted()?"ДА":"нет")));}case "search"->inq.search(p,t);}return true;}
}
