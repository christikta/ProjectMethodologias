package com;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

public class MainController {

    @FXML
    private WebView mapWebView; // Inject the WebView from FXML

    @FXML
    public void initialize() {
        // Load a web page (e.g., OpenStreetMap or Google Maps)
        String mapUrl = "https://www.openstreetmap.org/";
        mapWebView.getEngine().load(mapUrl);
    }
}