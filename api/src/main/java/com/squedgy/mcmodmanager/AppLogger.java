package com.squedgy.mcmodmanager;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AppLogger {

    private static final Logger logger = LogManager.getLogger();

    public static void info(String message, Class<?> clazz){ logger.info(getFormattedClassString(clazz) + message ); }

    public static void error(Throwable mess, Class<?> clazz){ logger.error(getFormattedClassString(clazz), mess); }

    public static void debug(String message, Class<?> clazz){ logger.debug( getFormattedClassString(clazz) + message);}

    public static void fatal(Throwable mess, Class<?> clazz){ logger.fatal(getFormattedClassString(clazz), mess); }

    private static String getFormattedClassString(Class<?> clazz){
        return "[" + clazz.getSimpleName() + "] ";
    }
}
