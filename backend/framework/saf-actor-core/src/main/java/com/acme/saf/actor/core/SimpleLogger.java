package com.acme.saf.actor.core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SimpleLogger implements Logger {

    private final String actorName;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public SimpleLogger(String actorName) {
        this.actorName = actorName;
    }

    // Méthode privée pour formater chaque ligne de log
    private String format(String level, String msg) {
        return String.format("[%s] [%s] [%s] [%s] %s",
                LocalTime.now().format(FMT),      // L'heure
                Thread.currentThread().getName(), // Le thread
                level,                            // INFO/WARN/ERROR
                actorName,                        // Nom de l'acteur
                msg
        );
    }

    @Override
    public void info(String msg) {
        System.out.println(format("INFO", msg));
    }

    @Override
    public void warning(String msg) {
        System.out.println(format("WARN", msg));
    }

    @Override
    public void error(String msg, Throwable t) {
        System.err.println(format("ERROR", msg));
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }

    public void logEvent(ActorLifecycleEvent event) {
        System.out.println(format("EVENT", event.toString()));
    }
}