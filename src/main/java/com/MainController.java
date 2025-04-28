package com;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import javafx.scene.control.Button;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MainController {
    @FXML
    public Button uploadButton;
    @FXML
    private HBox imageContainer; // Container for images


    @FXML
    private WebView mapView;

    @FXML
    private Button savePlaceButton;

    @FXML
    private Button confirmPlaceButton;

    @FXML
    private TextField placeNameField; // TextField to enter the name of the place

    private WebEngine webEngine;
    private double selectedLat;
    private double selectedLng;

    public void initialize() {
        webEngine = mapView.getEngine();
        webEngine.load(getClass().getResource("/map.html").toExternalForm());

        // Wait until map is loaded
        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                // Inject Java bridge
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", this);

                // Add map click handler
                webEngine.executeScript("""
                    map.on('click', function(e) {
                        const lat = e.latlng.lat.toFixed(5);
                        const lng = e.latlng.lng.toFixed(5);

                        javaConnector.setCoordinates(lat, lng);
                    });
                """);
            }
        });
    }

    // Called from JS when the user clicks on the map
    public void setCoordinates(String lat, String lng) {
        selectedLat = Double.parseDouble(lat);
        selectedLng = Double.parseDouble(lng);
        savePlaceButton.setDisable(false);
        System.out.println("Selected: " + lat + ", " + lng);
    }

    // Called when "Save Place" button is clicked
    @FXML
    private void onSavePlace() {
        // Show the TextField for entering the name and the Confirm button
        placeNameField.setVisible(true);
        confirmPlaceButton.setVisible(true);
    }

    // Called when "Confirm" button is clicked
    @FXML
    private void onConfirmSavePlace() {
        // Get the name of the place from the TextField
        String placeName = placeNameField.getText().trim();

        if (placeName.isEmpty()) {
            // Show alert or error message if the name is empty
            showAlertName("Please enter a name for the place!");
            return;
        }

        // Save the location and name to the database
        saveLocationToDatabase(placeName, selectedLat, selectedLng);

        // Add the marker on the map
        String js = String.format("""
            L.marker([%f, %f]).addTo(map)
                .bindPopup("Saved place: %s, %f, %f").openPopup();
        """, selectedLat, selectedLng, placeName, selectedLat, selectedLng);

        webEngine.executeScript(js);

        // Disable the button and hide input fields after saving
        savePlaceButton.setDisable(true);
        placeNameField.setVisible(false);
        confirmPlaceButton.setVisible(false);
    }

    // Method to save the place and coordinates to the database
    private void saveLocationToDatabase(String name, double lat, double lng) {
        String insertQuery = "INSERT INTO places (name, latitude, longitude) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            stmt.setString(1, name);
            stmt.setDouble(2, lat);
            stmt.setDouble(3, lng);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Place saved to database!");
            }

        } catch (SQLException e) {
            System.out.println("Error saving place to database: " + e.getMessage());
        }
    }

    private void showAlertName(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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

        // Correct way: use the uploadButton to get the Stage
        Stage stage = (Stage) uploadButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            addImageToAlbum(image);
        } else {
            showAlert("Δεν επιλέχθηκε εικόνα.");
        }
    }


    private void addImageToAlbum(Image image) {
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        imageView.setFitWidth(500);   // Set size if you want
        imageView.setFitHeight(500);
        imageView.setPreserveRatio(true);

        imageContainer.getChildren().add(imageView);
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Προειδοποίηση");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void goToRegistration(MouseEvent event) {
        System.out.println("Navigating to registration...");

        try {
            Parent registrationRoot = FXMLLoader.load(getClass().getResource("/Registration.fxml"));
            Scene scene = new Scene(registrationRoot);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    }







