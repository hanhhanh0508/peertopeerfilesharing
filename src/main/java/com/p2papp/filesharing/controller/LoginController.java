package com.p2papp.filesharing.controller;

import com.p2papp.filesharing.App;
import com.p2papp.filesharing.database.dao.FileDAO;
import com.p2papp.filesharing.database.dao.PeerDAO;
import com.p2papp.filesharing.database.dao.UserDAO;
import com.p2papp.filesharing.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.net.*;


/**
 * LoginController.java - Controller cho màn hình Login
 */
public class LoginController {
    
    // ============================================
    // FXML COMPONENTS
    // ============================================
    
    @FXML
    private TextField txtUsername;
    
    @FXML
    private PasswordField txtPassword;
    
    @FXML
    private Label lblError;
    
    @FXML
    private Button btnLogin;
    
    @FXML
    private Button btnRegister;
    
    // ============================================
    // DAO
    // ============================================
      private User currentUser;
    
    private UserDAO userDAO = new UserDAO();
    private PeerDAO peerDAO ;
     public LoginController() {
        peerDAO = new PeerDAO(); // khởi tạo DAO 1 lần
    }
    // ============================================
    // INITIALIZE
    // ============================================
    
    /**
     * Initialize - tự động gọi khi FXML load
     */
    @FXML
    public void initialize() {
        System.out.println("✅ LoginController initialized");
        
        // Enter key → login
        txtPassword.setOnAction(event -> handleLogin());
    }
    
    // ============================================
    // EVENT HANDLERS
    // ============================================
    
    /**
     * Xử lý nút Login
     */
   @FXML
private void handleLogin() {
    String username = txtUsername.getText().trim();
    String password = txtPassword.getText().trim(); // password thô

    if (username.isEmpty() || password.isEmpty()) {
        showError("Please enter username and password!");
        return;
    }

    btnLogin.setDisable(true);
    btnLogin.setText("Logging in...");

    new Thread(() -> {
        // DAO sẽ hash password trước khi so sánh
        User user = userDAO.login(username, password);

        javafx.application.Platform.runLater(() -> {
            btnLogin.setDisable(false);
            btnLogin.setText("Login");

           if (user != null) {
    currentUser = user;
    String ipAddress = getLocalIpAddress();
    int port = getAvailablePort();

    boolean registered = peerDAO.registerPeer(currentUser.getUserId(), ipAddress, port);
    if (registered) {
        System.out.println("✅ Peer registered at " + ipAddress + ":" + port);
        App.showDashboardScreen(currentUser); // truyền user vào dashboard
    } else {
        showError("Failed to register peer.");
    }
} else {
    showError("Invalid username or password!");
}

        });
    }).start();
}

    
    /**
     * Lấy IP hiện tại
     */
   @FXML
 private String getLocalIpAddress() {
    try {
        InetAddress localHost = InetAddress.getLocalHost();
        return localHost.getHostAddress();
    } catch (UnknownHostException e) {
        e.printStackTrace();
        return "127.0.0.1"; // fallback
    }
}
 /**
  * Chọn port tự động
  */
 @FXML
 private int getAvailablePort() {
    for (int port = 8000; port <= 9000; port++) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return port; // port chưa dùng → return
        } catch (Exception e) {
            // port bận → thử port tiếp theo
        }
    }
    return 8000; // fallback
}
    /**
     * Xử lý nút Register
     */
    @FXML
    private void handleRegister() {
        // TODO: Mở màn hình Register
        /*
        showError("Register feature coming soon!");
        
        // Tạm thời: register với username/password hiện tại
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        
       
        // Default email
        String email = username + "@p2p.local";
        
        btnRegister.setDisable(true);
        btnRegister.setText("Registering...");
        
        new Thread(() -> {
            boolean success = userDAO.registerUser(username, password, email);
            
            javafx.application.Platform.runLater(() -> {
                btnRegister.setDisable(false);
                btnRegister.setText("Register");
                
                if (success) {
                    showInfo("Registration successful! You can now login.");
                    txtPassword.clear();
                } else {
    App.showRegisterScreen();
                }
            });
        }).start();
    }
    */
        App.showRegisterScreen();  
    }
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Hiển thị error message
     */
    private void showError(String message) {
        lblError.setText("❌ " + message);
        lblError.setStyle("-fx-text-fill: red;");
        lblError.setVisible(true);
        
        // Ẩn sau 3 giây
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    lblError.setVisible(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    
    /**
     * Hiển thị info message
     */
    private void showInfo(String message) {
        lblError.setText("✅ " + message);
        lblError.setStyle("-fx-text-fill: green;");
        lblError.setVisible(true);
        
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    lblError.setVisible(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}