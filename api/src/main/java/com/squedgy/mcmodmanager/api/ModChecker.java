package com.squedgy.mcmodmanager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.Cacher;
import com.squedgy.mcmodmanager.api.response.CurseForgeResponseDeserializer;
import com.squedgy.mcmodmanager.api.abstractions.CurseForgeResponse;
import com.squedgy.mcmodmanager.api.response.ModIdNotFoundException;
import com.squedgy.mcmodmanager.api.response.ModIdFailedException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystems;
import java.util.Comparator;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public abstract class ModChecker {

    private static boolean check = false,
                            idChecked = false;
    private static final String CACHE_BASE = "cache/",
                                MOD_CACHE = CACHE_BASE + "mods/";

    private static String currentRead = "", currentWrite = "";



    public static CurseForgeResponse getForVersion(String mod, String version) throws Exception {
        return get(mod, new CurseForgeResponseDeserializer(version));
    }

    public static CurseForgeResponse get(String mod)throws Exception{
        return get(mod, new CurseForgeResponseDeserializer());
    }

    private static synchronized void setReadWrite(Runnable r){
        r.run();
    }

    public static synchronized ModVersion getCurrentVersion(String mod, String mcVersion){
        ModVersion ret = null;
        while(currentWrite.equals(mod + "." + mcVersion));
        setReadWrite(() -> currentRead = mod + "." + mcVersion);
        Cacher c = new Cacher();

        try { ret = c.readCache(mod, mcVersion); }
        catch (Exception e) { AppLogger.error(e, ModChecker.class); }

        setReadWrite(() -> currentRead = "");
        return ret;
    }

    public static synchronized void writeCurrentVersion(ModVersion fromCurse, String modId){
        while(currentRead.equals(modId + "." + fromCurse));
        setReadWrite(() -> currentWrite = modId + "." + fromCurse);


        setReadWrite(() -> currentWrite = "");
    }

    private static CurseForgeResponse get(String mod, CurseForgeResponseDeserializer deserializer) throws Exception{

        URL url = new URL("https://api.cfwidget.com/minecraft/mc-mods/" + mod);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
        if(con.getResponseCode() == 202 && !check){
            Thread.sleep(1000 * 2);
            check = true;
            return get(mod, deserializer);
        }else if ((con.getResponseCode() == 400 || con.getResponseCode() == 404) && idChecked){
            idChecked = false;
            throw new ModIdNotFoundException(mod);
        }else if (con.getResponseCode() == 400 || con.getResponseCode() == 404){
            idChecked = true;
            throw new ModIdFailedException();
        }
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))){
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new SimpleModule()
                            .addDeserializer(CurseForgeResponse.class, deserializer));
            return mapper.readValue(reader.lines().collect(Collectors.joining("")), CurseForgeResponse.class);
        }catch(FileNotFoundException e){
            throw new ModIdNotFoundException(mod);
        }catch (Exception e){
            throw new RuntimeException(String.format("Error with mod %s.", mod), e);
        }finally {
            check = false;
            idChecked = false;
        }
    }

    public static ModVersion getNewest(String mId, String mcV) throws Exception{
        ModVersion ret = getCurrentVersion(mId, mcV);
        //If no cached type
        if(ret == null){
            ret = getForVersion(mId, mcV)
                .getVersions()
                .stream()
                .max(Comparator.comparing(ModVersion::getUploadedAt))
                .orElse(null);

            if(ret == null){
                throw new ModIdNotFoundException("Couldn't find the mod Id : " + mId + ". It's not cached and DOESN'T match a Curse Forge mod.\nTalk to the mod author about having the Id within their mcmod.info file match their Curse Forge mod id.");
            }
        }

        return ret;
    }

    public static boolean download(ModVersion v, String location){
        URL u = null;
        try {
            u = new URL(v.getDownloadUrl());
        } catch (MalformedURLException e) {
            AppLogger.error(e, ModChecker.class);
            return  false;
        }
        ReadableByteChannel in = null;
        try {
            in = Channels.newChannel(u.openStream());
        } catch (IOException e) {
            AppLogger.error(e, ModChecker.class);
            return false;
        }

        boolean append = !v.getFileName().endsWith(".jar");
        File f = new File(location + v.getFileName() + (append ? ".jar": ""));

        FileOutputStream outFile = null;
        try {
            outFile = new FileOutputStream(f);
            FileChannel out = outFile.getChannel();
            out.transferFrom(in, 0, Long.MAX_VALUE);
            String modId = Cacher.getJarModId(new JarFile(f.getAbsolutePath()));

            Cacher c = new Cacher();

            c.writeCache(v, modId);
        } catch (IOException e) {
            AppLogger.error(e, ModChecker.class);
            return false;
        } catch(Exception e){
            AppLogger.error(e, ModChecker.class);
        }
        return true;
    }

}
