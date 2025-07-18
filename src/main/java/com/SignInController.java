package com;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Node;

import javax.swing.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SignInController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private int getUserIdFromDatabase(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // λάθος ή μη υπαρκτός χρήστης
    }



    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (isValidCredentials(email, password)) {
            try {
                String username = getUsernameFromDatabase(email);
                int userId = getUserIdFromDatabase(email);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainView.fxml"));
                Parent root = loader.load();

                MainController controller = loader.getController();
                controller.setLoggedInUser(username, userId);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                // ✨ Ρυθμίζουμε το μέγεθος σε 80% της οθόνης
                Screen screen = Screen.getPrimary();
                Rectangle2D bounds = screen.getVisualBounds();

                stage.setScene(new Scene(root));
                stage.setWidth(bounds.getWidth() * 0.8);
                stage.setHeight(bounds.getHeight() * 0.8);
                stage.setX(bounds.getMinX() + bounds.getWidth() * 0.1);
                stage.setY(bounds.getMinY() + bounds.getHeight() * 0.1);

                stage.show();

            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load main view.");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password.");
        }
    }


    private String getUsernameFromDatabase(String email) {
        String query = "SELECT username FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "User";
    }










    private boolean isValidCredentials(String email, String password) {
        String query = "SELECT password FROM users WHERE email = ?";  // Only select password for validation

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            // If a user is found with that email
            if (rs.next()) {
                String storedPasswordHash = rs.getString("password");

                // Check if entered password matches the hashed password in the database
                if (PasswordUtil.checkPassword(password, storedPasswordHash)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while accessing the database.");
        }
        return false;
    }

    @FXML
    private void open_registration(MouseEvent event) {
        System.out.println("Navigate to registration screen.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Registration.fxml"));
            Parent root = loader.load();
            // Example of opening new stage
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.show();

            // Optionally close current stage
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @FXML
    private void close_app(MouseEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
