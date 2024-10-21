module app.program {
    requires javafx.controls;
    requires javafx.fxml;

    opens app.program.controller to javafx.fxml;
    exports app.program;
}
