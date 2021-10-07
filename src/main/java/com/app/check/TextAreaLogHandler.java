package com.app.check;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;


/*
 * Handler to write log messages to the app's GUI (status message area,
 * a TextArea control, of the CopyDialog.java).
 */
public class TextAreaLogHandler extends StreamHandler {

    TextArea textArea;

    public TextAreaLogHandler(TextArea textArea) {

        this.textArea = textArea;
    }

    @Override
    public void publish(LogRecord record) {

        super.publish(record);
        flush();
        Platform.runLater(() -> textArea.appendText(getFormatter().format(record)));
    }
}
