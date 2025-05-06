package com;
import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
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

    @FXML  private WebEngine webEngine;
    @FXML private double selectedLat;
    @FXML private double selectedLng;



    @FXML private Button logoutButton;




    @FXML
    public void setCoordinates(String lat, String lng) {
        selectedLat = Double.parseDouble(lat);
        selectedLng = Double.parseDouble(lng);
        System.out.println("Coordinates received: " + selectedLat + ", " + selectedLng);
    }

    @FXML
    public void initialize() {
        webEngine = mapView.getEngine();
        webEngine.load(getClass().getResource("/map.html").toExternalForm());

        // Προσθήκη Listener για να βεβαιωθούμε ότι το map έχει φορτωθεί
        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaConnector", this); // Συνδέει την Java μέθοδο με την JS
                webEngine.executeScript("""
                map.on('click', function(e) {
                    const lat = e.latlng.lat.toFixed(5);
                    const lng = e.latlng.lng.toFixed(5);
                    javaConnector.onMapClicked(lat, lng); // Καλεί τη μέθοδο στην Java
                });
            """);
            }
        });
    }

    @FXML
    public void onMapClicked(String lat, String lng) {
        System.out.println("Map clicked at: " + lat + ", " + lng);
        if (allowUserToPickLocation) {
            selectedLat = Double.parseDouble(lat);
            selectedLng = Double.parseDouble(lng);
            System.out.println("User selected location: " + selectedLat + ", " + selectedLng);

            // Χρησιμοποιώντας String.format για να ενσωματώσουμε τις παραμέτρους στην JavaScript
            String script = String.format("""
    if (window.userSelectedMarker) {
        map.removeLayer(window.userSelectedMarker);
    }
    window.userSelectedMarker = L.marker([%f, %f]).addTo(map)
        .bindPopup("Selected Location").openPopup();
""", selectedLat, selectedLng);

// Εκτέλεση του script στον webEngine
            webEngine.executeScript(script);

        }
    }


    @FXML
    private boolean allowUserToPickLocation = false;




    @FXML
    private void onSavePlace() {
        placeNameField.setVisible(true);
        confirmPlaceButton.setVisible(true);
    }

    @FXML
    public void onConfirmSavePlace() {
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
        placeNameField.setVisible(false);
        confirmPlaceButton.setVisible(false);
    }

    @FXML
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
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Do you want to log out?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignIn.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) logoutButton.getScene().getWindow();  // παίρνει το τρέχον παράθυρο
                    stage.setScene(new Scene(root));
                    stage.setTitle("Login");
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Error loading login screen.");
                }
            }
        });
    }



















    @FXML private Label usernameLabel;  // Στο main.fxml θα πρέπει να υπάρχει Label με fx:id="usernameLabel"

    private String loggedInUser;

    public void setLoggedInUser(String username) {
        this.loggedInUser = username;
        if (usernameLabel != null) {
            usernameLabel.setText("Welcome, " + username + "!");
        }
    }












    @FXML
    private void extractGPSFromImage(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);

            if (gpsDirectory != null) {
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null && !geoLocation.isZero()) {
                    selectedLat = geoLocation.getLatitude();
                    selectedLng = geoLocation.getLongitude();
                    allowUserToPickLocation = false;
                    webEngine.executeScript(String.format("""
                    L.marker([%f, %f]).addTo(map)
                        .bindPopup("Image GPS Location").openPopup();
                """, selectedLat, selectedLng));
                } else {
                    allowUserToPickLocation = true;
                    showAlert("Image does not contain GPS data. Select a point on the map.");
                }
            }
        } catch (Exception e) {
            showAlert("Error reading image metadata: " + e.getMessage());
        }
    }

    @FXML private Double gpsLat = null;
    @FXML private Double gpsLng = null;

    @FXML
    private void uploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.jpeg"));
        Stage stage = (Stage) uploadButton.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(file);
                GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);

                if (gpsDir != null && gpsDir.getGeoLocation() != null) {
                    GeoLocation loc = gpsDir.getGeoLocation();
                    selectedLat = loc.getLatitude();
                    selectedLng = loc.getLongitude();
                    allowUserToPickLocation = false;  // Απενεργοποιούμε την επιλογή τοποθεσίας

                    // Προσθήκη marker με βάση τις συντεταγμένες GPS της εικόνας
                    webEngine.executeScript(String.format("""
                    L.marker([%f, %f]).addTo(map)
                        .bindPopup("Image GPS Location").openPopup();
                """, selectedLat, selectedLng));
                } else {
                    allowUserToPickLocation = true;  // Ενεργοποιούμε την επιλογή τοποθεσίας
                    showAlert("Η εικόνα δεν περιέχει GPS. Διάλεξε σημείο στον χάρτη.");
                }

            } catch (Exception e) {
                showAlert("Σφάλμα ανάγνωσης metadata: " + e.getMessage());
            }

            // Εμφάνιση της εικόνας στο UI
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
        deleteBtn.setOnAction(e -> showDeleteConfirmation(imageBox));

        final Image[] originalImage = {image};

        ContextMenu editMenu = new ContextMenu();

        MenuItem grayscaleItem = new MenuItem("Grayscale");
        grayscaleItem.setOnAction(e -> imageView.setImage(convertToGrayscale(imageView.getImage())));

        MenuItem invertItem = new MenuItem("Invert Colors");
        invertItem.setOnAction(e -> imageView.setImage(invertImage(imageView.getImage())));

        MenuItem sepiaItem = new MenuItem("Sepia Tone");
        sepiaItem.setOnAction(e -> imageView.setImage(sepiaTone(imageView.getImage())));

        
        MenuItem resetItem = new MenuItem("Remove Filter");
        resetItem.setOnAction(e -> {
            imageView.setImage(originalImage[0]);
            imageView.setEffect(null);
        });

        editMenu.getItems().addAll(grayscaleItem, invertItem, sepiaItem, resetItem);

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

    private void showDeleteConfirmation(VBox imageBox) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete this image?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                imageContainer.getChildren().remove(imageBox);
            }
        });
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


    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
