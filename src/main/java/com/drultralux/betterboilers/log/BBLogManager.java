package com.drultralux.betterboilers.log;

import com.drultralux.betterboilers.util.BBConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BBLogManager {

    private static final Logger LOG4J = LogManager.getLogger("Better Boilers");

    private static final Queue<LogEntry> QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger DROPPED_COUNT = new AtomicInteger(0);

    private static class DedupState {
        int count = 0;
        long windowStartMillis = 0;
    }

    private static final Map<String, DedupState> DEDUP = new ConcurrentHashMap<>();

    private BBLogManager() {}

    public static void submit(LogLevel level, String message, Object[] params, Throwable throwable) {
        try {
            if (level == LogLevel.DEBUG || level == LogLevel.TRACE) {
                if (!BBConfig.debugLoggingEnabled) return;
            }

            String resolved;
            try {
                resolved = (params != null && params.length > 0)
                        ? ParameterizedMessage.format(message, params)
                        : message;
            } catch (Exception formatEx) {
                resolved = message + " [log formatting error: " + formatEx.getMessage() + "]";
            }

            String dedupKey = level.name() + "|" + resolved;
            DedupState state = DEDUP.computeIfAbsent(dedupKey, k -> new DedupState());
            long now = System.currentTimeMillis();
            long windowMillis = BBConfig.logDedupWindowTicks * 50L;

            synchronized (state) {
                if (now - state.windowStartMillis > windowMillis) {
                    if (state.count > 1) {
                        enqueue(new LogEntry(level, "[repeated " + state.count + "x] " + resolved, null));
                    }
                    state.windowStartMillis = now;
                    state.count = 1;
                    enqueue(new LogEntry(level, resolved, throwable));
                } else {
                    state.count++;
                    if (state.count <= 1) {
                        enqueue(new LogEntry(level, resolved, throwable));
                    }
                }
            }
        } catch (Throwable t) {
            try {
                LOG4J.error("BBLogManager failed to process a log entry", t);
            } catch (Throwable ignored) {
                // Nothing more we can safely do here - logging must never crash the caller.
            }
        }
    }

    private static void enqueue(LogEntry entry) {
        if (QUEUE.size() >= BBConfig.maxLogQueueSize) {
            QUEUE.poll();
            DROPPED_COUNT.incrementAndGet();
        }
        QUEUE.add(entry);
    }

    public static void flush() {
        try {
            int budget = BBConfig.maxLogsPerTick;
            int dropped = DROPPED_COUNT.getAndSet(0);
            if (dropped > 0) {
                writeToLog4j(new LogEntry(LogLevel.WARN, "Log queue overflow: dropped " + dropped + " excess entries", null));
                budget--;
            }
            while (budget-- > 0) {
                LogEntry entry = QUEUE.poll();
                if (entry == null) break;
                writeToLog4j(entry);
            }
        } catch (Throwable t) {
            try {
                LOG4J.error("BBLogManager failed during flush", t);
            } catch (Throwable ignored) {
                // Nothing more we can safely do here.
            }
        }
    }

    private static void writeToLog4j(LogEntry entry) {
        // Route everything through .info() regardless of actual level - BBConfig.debugLoggingEnabled
        // is already our own gate deciding whether debug/trace messages get this far at all, so we
        // don't want log4j's own separate level filter potentially discarding them a second time
        // underneath us (which is exactly what was happening - some Forge setups default that
        // logger's threshold to INFO, silently swallowing .debug()/.trace() calls).
        String prefixed = "[" + entry.level.name() + "] " + entry.message;
        switch (entry.level) {
            case WARN:  LOG4J.warn(prefixed, entry.throwable); break;
            case ERROR: LOG4J.error(prefixed, entry.throwable); break;
            case FATAL: LOG4J.fatal(prefixed, entry.throwable); break;
            default:    LOG4J.info(prefixed, entry.throwable); break;
        }
    }
}