package com.squedgy.mcmodmanager.app.config;

import com.typesafe.config.ConfigFactory;
import com.squedgy.utilities.writer.FileWriter;

import java.io.File;
import java.io.IOException;

public abstract class Config {

    private static final com.typesafe.config.Config conf = buildConfig();
    public static final String CONFIG_FILE_PATH = "config/manager.json";

    private static com.typesafe.config.Config buildConfig(){
        File f = new File(CONFIG_FILE_PATH);
        try {
            if (!f.exists()) {
                if (f.createNewFile()){

                }
            }
        }catch (IOException e){

        }

        return ConfigFactory.parseFile(f);
    }

}
