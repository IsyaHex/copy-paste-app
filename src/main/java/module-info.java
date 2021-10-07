module com.app.copypasteapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;


    opens com.app to javafx.fxml;
    exports com.app;
}