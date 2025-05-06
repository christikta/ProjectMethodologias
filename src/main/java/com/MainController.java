package com;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import javafx.scene.control.Button;


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

//-------new
    @FXML
    private TextField commentField; // TextField for entering the comment
    private javafx.scene.image.ImageView selectedImageView; // Store the selected image for commenting


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
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        Button deleteButton = new Button("Delete");
        VBox wrapper = new VBox(imageView, deleteButton);
        wrapper.setSpacing(5);

        deleteButton.setOnAction(e -> imageContainer.getChildren().remove(wrapper));

        // Show preview when clicked
        imageView.setOnMouseClicked(event -> {
            selectedImageView = imageView;
            showImagePreview(image);
        });

        imageContainer.getChildren().add(wrapper);
    }


    private void showImagePreview(Image image) {
        Stage previewStage = new Stage();
        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600); // Adjust size as you like
        imageView.setFitHeight(600);

        Scene scene = new Scene(new javafx.scene.layout.StackPane(imageView), 650, 650);
        previewStage.setTitle("Προεπισκόπηση Εικόνας");
        previewStage.setScene(scene);
        previewStage.show();
    }

   //------------------ UPLOAD VIDEO/ START-----------------

    @FXML
    private void uploadVideo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Επιλογή βίντεο");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mov", "*.mkv"));

        Stage stage = (Stage) uploadButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            addVideoToAlbum(file);
        } else {
            showAlert("Δεν επιλέχθηκε βίντεο.");
        }
    }

    private void addVideoToAlbum(File file) {
        String mediaUrl = file.toURI().toString();
        javafx.scene.media.Media media = new javafx.scene.media.Media(mediaUrl);
        javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);

        javafx.scene.image.ImageView thumbnail = new javafx.scene.image.ImageView();
        thumbnail.setFitWidth(100);
        thumbnail.setFitHeight(100);
        thumbnail.setPreserveRatio(true);

        // The delete button will only be created inside the setOnReady method
        player.setOnReady(() -> {
            javafx.scene.media.MediaView mediaView = new javafx.scene.media.MediaView(player);
            mediaView.setFitWidth(100);
            mediaView.setFitHeight(100);
            mediaView.setPreserveRatio(true);

            // Capture the current video frame using snapshot
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            javafx.scene.image.WritableImage image = mediaView.snapshot(params, null);

            // Set the captured image as the thumbnail
            thumbnail.setImage(image);

            // Create the delete button inside setOnReady
            Button deleteButton = new Button("Delete");

            // Create wrapper VBox for the thumbnail and delete button
            VBox wrapper = new VBox(thumbnail, deleteButton);
            wrapper.setSpacing(5);

            // Set the action for the delete button
            deleteButton.setOnAction(e -> imageContainer.getChildren().remove(wrapper));

            // Add the wrapper (thumbnail and delete button) to the image container
            imageContainer.getChildren().add(wrapper);
        });

        // Set preview on click
        thumbnail.setOnMouseClicked(event -> showVideoPreview(file));

        // Start loading the media player (mute and pause immediately to avoid playback)
        player.setMute(true);
        player.play();
        player.pause(); // pause immediately to avoid full playback
    }




    private void showVideoPreview(File file) {
        javafx.scene.media.Media media = new javafx.scene.media.Media(file.toURI().toString());
        javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(media);
        javafx.scene.media.MediaView mediaView = new javafx.scene.media.MediaView(mediaPlayer);

        mediaView.setFitWidth(600);
        mediaView.setFitHeight(600);
        mediaView.setPreserveRatio(true);

        // Slider for progress
        javafx.scene.control.Slider progressSlider = new javafx.scene.control.Slider();
        progressSlider.setMin(0);
        progressSlider.setMax(100);
        progressSlider.setPrefWidth(580);

        mediaPlayer.setOnReady(() -> {
            progressSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());
        });

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newTime.toSeconds());
            }
        });

        progressSlider.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging) {
                mediaPlayer.seek(javafx.util.Duration.seconds(progressSlider.getValue()));
            }
        });

        progressSlider.setOnMouseReleased(e -> mediaPlayer.seek(javafx.util.Duration.seconds(progressSlider.getValue())));

        VBox vbox = new VBox(mediaView, progressSlider);
        vbox.setSpacing(10);
        Scene scene = new Scene(vbox, 650, 700);

        Stage previewStage = new Stage();
        previewStage.setTitle("Προεπισκόπηση Βίντεο");
        previewStage.setScene(scene);
        previewStage.show();

        mediaPlayer.play();
    }


    //------------------ UPLOAD VIDEO/ END-----------------


    //------------ comment--------------
    @FXML
    private void onAddCommentClick() {
        if (selectedImageView == null) {
            showAlert("Please select an image first!");
            return;
        }

        String comment = commentField.getText().trim();
        if (comment.isEmpty()) {
            showAlert("Please enter a comment!");
            return;
        }

        // For now, we just show the comment in an alert. You could also add it below the image, save it to a database, etc.
        showCommentForImage(comment);
    }

    private void showCommentForImage(String comment) {
        if (selectedImageView != null) {
            // Create a Label to display the comment below the selected image
            javafx.scene.control.Label commentLabel = new javafx.scene.control.Label(comment);
            commentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");

            // Add the comment label below the selected image (or wherever you want in the layout)
            selectedImageView.setOnMouseClicked(event -> {
                // Create a StackPane or similar layout to hold the image and the comment together
                javafx.scene.layout.VBox commentBox = new javafx.scene.layout.VBox();
                commentBox.getChildren().addAll(selectedImageView, commentLabel);

                // Optionally, add this to a different container if you want
                imageContainer.getChildren().add(commentBox);
            });
        }
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







