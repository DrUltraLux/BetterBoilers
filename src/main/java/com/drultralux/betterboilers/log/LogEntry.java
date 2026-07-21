package com.drultralux.betterboilers.log;

public class LogEntry {
    public final LogLevel level;
    public final String message;
    public final Throwable throwable;
    public final long timestampMillis;

    public LogEntry(LogLevel level, String message, Throwable throwable) {
        this.level = level;
        this.message = message;
        this.throwable = throwable;
        this.timestampMillis = System.currentTimeMillis();
    }
}
