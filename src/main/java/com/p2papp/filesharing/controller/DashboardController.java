package com.p2papp.filesharing.controller;

import com.p2papp.filesharing.App;
import com.p2papp.filesharing.database.dao.*;
import com.p2papp.filesharing.model.*;
import com.p2papp.filesharing.network.*;
import com.p2papp.filesharing.utils.HashUtil;

// ============================================
// IMPORT BỔ SUNG - FIX LỖI
// ============================================
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.stage.FileChooser;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * DashboardController.java - Controller cho màn hình Dashboard
 * 
 * ĐÃ SỬA: Thêm import java.net.InetAddress
 */
public class DashboardController {
    
    // ============================================
    // FXML COMPONENTS
    // ============================================
    
    @FXML private Label lblUsername;
    @FXML private Label lblStatus;
    @FXML private Button btnLogout;
    
    // Tabs
    @FXML private Tab tabMyFiles;
    @FXML private Tab tabAllFiles;
    @FXML private Tab tabPeers;
    
    // My Files
    @FXML private TableView<FileInfo> tblMyFiles;
@FXML private TableColumn<FileInfo, Integer> colMyFileId;
@FXML private TableColumn<FileInfo, String> colMyFileName;
@FXML private TableColumn<FileInfo, String> colMyFileSize;
@FXML private TableColumn<FileInfo, String> colSharedDate;
@FXML private TableColumn<FileInfo, String> colAllSharedDate;

    @FXML private Button btnUpload;
    @FXML private Button btnDeleteFile;
    
    // All Files
    @FXML private TableView<FileInfo> tblAllFiles;
    @FXML private TableColumn<FileInfo, String> colAllFileName;
    @FXML private TableColumn<FileInfo, String> colAllFileSize;
    @FXML private TableColumn<FileInfo, String> colOwner;
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private Button btnDownload;
    
    // Peers
    @FXML private TableView<Peer> tblPeers;
    @FXML private TableColumn<Peer, String> colPeerName;
    @FXML private TableColumn<Peer, String> colPeerAddress;
    @FXML private TableColumn<Peer, String> colPeerStatus;
    @FXML private Button btnRefreshPeers;
    @FXML private Button btnDiscoverPeers;
    
    // ============================================
    // FIELDS
    // ============================================
    
    private User currentUser;
    private FileDAO fileDAO = new FileDAO();
    private PeerDAO peerDAO = new PeerDAO();
    private PeerServer peerServer;
    
    // Observable lists for tables
    private ObservableList<FileInfo> myFilesList = FXCollections.observableArrayList();
    private ObservableList<FileInfo> allFilesList = FXCollections.observableArrayList();
    private ObservableList<Peer> peersList = FXCollections.observableArrayList();
    
    // ============================================
    // INITIALIZE
    // ============================================
    
    @FXML
    public void initialize() {
        System.out.println("✅ DashboardController initialized");
        
        // TODO: Get current user from session
        // currentUser = SessionManager.getCurrentUser();
        
        // Temporary: load user1 for testing
   
        
        if (currentUser != null) {
            lblUsername.setText("Welcome, " + currentUser.getUsername() + "!");
        }
        
        // Setup tables
        setupMyFilesTable();
        setupAllFilesTable();
        
        setupPeersTable();
        
        // Load data
        loadMyFiles();
        loadAllFiles();
        loadPeers();
        
        // Start PeerServer
        startPeerServer();
        
        lblStatus.setText("🟢 Online");
        
        
    // Khi nhấn Enter ở ô tìm kiếm
    txtSearch.setOnAction(e -> handleSearch());

    // Nếu có nút Search, vẫn giữ click button
    btnSearch.setOnAction(e -> handleSearch());
    }
    
    // ============================================
    // SETUP TABLES
    // ============================================
    
    private void setupMyFilesTable() {
            colMyFileId.setCellValueFactory(new PropertyValueFactory<>("fileId"));

        colMyFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colMyFileSize.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getFormattedFileSize()
            )
        );
          colSharedDate.setCellValueFactory(cellData -> {
              Timestamp ts = cellData.getValue().getSharedDate();
        String formatted = ts != null ? 
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts) : "";
        return new javafx.beans.property.SimpleStringProperty(formatted);
    });
        tblMyFiles.setItems(myFilesList);
    }
    
    private void setupAllFilesTable() {
        colAllFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colAllFileSize.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getFormattedFileSize()
            )
        );
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerUsername"));
     //    TableColumn<FileInfo, String> colAllSharedDate = new TableColumn<>("Shared Date");
        colAllSharedDate.setCellValueFactory(c -> {
        Timestamp ts = c.getValue().getSharedDate();
        String formatted = ts != null ?
            new SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts) : "";
        return new SimpleStringProperty(formatted);
    });
        
 //   tblAllFiles.getColumns().add(colAllSharedDate);

    tblAllFiles.setItems(allFilesList);
    
    }
    
    private void setupPeersTable() {
        colPeerName.setCellValueFactory(new PropertyValueFactory<>("username"));
        colPeerAddress.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getAddress()
            )
        );
        colPeerStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tblPeers.setItems(peersList);
    }
    
    // ============================================
    // LOAD DATA
    // ============================================
    public void setCurrentUser(User user) {
    this.currentUser = user;
    lblUsername.setText("Welcome, " + currentUser.getUsername() + "!");
    loadMyFiles();  // gọi lại load dữ liệu theo user hiện tại
    loadAllFiles();
}
    /*private void loadMyFiles() {
        if (currentUser == null) return;
        
        new Thread(() -> {
            var files = fileDAO.getFilesByUser(currentUser.getUserId());
            System.out.println("Loaded my files: " + files.size());

            javafx.application.Platform.runLater(() -> {
                myFilesList.clear();
                myFilesList.addAll(files);
            });
        }).start();
    }
    */
    /*
    private void loadAllFiles() {
        new Thread(() -> {
            var files = fileDAO.getAllSharedFiles();
            javafx.application.Platform.runLater(() -> {
                allFilesList.clear();
                allFilesList.addAll(files);
            });
        }).start();
    }
    */
    /*
    private void loadPeers() {
        new Thread(() -> {
            var peers = peerDAO.getOnlinePeers();
            javafx.application.Platform.runLater(() -> {
                peersList.clear();
                peersList.addAll(peers);
            });
        }).start();
    }
    */
    // Sử dụng ExecutorService cho background tasks
private final ExecutorService executor = Executors.newCachedThreadPool();

private void loadMyFiles() {
    if (currentUser == null) return;

    executor.submit(() -> {
        try {
            var files = fileDAO.getFilesByUser(currentUser.getUserId());
            Platform.runLater(() -> {
                myFilesList.clear();
                myFilesList.addAll(files);
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> 
                showError("Error loading your files: " + e.getMessage())
            );
        }
    });
}

private void loadAllFiles() {
    executor.submit(() -> {
        try {
            var files = fileDAO.getAllSharedFiles();
            Platform.runLater(() -> {
                allFilesList.clear();
                allFilesList.addAll(files);
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> 
                showError("Error loading all shared files: " + e.getMessage())
            );
        }
    });
}

private void loadPeers() {
    executor.submit(() -> {
        try {
            var peers = peerDAO.getOnlinePeers();
            Platform.runLater(() -> {
                peersList.clear();
                peersList.addAll(peers);
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> 
                showError("Error loading online peers: " + e.getMessage())
            );
        }
    });
}
public void shutdown() {
    executor.shutdownNow();
}

    
    // ============================================
    // EVENT HANDLERS
    // ============================================
    
    @FXML
    private void handleUpload() {
   if (currentUser == null) {
        showError("User is not logged in!");
        return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select File to Upload");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));

    File selectedFile = fileChooser.showOpenDialog(null);
    if (selectedFile == null) return; // user cancel

    new Thread(() -> {
        try {
            // 1️⃣ Tạo folder lưu trữ của user
            String folderPath = "storage/user_" + currentUser.getUserId();
            File folder = new File(folderPath);
            if (!folder.exists()) folder.mkdirs();

            // 2️⃣ Destination file
            File destFile = new File(folder, selectedFile.getName());

            // 3️⃣ Copy file
            Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 4️⃣ Tạo SHA-256 hash bằng HashUtil
            String hash = HashUtil.hashFile(destFile);

            // 5️⃣ Kiểm tra trùng lặp
            FileInfo existing = fileDAO.getFileByHash(hash);
            if (existing != null) {
                javafx.application.Platform.runLater(() -> 
                    showError("File already exists: " + existing.getFileName())
                );
                return;
            }

            // 6️⃣ Lưu metadata vào database
            FileInfo info = new FileInfo();
            info.setFileName(selectedFile.getName());
            info.setFileSize(selectedFile.length());
            info.setFilePath(destFile.getAbsolutePath());
            info.setFileHash(hash);
            info.setUserId(currentUser.getUserId());
            info.setOwnerUsername(currentUser.getUsername());

            boolean ok = fileDAO.addFile(info);

            // 7️⃣ Reload My Files table và thông báo
            javafx.application.Platform.runLater(() -> {
                if (ok) {
                    showInfo("Upload successful!\nFile: " + selectedFile.getName());
                    loadMyFiles();
                        loadAllFiles(); // Refresh All Files table nếu muốn

                } else {
                    showError("Failed to save file info to database.");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() -> 
                showError("Upload failed: " + e.getMessage())
            );
        }
    }).start();
    }
    
    @FXML
    private void handleDeleteFile() {
        FileInfo selected = tblMyFiles.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a file to delete!");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete file: " + selected.getFileName());
        confirm.setContentText("Are you sure?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (fileDAO.deleteFile(selected.getFileId())) {
                    showInfo("File deleted!");
                    loadMyFiles();
                } else {
                    showError("Delete failed!");
                }
            }
        });
    }
    
    @FXML
private void handleSearch() {
    String keyword = txtSearch.getText().trim();
     // Debug: in ra keyword
    System.out.println("Searching keyword: '" + keyword + "'");
    // Nếu rỗng, load tất cả
    if (keyword.isEmpty()) {
        loadAllFiles();
        return;
    }
    
    // Tìm kiếm
    new Thread(() -> {
        var results = fileDAO.searchFilesByName(keyword);
        javafx.application.Platform.runLater(() -> {
            allFilesList.setAll(results); // clear + addAll
        });
    }).start();
}

  
@FXML
private void handleDownload() {
    FileInfo selected = tblAllFiles.getSelectionModel().getSelectedItem();

    if (selected == null) {
        showError("Please select a file to download!");
        return;
    }

    // Hộp thoại chọn nơi lưu file
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Save File");
    fileChooser.setInitialFileName(selected.getFileName());

    File saveLocation = fileChooser.showSaveDialog(btnDownload.getScene().getWindow());
    if (saveLocation == null) {
        return; // user bấm Cancel
    }

    // Chạy trong Thread nền để không làm đứng UI
    new Thread(() -> {
        try {
            boolean ok = fileDAO.downloadFile(selected.getFileId(), saveLocation.getAbsolutePath());

            Platform.runLater(() -> {
                if (ok)
                    showInfo("Downloaded successfully:\n" + saveLocation.getAbsolutePath());
                else
                    showError("Download failed!");
            });

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() ->
                showError("An error occurred while downloading!")
            );
        }
    }).start();
}

    @FXML
    private void handleRefreshPeers() {
        loadPeers();
        showInfo("Peers list refreshed!");
    }
    
    @FXML
    private void handleDiscoverPeers() {
        btnDiscoverPeers.setDisable(true);
        btnDiscoverPeers.setText("Discovering...");
        
        new Thread(() -> {
            var discoveredPeers = PeerDiscoveryClient.discoverPeers();
            
            javafx.application.Platform.runLater(() -> {
                btnDiscoverPeers.setDisable(false);
                btnDiscoverPeers.setText("Discover Peers");
                
                showInfo("Found " + discoveredPeers.size() + " peers!");
                loadPeers();
            });
        }).start();
    }
    
    @FXML
    private void handleLogout() {
        // Stop PeerServer
        if (peerServer != null) {
            peerServer.stopServer();
        }
        
        // Set peer offline
        if (currentUser != null) {
            peerDAO.setPeerOffline(currentUser.getUserId());
        }
        
        // Back to login
        App.showLoginScreen();
    }
    
    // ============================================
    // HELPER METHODS
    // ============================================
    
    /**
     * Start PeerServer và register peer
     * ĐÃ SỬA: Thêm try-catch cho UnknownHostException
     */
    private void startPeerServer() {
        new Thread(() -> {
            try {
                // Start server
                peerServer = new PeerServer(8000);
                peerServer.start();
                
                // Register peer in database
                if (currentUser != null) {
                    try {
                        // Lấy IP của máy hiện tại
                        String myIP = InetAddress.getLocalHost().getHostAddress();
                        peerDAO.registerPeer(currentUser.getUserId(), myIP, 8000);
                        
                        System.out.println("✅ Peer registered: " + myIP + ":8000");
                        
                    } catch (UnknownHostException e) {
                        System.err.println("❌ Cannot get local IP: " + e.getMessage());
                        // Fallback: dùng localhost
                        peerDAO.registerPeer(currentUser.getUserId(), "127.0.0.1", 8000);
                    }
                }
                
            } catch (Exception e) {
                System.err.println("❌ Start server error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}