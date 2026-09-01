package ru.allin.factions.model;

import net.kyori.adventure.text.format.NamedTextColor;

public enum FactionType {
    INQUISITION("Инквизиция", NamedTextColor.RED),
    THIEVES("Гильдия воров", NamedTextColor.LIGHT_PURPLE),
    WORKERS("Работяги", NamedTextColor.GREEN);

    private final String display;
    private final NamedTextColor color;
    FactionType(String display, NamedTextColor color) { this.display = display; this.color = color; }
    public String display() { return display; }
    public NamedTextColor color() { return color; }
    public static FactionType parse(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase()) {
            case "inquisition", "inq", "инквизиция" -> INQUISITION;
            case "thieves", "thief", "воры", "гильдия" -> THIEVES;
            case "workers", "worker", "работяги" -> WORKERS;
            default -> null;
        };
    }
}
