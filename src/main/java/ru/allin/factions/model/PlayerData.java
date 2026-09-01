package ru.allin.factions.model;

import java.util.UUID;

public record PlayerData(UUID uuid, String name, FactionType faction, FactionRank rank,
                         long joinedAt, long factionSeconds, long salarySeconds, double salaryDebt,
                         int warns, boolean wanted, long wantedSince, int imprisonments,
                         long prisonRemainingSeconds, long rejoinBlockedUntil,
                         long lastWarnAt, long lastSearchAt, long lastStealAt,
                         long lastBatonAt, boolean factionChat) {
    public boolean jailed(){ return prisonRemainingSeconds > 0; }
}
