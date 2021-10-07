package com.app.check;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/*
 * Class has one static method.
 * Creates a ZIP (compressed archive) file for the supplied directory with
 * files. This directory is the same as the target directory as a result of
 * copy action in Copy Dialog.
 * The ZIP file created is in the same directory in which the supplied directory
 * is present.
 */
public class ZipFileCreator {


    public static String zip(Path input)
            throws IOException {

        String targetFileNameStr = input.getFileName().toString() + ".zip";
        Path targetPath =
                Paths.get(input.getParent().toString(), targetFileNameStr);
        ZipOutputStream zipOutputStream =
                new ZipOutputStream(new FileOutputStream(targetPath.toString()));

        Files.walkFileTree(input, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                Path targetFile = input.relativize(file);
                ZipEntry zipEntry = new ZipEntry(targetFile.toString());
                zipOutputStream.putNextEntry(zipEntry);

                try(FileInputStream fileInputStream =
                            new FileInputStream(file.toString())) {

                    byte [] buf = new byte [512];
                    int bytesRead;

                    while ((bytesRead = fileInputStream.read(buf)) > 0) {

                        zipOutputStream.write(buf, 0, bytesRead);
                    }
                }

                zipOutputStream.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        });

        zipOutputStream.close();
        return targetPath.toString();
    }
}
