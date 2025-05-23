package com.ijse.inpexamprojectchatapplication.Client;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Client {
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String clientName;
    private boolean nameAccepted = false;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss  a");
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMMM-yyyy");

    @FXML
    private Button btnSend;

    @FXML
    private Label lblTime;

    @FXML
    private Label lblDate;

    @FXML
    private Label client;

    @FXML
    private ListView<Object> messageView;

    @FXML
    private TextArea txtArea;

    @FXML
    void BtnSendOnAction(ActionEvent event) {
        String message = txtArea.getText().trim();
        if (message.isEmpty()) return;
        try {
            out.writeObject(message);
            out.flush();
            txtArea.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initialize(){
        messageView.setCellFactory(listView -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (item instanceof String) {
                    setText((String) item);
                    setGraphic(null);
                }

                updateDateTime();
            }
        });


        try {
            Socket socket = new Socket("localhost", 5000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Thread thread = new Thread(() -> listenForMessages());
            thread.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //Method to close the connection
    public void closeConnection() {
        try {
            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateDateTime() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    String currentDate = LocalDate.now().format(formatter);
                    lblTime.setText(currentDate);
                    String currentTime = LocalTime.now().format(timeFormatter);
                    lblDate.setText(currentTime);
                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

    }

    //Client Name
    private void promptForName(){
        Platform.runLater(()->{
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Enter your name");
            dialog.setHeaderText("Please enter your name");

            dialog.showAndWait().ifPresent(name ->{
                clientName = name.trim();
                if (clientName.isEmpty()){
                    messageView.getItems().add("Enter a Valid Name");
                    promptForName();
                }else {
                    try {
                        out.writeObject(clientName);
                        client.setText(clientName);
                        out.flush();
                        dialog.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        messageView.getItems().add("Name not sent");
                    }
                }


            });

            if (dialog.getResult().isEmpty()){
                Platform.exit();
            }
        });
    }

    private void listenForMessages() {
        try {
            while(true){
                Object message = in.readObject();
                if (message == null) break;
                if (message instanceof String){
                    String text = (String) message;
                    if (text.startsWith("SUBMITNAME")){
                        if (!nameAccepted){
                            promptForName();
                        }
                    }else if (text.startsWith("NAMEACCEPTED")){
                        nameAccepted = true;
                        Platform.runLater(() -> messageView.getItems().add("Connected as " + clientName));
                    }else if (text.startsWith("TEXT")){
                        Platform.runLater(() ->{
                            if (text.startsWith("TEXT " + clientName + ": ")){
                                messageView.getItems().add("You: " + text.substring(clientName.length()+2+5));
                            }else {
                                messageView.getItems().add(text.substring(5));
                            }
                        });
                    } else if (text.startsWith("IMAGE")) {
                        byte[] imageData = (byte[]) in.readObject();
                        Image image = new Image(new ByteArrayInputStream(imageData));
                        Platform.runLater(() -> {
                            messageView.getItems().add(text.substring(6) + " sent an image");
                            messageView.getItems().add(image);
                        });
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Platform.runLater(() -> messageView.getItems().add("Disconnected" + e.getMessage()));
        }finally {
            closeConnection();
        }
    }

}
