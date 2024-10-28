package app.program.controller;

import java.io.IOException;

import app.program.App;
import javafx.fxml.FXML;

public class EmployeeController {

    @FXML
    private void switchToEmployees() throws IOException {
        App.setRoot("Algorithm");
        //  select pane "employees"
    }
}