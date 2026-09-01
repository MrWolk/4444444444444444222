package ru.allin.factions.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.allin.factions.gui.GuiService;
import ru.allin.factions.model.*;
import ru.allin.factions.service.FactionService;
import ru.allin.factions.util.Text;

public final class FactionCommand implements CommandExecutor {
    private final GuiService gui; private final FactionService factions;
    public FactionCommand(GuiService gui,FactionService factions){this.gui=gui;this.factions=factions;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){if(!(sender instanceof Player p)){sender.sendMessage("Players only");return true;}if(args.length==0){gui.open(p);return true;}if(args[0].equalsIgnoreCase("announce")){PlayerData d=factions.data(p);if(d.faction()==null){p.sendMessage(Text.msg("Вы не состоите во фракции."));return true;}boolean allowed=d.rank().leader()||(d.faction()==FactionType.THIEVES&&d.rank()==FactionRank.LAW_THIEF);if(!allowed){p.sendMessage(Text.msg("У вас нет права делать объявления."));return true;}if(args.length<2){p.sendMessage(Text.msg("Использование: /f announce <текст>"));return true;}String msg=String.join(" ",java.util.Arrays.copyOfRange(args,1,args.length));Component c=Component.text("📣 "+d.faction().display()+" • ",d.faction().color()).append(Component.text(msg,NamedTextColor.WHITE)).append(Component.text(" — "+p.getName(),NamedTextColor.GRAY));factions.broadcastFaction(d.faction(),c);return true;}gui.open(p);return true;}
}
