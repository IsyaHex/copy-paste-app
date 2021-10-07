package com.app.beta;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;


/*
 * Class has a single method which returns the collection of
 * Path objects after the file filters are applied. Additionally,
 * any empty directories are removed from the filtered collection.
 * The input is the selected files, source directory and the
 * file filters.
 */
public class FileFilterApplication {


    public FileFilterApplication() {
    }

    public Set<Path> apply(Path sourceDir,
                           Set<Path> selectedFiles,
                           FileFilters filters)
            throws IOException {

        Set<Path> filteredFiles = new HashSet<>();

        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                if (! selectedFiles.contains(dir)) {

                    // Not a selected directory, skip it
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs)
                    throws IOException {

                if (selectedFiles.contains(file) &&
                        applyFileTypeFilter(filters, file) &&
                        applyDateOptionFilter(filters, file))  {

                    // Add selected files that match the
                    // file filter criteria
                    filteredFiles.add(file);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                if (dirHasFiles(dir, filteredFiles)) {

                    // Add directories with files in it
                    filteredFiles.add(dir);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return filteredFiles;
    }

    /*
     * Check if the input dir has any files in it that are already
     * filtered. Note this check is not on the file system.
     */
    private boolean dirHasFiles(Path inDir, Set<Path> list) {

        return list
                .stream()
                .anyMatch(p -> ((!p.equals(inDir)) && p.startsWith(inDir)));
    }

    private boolean applyFileTypeFilter(FileFilters filters, Path file) {

        return filters.getFileTypes().contains("All") ||
                filters.getFileTypes().contains(getFileExtension(file));
    }

    private String getFileExtension(Path file) {

        String fileName = file.getFileName().toString();
        int ix = fileName.lastIndexOf(".");
        return (ix == -1) ? "" : fileName.substring(ix + 1);
    }

    private boolean applyDateOptionFilter(FileFilters filters, Path file)
            throws IOException  {

        boolean returnValue;

        switch (filters.getDateOption()) {

            case TODAY:
                returnValue = getFileDate(file).isAfter(LocalDate.now());
                break;
            case LAST_7_DAYS:
                returnValue = getFileDate(file).isAfter(LocalDate.now().minusDays(7));
                break;
            case LAST_30_DAYS:
                returnValue = getFileDate(file).isAfter(LocalDate.now().minusDays(30));
                break;
            default:
                returnValue = true;
        }

        return returnValue;
    }

    /*
     * Returns file's last modified date as a LocalDate.
     */
    private LocalDate getFileDate(Path file)
            throws IOException {

        Instant fileTime = Files.getLastModifiedTime(file).toInstant();
        return fileTime.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
