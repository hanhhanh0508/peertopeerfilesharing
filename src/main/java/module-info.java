
module com.p2papp.filesharing {
    requires javafx.controls;
    requires javafx.fxml;
requires java.sql;
    requires mysql.connector.j;

   exports com.p2papp.filesharing;
    exports com.p2papp.filesharing.controller;
exports com.p2papp.filesharing.model;

    opens com.p2papp.filesharing to javafx.fxml;
    opens com.p2papp.filesharing.view to javafx.fxml;
    opens com.p2papp.filesharing.controller to javafx.fxml;
    opens com.p2papp.filesharing.model to javafx.base; // ← QUAN TRỌNG NHẤT


}
