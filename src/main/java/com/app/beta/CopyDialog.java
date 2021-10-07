package com.app.beta;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/*
 * Constructs the copy files dialog. This is invoked from the app's main GUI.
 * This dialog captures the info to perform the copy of the selected files
 * in the main view: the target directory, the file filters and an option to
 * create a ZIP file. Starts the copy process: the file filters are applied
 * to the selected files and the filtered files are copied to the target. The
 * process can be cancelled if needed. The process progress is viewed throw a
 * progress bar and the status is logged to the status area.
 */
public class CopyDialog {


    private final TextArea statusArea;
    private Button selectTargetBtn;
    private Button filtersBtn;
    private Button copyBtn;
    private Button cancelBtn;
    private Button closeBtn;
    private CheckBox zipCheckBox;
    private ProgressBar progressBar;

    private final FileFilterDialog fileFiltersDialog;
    private FileFilters fileFilters;

    // The root or the source directory input from the main GUI with the
    // file tree view.
    private Path sourceDir;

    // The target or destination directory to which the files are copied to.
    // This is obtained from a directory chooser in this dialog.
    private Path targetDir;

    // The files copy is performed in a background thread by this Task object.
    // See copyRoutine() method.
    private Task<Void> copyTask;

    // Counters for total files and directories that are actually copied.
    // These are used to show the status after the copy task is complete.
    private int copiedFilesCount;
    private int copiedDirsCount;


    private static final String DEFAULT_DIRECTORY =
            System.getProperty("user.dir"); //  or "user.home"
    private static Logger logger;


    /*
     * Constructor.
     * Creates a copy of the FileFilterDialog.
     * Obtains the logger and configures it with the TextAreaLogHandler.
     */
    public CopyDialog() {

        logger = Logger.getLogger("copy_app_logger");
        statusArea = getTextArea();
        TextAreaLogHandler handler = new TextAreaLogHandler(statusArea);
        logger.addHandler(handler);
        fileFiltersDialog = new FileFilterDialog();
    }

    /*
     * Constructs the GUI for the Copy dialog.
     */
    public void create(Path sourceDir, Set<Path> selectedFiles) {

        this.sourceDir = sourceDir;

        Stage dialog = new Stage();
        dialog.setResizable(false);
        dialog.setTitle("Copy Files");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setOnCloseRequest(Event::consume); // disable Close (x) button

        Platform.runLater(() -> statusArea.setText(""));

        selectTargetBtn = new Button("Target directory...");
        selectTargetBtn.setTooltip(new Tooltip("Select a target directory"));
        selectTargetBtn.setOnAction(e -> chooseTargetDirectory());
        filtersBtn = new Button("Filters...");
        filtersBtn.setTooltip(new Tooltip("Apply file filters"));
        filtersBtn.setOnAction(e -> getFilters());
        filtersBtn.setDisable(true);
        zipCheckBox = new CheckBox("Create ZIP file");
        zipCheckBox.setDisable(true);
        copyBtn = new Button("Copy files...");
        copyBtn.setTooltip(new Tooltip("Copy files to target directory"));
        copyBtn.setOnAction(e -> copyRoutine(selectedFiles));
        copyBtn.setDisable(true);
        cancelBtn = new Button("Cancel copy");
        cancelBtn.setTooltip(new Tooltip("Cancel or abort the copy process"));
        cancelBtn.setOnAction(e -> {
            if (copyTask != null) {
                copyTask.cancel();
            }
        });
        cancelBtn.setDisable(true);
        closeBtn = new Button("Close");
        closeBtn.setTooltip(new Tooltip("Close the dialog"));
        closeBtn.setOnAction(e -> dialog.close());

        HBox btnHb = new HBox(15);
        btnHb.setAlignment(Pos.CENTER);

        /*
         * The ZIP file create option checkbox is available with the
         * Windows operating system only.
         */
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {

            btnHb.getChildren().addAll(selectTargetBtn, filtersBtn, zipCheckBox, copyBtn, cancelBtn, closeBtn);
        }
        else {
            btnHb.getChildren().addAll(selectTargetBtn, filtersBtn, copyBtn, cancelBtn, closeBtn);
        }

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(600.0d);
        progressBar.setTooltip(new Tooltip("Copy files process progress"));

        HBox statusHb = new HBox();
        statusHb.setAlignment(Pos.CENTER);
        statusHb.getChildren().addAll(progressBar);

        VBox vb = new VBox(20);
        vb.setPadding(new Insets(15, 15, 5, 15));
        vb.getChildren().addAll(statusArea, statusHb, btnHb);

        dialog.setScene(new Scene(vb));

        String initialText =
                "* Copy Files to a Target Directory * \n" +
                        "Total directories (includes root) and files selected: " +
                        selectedFiles.size() + " " +
                        "\nSource directory: " + sourceDir.toString() + " " +
                        "\n\nSelect a target directory, apply file filters and copy.\n";
        logger.info(initialText);

        dialog.showAndWait(); // this shows a modal window
    }

    private TextArea getTextArea() {

        TextArea textArea = new TextArea();
        textArea.setTooltip(new Tooltip("Status message area"));
        textArea.setPrefRowCount(14);
        textArea.setPrefColumnCount(60);
        textArea.setEditable(false);
        textArea.setFont(new Font("Verdana", 16));
        return textArea;
    }

    /*
     * Opens the directory chooser and lets the user select a
     * target directory for copying the selected files. The
     * directory is verified if it is valid.
     */
    private void chooseTargetDirectory() {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a target directory");
        chooser.setInitialDirectory(new File(DEFAULT_DIRECTORY));
        File chosenDir = chooser.showDialog(null);

        targetDir = (chosenDir == null) ? null : chosenDir.toPath();

        if (! verifyDirectories()) {

            return;
        }

        logger.info("Target directory: " + targetDir.toString());
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
        fileFilters = null;
        zipCheckBox.setDisable(false);
        copyBtn.setDisable(false);
        filtersBtn.setDisable(false);
        filtersBtn.requestFocus();
    }

    /*
     * Checks if the target directory path is not the same as
     * that of the source path, or the target is not within the
     * source directory structure; shows an alert message.
     */
    private boolean verifyDirectories() {

        if (targetDir == null) {

            showAlertDialog("No directory selected!");
            return false;
        }

        if ((sourceDir.equals(targetDir)) ||
                (targetDir.startsWith(sourceDir))) {

            showAlertDialog("Source and target directories are same, or " +
                    "the target is within the source.");
            return false;
        }

        if (Objects.requireNonNull(targetDir.toFile().list()).length > 0) {

            logger.warning("The target directory is not empty.");
        }

        return true;
    }

    /*
     * Displays a modal alert with the supplied message.
     */
    private void showAlertDialog(String msg) {

        Alert alert = new Alert(AlertType.NONE);
        alert.setTitle("Files Copy");
        alert.getDialogPane().getButtonTypes().add(ButtonType.OK);
        alert.setContentText(msg);
        alert.show();
    }

    /*
     * Displays the file filters dialog and captures user input. Gets
     * the selected file filter options as an instance of FileFilters.
     */
    private void getFilters() {

        Dialog<FileFilters> dialog = fileFiltersDialog.create();
        Optional<FileFilters> result = dialog.showAndWait();

        if (result.isPresent()) {

            fileFilters = result.get();

            if (fileFilters.getFileTypes().isEmpty()) {

                // In case there is no selection in the file types list,
                // which is possible with a ListView, the value is set.
                fileFilters.setFileTypes(FXCollections.observableArrayList("All"));
            }
        }

        logger.info("File filters: " + fileFilters.toString());
        zipCheckBox.requestFocus();
    }

    /*
     * Routine for the Copy files button action.
     * 1. Applies the file filters to the selected files.
     * 2. Copies the filtered files to target directory.
     * 3. Creates a ZIP file if the option is selected.
     * These tasks are performed as a JavaFX concurrent Task. At end,
     * a status (Succeeded, Failed/exception or Cancelled) is displayed
     * in the status message area.
     */
    private void copyRoutine(Set<Path> inputSelectedFiles) {

        copiedFilesCount = 0;
        copiedDirsCount = 0;

        copyTask = new Task<>() {

            int currentCounter;

            @Override
            protected Void call()
                    throws Exception {

                logger.info("Copying files.");
                Platform.runLater(() -> {
                    copyBtn.setDisable(true);
                    closeBtn.setDisable(true);
                    cancelBtn.setDisable(false);
                    filtersBtn.setDisable(true);
                    zipCheckBox.setDisable(true);
                    selectTargetBtn.setDisable(true);
                });

                Set<Path> filteredFiles = applyFileFilters(inputSelectedFiles);
                Map<Boolean, List<Path>> countsMap = filteredFiles.stream()
                        .collect(Collectors.partitioningBy(Files::isDirectory));
                int dirsCount = countsMap.get(true).size() - 1; // minus root dir
                int filesCount = countsMap.get(false).size();
                logger.info("Filters applied. " +
                        "Directories [" + (Math.max(dirsCount, 0)) + "], " +
                        "Files [" + filesCount + "].");

                Thread.sleep(100); // pause for n milliseconds
                logger.info("Copy in progress...");

                /*
                 * Walks the source file tree and copies the filtered source
                 * files to the target directory. The directories and files are
                 * copied. In case of any existing directories or files in the
                 * target, they are replaced.
                 */
                Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {

                    /*
                     * Copy the directories.
                     */
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir,
                                                             BasicFileAttributes attrs)
                            throws IOException {

                        if (isCancelled()) {

                            // Task's isCancelled() method returns true
                            // when its cancel() is executed; in this app
                            // when the Cancel copy button is clicked.
                            // Here, the files copy is terminated.
                            return FileVisitResult.TERMINATE;
                        }

                        if (! filteredFiles.contains(dir)) {

                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        Path target = targetDir.resolve(sourceDir.relativize(dir));

                        try {
                            Files.copy(dir, target);
                            copiedDirsCount++;
                            // Updates the Progress bar using the Task's
                            // updateProgress(workDone, max) method.
                            updateProgress(++currentCounter, dirsCount+filesCount);
                        }
                        catch (FileAlreadyExistsException e) {

                            if (! Files.isDirectory(target)) {

                                throw e;
                            }
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    /*
                     * Copy the files.
                     */
                    @Override
                    public FileVisitResult visitFile(Path file,
                                                     BasicFileAttributes attrs)
                            throws IOException {

                        if (isCancelled()) {

                            // Task's isCancelled() method
                            // terminates the files copy.
                            return FileVisitResult.TERMINATE;
                        }

                        if (filteredFiles.contains(file)) {

                            Files.copy(file,
                                    targetDir.resolve(sourceDir.relativize(file)),
                                    StandardCopyOption.REPLACE_EXISTING);
                            copiedFilesCount++;
                            // Updates the Progress bar using the Task's
                            // updateProgress(workDone, max) method.
                            updateProgress(++currentCounter, dirsCount+filesCount);
                        }

                        return FileVisitResult.CONTINUE;
                    }
                });

                if (zipCheckBox.isSelected()) {

                    if (copiedFilesCount > 0) {

                        logger.info("Creating ZIP file, wait... ");
                        Thread.sleep(100);
                        String zipFile = ZipFileCreator.zip(targetDir);
                        logger.info("ZIP file created: " + zipFile);
                    }
                    else {
                        logger.info("Cannot create ZIP file with files count = 0");
                    }
                }

                return null;
            }
        };
        // end copyTask class

        progressBar.progressProperty().bind(copyTask.progressProperty());

        new Thread(copyTask).start();    // Run the copy task

        // Calling event handlers as task's state is transitioned to
        // SUCCEEDED, FAILED or CANCELLED.

        copyTask.setOnFailed(e -> {
            Throwable t = copyTask.getException();
            String message = (t != null) ? t.toString() : "Unknown Exception!";
            logger.info("There was an error during the copy process:");
            logger.info(message);
            doTaskEventCloseRoutine(copyTask);
            //t.printStackTrace();
        });

        copyTask.setOnCancelled(e -> {
            logger.info("Copy is cancelled by user.");
            doTaskEventCloseRoutine(copyTask);
        });

        copyTask.setOnSucceeded(e -> {
            logger.info("Copy completed. " +
                    "Directories copied [" +
                    ((copiedDirsCount < 1) ? 0 : copiedDirsCount) + "], " +
                    "Files copied [" + copiedFilesCount + "]");
            doTaskEventCloseRoutine(copyTask);
        });
    }

    private void doTaskEventCloseRoutine(Task<Void> copyTask) {

        logger.info("Status: " + copyTask.getState() + "\n");
        logger.info("Select a target directory, apply file filters and copy.");
        Platform.runLater(() -> {
            selectTargetBtn.setDisable(false);
            closeBtn.setDisable(false);
            cancelBtn.setDisable(true);
        });
    }

    /*
     * Sets the file filters to its default value in case the filter's dialog
     * is not opened at all, otherwise the already set value is used. Apply
     * the file filters; the filtered files are returned as a Set collection.
     */
    private Set<Path> applyFileFilters(Set<Path> selectedFiles)
            throws IOException {

        if (fileFilters == null) {

            fileFilters = FileFilters.getDefault();
            logger.info("File filters: " + fileFilters);
        }

        return new FileFilterApplication().apply(sourceDir,
                selectedFiles,
                fileFilters);
    }
}
