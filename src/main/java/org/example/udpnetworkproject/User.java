package org.example.udpnetworkproject;

import javafx.beans.property.*;
import java.net.InetAddress;

public class User {
    private final IntegerProperty ID = new SimpleIntegerProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();
    private final BooleanProperty status = new SimpleBooleanProperty();
    private final ObjectProperty<InetAddress> address = new SimpleObjectProperty<>();
    private final IntegerProperty port = new SimpleIntegerProperty();
    private String userStatus = "Offline";
    private  int realPort;
    // ID property
    public int getRealPort() {
        return realPort;
    }
    public void setRealPort(int realPort) {
        this.realPort = realPort;
    }
    public int getID() {
        return ID.get();
    }
    public void setID(int value) {
        ID.set(value);
    }
    public IntegerProperty IDProperty() {
        return ID;
    }
    // Username property
    public String getUsername() {
        return username.get();
    }
    public void setUsername(String value) {
        username.set(value);
    }
    public StringProperty usernameProperty() {
        return username;
    }
    // Password property
    public String getPassword() {
        return password.get();
    }
    public void setPassword(String value) {
        password.set(value);
    }
    public StringProperty passwordProperty() {
        return password;
    }
    // Status property
    public boolean isStatus() {
        return status.get();
    }
    public void setStatus(boolean value) {
        status.set(value);
    }
    public BooleanProperty statusProperty() {
        return status;
    }
    // Address property
    public InetAddress getAddress() {
        return address.get();
    }
    public void setAddress(InetAddress value) {
        address.set(value);
    }
    public ObjectProperty<InetAddress> addressProperty() {
        return address;
    }
    // Port property
    public int getPort() {
        return port.get();
    }
    public void setPort(int value) {
        port.set(value);
    }
    public IntegerProperty portProperty() {
        return port;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }
}
