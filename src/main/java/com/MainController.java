package com;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.security.cert.PolicyNode;

public class MainController {

    @FXML
    private HBox imageContainer; // Container for images

    @FXML
    private WebView mapWebView;

    @FXML
    private void initialize() {
        // Load Google Maps
        WebEngine webEngine = mapWebView.getEngine();
        String html = """
            <html>
            <head>
                <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyDc339GqJiO8n4zDGxreRW8ibnRmu7TZfQ"></script>
                <script>
                    function initMap() {
                        var map = new google.maps.Map(document.getElementById('map'), {
                            center: {lat: 37.9838, lng: 23.7275},
                            zoom: 12
                        });
                    }
                </script>
            </head>
            <body onload='initMap()'>
                <div id='map' style='width:100%; height:100%;'></div>
            </body>
            </html>
        """;
        webEngine.loadContent(html);
    }

    @FXML
    private void openCreateAlbumWindow() {
        imageContainer.getParent().setVisible(true);
    }

    @FXML
    private void closeAlbumWindow() {
        imageContainer.getParent().setVisible(false);
    }

    @FXML
    private void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Επιλογή εικόνας για το άλμπουμ");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            addImageToAlbum(image);
        } else {
            showAlert("Δεν επιλέχθηκε εικόνα.");
        }
    }

    private void addImageToAlbum(Image image) {
        // Create image container
        StackPane imagePane = new StackPane();
        imagePane.setStyle("-fx-border-color: black; -fx-padding: 5px;");

        // ImageView
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);

        // Delete button (X)
        Button deleteButton = new Button("X");
        deleteButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
        deleteButton.setOnAction(e -> imageContainer.getChildren().remove(imagePane));

        // Comment field
        TextField commentField = new TextField();
        commentField.setPromptText("Γράψε ένα σχόλιο...");

        // Layout
        VBox imageBox = new VBox(imageView, commentField);
        imageBox.setSpacing(5);
        StackPane.setAlignment(deleteButton, javafx.geometry.Pos.TOP_RIGHT);

        imagePane.getChildren().addAll(imageBox, deleteButton);
        imageContainer.getChildren().add(imagePane);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Προειδοποίηση");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }




    public void close_app(javafx.scene.input.MouseEvent mouseEvent) {
        System.exit(0);
    }

    public void open_registration (MouseEvent mouseEvent, PolicyNode content_area) throws IOException {
        Parent fxml = FXMLLoader.load(getClass().getResource("RegistrationUi.fxml"));
        content_area.getChildren().remove();
        content_area.getChildren().equals(fxml);
    }

    public void open_registration(MouseEvent mouseEvent) {
    }
}





