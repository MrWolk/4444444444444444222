package ru.allin.factions.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import ru.allin.factions.model.*;

import java.io.File;
import java.sql.*;
import java.util.*;

public final class Database implements AutoCloseable {
    private final JavaPlugin plugin;
    private Connection connection;
    public Database(JavaPlugin plugin){this.plugin=plugin;}

    public synchronized void open() throws SQLException {
        File db = new File(plugin.getDataFolder(), "data.db");
        db.getParentFile().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + db.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL"); st.execute("PRAGMA foreign_keys=ON");
            st.execute("CREATE TABLE IF NOT EXISTS players(uuid TEXT PRIMARY KEY,name TEXT,faction TEXT,rank TEXT,joined_at INTEGER DEFAULT 0,faction_seconds INTEGER DEFAULT 0,salary_seconds INTEGER DEFAULT 0,salary_debt REAL DEFAULT 0,warns INTEGER DEFAULT 0,wanted INTEGER DEFAULT 0,wanted_since INTEGER DEFAULT 0,imprisonments INTEGER DEFAULT 0,prison_remaining INTEGER DEFAULT 0,rejoin_until INTEGER DEFAULT 0,last_warn INTEGER DEFAULT 0,last_search INTEGER DEFAULT 0,last_steal INTEGER DEFAULT 0,last_baton INTEGER DEFAULT 0,faction_chat INTEGER DEFAULT 0)");
            st.execute("CREATE TABLE IF NOT EXISTS factions(faction TEXT PRIMARY KEY,leader_uuid TEXT,treasury REAL DEFAULT 0,salary1 REAL DEFAULT 200,salary2 REAL DEFAULT 200,salary3 REAL DEFAULT 200)");
            st.execute("CREATE TABLE IF NOT EXISTS blacklist(faction TEXT,uuid TEXT,name TEXT,added_by TEXT,added_at INTEGER,PRIMARY KEY(faction,uuid))");
            st.execute("CREATE TABLE IF NOT EXISTS stats(uuid TEXT,key TEXT,value REAL DEFAULT 0,PRIMARY KEY(uuid,key))");
            st.execute("CREATE TABLE IF NOT EXISTS locations(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT,world TEXT,x REAL,y REAL,z REAL,yaw REAL,pitch REAL)");
            st.execute("CREATE TABLE IF NOT EXISTS logs(id INTEGER PRIMARY KEY AUTOINCREMENT,ts INTEGER,actor TEXT,action TEXT,details TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS contraband(uuid TEXT PRIMARY KEY,operation_uuid TEXT,amount INTEGER,status TEXT,started_at INTEGER)");
        }
        for(FactionType f:FactionType.values()) ensureFaction(f);
    }

    private void ensureFaction(FactionType f) throws SQLException {
        try(PreparedStatement ps=connection.prepareStatement("INSERT OR IGNORE INTO factions(faction) VALUES(?)")){ps.setString(1,f.name());ps.executeUpdate();}
    }

    public synchronized PlayerData getPlayer(UUID uuid, String name) {
        try {
            try(PreparedStatement ins=connection.prepareStatement("INSERT OR IGNORE INTO players(uuid,name) VALUES(?,?)")){ins.setString(1,uuid.toString());ins.setString(2,name);ins.executeUpdate();}
            try(PreparedStatement up=connection.prepareStatement("UPDATE players SET name=? WHERE uuid=?")){up.setString(1,name);up.setString(2,uuid.toString());up.executeUpdate();}
            try(PreparedStatement ps=connection.prepareStatement("SELECT * FROM players WHERE uuid=?")){ps.setString(1,uuid.toString());try(ResultSet rs=ps.executeQuery()){if(rs.next()) return map(rs);}}
        } catch(SQLException e){plugin.getLogger().severe("DB getPlayer: "+e.getMessage());}
        return new PlayerData(uuid,name,null,null,0,0,0,0,0,false,0,0,0,0,0,0,0,0,false);
    }
    private PlayerData map(ResultSet rs)throws SQLException{
        String f=rs.getString("faction"),r=rs.getString("rank");
        return new PlayerData(UUID.fromString(rs.getString("uuid")),rs.getString("name"),f==null?null:FactionType.valueOf(f),r==null?null:FactionRank.valueOf(r),rs.getLong("joined_at"),rs.getLong("faction_seconds"),rs.getLong("salary_seconds"),rs.getDouble("salary_debt"),rs.getInt("warns"),rs.getInt("wanted")!=0,rs.getLong("wanted_since"),rs.getInt("imprisonments"),rs.getLong("prison_remaining"),rs.getLong("rejoin_until"),rs.getLong("last_warn"),rs.getLong("last_search"),rs.getLong("last_steal"),rs.getLong("last_baton"),rs.getInt("faction_chat")!=0);
    }
    public synchronized void updatePlayer(UUID uuid,String assignments,Object... vals){
        String sql="UPDATE players SET "+assignments+" WHERE uuid=?";
        try(PreparedStatement ps=connection.prepareStatement(sql)){int i=1;for(Object v:vals) ps.setObject(i++,v);ps.setString(i,uuid.toString());ps.executeUpdate();}catch(SQLException e){plugin.getLogger().severe("DB updatePlayer: "+e.getMessage());}
    }
    public synchronized List<PlayerData> factionMembers(FactionType faction){
        List<PlayerData> out=new ArrayList<>();
        try(PreparedStatement ps=connection.prepareStatement("SELECT * FROM players WHERE faction=?")){ps.setString(1,faction.name());try(ResultSet rs=ps.executeQuery()){while(rs.next())out.add(map(rs));}}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}
        return out;
    }
    public synchronized List<PlayerData> wantedPlayers(){
        List<PlayerData> out=new ArrayList<>();
        try(PreparedStatement ps=connection.prepareStatement("SELECT * FROM players WHERE wanted=1")){try(ResultSet rs=ps.executeQuery()){while(rs.next())out.add(map(rs));}}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}
        return out;
    }
    public synchronized UUID leader(FactionType f){try(PreparedStatement ps=connection.prepareStatement("SELECT leader_uuid FROM factions WHERE faction=?")){ps.setString(1,f.name());try(ResultSet rs=ps.executeQuery()){if(rs.next()){String s=rs.getString(1);return s==null?null:UUID.fromString(s);}}}catch(Exception e){plugin.getLogger().severe(e.getMessage());}return null;}
    public synchronized void setLeader(FactionType f,UUID u){try(PreparedStatement ps=connection.prepareStatement("UPDATE factions SET leader_uuid=? WHERE faction=?")){ps.setString(1,u==null?null:u.toString());ps.setString(2,f.name());ps.executeUpdate();}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}}
    public synchronized double treasury(FactionType f){try(PreparedStatement ps=connection.prepareStatement("SELECT treasury FROM factions WHERE faction=?")){ps.setString(1,f.name());try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getDouble(1):0;}}catch(SQLException e){return 0;}}
    public synchronized void setTreasury(FactionType f,double amount){try(PreparedStatement ps=connection.prepareStatement("UPDATE factions SET treasury=? WHERE faction=?")){ps.setDouble(1,Math.max(0,amount));ps.setString(2,f.name());ps.executeUpdate();}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}}
    public synchronized double salary(FactionType f,FactionRank r){int idx=Math.max(1,Math.min(3,r.level()));String col="salary"+idx;try(PreparedStatement ps=connection.prepareStatement("SELECT "+col+" FROM factions WHERE faction=?")){ps.setString(1,f.name());try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getDouble(1):200;}}catch(SQLException e){return 200;}}
    public synchronized void setSalary(FactionType f,FactionRank r,double amount){String col="salary"+r.level();try(PreparedStatement ps=connection.prepareStatement("UPDATE factions SET "+col+"=? WHERE faction=?")){ps.setDouble(1,Math.max(0,amount));ps.setString(2,f.name());ps.executeUpdate();}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}}
    public synchronized boolean blacklisted(FactionType f,UUID u){try(PreparedStatement ps=connection.prepareStatement("SELECT 1 FROM blacklist WHERE faction=? AND uuid=?")){ps.setString(1,f.name());ps.setString(2,u.toString());try(ResultSet rs=ps.executeQuery()){return rs.next();}}catch(SQLException e){return false;}}
    public synchronized void blacklist(FactionType f,UUID u,String name,String by){try(PreparedStatement ps=connection.prepareStatement("INSERT OR REPLACE INTO blacklist(faction,uuid,name,added_by,added_at) VALUES(?,?,?,?,?)")){ps.setString(1,f.name());ps.setString(2,u.toString());ps.setString(3,name);ps.setString(4,by);ps.setLong(5,System.currentTimeMillis());ps.executeUpdate();}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}}
    public synchronized void unblacklist(FactionType f,UUID u){try(PreparedStatement ps=connection.prepareStatement("DELETE FROM blacklist WHERE faction=? AND uuid=?")){ps.setString(1,f.name());ps.setString(2,u.toString());ps.executeUpdate();}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}}
    public synchronized void addStat(UUID u,String key,double delta){try(PreparedStatement ps=connection.prepareStatement("INSERT INTO stats(uuid,key,value) VALUES(?,?,?) ON CONFLICT(uuid,key) DO UPDATE SET value=value+excluded.value")){ps.setString(1,u.toString());ps.setString(2,key);ps.setDouble(3,delta);ps.executeUpdate();}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}}
    public synchronized double stat(UUID u,String key){try(PreparedStatement ps=connection.prepareStatement("SELECT value FROM stats WHERE uuid=? AND key=?")){ps.setString(1,u.toString());ps.setString(2,key);try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getDouble(1):0;}}catch(SQLException e){return 0;}}
    public synchronized void log(String actor,String action,String details){try(PreparedStatement ps=connection.prepareStatement("INSERT INTO logs(ts,actor,action,details) VALUES(?,?,?,?)")){ps.setLong(1,System.currentTimeMillis());ps.setString(2,actor);ps.setString(3,action);ps.setString(4,details);ps.executeUpdate();}catch(SQLException ignored){}}
    public synchronized int addLocation(LocationType type,Location l){try(PreparedStatement ps=connection.prepareStatement("INSERT INTO locations(type,world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,type.name());ps.setString(2,l.getWorld().getName());ps.setDouble(3,l.getX());ps.setDouble(4,l.getY());ps.setDouble(5,l.getZ());ps.setFloat(6,l.getYaw());ps.setFloat(7,l.getPitch());ps.executeUpdate();try(ResultSet rs=ps.getGeneratedKeys()){return rs.next()?rs.getInt(1):-1;}}catch(SQLException e){return -1;}}
    public synchronized List<Location> locations(LocationType type){List<Location> out=new ArrayList<>();try(PreparedStatement ps=connection.prepareStatement("SELECT * FROM locations WHERE type=?")){ps.setString(1,type.name());try(ResultSet rs=ps.executeQuery()){while(rs.next()){World w=Bukkit.getWorld(rs.getString("world"));if(w!=null)out.add(new Location(w,rs.getDouble("x"),rs.getDouble("y"),rs.getDouble("z"),rs.getFloat("yaw"),rs.getFloat("pitch")));}}}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}return out;}
    public synchronized void removeNearest(LocationType type,Location at,double radius){double best=radius*radius;int id=-1;try(PreparedStatement ps=connection.prepareStatement("SELECT * FROM locations WHERE type=? AND world=?")){ps.setString(1,type.name());ps.setString(2,at.getWorld().getName());try(ResultSet rs=ps.executeQuery()){while(rs.next()){double dx=rs.getDouble("x")-at.getX(),dy=rs.getDouble("y")-at.getY(),dz=rs.getDouble("z")-at.getZ();double d=dx*dx+dy*dy+dz*dz;if(d<best){best=d;id=rs.getInt("id");}}}}catch(SQLException ignored){}if(id!=-1)try(PreparedStatement ps=connection.prepareStatement("DELETE FROM locations WHERE id=?")){ps.setInt(1,id);ps.executeUpdate();}catch(SQLException ignored){}
    }
    public synchronized void setContraband(UUID u,UUID op,int amount,String status){try(PreparedStatement ps=connection.prepareStatement("INSERT OR REPLACE INTO contraband(uuid,operation_uuid,amount,status,started_at) VALUES(?,?,?,?,?)")){ps.setString(1,u.toString());ps.setString(2,op.toString());ps.setInt(3,amount);ps.setString(4,status);ps.setLong(5,System.currentTimeMillis());ps.executeUpdate();}catch(SQLException e){plugin.getLogger().severe(e.getMessage());}}
    public synchronized int contrabandAmount(UUID u){try(PreparedStatement ps=connection.prepareStatement("SELECT amount FROM contraband WHERE uuid=? AND status='ACTIVE'")){ps.setString(1,u.toString());try(ResultSet rs=ps.executeQuery()){return rs.next()?rs.getInt(1):0;}}catch(SQLException e){return 0;}}
    public synchronized void clearContraband(UUID u){try(PreparedStatement ps=connection.prepareStatement("DELETE FROM contraband WHERE uuid=?")){ps.setString(1,u.toString());ps.executeUpdate();}catch(SQLException ignored){}}
    @Override public synchronized void close(){try{if(connection!=null)connection.close();}catch(SQLException ignored){}}
}
