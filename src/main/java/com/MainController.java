package com;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MainController {

    @FXML private WebView mapView;
    @FXML private Button uploadButton;
    @FXML private Button savePlaceButton;
    @FXML private Button confirmPlaceButton;
    @FXML private TextField placeNameField;
    @FXML private HBox imageContainer;
    @FXML private Pane albumPane;

    private WebEngine webEngine;
    private double selectedLat;
    private double selectedLng;

    public void initialize() {
        webEngine = mapView.getEngine();
        webEngine.load(getClass().getResource("/map.html").toExternalForm());

        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", this);

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

    public void setCoordinates(String lat, String lng) {
        selectedLat = Double.parseDouble(lat);
        selectedLng = Double.parseDouble(lng);
        savePlaceButton.setDisable(false);
        System.out.println("Selected coordinates: " + lat + ", " + lng);
    }

    @FXML
    private void onSavePlace() {
        placeNameField.setVisible(true);
        confirmPlaceButton.setVisible(true);
    }

    @FXML
    private void onConfirmSavePlace() {
        String placeName = placeNameField.getText().trim();
        if (placeName.isEmpty()) {
            showAlert("Please enter a name for the place!");
            return;
        }

        saveLocationToDatabase(placeName, selectedLat, selectedLng);

        String js = String.format("""
            L.marker([%f, %f]).addTo(map)
              .bindPopup("Saved: %s, %f, %f").openPopup();
        """, selectedLat, selectedLng, placeName, selectedLat, selectedLng);

        webEngine.executeScript(js);
        savePlaceButton.setDisable(true);
        placeNameField.setVisible(false);
        confirmPlaceButton.setVisible(false);
    }

    private void saveLocationToDatabase(String name, double lat, double lng) {
        String query = "INSERT INTO places (name, latitude, longitude) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            stmt.setDouble(2, lat);
            stmt.setDouble(3, lng);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("DB error: " + e.getMessage());
        }
    }

    @FXML
    private void uploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.jpeg"));
        Stage stage = (Stage) uploadButton.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            Image img = new Image(file.toURI().toString());
            VBox container = createImageBox(img);
            imageContainer.getChildren().add(container);
        }
    }

    private VBox createImageBox(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        imageView.setOnMouseClicked(e -> showImageInPopOut(imageView.getImage()));

        TextField commentField = new TextField();
        commentField.setPromptText("Add comment...");

        Button deleteBtn = new Button("Delete");
        Button editBtn = new Button("⚙");

        VBox imageBox = new VBox(5);
        deleteBtn.setOnAction(e -> imageContainer.getChildren().remove(imageBox));

        final Image[] originalImage = {image};

        ContextMenu editMenu = new ContextMenu();

        MenuItem grayscaleItem = new MenuItem("Grayscale");
        grayscaleItem.setOnAction(e -> imageView.setImage(convertToGrayscale(imageView.getImage())));

        MenuItem invertItem = new MenuItem("Invert Colors");
        invertItem.setOnAction(e -> imageView.setImage(invertImage(imageView.getImage())));

        MenuItem sepiaItem = new MenuItem("Sepia Tone");
        sepiaItem.setOnAction(e -> imageView.setImage(sepiaTone(imageView.getImage())));

        MenuItem blurItem = new MenuItem("Blur");
        blurItem.setOnAction(e -> imageView.setEffect(new javafx.scene.effect.GaussianBlur(5)));

        MenuItem resetItem = new MenuItem("Remove Filter");
        resetItem.setOnAction(e -> {
            imageView.setImage(originalImage[0]);
            imageView.setEffect(null);
        });

        editMenu.getItems().addAll(grayscaleItem, invertItem, sepiaItem, blurItem, resetItem);

        editBtn.setOnMouseClicked(e -> editMenu.show(editBtn, e.getScreenX(), e.getScreenY()));

        HBox buttons = new HBox(5, deleteBtn, editBtn);
        imageBox.getChildren().addAll(imageView, commentField, buttons);
        imageBox.setStyle("-fx-border-color: black; -fx-padding: 5; -fx-background-color: #f4f4f4;");
        imageBox.setPrefWidth(120);

        return imageBox;
    }

    private void showImageInPopOut(Image image) {
        Stage stage = new Stage();
        ImageView view = new ImageView(image);
        view.setPreserveRatio(true);
        view.setFitWidth(600);
        view.setFitHeight(600);

        Scene scene = new Scene(new Pane(view), 600, 600);
        stage.setTitle("Image Preview");
        stage.setScene(scene);
        stage.show();
    }

    private Image convertToGrayscale(Image input) {
        int width = (int) input.getWidth();
        int height = (int) input.getHeight();
        WritableImage gray = new WritableImage(width, height);
        PixelReader reader = input.getPixelReader();
        PixelWriter writer = gray.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                double g = (color.getRed() + color.getGreen() + color.getBlue()) / 3.0;
                writer.setColor(x, y, new Color(g, g, g, color.getOpacity()));
            }
        }
        return gray;
    }

    private Image invertImage(Image input) {
        int width = (int) input.getWidth();
        int height = (int) input.getHeight();
        WritableImage inverted = new WritableImage(width, height);
        PixelReader reader = input.getPixelReader();
        PixelWriter writer = inverted.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                writer.setColor(x, y, new Color(1 - color.getRed(), 1 - color.getGreen(), 1 - color.getBlue(), color.getOpacity()));
            }
        }
        return inverted;
    }

    private Image sepiaTone(Image input) {
        int width = (int) input.getWidth();
        int height = (int) input.getHeight();
        WritableImage sepia = new WritableImage(width, height);
        PixelReader reader = input.getPixelReader();
        PixelWriter writer = sepia.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                double tr = 0.393 * color.getRed() + 0.769 * color.getGreen() + 0.189 * color.getBlue();
                double tg = 0.349 * color.getRed() + 0.686 * color.getGreen() + 0.168 * color.getBlue();
                double tb = 0.272 * color.getRed() + 0.534 * color.getGreen() + 0.131 * color.getBlue();

                writer.setColor(x, y, new Color(
                        Math.min(1.0, tr),
                        Math.min(1.0, tg),
                        Math.min(1.0, tb),
                        color.getOpacity()
                ));
            }
        }
        return sepia;
    }

    @FXML
    private void openCreateAlbumWindow() {
        albumPane.setVisible(true);
    }

    @FXML
    private void closeAlbumWindow() {
        albumPane.setVisible(false);
    }

    @FXML
    private void goToRegistration(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Registration.fxml"));
            Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Register");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
