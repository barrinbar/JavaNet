package chat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import chat.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class ChatClient extends Application {

	private TextField tfName = new TextField();
	private TextField tfMsg = new TextField();
	private TextArea ta = new TextArea();
	private ListView<String> lvClients = new ListView<>();

	private String name;
	private ClientsList clients = null;
	private Socket socket = null;

	// Host name or ip
	String host = "localhost";

	@Override // Override the start method in the Application class
	public void start(Stage primaryStage) {
		GridPane inputPane = new GridPane();
		inputPane.add(new Label("Name: "), 0, 0);
		inputPane.add(tfName, 1, 0);
		inputPane.add(new Label("Message: "), 0, 1);
		inputPane.add(tfMsg, 1, 1);
		tfName.setPrefColumnCount(20);
		tfMsg.setPrefColumnCount(20);
		tfMsg.setDisable(true);

		BorderPane textPane = new BorderPane();
		ta.setEditable(false);
		textPane.setCenter(ta);
		textPane.setTop(inputPane);
		
		BorderPane mainPane = new BorderPane();
		mainPane.setLeft(textPane);
		mainPane.setRight(lvClients);
		lvClients.setPrefWidth(10);
		Platform.runLater(() -> ta.appendText("Welcome, please enter your name to start chatting\n"));

		// Create a scene and place it in the stage
		Scene scene = new Scene(mainPane, 450, 200);
		primaryStage.setTitle("Chat Client"); // Set the stage title
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show(); // Display the stage
		primaryStage.setAlwaysOnTop(true);

		tfName.setOnAction(e -> {
			// If name is not empty - keep it
			if (tfName.getText().trim() != "") {

				name = tfName.getText().trim();

				// Establish connection with the server
				//Socket socket;
				try {
					socket = new Socket(host, 8000);

					// Create an output stream to the server
					DataOutputStream toServer = new DataOutputStream(socket.getOutputStream());
					ObjectInputStream fromServer = new ObjectInputStream(socket.getInputStream());

					toServer.writeChars(name);

					clients = (ClientsList)fromServer.readObject();

					if (clients == null) {
						Platform.runLater(() -> {
							ta.appendText("The name " + name + " is already taken, please choose a different name.\n");
							tfName.setText("");
							tfName.requestFocus();
						});
					} else {
						Platform.runLater(() -> {
							// Update clients list and enable chatting
							lvClients.setItems((ObservableList<String>) clients.getNamesList());
							ta.appendText("Hello " + name + "\nYou are now connected to the chat.\n");
							tfName.setDisable(true);
							tfMsg.setDisable(false);
						});

						new Thread(new ChatListener(socket)).start();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		tfMsg.setOnAction(new MessageListener(socket));
	}

	@Override
	public void stop() throws Exception {
		// Inform server of disconnection
		//Socket socket = new Socket(host, 8000);
		
		if (this.socket != null) {
			// Create an output stream to the server
			ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());
	
			// Send a null indicator to the server
			toServer.writeObject(null);
		}
	}

	// Handle message action
	private class MessageListener implements EventHandler<ActionEvent> {
		
		Socket socket;
		public MessageListener(Socket socket) {
			this.socket = socket;
		}
		@Override
		public void handle(ActionEvent e) {
			try { // Establish connection with the server
				//Socket socket = new serverSocket(host, 8000);
				// Create an output stream to the server
				ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());

				// Get text fields
				Message message = new Message(name, tfMsg.getText().trim(), null);

				// Validate input
				if (message.getMsg() == "")
					// ignore
					;
				else {
					// Send loan request to the server
					toServer.writeObject(message);
					Platform.runLater(() -> tfMsg.setText(""));
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private class ChatListener implements Runnable {

		private ObjectInputStream fromServer;
		private Message message;

		ChatListener(Socket socket) throws IOException {
			this.fromServer = new ObjectInputStream(socket.getInputStream());
		}

		@Override
		public void run() {
			// Create an output stream to the server
			while (true) {
				try {
					// Check if the server has sent a new message or an updated
					// clients list
					Object obj = fromServer.readObject();

					if (obj instanceof Message) {
						message = (Message) fromServer.readObject();
						Platform.runLater(() -> {
							ta.appendText(message.toString() + '\n');
						});
					} else if (obj instanceof HashMap) {
						clients = (ClientsList) obj;
						Platform.runLater(() ->
								lvClients.setItems((ObservableList<String>) clients.getNamesList()));
					} else
						throw new ClassNotFoundException(
								"Class " + obj.getClass() + " not expected. Should be either Message or Hashmap");
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}