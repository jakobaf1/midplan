module app.program {
    requires javafx.controls;
    requires javafx.fxml;

    opens app.program to javafx.fxml;
    exports app.program;
}
