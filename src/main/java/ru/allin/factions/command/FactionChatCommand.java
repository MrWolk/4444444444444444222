package ru.allin.factions.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import ru.allin.factions.data.Database;
import ru.allin.factions.model.PlayerData;
import ru.allin.factions.service.FactionService;
import ru.allin.factions.util.Text;

public final class FactionChatCommand implements CommandExecutor {
    private final Database db; private final FactionService factions;
    public FactionChatCommand(Database db,FactionService factions){this.db=db;this.factions=factions;}
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){if(!(sender instanceof Player p))return true;PlayerData d=factions.data(p);if(d.faction()==null){p.sendMessage(Text.msg("Вы не состоите во фракции."));return true;}if(args.length==0){db.updatePlayer(p.getUniqueId(),"faction_chat=?",d.factionChat()?0:1);p.sendMessage(Text.msg("Режим чата: "+(d.factionChat()?"общий":"фракционный")+"."));return true;}send(p,d,String.join(" ",args));return true;}
    public void send(Player p,PlayerData d,String text){Component msg=Component.text("["+d.faction().display()+"] ",d.faction().color()).append(Component.text("["+d.rank().display()+"] ",d.faction().color())).append(Component.text(p.getName()+": "+text));factions.broadcastFaction(d.faction(),msg);}
}
