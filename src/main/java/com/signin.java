package com;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.cert.PolicyNode;
import java.util.Iterator;
import java.util.ResourceBundle;

public class signin implements Initializable {

    @FXML
    private AnchorPane pane;
    private Parent root;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //to do
       
        MetaData.parent =root;
    }


    public void open_registration(MouseEvent mouseEvent) throws IOException {
        Parent FXML = FXMLLoader.load(getClass().getResource("RegistrationUi.fxml"));
        PolicyNode content_area = null;
        content_area.getChildren().remove();
        content_area.getChildren().equals(FXML);
        
    }

    public void close_app(MouseEvent mouseEvent) {
        System.exit(0);
    }
}