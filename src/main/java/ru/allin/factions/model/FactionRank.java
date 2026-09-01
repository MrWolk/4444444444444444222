package ru.allin.factions.model;

public enum FactionRank {
    INITIATE(FactionType.INQUISITION, "Посвящённый", 1),
    INQUISITOR(FactionType.INQUISITION, "Инквизитор", 2),
    MASTER(FactionType.INQUISITION, "Магистр", 3),
    THIEF(FactionType.THIEVES, "Вор", 1),
    LAW_THIEF(FactionType.THIEVES, "Вор в законе", 2),
    KING(FactionType.THIEVES, "Король воров", 3),
    FACTORY_WORKER(FactionType.WORKERS, "Заводчанин", 1),
    WORKAHOLIC(FactionType.WORKERS, "Трудоголик", 2),
    FATHER(FactionType.WORKERS, "Батя", 3);

    private final FactionType faction; private final String display; private final int level;
    FactionRank(FactionType faction, String display, int level) { this.faction=faction; this.display=display; this.level=level; }
    public FactionType faction(){return faction;} public String display(){return display;} public int level(){return level;}
    public boolean leader(){return level==3;}
    public static FactionRank starter(FactionType f){return switch(f){case INQUISITION->INITIATE;case THIEVES->THIEF;case WORKERS->FACTORY_WORKER;};}
    public static FactionRank leader(FactionType f){return switch(f){case INQUISITION->MASTER;case THIEVES->KING;case WORKERS->FATHER;};}
    public static FactionRank second(FactionType f){return switch(f){case INQUISITION->INQUISITOR;case THIEVES->LAW_THIEF;case WORKERS->WORKAHOLIC;};}
    public static FactionRank parse(String s){
        if(s==null)return null; String n=s.toUpperCase().replace('-','_');
        try{return valueOf(n);}catch(Exception ignored){}
        for(FactionRank r:values()) if(r.display.equalsIgnoreCase(s)) return r;
        return null;
    }
}
