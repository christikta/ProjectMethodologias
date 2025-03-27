package com;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Φορτώνουμε το FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
        Parent root = loader.load();

        // Ορισμός του παραθύρου
        primaryStage.setTitle("Smart Travel Journal");
        primaryStage.setScene(new Scene(root, 1466, 911)); // Το μέγεθος ταιριάζει με το FXML
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
