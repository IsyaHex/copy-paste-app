package com.app.beta;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


/*
 * This class has a single method which builds a dialog to capture file
 * filter options. The method returns a dialog with an instance of
 * FileFilters with the selected input or a default instance in case the
 * dialog is cancelled.
 */
public class FileFilterDialog {


    public Dialog<FileFilters> create() {

        Dialog<FileFilters> dialog = new Dialog<>();
        dialog.setTitle("File Filters");
        dialog.setResizable(false);

        CheckBox allCheckBox = new CheckBox("Select all files");

        ToggleGroup radioGroup = new ToggleGroup();
        RadioButton radio1 = new RadioButton(DateOption.ALL_DAYS.toString());
        radio1.setSelected(true);
        RadioButton radio2 = new RadioButton(DateOption.TODAY.toString());
        RadioButton radio3 = new RadioButton(DateOption.LAST_7_DAYS.toString());
        RadioButton radio4 = new RadioButton(DateOption.LAST_30_DAYS.toString());
        radio1.setToggleGroup(radioGroup);
        radio2.setToggleGroup(radioGroup);
        radio3.setToggleGroup(radioGroup);
        radio4.setToggleGroup(radioGroup);

        HBox radioHb = new HBox(15);
        radioHb.getChildren().addAll(radio1, radio2, radio3, radio4);

        ListView<String> fileTypesList = new ListView<>();
        fileTypesList.setPrefHeight(50.0d);
        fileTypesList.setItems(FXCollections.observableArrayList(FileFilters.FILE_EXTENSIONS));
        fileTypesList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fileTypesList.setOrientation(Orientation.HORIZONTAL);

        allCheckBox.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> ov, Boolean oldVal,
                 Boolean newVal) -> {

                    fileTypesList.getSelectionModel().clearSelection();
                    fileTypesList.getSelectionModel().selectFirst();
                    fileTypesList.setDisable(newVal);
                    radio1.setSelected(true);
                    radio1.setDisable(newVal);
                    radio2.setDisable(newVal);
                    radio3.setDisable(newVal);
                    radio4.setDisable(newVal);
                });

        VBox vb = new VBox(20);
        vb.setPadding(new Insets(20));
        vb.getChildren().addAll(allCheckBox, radioHb, fileTypesList);

        ButtonType okButtonType = new ButtonType("Okay", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okButtonType);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(okButtonType);

        dialog.setResultConverter((ButtonType b) -> {

            if (b == okButtonType) {

                FileFilters ff = new FileFilters();
                ff.setAllFiles(allCheckBox.isSelected());
                String s = ((RadioButton) radioGroup.getSelectedToggle()).getText();
                ff.setDateOption(DateOption.lookup(s));
                ff.setFileTypes(fileTypesList.getSelectionModel().getSelectedItems());
                return ff;
            }

            return FileFilters.getDefault();
        });

        dialog.getDialogPane().setContent(vb);
        fileTypesList.getSelectionModel().selectFirst();

        return dialog;
    }
}
