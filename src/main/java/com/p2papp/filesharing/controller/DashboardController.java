package com.p2papp.filesharing.controller;

import com.p2papp.filesharing.App;
import com.p2papp.filesharing.database.dao.*;
import com.p2papp.filesharing.model.*;
import com.p2papp.filesharing.network.*;
import com.p2papp.filesharing.utils.HashUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javafx.stage.FileChooser;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import java.util.concurrent.Executors;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * DashboardController.java - FIXED VERSION
 * 
 * ✅ Sửa download P2P trực tiếp từ peer
 */
public class DashboardController {
    
    @FXML private Label lblUsername;
    @FXML private Label lblStatus;
    @FXML private Button btnLogout;
    
    @FXML private Tab tabMyFiles;
    @FXML private Tab tabAllFiles;
    @FXML private Tab tabPeers;
    
    @FXML private TableView<FileInfo> tblMyFiles;
    @FXML private TableColumn<FileInfo, Integer> colMyFileId;
    @FXML private TableColumn<FileInfo, String> colMyFileName;
    @FXML private TableColumn<FileInfo, String> colMyFileSize;
    @FXML private TableColumn<FileInfo, String> colSharedDate;
    @FXML private TableColumn<FileInfo, String> colAllSharedDate;
    @FXML private Button btnUpload;
    @FXML private Button btnDeleteFile;
    
    @FXML private TableView<FileInfo> tblAllFiles;
    @FXML private TableColumn<FileInfo, String> colAllFileName;
    @FXML private TableColumn<FileInfo, String> colAllFileSize;
    @FXML private TableColumn<FileInfo, String> colOwner;
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private Button btnDownload;
    
    @FXML private TableView<Peer> tblPeers;
    @FXML private TableColumn<Peer, String> colPeerName;
    @FXML private TableColumn<Peer, String> colPeerAddress;
    @FXML private TableColumn<Peer, String> colPeerStatus;
    @FXML private Button btnRefreshPeers;
    @FXML private Button btnDiscoverPeers;
    
    private User currentUser;
    private FileDAO fileDAO = new FileDAO();
    private PeerDAO peerDAO = new PeerDAO();
    private UserDAO userDAO = new UserDAO();
    private PeerServer peerServer;
    
    private ObservableList<FileInfo> myFilesList = FXCollections.observableArrayList();
    private ObservableList<FileInfo> allFilesList = FXCollections.observableArrayList();
    private ObservableList<Peer> peersList = FXCollections.observableArrayList();
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    @FXML
    public void initialize() {
        System.out.println("✅ DashboardController initialized");
        
        if (currentUser != null) {
            lblUsername.setText("Welcome, " + currentUser.getUsername() + "!");
        }
        
        setupMyFilesTable();
        setupAllFilesTable();
        setupPeersTable();
        
        loadMyFiles();
        loadAllFiles();
        loadPeers();
        
        startPeerServer();
        
        lblStatus.setText("🟢 Online");
        
        txtSearch.setOnAction(e -> handleSearch());
        btnSearch.setOnAction(e -> handleSearch());
    }
    
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
        colAllSharedDate.setCellValueFactory(c -> {
            Timestamp ts = c.getValue().getSharedDate();
            String formatted = ts != null ?
                new SimpleDateFormat("yyyy-MM-dd HH:mm").format(ts) : "";
            return new SimpleStringProperty(formatted);
        });
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
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
        lblUsername.setText("Welcome, " + currentUser.getUsername() + "!");
        loadMyFiles();
        loadAllFiles();
    }
    
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
    
    @FXML
private void handleUpload() {
    if (currentUser == null) {
        showError("User is not logged in!");
        return;
    }
    
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select File to Upload");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("All Files", "*.*")
    );
    
    File selectedFile = fileChooser.showOpenDialog(null);
    if (selectedFile == null) return;
    
    new Thread(() -> {
        try {
            // ✅ NORMALIZE tên file để tránh lỗi encoding
            String originalName = selectedFile.getName();
            String normalizedName = normalizeFileName(originalName);
            
            System.out.println("📁 Original name: " + originalName);
            System.out.println("📝 Normalized name: " + normalizedName);
            
            String folderPath = "storage/user_" + currentUser.getUserId();
            File folder = new File(folderPath);
            if (!folder.exists()) folder.mkdirs();
            
            // Dùng tên đã normalize
            File destFile = new File(folder, normalizedName);
            Files.copy(
                selectedFile.toPath(), 
                destFile.toPath(), 
                StandardCopyOption.REPLACE_EXISTING
            );
            
            String hash = HashUtil.hashFile(destFile);
            
            FileInfo existing = fileDAO.getFileByHash(hash);
            if (existing != null) {
                Platform.runLater(() -> 
                    showError("File already exists: " + existing.getFileName())
                );
                return;
            }
            
            FileInfo info = new FileInfo();
            info.setFileName(normalizedName);  // ← Dùng tên normalized
            info.setFileSize(selectedFile.length());
            info.setFilePath(destFile.getAbsolutePath());
            info.setFileHash(hash);
            info.setUserId(currentUser.getUserId());
            info.setOwnerUsername(currentUser.getUsername());
            
            boolean ok = fileDAO.addFile(info);
            
            Platform.runLater(() -> {
                if (ok) {
                    showInfo("Upload successful!\nFile: " + normalizedName);
                    loadMyFiles();
                    loadAllFiles();
                } else {
                    showError("Failed to save file info to database.");
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> 
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
        System.out.println("Searching keyword: '" + keyword + "'");
        
        if (keyword.isEmpty()) {
            loadAllFiles();
            return;
        }
        
        new Thread(() -> {
            var results = fileDAO.searchFilesByName(keyword);
            javafx.application.Platform.runLater(() -> {
                allFilesList.setAll(results);
            });
        }).start();
    }
    
    /**
     * ✅ FIXED: Download P2P trực tiếp từ peer
     */
   @FXML
    private void handleDownload() {
        FileInfo selected = tblAllFiles.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showError("Please select a file to download!");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialFileName(selected.getFileName());
        
        File saveLocation = fileChooser.showSaveDialog(btnDownload.getScene().getWindow());
        if (saveLocation == null) {
            return;
        }
        
        // Disable button during download
        btnDownload.setDisable(true);
        btnDownload.setText("Downloading...");
        
        new Thread(() -> {
            try {
                // Lấy thông tin peer owner
                User owner = userDAO.getUserById(selected.getUserId());
                if (owner == null) {
                    Platform.runLater(() -> {
                        showError("Owner not found!");
                        btnDownload.setDisable(false);
                        btnDownload.setText("Download");
                    });
                    return;
                }
                
                // Lấy thông tin peer (IP + Port)
                Peer peer = peerDAO.getPeerByUserId(owner.getUserId());
                if (peer == null || !peer.isOnline()) {
                    Platform.runLater(() -> {
                        showError("Peer " + owner.getUsername() + " is offline!");
                        btnDownload.setDisable(false);
                        btnDownload.setText("Download");
                    });
                    return;
                }
                
                System.out.println("📥 Downloading from peer: " + peer.getAddress());
                System.out.println("   File: " + selected.getFileName());
                System.out.println("   Save to: " + saveLocation.getAbsolutePath());
                
                // ✅ DÙNG STATIC METHOD downloadFileDirect()
                boolean success = PeerClient.downloadFileDirect(
                    peer.getIpAddress(), 
                    peer.getPort(), 
                    selected.getFileName(), 
                    saveLocation.getAbsolutePath()
                );
                
                Platform.runLater(() -> {
                    btnDownload.setDisable(false);
                    btnDownload.setText("Download");
                    
                    if (success) {
                        showInfo("✅ Download successful!\n\n" +
                                "From: " + owner.getUsername() + " (" + peer.getAddress() + ")\n" +
                                "File: " + selected.getFileName() + "\n" +
                                "Saved to: " + saveLocation.getAbsolutePath());
                        
                        // Optional: Log download to database
                        // DownloadDAO downloadDAO = new DownloadDAO();
                        // downloadDAO.addDownload(selected.getFileId(), currentUser.getUserId());
                    } else {
                        showError("Download failed! Check console for details.");
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    btnDownload.setDisable(false);
                    btnDownload.setText("Download");
                    showError("Download error: " + e.getMessage());
                });
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
        if (peerServer != null) {
            peerServer.stopServer();
        }
        
        if (currentUser != null) {
            peerDAO.setPeerOffline(currentUser.getUserId());
        }
        
        App.showLoginScreen();
    }
    
    private void startPeerServer() {
        new Thread(() -> {
            try {
                peerServer = new PeerServer(8000);
                peerServer.start();
                
                if (currentUser != null) {
                    try {
                        String myIP = InetAddress.getLocalHost().getHostAddress();
                        peerDAO.registerPeer(currentUser.getUserId(), myIP, 8000);
                        
                        System.out.println("✅ Peer registered: " + myIP + ":8000");
                        
                    } catch (UnknownHostException e) {
                        System.err.println("❌ Cannot get local IP: " + e.getMessage());
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
    // ============================================
    // HELPER METHODS - File name normalization
    // ============================================
    
    /**
     * Normalize tên file để tránh lỗi encoding
     * 
     * @param fileName Tên file gốc
     * @return Tên file đã normalize (bỏ ký tự đặc biệt)
     */
    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "unnamed_file";
        }
        
        // Replace ký tự đặc biệt Windows không cho phép
        String normalized = fileName
            .replaceAll("[\\\\/:*?\"<>|]", "_")  // \ / : * ? " < > |
            .replaceAll("\\s+", "_")             // Space → underscore
            .replaceAll("_{2,}", "_");            // Multiple underscores → 1
        
        return normalized;
    }
    
    /**
     * Optional: Bỏ dấu tiếng Việt nếu cần
     * 
     * @param s String cần bỏ dấu
     * @return String không dấu
     */
    private String removeVietnameseAccents(String s) {
        if (s == null) return null;
        
        String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        return temp.replaceAll("\\p{M}", "");
    }
}