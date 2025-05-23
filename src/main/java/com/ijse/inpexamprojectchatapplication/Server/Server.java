package com.ijse.inpexamprojectchatapplication.Server;

import com.ijse.inpexamprojectchatapplication.Client.Client;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
    private static final int PORT = 5000;
    private boolean isRunning = false;
    private ServerSocket serverSocket;
    private static HashSet<ObjectOutputStream> hashSet = new HashSet<>();
    private Timer timer;

    @FXML
    private Button btnClient;

    @FXML
    private TextArea txtBody;

    @FXML
    void btnClientOnAction(ActionEvent event) {
        if (!isRunning){
            serverStart();
        }

        openClientWindow();
    }

    @FXML
    public void initialize() {
        showStatus("Server Started. Add a client to continue.");
    }

    // This is a private class to handle the clients
    private class ClientHandler implements Runnable{
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String clientName;

        ClientHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                while(true){
                    out.writeObject("SUBMITNAME");
                    clientName = (String) in.readObject();
                    if (!clientName.trim().isEmpty() && clientName != null){
                        break;
                    }
                    showStatus("Invalid name, requesting again");
                }

                out.writeObject("NAMEACCEPTED");
                showStatus("Client " + clientName + " connected");
                broadcast("TEXT " + clientName + " joined, Say Hii!");

                synchronized (hashSet){
                    hashSet.add(out);
                }

                while(true){
                    Object message = in.readObject();
                    if (message == null) break;
                    if (message instanceof String){
                        String text = (String) message;
                        broadcast("TEXT " + clientName + ": " +text);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                showStatus("Error connecting to client");
            }finally {
                if (clientName != null){
                    showStatus("Client " + clientName + " disconnected");
                    broadcast("TEXT " + clientName + " left the chat");
                }

                synchronized (hashSet){
                    hashSet.remove(out);
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    showStatus("Error closing client socket");
                    e.printStackTrace();
                }
            }
        }
        // Method to broadcast the message
        private void broadcast ( String message){
            synchronized (hashSet){
                for (ObjectOutputStream writer : hashSet){
                    try {
                        writer.writeObject(message);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showStatus("Error broadcasting the message..");
                    }
                }
            }
        }
    }

    // This Method is to put messages to client body
    void showStatus(String message) {
        Platform.runLater(() -> txtBody.appendText(message + "\n"));
    }

    // This Method is to start a new server with a new thread
    private void serverStart() {
        isRunning = true;

        //Open a new thread
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                showStatus("Server started on port " + PORT);

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    showStatus("New client connected");

                    Thread clientThread = new Thread(new ClientHandler(clientSocket));
                    clientThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (isRunning) {
                    showStatus("Error starting server: " + e.getMessage());
                }
            }
        }).start();
    }

    // This method is to open a new client window when add client button is pressed
    private void openClientWindow() {
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Client.fxml"));
                Scene scene = new Scene(loader.load());
                Stage stage = new Stage();
                stage.setTitle("Chat Client");
                stage.setScene(scene);
                ;               stage.setOnCloseRequest(event -> {
                    Client controller = loader.getController();
                    controller.closeConnection();
                });
                stage.show();
                showStatus("New client window opened");
            } catch (IOException e) {
                showStatus("Error opening client window");
                e.printStackTrace();
            }
        });
    }
}
