package com.squedgy.mcmodmanager.app.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.response.ModIdFailedException;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.squedgy.mcmodmanager.app.Startup;
import com.squedgy.mcmodmanager.app.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import static com.squedgy.mcmodmanager.app.Startup.DOT_MINECRAFT_LOCATION;
import static com.squedgy.mcmodmanager.app.Startup.MINECRAFT_VERSION;

public class ModUtils {


    public final Config CONFIG;
    private static Map<String,String> badJars = new HashMap<>();

    public static Map<String,String> viewBadJars(){ return new HashMap<>(badJars); }

    private static ModUtils instance;

    private ModUtils(){
        CONFIG = Config.getInstance();
    }

    public static ModUtils getInstance(){
        if(instance == null){
            instance = new ModUtils();
        }
        return instance;
    }

    public ModVersion readNode(JsonNode modInfo, JarFile modJar) throws ModIdFailedException{
        ModVersionFactory factory = new ModVersionFactory();
        if(modInfo.has("name")) factory.withName(modInfo.get("name").textValue());
        else throw new ModIdFailedException("Node doesn't have a name within the mcmod.info");

        String[] names = modJar.getName().split("[\\\\/]");
        factory.withFileName(names[names.length-1]);

        if(modInfo.has("modid"))factory.withModId(modInfo.get("modid").textValue());
        else factory.withModId(formatModName(modInfo.get("name").textValue()));

        if(modInfo.has("mcversion")) factory.withMcVersion(modInfo.get("mcversion").textValue().replaceAll("[^0-9.]", ""));
        else factory.withMcVersion("");

        if(modInfo.has("url"))factory.withUrl(modInfo.get("url").textValue());
        else factory.withUrl("");

        modJar.stream().min(Comparator.comparing(ZipEntry::getLastModifiedTime))
            .ifPresent(e -> factory.uploadedAt(LocalDateTime.ofInstant(e.getLastModifiedTime().toInstant(), ZoneId.systemDefault())));

        return factory.build();
    }

    public  List<ModVersion> scanForMods(File folder){
        List<ModVersion> ret = new LinkedList<>();
        for(File f : Objects.requireNonNull(folder.listFiles())){
            if(f.isDirectory()){
                ret.addAll(scanForMods(f));
            }else if(f.getName().endsWith(".jar")){
                JarFile file = null;
                try { file = new JarFile(f); }
                catch (IOException e) { continue; }

                ZipEntry e = file.getEntry("mcmod.info");
                if(e == null){
                    addBadJar(f.getName(), "mcmod.info doesn't exist");
                    continue;
                }
                try {
                    String jarId;
                    try {
                        jarId = Cacher.getJarModId(file);
                    }catch(IOException e1){
                        AppLogger.debug("mod: " + file.getName() + " didn't contain an mcmod.info", Startup.class);
                        continue;
                    }
                    try{
                        searchWithId(jarId, jarId, ret);
                        continue;
                    }catch(ModIdNotFoundException ignored){ }

                    JsonNode root;
                    try(BufferedReader r = new BufferedReader(new InputStreamReader(file.getInputStream(e)))) {
                        root = new ObjectMapper()
                            .readValue(
                                r.lines().map(l -> l.replaceAll("\n", "\\n")).collect(Collectors.joining()),
                                JsonNode.class
                            );
                    }catch(JsonParseException e1){
                        addBadJar(jarId, "Error parsing Json");
                        continue;
                    }
                    if(root.has("modList")){
                        root = root.get("modList");//this will be an array
                    }

                    if(!checkNode(root, jarId, root.isArray() ? root.size() : 0, ret)){
                        addToList(ret, readNode(root.isArray() ? root.get(0) : root, file));
                        addBadJar(f.getName(), "Couldn't find a working mod Id within the mcmod.info");
                    }
                } catch (Exception e2) { AppLogger.error(e2, Startup.class);}
            }
        }
        return ret;
    }

    private boolean checkNode(JsonNode array, String jarId, int length, List<ModVersion> ret){
        int i = 0;
        do{
            JsonNode root = length > 0 ? array.get(i) : array;
            i++;
            if(root == null) continue;
            try {
                searchWithId(root.get("modid").textValue(), jarId, ret);
                return true;
            }catch(ModIdNotFoundException | ModIdFailedException ex2){
                try{
                    searchWithId(formatModName(root.get("name").textValue()), jarId, ret);
                    return true;
                }catch (ModIdNotFoundException | ModIdFailedException ignored){}
            }
        }while( i < length);
        return false;
    }

    private  void searchWithId(String id, String jarId, List<ModVersion> list) throws ModIdNotFoundException{
        ModVersion v = CONFIG.getCachedMods().getMod(id);

        if(v == null) v = ModChecker.getNewest(id, MINECRAFT_VERSION);
        if(v.getDescription() == null) v = ModChecker.getNewest(v.getModId(), MINECRAFT_VERSION);


        if(v != null){
            CONFIG.getCachedMods().addMod(jarId, v);
            addToList(list, v);
        }else throw new ModIdNotFoundException("Id not found: " + id);
    }

    public  void addToList(List<ModVersion> list, ModVersion item){
        list.add(item);
    }

    public ModVersion[] getMods(){
        List<ModVersion> strings = new LinkedList<>();
        File f = new File(DOT_MINECRAFT_LOCATION);
        if(f.exists() && f.isDirectory()){
            f = FileSystems.getDefault().getPath(DOT_MINECRAFT_LOCATION).resolve("mods").toFile();
            if(f.exists() && f.isDirectory()){
                strings.addAll(scanForMods(f));
                try { CONFIG.getCachedMods().writeCache(); }
                catch (IOException e) {
                    AppLogger.error(e, Startup.class);
                }
            }
        }
        return strings.toArray(new ModVersion[0]);
    }

    public String formatModName(String name){
        name = name.toLowerCase().replace(' ' , '-');
        if(name.contains(":"))name = name.substring(0, name.indexOf(':'));
        name = name
            .replaceAll("[^-a-z0-9]", "")
            .replaceAll("([^-])([0-9]+)|([0-9]+)([^-])", "$1-$2");
        return name;
    }

    public void addBadJar(String file, String reason){
        badJars.put(file, reason);
    }
}
