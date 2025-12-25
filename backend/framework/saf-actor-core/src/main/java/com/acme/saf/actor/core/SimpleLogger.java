package com.acme.saf.actor.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class SimpleLogger implements Logger {

    private final String actorName;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // fichier où tous les logs sont stockés
    private static final Path LOG_FILE_PATH = Path.of("saf-runtime.log");

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

    private synchronized void writeToFile(String content) {
        try {
            // On écrit aussi dans la console pour que debugger
            System.out.println(content);

            // Écriture dans le fichier (Mode append pour ne pas écraser les logs précédents mais à voir)
            Files.writeString(LOG_FILE_PATH, content + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            // Si le log échoue on l'affiche en erreur
            System.err.println("ERREUR LOGGING: Impossible d'écrire dans " + LOG_FILE_PATH);
            e.printStackTrace();
        }
    }

    @Override
    public void info(String msg) {
        writeToFile(format("INFO", msg));
    }

    @Override
    public void warning(String msg) {
        writeToFile(format("WARN", msg));
    }

    @Override
    public void error(String msg, Throwable t) {
        writeToFile(format("ERROR", msg));
        if (t != null) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement elem : t.getStackTrace()) {
                sb.append("\t").append(elem.toString()).append(System.lineSeparator());
            }
            writeToFile(sb.toString());
        }
    }

    public void logEvent(ActorLifecycleEvent event) {
        writeToFile(format("EVENT", event.toString()));
    }
}