package com.acme.saf.actor.core;

public interface Logger {
    void info(String msg);
    void warning(String msg);
    void error(String msg, Throwable t);
}
