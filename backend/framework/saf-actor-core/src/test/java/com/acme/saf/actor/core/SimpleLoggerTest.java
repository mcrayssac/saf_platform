package com.acme.saf.actor.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleLoggerTest {

    // Le fichier que le logger créer
    private static final Path LOG_FILE = Path.of("saf-runtime.log");

    @BeforeEach
    @AfterEach
    void cleanUp() throws IOException {
        // On supprime le fichier avant et après chaque test pour être sûr de partir de 0
        Files.deleteIfExists(LOG_FILE);
    }

    @Test
    void shouldWriteLogToFile() throws IOException {
        SimpleLogger logger = new SimpleLogger("TestActor");
        String message = "Ceci est un test d'écriture fichier";

        logger.info(message);

        // Vérifications
        // Le fichier doit exister
        assertTrue(Files.exists(LOG_FILE), "Le fichier de log n'a pas été créé");

        // Le fichier doit contenir le message
        String content = Files.readString(LOG_FILE);
        assertTrue(content.contains(message), "Le fichier ne contient pas le message loggé");
        assertTrue(content.contains("[INFO]"), "Le niveau de log est manquant");
        assertTrue(content.contains("TestActor"), "Le nom de l'acteur est manquant");
    }
}