package com.app.beta;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


/*
 * The copy files app's starter program.
 * Launches the app and the main GUI.
 */
public class AppStarter extends Application {


    private static Logger logger;
    private static final String LOG_FILE = "copy_files_app_log_%g.txt";


    public static void main(String... args) {

        Application.launch(args);
    }

    /*
     * Does some initial configuration for the app.
     * Configures logger and its handlers.
     * Exits the application if there is an exception.
     */
    @Override
    public void init() {

        try {
            configureLogging();
        }
        catch (IOException ex) {

            System.out.println("An IOException occurred during app initialization.");
            System.out.println("This happened during the logger configuration.");
            System.out.println("See the stack trace below:");
            ex.printStackTrace();
            Platform.exit(); // NOTE: doesn't run the stop() method.
        }
    }

    /*
     * Configures the logger and registers a file handler.
     */
    private void configureLogging()
            throws IOException {

        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS  %5$s%n");
        logger = Logger.getLogger("copy_app_logger");
        logger.setUseParentHandlers(false); // disables console logging
        logger.addHandler(new LogFileHandler(LOG_FILE));
        logger.setLevel(Level.INFO);
        logger.info("Logger is configured");
    }

    /*
     * Displays the main GUI.
     */
    @Override
    public void start(Stage primaryStage) {

        logger.info("Launching the GUI");
        new FileTreeView(primaryStage);
    }

    /*
     * Does cleanup and releases resources.
     */
    @Override
    public void stop() {

        logger.info("Closing the app");

        // Close the logger's file and stream handlers
        Stream.of(logger.getHandlers()).forEach(Handler::close);
    }
}
