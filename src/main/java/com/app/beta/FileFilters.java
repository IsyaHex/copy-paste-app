package com.app.beta;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


/*
 * This class represents the file filters data. This data is
 * captured in the FileFiltersDialog and is applied in the files
 * copy routine.
 */
public class FileFilters {


    private boolean allFiles;
    private DateOption dateOpt;
    private ObservableList<String> fileTypes;

    /*
     * List of file extensions for selection. "All" specifies that select
     * all file types. The "" (empty string) specifies that the file has no
     * extension.
     */
    public static final String [] FILE_EXTENSIONS =
            {"All", "java", "class", "txt", "doc", "docx", "xls",
                    "xlsx", "ppt", "png", "jpg", "pdf", "jar", "exe", "html",
                    "xhtml", "htm", "mp3", "wmv", ""};


    /*
     * Constructor with default values.
     */
    public FileFilters() {

        allFiles = false;
        dateOpt = DateOption.ALL_DAYS;
        fileTypes = FXCollections.observableArrayList("All");
    }

    /*
     * Returns an instance of FileFilters with following options:
     * All files for all days.
     */
    public static FileFilters getDefault() {

        return new FileFilters();
    }

    public void setAllFiles(boolean b) {

        allFiles = b;
    }
    public boolean getAllFiles() {

        return allFiles;
    }

    public void setDateOption(DateOption d) {

        dateOpt = d;
    }
    public DateOption getDateOption() {

        return dateOpt;
    }

    public void setFileTypes(ObservableList<String> types) {

        fileTypes = types;
    }
    public ObservableList<String> getFileTypes() {

        return fileTypes;
    }

    @Override
    public String toString() {

        return dateOpt.toString() + ", " + fileTypes.toString();
    }
}
