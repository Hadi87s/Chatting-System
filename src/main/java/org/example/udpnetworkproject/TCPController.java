package org.example.udpnetworkproject;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.ResourceBundle;

public class TCPController implements Initializable {
    public TextField status;
    public TextField username;
    @FXML
    public TextArea nameIPPort;
    @FXML
    private TableColumn<User, InetAddress> address;

    @FXML
    private TableColumn<User, Integer> id;

    @FXML
    private ChoiceBox<String> interfaces;

    @FXML
    private TableColumn<User, String> password;

    @FXML
    private PasswordField passwordTextField;

    @FXML
    private TableColumn<User, Integer> port;

    @FXML
    private Button signUpButton;

    @FXML
    private Button startListeningButton;

    @FXML
    private TableColumn<User, Boolean> statusCol;

    @FXML
    private TableView<User> table;

    @FXML
    private TextField tcpServerIP;

    @FXML
    private TextField tcpServerPort;

    @FXML
    private TableColumn<User, String> usernameCol;
    private boolean running;
    private Thread listeningThread;
    private ServerSocket socket;

    private int ID;

    private static void handle(KeyEvent event) {
        if (" ".equals(event.getCharacter())) {
            event.consume();
        }
    }

    @FXML
    void SingUp(ActionEvent event) {
        if (username.getText().isEmpty() || passwordTextField.getText().isEmpty()){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Please fill the Username and Password fields");
            alert.show();
            return;
        }
        boolean accepted = true;
        for (User u : table.getItems())
            if (u.getUsername().equalsIgnoreCase(username.getText()))
                accepted = false;
        if (accepted) {
            writeUserToFile(username.getText(), passwordTextField.getText(), "users.txt");
            User user = new User();
            user.setStatus(false);
            user.setUsername(username.getText());
            user.setPassword(passwordTextField.getText());
            user.setID(ID++);
            table.getItems().add(user);
            table.refresh();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Added Successfully");
            alert.setHeaderText(null);
            alert.setContentText("the username was added successfully");
            alert.show();
        }else{
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("username exists");
            alert.show();
        }
    }

    @FXML
    void startListening(ActionEvent event) {

        if (running) {
            if (listeningThread != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                listeningThread.interrupt(); // Interrupt the thread if it's not null
                startListeningButton.setText("Start listening");
                running = false;
            }
        } else {
            try {
                if (tcpServerIP.getText().isEmpty() || tcpServerPort.getText().isEmpty())
                    throw new Exception();
                int serverPort = Integer.parseInt(tcpServerPort.getText());
                socket = new ServerSocket(serverPort);
                listeningThread = new Thread(() -> {
                    try {
                        while (running) {
                            Socket clientSocket = socket.accept();
                            // Handle client communication in a separate thread
                            Thread clientThread = new Thread(new ClientHandler(clientSocket));
                            clientThread.start();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                running = true;
                tcpServerPort.setDisable(true);
//                loading.setVisible(true);
                status.setText("Listening to Users");
                listeningThread.start();
                startListeningButton.setText("Stop listening");
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Pls check that all fields filled with correct data types");
                alert.show();
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ID = 1;
        running = false;
        username.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, TCPController::handle);
        passwordTextField.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {handle(event);});
        try {
            tcpServerIP.setText(getLocalIPAddress());
        } catch (SocketException e) {
            tcpServerIP.setText("");
        }
        String filename = "users.txt";
        File file = new File(filename);

        // Check if the file exists
        if (!file.exists()) {
            try {
                // Create a new file
                if (!file.createNewFile()) {
                    System.err.println("Failed to create file: " + filename);
                }
            } catch (IOException ignored) {
            }
        }
        address.setCellValueFactory(new PropertyValueFactory<>("address"));
        id.setCellValueFactory(new PropertyValueFactory<>("ID"));
        password.setCellValueFactory(new PropertyValueFactory<>("password"));
        password.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("*".repeat(item.length()));
                }
            }
        });
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        port.setCellValueFactory(new PropertyValueFactory<>("port"));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item ? "Online" : "Offline");
                }
            }
        });

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle(""); // Clear style for empty cells
                } else {
                    if (item.isStatus()) {
                        setStyle("-fx-background-color: #aeffae;"); // Green for active status
                    } else {
                        setStyle("-fx-background-color: #ff6565;"); // Red for inactive status
                    }
                }
            }
        });
        readUsersFromFile("users.txt", table);
        setupPortChangeListener(table);


    }

    private void updateTextArea() {
        ObservableList<User> items = table.getItems();
        StringBuilder sb = new StringBuilder();

        for (User user : items) {
            sb.append("Username: ").append(user.getUsername()).append("\n")
                    .append("Address: ").append(user.getAddress()).append("\n")
                    .append("Port: ").append(user.getPort()).append("\n")
                    .append("-----\n"); // Separator for each user
        }

        // Update the TextArea
        Platform.runLater(() -> nameIPPort.setText(sb.toString()));
    }

    private void setupPortChangeListener(TableView<User> table) {
        // Get the items (users) from the table
        ObservableList<User> items = table.getItems();

        // Add a listener to each user's port property
        for (User user : items) {
            user.portProperty().addListener((observable, oldValue, newValue) -> {
                // Update the TextArea whenever the port changes
                updateTextArea();
            });
        }

        // Add a listener to the table items list for any future additions
        items.addListener((ListChangeListener<User>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (User user : change.getAddedSubList()) {
                        user.portProperty().addListener((observable, oldValue, newValue) -> {
                            // Update the TextArea whenever the port changes
                            updateTextArea();
                        });
                    }
                }
            }
        });
    }

    public static void writeUserToFile(String username, String password, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.newLine();
            writer.write(username + " " + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getLocalIPAddress() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress ia = inetAddresses.nextElement();
                if (!ia.isLoopbackAddress() && ia instanceof Inet4Address) {
                    return ia.getHostAddress();
                }
            }
        }
        return "No IP Address Found";
    }

    public void readUsersFromFile(String filename, TableView<User> table) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    String username = parts[0];
                    String password = parts[1];
                    User user = new User();
                    user.setStatus(false);
                    user.setUsername(username);
                    user.setPassword(password);
                    user.setID(ID++);


                    table.getItems().add(user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


 class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
     public void sendToUser(Message message, String ipAddress, int port) {
         try {
             Socket socket = new Socket(ipAddress, port);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
             outputStream.writeObject(message);
             outputStream.flush();
             outputStream.close();
             socket.close();
         } catch (IOException e) {
             // Handle exception
             e.printStackTrace();
         }
     }

        @Override
        public void run() {
            try (ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream())) {

                // Read messages from the client
                while (true) {
                    Message message = (Message) inputStream.readObject();
                    if (message.getType() == 0)
                    {
                        String[] parts = message.getMessage().split("\\s+");
                        User goal = null;
                        for (User u : table.getItems())
                        {
                            if (u.getUsername().equalsIgnoreCase(parts[0]) && u.getPassword().equalsIgnoreCase(parts[1])){
                                u.setStatus(true);
                                u.setAddress(message.getAddress());
                                u.setPort(message.getPort());
                                u.setRealPort(Integer.parseInt(parts[2]));
                                Platform.runLater(() -> {
                                    table.refresh();
                                    status.setText(u.getUsername() + " logged in, ip: " + u.getAddress()+ ", Port: "+ u.getPort());
                                });

                                Message toSend  = new Message("users");
                                toSend.setType((short) 1);
                                outputStream.writeObject(toSend);
                                outputStream.flush();
                                goal = u;
                                break;
                            }
                        }

                        if (goal != null) {
                            for (User u : table.getItems()) {
                                if (u.isStatus() && u != goal) {
                                    Message tmp = new Message(goal.getUsername());
                                    tmp.setType((short) 3);
                                    tmp.setPort(goal.getPort());
                                    tmp.setAddress(goal.getAddress());
                                    sendToUser(tmp, tmp.getAddress().toString().substring(1), u.getRealPort());
                                }
                            }
                        }
                        else
                        {  Message toSend  = new Message("users");
                            toSend.setType((short) 2);
                            outputStream.writeObject(toSend);
                            outputStream.flush();
                        }

                    } else if (message.getType() == 10) {
                        System.out.println(message.getMessage()+message.getType()+message.getUserStatus());
                        String[] parts = message.getMessage().split("\\s+");
                        User goal = null;
                        for (User u : table.getItems())
                        {
                            if (u.getUsername().equalsIgnoreCase(parts[0]) && u.getPassword().equalsIgnoreCase(parts[1])){
                                u.setUserStatus(message.getUserStatus());
                                u.setAddress(message.getAddress());
                                u.setPort(message.getPort());
                                u.setRealPort(Integer.parseInt(parts[2]));
                                Platform.runLater(() -> {
                                    table.refresh();
                                    status.setText(u.getUsername() + ": "+u.getUserStatus()+", ip: " + u.getAddress()+ ", Port: "+ u.getPort());
                                });
                                goal = u;
                                break;
                            }
                        }

                        if (goal != null) {
                            for (User u : table.getItems()) {
                                if (u.isStatus() && u != goal) {
                                    Message tmp = new Message(goal.getUsername());
                                    tmp.setType((short) 10);
                                    tmp.setPort(goal.getPort());
                                    tmp.setUserStatus(goal.getUserStatus());
                                    tmp.setAddress(goal.getAddress());
                                    sendToUser(tmp, tmp.getAddress().toString().substring(1), u.getRealPort());
                                }
                            }
                        }
                    }
                    else if (message.getType() == 5){
                        ArrayList<Message> arrayList = new ArrayList<>();
                        for (User u : table.getItems()) {
                            if (u.isStatus()) {
                                Message tmp = new Message(u.getUsername());
                                tmp.setPort(u.getPort());
                                tmp.setAddress(u.getAddress());
                                arrayList.add(tmp);

                            }
                        }
                        Message [] array = new Message[arrayList.size()];
                        arrayList.toArray(array);
                        outputStream.writeObject(array);
                        outputStream.flush();
                    } else if (message.getType() == 4) {
                        for (User u: table.getItems()){
                            if (u.getUsername().equalsIgnoreCase(message.getMessage()) && u.getPort() == u.getPort()) {
                                u.setStatus(false);
                                Platform.runLater(() -> {
                                    status.setText(u.getUsername() + " logged out, ip: " + u.getAddress()+ ", Port: "+ u.getPort());
                                });
                                break;
                            }
                        }
                        for (User u : table.getItems()) {
                            if (u.isStatus() && !u.getUsername().equalsIgnoreCase(message.getMessage())  && u.getPort() != message.getPort()) {
                                sendToUser(message, message.getAddress().toString().substring(1), u.getRealPort());
                            }
                        }
                        Platform.runLater(() -> {
                            table.refresh();
                        });
                    }


                }
            } catch (IOException | ClassNotFoundException ignored) {

            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



}
