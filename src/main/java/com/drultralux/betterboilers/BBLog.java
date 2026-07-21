package com.drultralux.betterboilers;

import com.drultralux.betterboilers.log.BBLogManager;
import com.drultralux.betterboilers.log.LogLevel;

public class BBLog {

    private BBLog() {}

    public static void trace(String message, Object... params) {
        BBLogManager.submit(LogLevel.TRACE, message, params, null);
    }

    public static void debug(String message, Object... params) {
        BBLogManager.submit(LogLevel.DEBUG, message, params, null);
    }

    public static void info(String message, Object... params) {
        BBLogManager.submit(LogLevel.INFO, message, params, null);
    }

    public static void info(Object message) {
        BBLogManager.submit(LogLevel.INFO, String.valueOf(message), null, null);
    }

    public static void warn(String message, Object... params) {
        BBLogManager.submit(LogLevel.WARN, message, params, null);
    }

    public static void warn(String message, Throwable t) {
        BBLogManager.submit(LogLevel.WARN, message, null, t);
    }

    public static void error(String message, Object... params) {
        BBLogManager.submit(LogLevel.ERROR, message, params, null);
    }

    public static void error(String message, Throwable t) {
        BBLogManager.submit(LogLevel.ERROR, message, null, t);
    }

    public static void fatal(String message, Throwable t) {
        BBLogManager.submit(LogLevel.FATAL, message, null, t);
    }
}