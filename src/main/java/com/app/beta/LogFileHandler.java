package com.app.beta;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

/*
 * Handler to write log messages to a log file.
 */
public class LogFileHandler extends FileHandler {


    public LogFileHandler(String pattern)
            throws IOException {

        super(pattern);
        setFormatter(new SimpleFormatter()); // overrides the default xml formatter
    }
}
