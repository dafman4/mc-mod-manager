package com.squedgy.mcmodmanager.app.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.ModChecker;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.squedgy.mcmodmanager.app.MainController;
import com.squedgy.mcmodmanager.app.config.Config;
import com.sun.javafx.collections.UnmodifiableObservableMap;

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

import static com.squedgy.mcmodmanager.app.MainController.DOT_MINECRAFT_LOCATION;
import static com.squedgy.mcmodmanager.app.MainController.MINECRAFT_VERSION;

public class ModUtils {


    public final Config CONFIG;
    private static Map<String,String> badJars = new HashMap<>();

    public static Map<String,String> viewBadJars(){ return Collections.unmodifiableMap(badJars); }

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

    public ModVersion readNode(JsonNode modInfo, JarFile modJar){
        ModVersionFactory factory = new ModVersionFactory();
        if(modInfo.has("name")) factory.withName(modInfo.get("name").textValue());
        else factory.withName("");

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
                    badJars.put(f.getName(), "mcmod.info doesn't exist");
                    continue;
                }
                try {
                    String jarId;
                    try {
                        jarId = Cacher.getJarModId(file);
                    }catch(IOException e1){
                        AppLogger.debug("mod: " + file.getName() + " didn't contain an mcmod.info", MainController.class);
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
                        badJars.put(jarId, "Error parsing Json");
                        continue;
                    }
                    if(root.has("modList")) root = root.get("modList");//this will be an array

                    checkNode(root, jarId, root.isArray() ? root.size() : 0, ret);

                    try {
                        ModVersion v = readNode(root, file);
                        addToList(ret, v);
                        badJars.put(v.getModName(), "This mod doesn't have a correct id within their mcmod.info file and I couldn't guess what it's id was, inform the mod creator if they're willing otherwise you'll have to add it to the cache if you want all the features");
                    } catch (ModIdNotFoundException e1){
                        badJars.put(f.getName(), e1.getMessage());
                    }
                } catch (Exception e2) { AppLogger.error(e2, MainController.class);}
            }
        }
        return ret;
    }

    private void checkNode(JsonNode array, String jarId, int length, List<ModVersion> ret){
        int i = 0;
        do{
            JsonNode root = length > 0 ? array.get(i) : array;
            i++;
            if(root == null) continue;
            try {
                searchWithId(root.get("modid").textValue(), jarId, ret);
                break;
            }catch(ModIdNotFoundException ex2){
                try{
                    searchWithId(formatModName(root.get("name").textValue()), jarId, ret);
                    break;
                }catch (ModIdNotFoundException ignored){}
            }
        }while( i < length);
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
                System.out.println("BAD JARS");
                badJars.forEach((id, mes) -> System.out.println(id + ": " + mes));
                try { CONFIG.getCachedMods().writeCache(); }
                catch (IOException e) {
                    AppLogger.error(e, MainController.class);
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
}
