package com.acme.saf.saf_control.domain.actors.core.logging;

public interface Logger {
    void info(String msg);
    void warning(String msg);
    void error(String msg, Throwable t);
}
