package app.program.controller;

import java.io.IOException;

import app.program.View;
import javafx.fxml.FXML;

public class SecondaryController {

    @FXML
    private void switchToPrimary() throws IOException {
        View.setRoot("primary");
    }
}