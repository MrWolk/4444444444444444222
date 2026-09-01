package ru.allin.factions.api;

import org.bukkit.entity.Player;
import ru.allin.factions.model.FactionRank;
import ru.allin.factions.model.FactionType;

public interface ALLINFactionsAPI {
    FactionType factionOf(Player player);
    FactionRank rankOf(Player player);
    LootBonusResult applyProfessionBonus(Player player, Profession profession, RewardType rewardType, int amount);
}
