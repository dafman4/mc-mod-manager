package com.squedgy.mcmodmanager.app.config;

import com.squedgy.utilities.reader.FileReader;
import com.squedgy.utilities.writer.FileWriter;

import java.util.HashMap;
import java.util.Map;

public abstract class Config {

    public static final String CONFIG_FILE_PATH = "config/manager.json";
    private static final JsonFileFormat format = new JsonFileFormat();
    private static final FileReader<Map<String,String>> READER = new FileReader<>(format, CONFIG_FILE_PATH);
    private static final FileWriter<Map<String,String>> WRITER = new FileWriter<>(CONFIG_FILE_PATH, format, false);
    private static Map<String,String> CONFIG;

    public static void init(){
        try {
            CONFIG = readProps();
        } catch (Exception e) {
            CONFIG = new HashMap<>();
        }
    }

    public static String getProperty(String key) {
        if(CONFIG == null)init();
        return CONFIG.get(key);
    }

    public static String setProperty(String key, String prop) {
        if(CONFIG == null)init();
        return CONFIG.put(key,prop);
    }

    public static Map<String,String> readProps() throws Exception{
        return readProps(CONFIG_FILE_PATH);
    }

    public static Map<String,String> readProps(String file) throws Exception {
        READER.setFileLocation(file);
        return READER.read();
    }

    public static void writeProps(){ writeProps(CONFIG_FILE_PATH); }

    public static void writeProps(String file){ writeProps(file, CONFIG);}

    public static void writeProps(String file, Map<String,String> props){
        try {

            WRITER.setFileLocation(file);
            WRITER.write(props);
        } catch (Exception e) {
            throw new RuntimeException("The file " + file + " couldn't be written to, check logs for more information.", e);
        }
    }

}
