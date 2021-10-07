package com.app.beta;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.CheckBoxTreeItem.TreeModificationEvent;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;


/*
 * Main GUI class for this app.
 * Builds a tree view of the files for the selected file system root
 * directory. Has functions to expand, collapse the file tree and check
 * the files and directories for copy and open the copy files dialog.
 */
public class FileTreeView {


    private TreeView<Path> tree;
    private final Button copyBtn;

    private final CopyDialog copyDialog;
    private Path rootDir; // The chosen root or source directory

    // All file and directories that are checked are stored here
    private final Set<Path> checkedItems;

    // Indexes of selected directory nodes, used with expand or collapse
    // of specific directory nodes.
    private List<Integer> selectedIndexes;

    // Flag to indicate if the root directory node is in expanded or
    // collapsed state. This is used with the tree expand/collapse toggle
    // button. In case the node is collapsed it is expanded and vice-versa.
    private boolean isExpanded;

    private static final String DEFAULT_DIRECTORY =
            System.getProperty("user.dir"); //  or "user.home"
    private static Logger logger;


    /*
     * Constructor builds and displays the app's main view.
     */
    public FileTreeView(Stage primaryStage) {

        logger = Logger.getLogger("copy_app_logger");
        copyDialog = new CopyDialog();
        checkedItems = new HashSet<>();

        Button expandBtn = new Button('\u2039' + " " + '\u203A');
        expandBtn.setTooltip(new Tooltip("Expand or collapse selected tree items"));
        expandBtn.setOnAction(e -> expandOrCollapseSelectedItemsRoutine());
        Button expandAllBtn = new Button('\u00AB' + " " + '\u00BB');
        expandAllBtn.setTooltip(new Tooltip("Expand or collapse entire tree"));
        expandAllBtn.setOnAction(e -> expandOrCollapseTreeRoutine());

        HBox hb1 = new HBox(15);
        hb1.setAlignment(Pos.CENTER);
        hb1.getChildren().addAll(expandAllBtn, expandBtn);

        Button sourceDirBtn = new Button("Source directory...");
        sourceDirBtn.setTooltip(new Tooltip("Set a new root directory"));
        sourceDirBtn.setOnAction(e -> {
            chooseSourceDirectory(primaryStage);
            tree.setRoot(getRootItem());
        });
        copyBtn = new Button("Copy dialog...");
        copyBtn.setTooltip(new Tooltip("Initiate the copy action: opens copy dialog"));
        copyBtn.setDisable(true);
        copyBtn.setOnAction(e -> copyDialogRoutine());

        HBox hb2 = new HBox(15);
        hb2.setAlignment(Pos.CENTER);
        hb2.getChildren().addAll(sourceDirBtn, copyBtn);

        VBox vb = new VBox(20);
        vb.setPadding(new Insets(20));

        primaryStage.setScene(new Scene(vb, 800, 600)); // w, h
        primaryStage.setTitle("Copy Files App");
        primaryStage.show();

        chooseSourceDirectory(primaryStage);
        vb.getChildren().addAll(buildFileTreeView(), hb1, hb2);
    }

    /*
     * Opens the directory chooser with an initial root directory.
     * Allows the user to use the same, or select another one.
     * In case the chooser is cancelled a root directory is derived.
     */
    private void chooseSourceDirectory(Stage primaryStage) {

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a source directory");
        chooser.setInitialDirectory(getInitialDirectory().toFile());
        File chosenDir = chooser.showDialog(primaryStage);
        determineRootDirectory(chosenDir);
        copyBtn.setDisable(true);
        checkedItems.clear();
        isExpanded = false;
        logger.info("Root dir chosen: " + rootDir);
    }

    /*
     * Initial root directory set for the directory chooser.
     */
    private Path getInitialDirectory() {

        return (rootDir == null) ? Paths.get(DEFAULT_DIRECTORY) : rootDir;
    }

    /*
     * The root directory for the tree is derived here.
     * If a directory is chosen it is the root directory.
     * If not, the previous root directory is used. In case of no
     * previous directory the DEFAULT_DIRECTORY is the root.
     */
    private void determineRootDirectory(File chosenDir) {

        rootDir = (chosenDir != null) ?
                chosenDir.toPath() : getInitialDirectory();
    }

    /*
     * Creates and returns the root item for the root directory.
     */
    private FileTreeItem getRootItem() {

        FileTreeItem rootItem = new FileTreeItem(rootDir);
        rootItem.setIndependent(false);
        rootItem.addEventHandler(
                CheckBoxTreeItem.checkBoxSelectionChangedEvent(),
                this::handleItemCheckedEvent);
        return rootItem;
    }

    /*
     * Builds and returns the tree view for the given root item.
     */
    private TreeView<Path> buildFileTreeView() {

        tree = new TreeView<>(getRootItem());
        tree.setPrefHeight(600.0d);
        tree.setTooltip(new Tooltip("Expand (all) or collapse the tree, select items and copy..."));
        tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tree.setCellFactory((TreeView<Path> t) -> new TreeCellImpl());
        return tree;
    }

    /*
     * Event handler for the tree item checked or unchecked event.
     * On checking or unchecking an item the related items are stored
     * or removed from a Set collection respectively. These items in
     * the collection are used for copying.
     */
    private void handleItemCheckedEvent(TreeModificationEvent<Path> e) {

        FileTreeItem item = (FileTreeItem) e.getTreeItem();

        if (item.isSelected() || item.isIndeterminate()) {

            collectCheckedItems(item);
        }
        else {
            removeCollectedCheckedItems(item);
        }

        copyBtn.setDisable(checkedItems.isEmpty());
    }

    private void collectCheckedItems(FileTreeItem item) {

        if (item.isSelected() || item.isIndeterminate()) {

            // A file or directory is checked, or if a directory
            // with files has some items checked: collect these items
            checkedItems.add(item.getValue());
        }

        item.getChildren().forEach(t -> collectCheckedItems((FileTreeItem) t));
    }

    private void removeCollectedCheckedItems(FileTreeItem item) {

        if (! item.isSelected()) {

            // A file or directory is unchecked: remove these items
            checkedItems.remove(item.getValue());
        }

        item.getChildren()
                .forEach(t -> removeCollectedCheckedItems((FileTreeItem) t));
    }

    /*
     * Expand or collapse the entire file tree.
     */
    private void expandOrCollapseTreeRoutine() {

        FileTreeItem rootItem = (FileTreeItem) tree.getRoot();
        expandOrCollapseTree(rootItem);
        isExpanded = !isExpanded;
    }

    private void expandOrCollapseTree(FileTreeItem item) {

        if (! item.isLeaf()) {

            item.setExpanded(!isExpanded);
            item.getChildren().forEach(t -> expandOrCollapseTree((FileTreeItem) t));
        }
    }

    /*
     * Expand or collapse the selected items (directories) in the file tree.
     * NOTE: this is not the checkbox select, it is the item selection.
     * An item is highlighted when it is selected. Multiple items can
     * be selected at a time.
     */
    private void expandOrCollapseSelectedItemsRoutine() {

        selectedIndexes = new ArrayList<>();
        ObservableList<TreeItem<Path>> items =
                tree.getSelectionModel().getSelectedItems();
        items.forEach(t -> expandOrCollapseSelectedItems((FileTreeItem) t));
        setSelectedItems();
    }

    private void expandOrCollapseSelectedItems(FileTreeItem item) {

        if ((item != null) && (! item.isLeaf())) {

            selectedIndexes.add(tree.getRow(item));
            item.setExpanded(!item.isExpanded());
            item.getChildren()
                    .forEach(t -> expandOrCollapseSelectedItems((FileTreeItem) t));
        }
    }

    /*
     * Select the items (retain the selection) which were in selected
     * state before the items expand or collapse action.
     */
    private void setSelectedItems() {

        if (selectedIndexes.isEmpty()) {

            return; // there is no selection, can't expand or collapse
        }

        // One or more items selected

        int firstIndex = selectedIndexes.get(0);
        int [] remaining = new int [selectedIndexes.size() - 1];

        for (int j = 0, i = 1; j < remaining.length; j++) {

            remaining [j] = selectedIndexes.get(i);
            i++;
        }

        tree.getSelectionModel().selectIndices(firstIndex, remaining);
    }

    /*
     * Copy dialog button action routine.
     * Opens the CopyDialog modal dialog.
     */
    private void copyDialogRoutine() {

        logger.info("Copy dialog...");

        FileTreeItem rootItem = (FileTreeItem) tree.getRoot();
        Path sourceDir = rootItem.getValue();
        copyDialog.create(sourceDir, checkedItems);
    }

    /*
     * Inner class to render check boxes with file names for the tree items.
     */
    private static class TreeCellImpl extends CheckBoxTreeCell<Path> {

        @Override
        public void updateItem(Path path, boolean empty) {

            super.updateItem(path, empty);

            if (empty) {

                setText(null);
            }
            else {
                if (path != null) {

                    String s = path.getFileName().toString();
                    setText(s);
                }
            }
        }
    }
}
