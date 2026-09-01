package ru.allin.factions.service;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class BackupService {
    private final JavaPlugin plugin;
    public BackupService(JavaPlugin plugin){this.plugin=plugin;}
    public void backup(){if(!plugin.getConfig().getBoolean("database.backup.enabled",true))return;try{File src=new File(plugin.getDataFolder(),"data.db");if(!src.exists())return;File dir=new File(plugin.getDataFolder(),"backups");dir.mkdirs();String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));Files.copy(src.toPath(),new File(dir,"data-"+stamp+".db").toPath(),StandardCopyOption.REPLACE_EXISTING);File[] files=dir.listFiles((d,n)->n.endsWith(".db"));if(files!=null){Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());int keep=plugin.getConfig().getInt("database.backup.keep",20);for(int i=keep;i<files.length;i++)files[i].delete();}}catch(Exception e){plugin.getLogger().warning("Backup failed: "+e.getMessage());}}
}
