package org.example.udpnetworkproject;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.net.*;
import java.io.*;

public class UDPController extends Thread implements Initializable {
    public Integer tcpPublicPort;
    private static final long AFK_TIME = 30000; // 30 seconds
    private static final long DELETE_FROM_Archive = 10000; //2 min
    private long deletePressTime;
    private long lastActivityTime;
    public Socket tcpSocket;
    private boolean isLogin = false;
    public PasswordField passwordTextField;
    public TextField tcpServerIP;
    public TextField tcpServerPort;
    public Button logoutButton;
    private volatile boolean isBusy = false;
    private boolean threadIsRunning = false;
    public Button sendButton;
    public Text userStatusLabel;
    public ComboBox statusComboBox;
    private ArchiveCleanupThread cleanupThread;
//    @FXML
//    private ProgressIndicator loading;
    private DatagramSocket socket;
    private boolean runStatus = true;
    private boolean running;
    private Integer ID;
    @FXML
    private ListView<Message> Archive;

    @FXML
    private ChoiceBox<String> interfaces;

    @FXML
    private TextField localIP;

    @FXML
    private TextField localPort;

    @FXML
    private ListView<Message> message;

    @FXML
    private ListView<Message> onlineUsers;

    @FXML
    private TextArea messageBox;

    @FXML
    private TextField remoteIP;

    @FXML
    private TextField username;

    @FXML
    private TextField remotePort;


    @FXML
    private Button startListeningButton;

    @FXML
    private Button loginButton;

    @FXML
    private TextField status;

    @FXML
    private Text lastLogin;

    private Thread listeningThread;
    private ObjectOutputStream out;
    private ServerSocket tcpServerSocket;
    private boolean sendAll;
    private String userStatus = "Active";


    @FXML
    void delete(ActionEvent ignoredEvent) {
        if (message.getSelectionModel().isEmpty() || message.getSelectionModel().getSelectedItem().getDeletedTime() != -1 || message.getSelectionModel().getSelectedItem().getType() == 1) {
            return;
        }

        try {
            deletePressTime = System.currentTimeMillis();
            Message selectedMessage = message.getSelectionModel().getSelectedItem();
            selectedMessage.setDeletedTime(System.currentTimeMillis());
            selectedMessage.setMessage("[Deleted Message]");
            message.refresh();
            Archive.getItems().add(selectedMessage);

            InetAddress receiverAddress = InetAddress.getByName(remoteIP.getText().trim());
            int remotePortInt = Integer.parseInt(remotePort.getText());

            // Create DatagramSocket for sending data

            // Create message to send
            Message messageToSend = new Message(String.valueOf(selectedMessage.getMessageID()));
            messageToSend.setType((short) 2);
            messageToSend.setTime(LocalDateTime.now());
            messageToSend.setPort(selectedMessage.getPort());
            messageToSend.setAddress(InetAddress.getByName(localIP.getText().trim()));
            // Serialize the Message object into a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(messageToSend);
            byte[] sendData = baos.toByteArray();

            // Create DatagramPacket with specific destination address and port
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receiverAddress, remotePortInt);
            // Send the packet
            socket.send(sendPacket);
            // Clear the message box
            if (!cleanupThread.running){
                    cleanupThread =new ArchiveCleanupThread(true);
                    new Thread(cleanupThread).start();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Something went wrong ");
            alert.show();
        }

    }

    @FXML
    void deleteConvo(ActionEvent ignoredEvent) {
        if (message.getSelectionModel().isEmpty() || message.getSelectionModel().getSelectedItem().getDeletedTime() != -1 || message.getSelectionModel().getSelectedItem().getType() == 1 ) {
            return;
        }

        try {
            Message selectedMessage = message.getSelectionModel().getSelectedItem();
            Message messageToSend = new Message(localPort.getText());
            messageToSend.setType((short) 3);
            messageToSend.setTime(LocalDateTime.now());
            messageToSend.setPort(selectedMessage.getPort());
            messageToSend.setAddress(InetAddress.getByName(localIP.getText().trim()));
            deleteFunction(Integer.parseInt(localPort.getText()),true,messageToSend.getAddress());

            InetAddress receiverAddress = InetAddress.getByName(remoteIP.getText().trim());
            int remotePortInt = Integer.parseInt(remotePort.getText());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(messageToSend);
            byte[] sendData = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receiverAddress, remotePortInt);
            socket.send(sendPacket);

            if (!cleanupThread.running){
                cleanupThread =new ArchiveCleanupThread(true);
                new Thread(cleanupThread).start();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("something went wrong");
            alert.show();
        }
    }

    @FXML
    void restore(ActionEvent ignoredEvent) {
        if (Archive.getSelectionModel().isEmpty() ) {
            return;
        }

        try {
            Message selectedMessage = Archive.getSelectionModel().getSelectedItem();
            selectedMessage.setDeletedTime(-1);
            selectedMessage.setMessage(selectedMessage.getOriginalMessage());
            Archive.getItems().remove(selectedMessage);
            message.refresh();
            Archive.refresh();
            Message messageToSend = new Message(localPort.getText());
            messageToSend.setType((short) 4);
            messageToSend.setTime(LocalDateTime.now());
            messageToSend.setPort(selectedMessage.getPort());
            messageToSend.setMessageID(selectedMessage.getMessageID());
            messageToSend.setAddress(InetAddress.getByName(localIP.getText().trim()));
            InetAddress receiverAddress = InetAddress.getByName(remoteIP.getText().trim());
            int remotePortInt = Integer.parseInt(remotePort.getText());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(messageToSend);
            byte[] sendData = baos.toByteArray();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receiverAddress, remotePortInt);
            socket.send(sendPacket);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("something went wrong");
            alert.show();
        }

    }

    void deleteFunction(int port, Boolean B, InetAddress addr){
        if (B && (message.getSelectionModel().isEmpty() || message.getSelectionModel().getSelectedItem().getDeletedTime() != -1 || message.getSelectionModel().getSelectedItem().getType() == 1)) {
            return;
        }

        try {
            for (Message m : message.getItems()) {
                if (m.getPort() == port && m.getAddress().equals(addr)) {
                    m.setMessage("[Deleted Message]");
                    m.setDeletedTime(System.currentTimeMillis());
                    if (B)
                        Archive.getItems().add(m);
                }
            }
            Archive.refresh();
            message.refresh();
            if (!cleanupThread.running){
                cleanupThread =new ArchiveCleanupThread(true);
                new Thread(cleanupThread).start();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("something went wrong");
            alert.show();
        }
    }

    @FXML
    void Active(ActionEvent ignoredEvent){
        userStatus = "Active";
        userStatusLabel.setText("User Status: "+userStatus);
    }

    @FXML
    void Busy(ActionEvent ignoredEvent){
        userStatus = "Busy";
        userStatusLabel.setText("User Status: "+userStatus);
    }

    @FXML
    void Away(ActionEvent ignoredEvent){
        userStatus = "Away";
        userStatusLabel.setText("User Status: "+userStatus);
    }


    @FXML
    void send(ActionEvent ignoredEvent) {
        if (messageBox.getText().isEmpty() || socket == null)
            return;
        try {
            if (remotePort.getText().isEmpty() || remoteIP.getText().isEmpty())
                throw new Exception();

            // Get remote address and port from text fields
            InetAddress receiverAddress = InetAddress.getByName(remoteIP.getText().trim());
            int remotePortInt = Integer.parseInt(remotePort.getText());

            // Create DatagramSocket for sending data

            // Create message to send
            Message messageToSend = new Message(messageBox.getText());
            messageToSend.setType((short) 0);
            messageToSend.setMessageID(ID++);
            messageToSend.setPort(Integer.parseInt(localPort.getText()));
            messageToSend.setTime(LocalDateTime.now());
            messageToSend.setAddress(InetAddress.getByName(localIP.getText().trim()));

            // Serialize the Message object into a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(messageToSend);
            byte[] sendData = baos.toByteArray();

            // Create DatagramPacket with specific destination address and port
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receiverAddress, remotePortInt);
            status.setText("Sent to IP= " + sendPacket.getAddress() + ", Port = " + sendPacket.getPort());
            // Send the packet
            socket.send(sendPacket);
            // Clear the message box
            message.getItems().add(messageToSend);
            if (!sendAll)
                messageBox.clear();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Please check that all fields are filled with correct data");
            alert.show();
        }
    }


    @FXML
    void startListening(ActionEvent ignoredEvent) {
        if (running) {
            if (listeningThread != null) {
                socket.close();
                listeningThread.interrupt(); // Interrupt the thread if it's not null
                startListeningButton.setText("Start listening");
                running = false;
                if (!username.isDisable()){
                    localIP.setDisable(false);
                    localPort.setDisable(false);
                }
                running = false;
//                loading.setVisible(false);
                remoteIP.setDisable(false);
                remotePort.setDisable(false);
            }
        } else {
            try {
                if (localPort.getText().isEmpty() || localIP.getText().isEmpty())
                    throw new Exception();
                int localPortInt = Integer.parseInt(localPort.getText());
                socket = new DatagramSocket(localPortInt);
                listeningThread = new Thread(new Node(status, socket, message));
                running = true;
                localIP.setDisable(true);
                localPort.setDisable(true);
//                loading.setVisible(true);
                status.setText("Listening to messages");
                listeningThread.start();
                startListeningButton.setText("Stop listening");
                //-----------------------------------------------------------

                //------------------------------------------------------------
                remoteIP.setDisable(true);
                remotePort.setDisable(true);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Pls check that all fields filled with correct data types");
                alert.show();
            }

        }
    }
    @FXML
    void test(ActionEvent ignoredEvent) {
        try {
            if (localIP.getText().isEmpty() || remotePort.getText().isEmpty() || remoteIP.getText().isEmpty())
                throw new Exception();

            // Get remote address and port from text fields
            InetAddress receiverAddress = InetAddress.getByName(remoteIP.getText().trim());
            int remotePortInt = Integer.parseInt(remotePort.getText());

            Message messageToSend = new Message("TEST");
            messageToSend.setType((short) 0);
            messageToSend.setMessageID(ID++);
            messageToSend.setPort(Integer.parseInt(localPort.getText()));
            messageToSend.setTime(LocalDateTime.now());
            messageToSend.setAddress(InetAddress.getByName(localIP.getText().trim()));

            // Serialize the Message object into a byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(messageToSend);
            byte[] sendData = baos.toByteArray();

            // Create DatagramPacket with specific destination address and port
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receiverAddress, remotePortInt);
            status.setText("Sent to IP= " + sendPacket.getAddress() + ", Port = " + sendPacket.getPort());
            // Send the packet
            socket.send(sendPacket);
            // Clear the message box
            message.getItems().add(messageToSend);
            messageBox.clear();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Please check that all fields are filled with correct data");
            alert.show();
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        running = false;
        ID = 0;
        cleanupThread = new ArchiveCleanupThread(false);
        sendAll = false;
        localPort.setText(String.valueOf(findFreePort()));
        lastLogin.setVisible(false);
        onlineUsers.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (onlineUsers.getSelectionModel().getSelectedItem() != null) {
                remoteIP.setText(onlineUsers.getSelectionModel().getSelectedItem().getAddress().toString().substring(1));
                remotePort.setText(onlineUsers.getSelectionModel().getSelectedItem().getPort() + "");
            }

        });

        username.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {
            if (" ".equals(event.getCharacter())) {
                event.consume();
            }
        });
        passwordTextField.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {
            if (" ".equals(event.getCharacter())) {
                event.consume();
            }
        });


        message.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                } else {
                    Platform.runLater(() -> {
                        String temp = "";
                        setWrapText(true);
                        if (message.getType() == 0) {
                            setTextFill(Color.BLUE);
                            temp = "Me: \n";
                        } else if (message.getType() == 1) {
                            setTextFill(Color.RED);
                            for (Message u : onlineUsers.getItems())
                            {
                                if (u.getAddress().equals(message.getAddress()) && u.getPort() == message.getPort()){
                                    temp =u.getMessage() + "\n";
                                }
                            }
                        }
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                        setText(temp + message.getMessage() + "\n" + message.getTime().format(formatter) + " From Port: " + message.getPort()); // Assuming there's a method getMessage() in Message class
                        status.requestFocus();
                        status.requestFocus();
                    });
                }
            }
        });
        Archive.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                } else {
                    Platform.runLater(() -> {
                        String temp = "";
                        setWrapText(true);
                        if (message.getType() == 0) {
                            setTextFill(Color.BLACK);
                        }

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        setText(temp + message.getOriginalMessage() + "\n" + message.getTime().format(formatter) + " From Port: " + message.getPort()); // Assuming there's a method getMessage() in Message class
                        status.requestFocus();
                        status.requestFocus();
                    });
                }
            }
        });


        message.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            status.requestFocus();
            status.requestFocus();
        });

        //Filling the box manually
        interfaces.getItems().addAll("WiFi", "Ethernet", "Loop-back");
        interfaces.getSelectionModel().selectFirst();
        setLocalIP();
        //Listener area
        interfaces.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            // Check if the new selection is "Loop-back"
            if ("Loop-back".equals(newValue)) {
                localIP.setText("127.0.0.1");
                localIP.setDisable(true);
            } else {
                setLocalIP();
                localIP.setDisable(false);

            }
        });

        //Adding restrictions to the ports to accept only numbers
        setTextFieldProperty(localPort);
        setTextFieldProperty(remotePort);

        //Adding restrictions to the remote IP
        remoteIP.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            String character = keyEvent.getCharacter();
            if (!character.matches("[\\d.]")) { // If not digit or .
                keyEvent.consume(); //remove it
            } else {
                String text = remoteIP.getText();
                // Multiple dots
                if (".".equals(character) && (text.isEmpty() || text.endsWith("."))) {
                    keyEvent.consume();
                }
            }
        });

        remoteIP.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 15) {
                remoteIP.setText(oldValue);
            }
        });
        onlineUsers.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                } else {

                        setWrapText(true);
                        setTextFill(Color.GREEN);
                        setText(message.getMessage() +"\nStatus: "+ message.getUserStatus() +"\nPort Number: "+ message.getPort() + " IP: " + message.getAddress()
                        ); // Assuming there's a method getMessage() in Message class
                }
            }
        });

        statusComboBox.getItems().addAll("Active", "Busy", "Away");
        statusComboBox.setValue("Active");
        userStatusLabel.setText("User Status: Offline");

        // Add different mouse event handlers
        message.setOnMouseEntered(this::handleMouseEnter);
        message.setOnMouseExited(this::handleMouseExit);
        message.setOnMousePressed(this::handleMousePress);
        message.setOnMouseReleased(this::handleMouseRelease);
        message.setOnMouseDragged(this::handleMouseDrag);
        message.setOnMouseMoved(this::handleMouseMove);
        message.setOnMouseClicked(this::handleMouseClick);

        // Add different keyboard event handlers
        messageBox.setOnKeyPressed(this::handleKeyPress);
        messageBox.setOnKeyReleased(this::handleKeyRelease);
        messageBox.setOnKeyTyped(this::handleKeyType);
    }

    private void handleMouseDrag(MouseEvent mouseEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleMousePress(MouseEvent mouseEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleMouseRelease(MouseEvent mouseEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleMouseMove(MouseEvent mouseEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleKeyType(KeyEvent keyEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleKeyRelease(KeyEvent keyEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleMouseExit(MouseEvent mouseEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleMouseEnter(MouseEvent mouseEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleKeyPress(KeyEvent keyEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void handleMouseClick(javafx.scene.input.MouseEvent mouseEvent) {
        if (isLogin) {
            lastActivityTime = System.currentTimeMillis(); // Reset the activity timer
            setStatusActive();
        }
    }

    private void setTextFieldProperty(TextField localPort) {
        localPort.addEventFilter(KeyEvent.KEY_TYPED, keyEvent -> {
            if (!keyEvent.getCharacter().matches("\\d")) {//If not digit
                keyEvent.consume(); //
            }
        });

        localPort.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 5) {//if bigger than 5
                remotePort.setText(oldValue);
            }
        });
    }

    private void setLocalIP() {
        try {
            localIP.setText(getLocalIPAddress());


        } catch (SocketException e) {
            localIP.setText("Error fetching IP");
        }
    }

    private String getLocalIPAddress() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress ia = inetAddresses.nextElement();
                if (!ia.isLoopbackAddress() && ia instanceof java.net.Inet4Address) {
                    return ia.getHostAddress();
                }
            }
        }
        return "No IP Address Found";
    }
    public static int findFreePort() {
        int port = 49152; // Start port number for dynamic/private ports

        while (true) {
            try (ServerSocket ignored = new ServerSocket(port)) {
                // The port is available
                return port;
            } catch (IOException e) {
                // Port is already in use, try the next one
                port++;
            }
        }
    }

    public void login(ActionEvent ignoredEvent) {
        runStatus=true;
        Platform.runLater(onlineUsers::refresh);
        if (username.getText().isEmpty() || passwordTextField.getText().isEmpty() || localPort.getText().isEmpty() || localIP.getText().isEmpty() || tcpServerIP.getText().isEmpty() || tcpServerPort.getText().isEmpty() || !running){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("pls fill everything with the data");
            alert.show();
            return;
        }
        try {
            tcpSocket = new Socket(tcpServerIP.getText(), Integer.parseInt(tcpServerPort.getText()));
            out = new ObjectOutputStream(tcpSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(tcpSocket.getInputStream());
            tcpPublicPort = findFreePort();
            Message messageToSend = new Message(username.getText()+" " +passwordTextField.getText()+ " " + tcpPublicPort);
            messageToSend.setType((short) 0);
            messageToSend.setPort(Integer.parseInt(localPort.getText()));
            messageToSend.setAddress(InetAddress.getByName(localIP.getText().trim()));

            out.writeObject(messageToSend);
            out.flush();
            isLogin = true;
            if (!threadIsRunning) {
                this.start();
                threadIsRunning = true;
            }
            Message receivedMessage = (Message) in.readObject();
            if (receivedMessage.getType()==2){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Wrong username or Password");
                alert.show();
            }
            else {

                Message temp = new Message(String.valueOf(tcpPublicPort));
                temp.setType((short)5);
                out.writeObject(temp);
                out.flush();


                Message[] receivedObjects = (Message[]) in.readObject();
                onlineUsers.getItems().addAll(receivedObjects);
                onlineUsers.refresh();
                tcpServerSocket = new ServerSocket(tcpPublicPort);

                username.setDisable(true);
                passwordTextField.setDisable(true);
                listeningThread = new Thread(new TCPClient());
                listeningThread.start();
                loginButton.setDisable(true);
                lastLogin.setVisible(true);
                lastLogin.setText("Last Login by " + username.getText() + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            userStatusLabel.setText("User Status: "+userStatus);
        } catch (IOException | ClassNotFoundException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Something went wrong pls try again later");
        }
    }

    public void sendToAll(ActionEvent ignoredEvent) {
        sendAll = true;
        for (Message m : onlineUsers.getItems())
        {
            if (m.getAddress().toString().substring(1).equals(localIP.getText()) && m.getPort() == Integer.parseInt(localPort.getText()))
                continue;
            remoteIP.setText(m.getAddress().toString().substring(1));
            remotePort.setText(m.getPort()+ "");
            sendButton.fire();
        }
        sendAll = false;
    }

    public void logout(ActionEvent ignoredEvent) {
        if (!loginButton.isDisable())
            return;
        try {
            Message messageToSend = new Message(username.getText());
            messageToSend.setType((short) 4);
            messageToSend.setPort(Integer.parseInt(localPort.getText()));
            messageToSend.setAddress(InetAddress.getByName(localIP.getText().trim()));
            out.writeObject(messageToSend);
            out.flush();
            onlineUsers.getItems().clear();
            username.setDisable(false);
            passwordTextField.setDisable(false);
            loginButton.setDisable(false);
            isLogin = false;
            userStatus = "Offline";
            userStatusLabel.setText("User Status: Offline");
        } catch (IOException ignored) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Something went wrong pls try again later");
        }
        runStatus = false;
    }

    public class Node implements Runnable {
        private final TextField statusTextField;
        private final DatagramSocket socket;
        @FXML
        private final ListView<Message> message;

        public Node(TextField statusTextField, DatagramSocket socket, ListView<Message> message) {
            this.statusTextField = statusTextField;
            this.socket = socket;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                if (socket == null)
                    return;
                while (true) {
                    byte[] receiveData = new byte[65536];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);

                    // Deserialize the received message
                    ByteArrayInputStream bais = new ByteArrayInputStream(receiveData);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Message receivedMessage = (Message) ois.readObject();

                    if (receivedMessage.getType() == 0) {
                        receivedMessage.setType((short) 1);
                        Platform.runLater(() -> statusTextField.setText("Received from: " + receivePacket.getAddress() + ", Port = " + receivePacket.getPort()));
                        message.getItems().add(receivedMessage);
                    } else if (receivedMessage.getType() == 2) {
                        for (Message m : message.getItems()) {
                            if (m.getPort() == receivedMessage.getPort() && m.getMessageID() == Integer.parseInt(receivedMessage.getMessage()) && m.getAddress().equals(receivedMessage.getAddress())) {
                                m.setMessage("[Deleted Message]");
                                Platform.runLater(message::refresh);
                                break;
                            }
                        }
                    } else if (receivedMessage.getType() == 3) {
                        deleteFunction(receivedMessage.getPort(), false,receivedMessage.getAddress());
                        Platform.runLater(message::refresh);

                        //restore
                    } else if (receivedMessage.getType() == 4) {
                        for (Message m: message.getItems()){
                            if (m.getPort() == receivedMessage.getPort() &&  m.getMessageID() == receivedMessage.getMessageID() && m.getType()==1 && m.getAddress().equals(receivedMessage.getAddress())){
                                m.setMessage(m.getOriginalMessage());
                                Platform.runLater(message::refresh);
                                break;
                            }
                        }
                    }


                    // Handle the received message
                }
            } catch (Exception ignored) {

            } finally {
                assert socket != null;
                socket.close();
            }

        }
    }

    private class ArchiveCleanupThread extends Thread {//////////////////////////////////////////////////////////////inner class
        private volatile boolean running;

        public ArchiveCleanupThread(Boolean b){
            this.running = b;
        }

        @Override
        public void run() {
            try {
                while (running) {
                    long currentTime = System.currentTimeMillis();
                    for (Message m: Archive.getItems()){
                        if (currentTime - m.getDeletedTime() >= 120000) {
                            Platform.runLater(() -> {
                                Archive.getItems().remove(m);
                                Archive.refresh();
                            });
                        }
                    }
                    if (Archive.getItems().isEmpty())
                        running = false;
                }
            }
            catch (Exception e){
                running =  false;
            }

        }

    }
    private class TCPClient implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Socket clientSocket = tcpServerSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                    Message receivedMessage = (Message) in.readObject();
                    if (receivedMessage.getType()==3){
                        Platform.runLater(() -> {
                            onlineUsers.getItems().add(receivedMessage);
                            onlineUsers.refresh();
                        });
                    }else if (receivedMessage.getType()==10){
                        Platform.runLater(() -> {
                            boolean ifFound=false;
                            for(Message m: onlineUsers.getItems()){
                                if (m.getMessage().equals(receivedMessage.getMessage())){
                                    m.setUserStatus(receivedMessage.getUserStatus());
                                    ifFound=true;
                                    break;
                                }
                            }
                            if (!ifFound) onlineUsers.getItems().add(receivedMessage);
                            onlineUsers.refresh();
                        });
                        System.out.println(receivedMessage.getMessage()+receivedMessage.getType()+receivedMessage.getUserStatus());
                    }
                    else if (receivedMessage.getType()==4){
                       for (Message m : onlineUsers.getItems()){
                           if (m.getMessage().equals(receivedMessage.getMessage()) && m.getPort() == receivedMessage.getPort()) {
                               Platform.runLater(() -> {
                                   onlineUsers.getItems().remove(m);
                                   onlineUsers.refresh();
                               });
                               break;
                           }
                       }
                    }
                }
            }catch (IOException | ClassNotFoundException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Something went wrong ");
                alert.show();
            }
        }
    }

    @FXML
    private void handleStatusChange(ActionEvent event) {
        String selectedStatus = (String) statusComboBox.getValue();
        // Update status based on the selected item
        if (isLogin) {
            try {
                Socket tcpSocket1 = new Socket(tcpServerIP.getText(), Integer.parseInt(tcpServerPort.getText()));
                ObjectOutputStream out1 = new ObjectOutputStream(tcpSocket1.getOutputStream());
                Message messageToSend = new Message(username.getText()+" " +passwordTextField.getText()+ " " + tcpPublicPort);
                messageToSend.setType((short) 10);
                messageToSend.setPort(Integer.parseInt(localPort.getText()));
                messageToSend.setAddress(InetAddress.getByName(localIP.getText().trim()));
                switch (selectedStatus) {
                    case "Active" -> {
                        userStatus = "Active";
                        userStatusLabel.setText("User Status: Active");
                        messageToSend.setUserStatus(userStatus);
                        setBusy(false);
                    }
                    case "Away" -> {
                        userStatus = "Away";
                        userStatusLabel.setText("User Status: Away");
                        messageToSend.setUserStatus(userStatus);
                    }
                    case "Busy" -> {
                        userStatus = "Busy";
                        userStatusLabel.setText("User Status: Busy");
                        messageToSend.setUserStatus(userStatus);
                        setBusy(true);
                    }
                }
                out1.writeObject(messageToSend);
                out1.flush();
//                out1.close();
//                tcpSocket1.close();

                for (int i = 0; i < onlineUsers.getItems().size(); i++) {

                    if (onlineUsers.getItems().get(i).getPort() == Integer.parseInt(localPort.getText())){
                        onlineUsers.getItems().get(i).setUserStatus(messageToSend.getUserStatus());
                        Platform.runLater(onlineUsers::refresh);
                        break;
                    }
                }
                Platform.runLater(onlineUsers::refresh);
            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }

    @Override
    public void run() {
        lastActivityTime = System.currentTimeMillis();

        while (runStatus) {
            try {
                Thread.sleep(1000); // Check every second

                System.out.println(System.currentTimeMillis() - lastActivityTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if ((System.currentTimeMillis() - lastActivityTime) > AFK_TIME) {
                setStatusAway();
            } else {
                setStatusActive();
            }
            if ((System.currentTimeMillis() - deletePressTime) > DELETE_FROM_Archive) {
                Message selectedMessage = message.getSelectionModel().getSelectedItem();
                if (selectedMessage != null) {
                    if (Archive.getItems().contains(selectedMessage)) {
                        Archive.getItems().remove(selectedMessage);
                    }
                }
            }
        }
    }

    private void setStatusAway() {
        Platform.runLater(() -> {
            statusComboBox.getSelectionModel().select(2);
        });
        userStatusLabel.setText("User Status: "+userStatus);
    }

    private Object lock = new Object();


    private void setStatusActive() {
        synchronized (lock) {
            if (!isBusy) {
                userStatus = "Active";
                Platform.runLater(() -> {
                    statusComboBox.getSelectionModel().select("Active");
                    userStatusLabel.setText("User Status: " + userStatus);
                });
            } else {
                userStatus = "Busy";
                Platform.runLater(() -> {
                    statusComboBox.getSelectionModel().select("Busy");
                    userStatusLabel.setText("User Status: " + userStatus);
                });
            }
        }
    }

    private void setBusy(boolean busy) {
        synchronized (lock) {
            isBusy = busy;
        }
    }

}