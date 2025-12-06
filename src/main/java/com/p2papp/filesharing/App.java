package com.p2papp.filesharing;

import com.p2papp.filesharing.controller.DashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.p2papp.filesharing.database.DatabaseConnection;
import com.p2papp.filesharing.model.User;

import java.io.IOException;

/**
 * App.java - JavaFX Main Application
 * 
 * Entry point của ứng dụng JavaFX
 * 
 * Cách chạy:
 * mvn clean javafx:run
 */
public class App extends Application {
    
    /**
     * Stage chính của app
     */
    private static Stage primaryStage;
    
    /**
     * Scene hiện tại
     */
    private static Scene currentScene;
    
    // ============================================
    // JavaFX LIFECYCLE
    // ============================================
    
    /**
     * Start method - JavaFX entry point
     */
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        
        // Set title
        primaryStage.setTitle("P2P File Sharing Application");
        
        // Load login screen
        showLoginScreen();
        
        // Set window size
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
      //  primaryStage.setResizable(false);
      primaryStage.setResizable(true); // Cho phép thay đổi kích thước
       // stage.setMaximized(true); // Mở cửa sổ tối đa

        // Show window
        primaryStage.show();
        
        // Test database connection
        testDatabaseConnection();
    }
    
    /**
     * Stop method - cleanup khi app đóng
     */
    @Override
    public void stop() {
        System.out.println("🔴 Application closing...");
        
        // Close database connection
        DatabaseConnection.closeConnection();
        
        // TODO: Stop PeerServer nếu đang chạy
        
        System.out.println("✅ Application closed");
    }
    
    // ============================================
    // SCREEN NAVIGATION
    // ============================================
    
    /**
     * Hiển thị màn hình Login
     */
    public static void showLoginScreen() {
        try {
            Parent root = loadFXML("view/login");
            Scene scene = new Scene(root);
            
            // Load CSS nếu có
            // scene.getStylesheets().add(App.class.getResource("css/style.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.setTitle("P2P File Sharing - Login");
            
            currentScene = scene;
            
        } catch (IOException e) {
            System.err.println("❌ Cannot load login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
      public static void showRegisterScreen() {
        try {
            Parent root = loadFXML("view/register");
            Scene scene = new Scene(root);
            
            // Load CSS nếu có
            // scene.getStylesheets().add(App.class.getResource("css/style.css").toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.setTitle("P2P File Sharing - Register");
            
            currentScene = scene;
            
        } catch (IOException e) {
            System.err.println("❌ Cannot load register screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Hiển thị màn hình Dashboard
     */
   public static void showDashboardScreen(User currentUser) {
    try {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("view/dashboard.fxml"));
        Parent root = loader.load();

        // Lấy controller và set currentUser
        DashboardController controller = loader.getController();
        controller.setCurrentUser(currentUser); // cần tạo setter trong DashboardController

        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.setTitle("P2P File Sharing - Dashboard");

    } catch (IOException e) {
        e.printStackTrace();
    }
}

    
    /**
     * Load FXML file
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Test database connection khi app khởi động
     */
    private void testDatabaseConnection() {
        new Thread(() -> {
            System.out.println("🔄 Testing database connection...");
            
            if (DatabaseConnection.getConnection() != null) {
                System.out.println("✅ Database connected successfully!");
            } else {
                System.err.println("❌ Database connection failed!");
                System.err.println("   App will continue but database features will not work.");
            }
        }).start();
    }
    
    /**
     * Get primary stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    /**
     * Get current scene
     */
    public static Scene getCurrentScene() {
        return currentScene;
    }
    
    // ============================================
    // MAIN METHOD
    // ============================================
    
    /**
     * Main method
     * 
     * Cách chạy:
     * 1. mvn clean javafx:run
     * 2. Hoặc: mvn clean package, sau đó java -jar target/P2PFileSharing.jar
     */
    public static void main(String[] args) {
        // Launch JavaFX application
        launch(args);
    }
}