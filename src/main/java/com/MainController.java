package com;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MainController {

    @FXML
    private Pane albumPane; // Matches fx:id in FXML

    @FXML
    private WebView mapWebView;

    @FXML
    public void initialize() {
        // Load Google Maps in WebView
        WebEngine webEngine = mapWebView.getEngine();
        String html = """
            <html>
            <head>
                <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyBGYUKhvtrfYNeI4EtbAqiBqllhF756Eqs"></script>
                <script>
                    function initMap() {
                        var map = new google.maps.Map(document.getElementById('map'), {
                            center: {lat: 37.9838, lng: 23.7275}, // Athens as default
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
        albumPane.setVisible(true);
    }

    @FXML
    private void closeCreateAlbumWindow() {
        albumPane.setVisible(false);
    }
}

