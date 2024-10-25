module org.example.udpnetworkproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens org.example.udpnetworkproject to javafx.fxml;
    exports org.example.udpnetworkproject;
}