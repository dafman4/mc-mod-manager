package com.squedgy.mcmodmanager.api.cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squedgy.mcmodmanager.AppLogger;
import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.response.ModVersionFactory;
import com.squedgy.utilities.reader.FileReader;
import com.squedgy.utilities.writer.FileWriter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class Cacher {

    public static final String MOD_CACHE_DIRECTORY = "cache/mods/";
    private final FileReader<Map<String,String>> READER;
    private final FileWriter<Map<String,String>> WRITER;
    private static final String MOD_ID = "mod-id",
        URL = "url",
        FILE_NAME = "file-name",
        RELEASE_TYPE = "release",
        MC_VERSION = "mc-version",
        UPLOADED_AT = "uploaded",
        DESCRIPTION = "desc",
        MOD_NAME = "mod-name",
        ID = "id";

    public Cacher(){
        JsonFileFormat f = new JsonFileFormat();
        READER = new FileReader<>(MOD_CACHE_DIRECTORY, f);
        WRITER = new FileWriter<>(MOD_CACHE_DIRECTORY, f, false);
    }
    //Pass in the modId separately as we want to cache the one that matches Curse Forge's knowledge
    public synchronized void writeCache(ModVersion v, String modId) throws Exception {
        WRITER.setFileLocation(getFileLocation(modId, v.getMinecraftVersion()));
        Map<String,String> toWrite = new HashMap<>();
        toWrite.put(ID, "" + v.getId());
        toWrite.put(MOD_ID, v.getModId());
        toWrite.put(MOD_NAME, v.getModName());
        toWrite.put(FILE_NAME, v.getFileName());
        toWrite.put(RELEASE_TYPE, v.getTypeOfRelease());
        toWrite.put(MC_VERSION, v.getMinecraftVersion());
        toWrite.put(UPLOADED_AT, v.getUploadedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        toWrite.put(DESCRIPTION, v.getDescription());
        toWrite.put(URL, v.getDownloadUrl());
        WRITER.write(toWrite);
    }

    public synchronized ModVersion readCache(String modId, String mcVersion) throws Exception {
        READER.setFileLocation(getFileLocation(modId, mcVersion));
        Map<String,String> info = READER.read();
        return new ModVersionFactory()
            .withId(Long.valueOf(info.get(ID)))
            .withModId(info.get(MOD_ID))
            .withName(info.get(MOD_NAME))
            .withFileName(info.get(FILE_NAME))
            .withType(info.get(RELEASE_TYPE))
            .withMcVersion(info.get(MC_VERSION))
            .withDescription(info.get(DESCRIPTION))
            .withUrl(info.get(URL))
            .uploadedAt(LocalDateTime.parse(info.get(UPLOADED_AT)))
            .build();
    }

    public static String getFileLocation(String mId, String v){
        return MOD_CACHE_DIRECTORY + mId + File.separator + v + File.separator + "cache.json";
    }

    public static String getJarModId(JarFile file) throws CachingFailedException{
        ZipEntry e = file.getEntry("mcmod.info");
        if(e!= null && !e.isDirectory()){
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode root = mapper.readValue(file.getInputStream(e), JsonNode.class);
                if(root.isArray()) root = root.get(0);
                if(root.has("modid")) return root.get("modid").textValue();
            } catch (IOException e1) {
                AppLogger.error(e1, Cacher.class);
            }
        }
        throw new CachingFailedException();
    }

}
