package com.squedgy.mcmodmanager;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AppLogger {

	private static final Logger logger = LogManager.getLogger(AppLogger.class);

	public static void info(String message, Class<?> clazz) {
		logger.info(getFormattedClassString(clazz) + message);
	}

	public static void error(Throwable mess, Class<?> clazz) {
		logger.error(getFormattedClassString(clazz), mess);
	}

	public static void debug(String message, Class<?> clazz) {
		logger.debug(getFormattedClassString(clazz) + message);
	}

	public static void fatal(Throwable mess, Class<?> clazz) {
		logger.fatal(getFormattedClassString(clazz), mess);
	}

	public static void error(String message, Class<?> clazz) {
		logger.error(getFormattedClassString(clazz) + message);
	}

	public static void trace(String message, Class<?> clazz) {
		logger.trace(getFormattedClassString(clazz) + message);
	}

	private static String getFormattedClassString(Class<?> clazz) {
		return "[" + clazz.getSimpleName() + "] ";
	}
}
