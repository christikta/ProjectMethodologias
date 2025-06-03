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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;



public class MainController {



    @FXML Button uploadButton1;
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

            allowUserToPickLocation = false;


        }
    }


    @FXML
    private boolean allowUserToPickLocation = false;




    @FXML
    private void onSavePlace() {
        saveImagesToDatabase();
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
    private String detectLandmark(File file) {
        try {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String jsonRequest = """
        {
          "requests": [
            {
              "image": {
                "content": "%s"
              },
              "features": [
                {
                  "type": "LANDMARK_DETECTION",
                  "maxResults": 1
                }
              ]
            }
          ]
        }
        """.formatted(base64Image);

            URL url = new URL("https://vision.googleapis.com/v1/images:annotate?key=" + "AIzaSyAHYLwd4j98pDHmeZacyKoUDjV5uPKu5A8");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes("UTF-8"));
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            String json = response.toString();

            // Εκτύπωση ολόκληρου του JSON για debug
            System.out.println("Google Vision API response: " + json);

            // Απλή αναζήτηση "description" στο JSON (όνομα landmark)
            int descIndex = json.indexOf("\"description\":");
            if (descIndex != -1) {
                int start = json.indexOf("\"", descIndex + 14) + 1;
                int end = json.indexOf("\"", start);
                if (start != -1 && end != -1) {
                    String landmark = json.substring(start, end);
                    System.out.println("Detected landmark: " + landmark); // debug εκτύπωση
                    return landmark;
                }
            }

        } catch (Exception e) {
            showAlert("Σφάλμα στο Google Vision API: " + e.getMessage());
        }

        return "";  // αν δεν βρει ή error
    }



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
                    allowUserToPickLocation = false;

                    webEngine.executeScript(String.format("""
                    L.marker([%f, %f]).addTo(map)
                        .bindPopup("Image GPS Location").openPopup();
                """, selectedLat, selectedLng));
                } else {
                    allowUserToPickLocation = true;
                    showAlert("Η εικόνα δεν περιέχει GPS. Διάλεξε σημείο στον χάρτη.");
                }

            } catch (Exception e) {
                showAlert("Σφάλμα ανάγνωσης metadata: " + e.getMessage());
            }

            Image img = new Image(file.toURI().toString());

            // Εδώ κάνουμε αναγνώριση landmark
            String landmarkComment = detectLandmark(file);

            VBox container = createImageBox(img, landmarkComment);
            imageContainer.getChildren().add(container);
        }
    }




    private VBox createImageBox(Image image, String initialComment) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        imageView.setOnMouseClicked(e -> showImageInPopOut(imageView.getImage()));

        TextField commentField = new TextField(initialComment);
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
        String uri = file.toURI().toString();
        System.out.println("Video Preview URI: " + uri);

        javafx.scene.media.Media media;
        try {
            media = new javafx.scene.media.Media(uri);
        } catch (Exception e) {
            showAlert("Error loading media: " + e.getMessage());
            return;
        }

        javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(media);
        javafx.scene.media.MediaView mediaView = new javafx.scene.media.MediaView(mediaPlayer);

        mediaView.setFitWidth(600);
        mediaView.setFitHeight(600);
        mediaView.setPreserveRatio(true);

        Slider progressSlider = new Slider();
        progressSlider.setPrefWidth(580);
        progressSlider.setMin(0);

        mediaPlayer.setOnReady(() -> {
            progressSlider.setMax(mediaPlayer.getTotalDuration().toSeconds());

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
            mediaPlayer.play();  // Only start after media is ready
        });

        mediaPlayer.setOnError(() -> {
            showAlert("MediaPlayer error: " + mediaPlayer.getError().getMessage());
        });

        VBox vbox = new VBox(mediaView, progressSlider);
        vbox.setSpacing(10);
        Scene scene = new Scene(vbox, 650, 700);

        Stage previewStage = new Stage();
        previewStage.setTitle("Προεπισκόπηση Βίντεο");
        previewStage.setScene(scene);
        previewStage.show();
    }


    //Save Photo To Database

    private String loggedInUser;
    private int loggedInUserId;

    public void setLoggedInUser(String username, int userId) {
        this.loggedInUser = username;
        this.loggedInUserId = userId;

        if (usernameLabel != null) {
            usernameLabel.setText("Welcome, " + username + "!");
        }
    }


    private void insertPhoto(int userId, byte[] imageData, String comment, double lat, double lng) {
        String sql = "INSERT INTO user_photos (user_id, image_data, comment, latitude, longitude, created_at) VALUES (?, ?, ?, ?, ?, now())";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setBytes(2, imageData);
            stmt.setString(3, comment);
            stmt.setDouble(4, lat);
            stmt.setDouble(5, lng);

            stmt.executeUpdate();
        } catch (SQLException e) {
            showAlert("DB Insert Error: " + e.getMessage());
        }
    }


    @FXML
    private void saveImagesToDatabase() {

        for (Node node : imageContainer.getChildren()) {
            if (node instanceof VBox imageBox) {
                ImageView imageView = (ImageView) imageBox.getChildren().get(0);
                TextField commentField = (TextField) imageBox.getChildren().get(1);
                String comment = commentField.getText();

                try {
                    // Μετατροπή Image σε byte[]
                    Image image = imageView.getImage();
                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "png", baos);
                    byte[] imageBytes = baos.toByteArray();
                    System.out.println("Original image bytes length: " + imageBytes.length);
                    System.out.println("First 10 bytes original: " + Arrays.toString(Arrays.copyOf(imageBytes, 10)));
                    // Κρυπτογράφηση των bytes της εικόνας
                    byte[] encryptedImageBytes = CryptoUtils.encrypt(imageBytes);
                    System.out.println("Encrypted image bytes length: " + encryptedImageBytes.length);
                    System.out.println("First 10 bytes encrypted: " + Arrays.toString(Arrays.copyOf(encryptedImageBytes, 10)));


                    insertPhoto(loggedInUserId, encryptedImageBytes, comment, selectedLat, selectedLng);

                    insertPhoto(loggedInUserId, imageBytes, comment, selectedLat, selectedLng);

                } catch (IOException e) {
                    showAlert("Image conversion error: " + e.getMessage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void loadPhotosForUser(int userId) {
        String sql = "SELECT image_data, comment, latitude, longitude FROM user_photos WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            imageContainer.getChildren().clear();  // Καθαρίζουμε προηγούμενες εικόνες

            while (rs.next()) {
                byte[] encryptedData = rs.getBytes("image_data");

                // Αποκρυπτογράφηση
                byte[] decryptedData = CryptoUtils.decrypt(encryptedData);

                // Μετατροπή σε Image
                InputStream is = new ByteArrayInputStream(decryptedData);
                BufferedImage bufferedImage = ImageIO.read(is);
                Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);

                String comment = rs.getString("comment");
                double lat = rs.getDouble("latitude");
                double lng = rs.getDouble("longitude");

                // Δημιουργούμε VBox με εικόνα και σχόλιο (παραλλαγή της δικής σου createImageBox)
                VBox imageBox = createImageBox(fxImage,"1");
                // Βάζουμε το comment μέσα στο TextField (δεδομένου ότι το createImageBox έχει TextField στη θέση 1)
                TextField commentField = (TextField) imageBox.getChildren().get(1);
                commentField.setText(comment);

                imageContainer.getChildren().add(imageBox);

                // Αν θέλεις, μπορείς να κάνεις κάτι με τις συντεταγμένες lat, lng (π.χ. να προσθέσεις marker στον χάρτη)
            }

        } catch (Exception e) {
            showAlert("Error loading photos: " + e.getMessage());
        }
    }


}
