package com;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegistrationController {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    // Handles registration logic
    @FXML
    private void handleRegister(MouseEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Form Error", "Please complete all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Password Error", "Passwords do not match.");
            return;
        }

        String insertQuery = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // For real-world apps, hash the password
            stmt.setString(3, email);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "User registered successfully!");
                clearFields();
            }

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // duplicate email if you add a unique constraint
                showAlert(Alert.AlertType.ERROR, "Error", "Email already exists.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
            }
            e.printStackTrace();
        }
    }
    private void clearFields() {
        // Clear the fields after successful registration
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }



    // Goes back to the previous screen
    @FXML
    private void goBack(MouseEvent event) {
        // You can use this to close current stage or switch to another scene
        System.out.println("Going back...");
        // Example to close window:
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        stage.close();
    }

    // Closes the application
    @FXML
    private void closeApp(MouseEvent event) {
        System.out.println("Application closing...");
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
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
