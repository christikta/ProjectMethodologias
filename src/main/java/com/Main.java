package com;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
        Parent root = loader.load();

        // Get references to the button and image view
        Button uploadButton = (Button) loader.getNamespace().get("uploadButton");
        ImageView imageView = (ImageView) loader.getNamespace().get("imageView");

        // Set the scene
        primaryStage.setTitle("Smart Travel Journal");
        primaryStage.setScene(new Scene(root, 600, 400)); // Set size to match your FXML
        primaryStage.show();

        //otkkkkkkkkk

        // Add action to the upload button
        uploadButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

            // Show open file dialog
            File file = fileChooser.showOpenDialog(primaryStage);

            if (file != null) {
                // Load the image and display it in the ImageView
                Image image = new Image(file.toURI().toString());
                imageView.setImage(image);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}