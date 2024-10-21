package app.program.Controller;
import app.program.View;

import java.io.IOException;
import javafx.fxml.FXML;

public class PrimaryController {

    @FXML
    private void switchToSecondary() throws IOException {
        View.setRoot("secondary");
    }
}
