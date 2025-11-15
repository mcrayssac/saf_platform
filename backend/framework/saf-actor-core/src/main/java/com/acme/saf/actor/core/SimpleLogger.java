package com.acme.saf.actor.core;

public class SimpleLogger implements Logger {
    private final String actorName;

    public SimpleLogger(String actorName) {
        this.actorName = actorName;
    }

    @Override
    public void info(String msg) {
        System.out.println("[INFO][" + actorName + "] " + msg);
    }

    @Override
    public void warning(String msg) {
        System.out.println("[WARNING][" + actorName + "] " + msg);
    }

    @Override
    public void error(String msg, Throwable t) {
        System.err.println("[ERROR][" + actorName + "] " + msg);
        t.printStackTrace();
    }
}
