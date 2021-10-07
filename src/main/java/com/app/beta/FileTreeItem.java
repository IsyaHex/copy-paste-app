package com.app.beta;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/*
 * This class is a TreeItem for the given file Path with a checkbox. It does
 * this by overriding the TreeItem's getChildren() and the isLeaf() methods.
 * Note that CheckBoxTreeItem extends TreeItem.
 */
public class FileTreeItem extends CheckBoxTreeItem<Path> {


    // Cache whether the file is a leaf or not. A file is a leaf if
    // it is not a directory. The isLeaf() is called often, and doing
    // the actual check on Path is expensive.
    private boolean isLeaf;

    // Do the children and leaf testing only once, and then set these
    // booleans to false so that we do not check again during this run.
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;


    /*
     * Constructor.
     * The parameter is the root or source input directory path used
     * to build the file tree with the TreeView control.
     */
    public FileTreeItem(Path path) {

        super(path);
    }

    @Override
    public boolean isLeaf() {

        if (isFirstTimeLeaf) {

            isFirstTimeLeaf = false;
            Path path = getValue();
            isLeaf = Files.isRegularFile(path);
        }

        return isLeaf;
    }

    /*
     * Returns a list that contains the child TreeItems belonging to the TreeItem.
     */
    @Override
    public ObservableList<TreeItem<Path>> getChildren() {

        if (isFirstTimeChildren) {

            isFirstTimeChildren = false;

            // First getChildren() call, so we actually go off and
            // determine the children of the file contained in this TreeItem.
            super.getChildren().setAll(buildChildren(this));
        }

        return super.getChildren();
    }

    private ObservableList<TreeItem<Path>> buildChildren(
            CheckBoxTreeItem<Path> treeItem) {

        Path path = treeItem.getValue();

        if ((path != null) && (Files.isDirectory(path))) {

            try(Stream<Path> pathStream = Files.list(path)) {

                return pathStream
                        .map(FileTreeItem::new)
                        .collect(Collectors.toCollection(FXCollections::observableArrayList));
            }
            catch(IOException e) {

                throw new UncheckedIOException(e);
            }
        }

        return FXCollections.emptyObservableList();
    }
}
