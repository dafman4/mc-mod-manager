package com.squedgy.mcmodmanager.app.config;

import com.squedgy.mcmodmanager.api.abstractions.ModVersion;
import com.squedgy.mcmodmanager.api.cache.JsonFileFormat;
import com.squedgy.utilities.reader.FileReader;
import com.squedgy.utilities.writer.FileWriter;
import javafx.scene.control.TableColumn;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Config {

    public static final String CONFIG_DIRECTORY = "config/";
    public static final String CONFIG_FILE_PATH = CONFIG_DIRECTORY + "manager.json";
    private static final String TABLE_CONFIG = "table.";

    private static final JsonFileFormat format = new JsonFileFormat();
    private static final FileReader<Map<String,String>> READER = new FileReader<>(CONFIG_FILE_PATH, format);
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

    public static void writeProps(){ writeProps(CONFIG_FILE_PATH, CONFIG); }

    public static <T> void writeProps(Map<String,T> config){
        //Props = config
        Map<String,String> props = new HashMap<>(CONFIG);
        //Add all the new props (therefore overriding existing if necessary
         props.putAll(
                 config.entrySet()
                     .stream()
                     .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().toString()))
                     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
         );
        writeProps(CONFIG_FILE_PATH, props);
    }

    public static void writeProps(String file, Map<String,String> props){
        try {
            WRITER.setFileLocation(file);
            WRITER.write(props);
        } catch (Exception e) {
            throw new RuntimeException("The file " + file + " couldn't be written to, check logs for more information.", e);
        }
    }



    public static int compareColumns(String a, String b){
        Integer one = Integer.getInteger(getProperty(TABLE_CONFIG + a)),
                two = Integer.getInteger(getProperty(TABLE_CONFIG + b));

        if(one == null && two == null) return 0;
        else if (one == null) return -1;
        else if (two == null) return 1;

        return one.compareTo(two);
    }

    public static void writeColumnOrder(List<TableColumn<ModVersion, ?>> order){
        Map<String,String> props = new HashMap<>();
        for(int i = 0; i < order.size(); i++) props.put(order.get(i).getText(), String.valueOf(i));
        //Rewrite the columns keys to table.{column_name} so it's within an inner object
        props = props.entrySet()
                .stream()
                .map(e -> new AbstractMap.SimpleEntry<>(TABLE_CONFIG + e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        //Add config so it's all nice and dandy
        props.putAll(CONFIG);
        System.out.println(props);
        Config.writeProps( CONFIG_FILE_PATH, props);
    }

}
