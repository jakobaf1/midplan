module app.program {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires org.apache.pdfbox;

    opens app.program.controller to javafx.fxml;
    exports app.program;
}
