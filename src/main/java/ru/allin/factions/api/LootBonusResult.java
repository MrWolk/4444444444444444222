package ru.allin.factions.api;
public record LootBonusResult(int originalAmount, int finalAmount, boolean bonusTriggered, int multiplier) {}
