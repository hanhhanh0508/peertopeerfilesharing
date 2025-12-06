package com.p2papp.filesharing.controller;

import com.p2papp.filesharing.App;
import com.p2papp.filesharing.database.dao.UserDAO;
import com.p2papp.filesharing.utils.HashUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtEmail;

    @FXML
    private Button btnRegister;

    @FXML
    private Button btnLogin; // Nút "Đăng nhập" trên register.fxml

    @FXML
    private Label lblMessage; // Hiển thị thông báo

    private UserDAO userDAO = new UserDAO();

    // ============================================
    // Handle nút Đăng ký
    // ============================================
    @FXML
    private void handleRegister() {
        String username = txtUsername.getText().trim();
       String password = txtPassword.getText();
  

        String email = txtEmail.getText().trim();

        // Validate
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            showMessage("❌ Vui lòng điền đầy đủ thông tin!", "red");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showMessage("❌ Email không hợp lệ!", "red");
            return;
        }

        // Hash password
     //   String passHash = HashUtil.hashPassword(password).trim();

        // Đăng ký
     
        
     //boolean success = userDAO.registerUser(username, passHash, email);
  boolean success = userDAO.registerUser(username, password, email);
        if (success) {
            showMessage("✅ Đăng ký thành công! Chuyển về login...", "green");

            // Clear input
            txtUsername.clear();
            txtPassword.clear();
            txtEmail.clear();

            // Chuyển về màn hình Login sau 1.5 giây
            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                    javafx.application.Platform.runLater(() -> App.showLoginScreen());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } else {
            showMessage("❌ Đăng ký thất bại! Username hoặc email đã tồn tại.", "red");
        }
    }

    // ============================================
    // Handle nút Đăng nhập (trên giao diện register)
    // ============================================
    @FXML
    private void handleLogin() {
        App.showLoginScreen();
    }

    // ============================================
    // Helper hiển thị message
    // ============================================
    private void showMessage(String message, String color) {
        if (lblMessage != null) {
            lblMessage.setText(message);
            lblMessage.setStyle("-fx-text-fill: " + color + ";");
            lblMessage.setVisible(true);
        }
    }
}
