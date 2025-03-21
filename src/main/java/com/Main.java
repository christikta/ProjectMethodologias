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
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
        Parent root = loader.load();

        // Set the scene
        primaryStage.setTitle("JavaFX App");
        primaryStage.setScene(new Scene(root, 600, 400)); // Set size to match your FXML
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
