package com.cn2.communication;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class App extends Frame implements WindowListener, ActionListener {

    /*
     * Definition of the app's fields
     */
    static TextField inputTextField;
    static JTextArea textArea;
    static JFrame frame;
    static JButton sendButton;
    static JTextField messageTextField;
    public static Color gray;
    final static String newline = "\n";
    static JButton callButton;

    private boolean isCallActive = false; // Flag to track call state
    private static final int PORT = 8080;      // Port number for chat
	private static final int CALL_PORT1 = 8081; // Port number for voice call
	private static final int CALL_PORT2 = 8082; // Port number for voice call
    private static Socket socket;             // Socket for client communication
    private static PrintWriter writer;        // Writer for sending messages
    private static BufferedReader reader;     // Reader for receiving messages

    private DatagramSocket callSocket;        // UDP socket for voice call


    public App(String title) {
        /*
         * 1. Defining the components of the GUI
         */
        super(title);

        // Setting up the characteristics of the frame
        gray = new Color(254, 254, 254);
        setBackground(gray);
        setLayout(new FlowLayout());
        addWindowListener(this);

        // Setting up the TextField and the TextArea
        inputTextField = new TextField();
        inputTextField.setColumns(20);

        // Setting up the TextArea.
        textArea = new JTextArea(10, 40);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Setting up the buttons
        sendButton = new JButton("Send");
        callButton = new JButton("Call");

        /*
         * 2. Adding the components to the GUI
         */
        add(scrollPane);
        add(inputTextField);
        add(sendButton);
        add(callButton);

        /*
         * 3. Linking the buttons to the ActionListener
         */
        sendButton.addActionListener(this);
        callButton.addActionListener(this);
    }

    public static void main(String[] args) {
        App app = new App("CN2 - AUTH");
        app.setSize(500, 250);
        app.setVisible(true);

        // Start server or connect to server in a new thread
        new Thread(app::startChat).start();
    }

    private void startChat() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            textArea.append("Open socket not found, opening a new one at "+ serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort() +"\n");
            socket = serverSocket.accept();
			setTitle("CN2 - SERVER");
            textArea.append("Client connected.\n");
            setupChatStreams(socket);
            listenForMessages();
			startCallReceiver();
        } catch (IOException e) {
            try {
                textArea.append("Trying to connect to server...\n");
                socket = new Socket("localhost", PORT);
				setTitle("CN2 - CLIENT");
                textArea.append("Connected to server.\n");
                setupChatStreams(socket);
                listenForMessages();
				startCallReceiver();
            } catch (IOException ex) {
                textArea.append("Error: Could not connect to server.\n");
            }
        }
    }

    private void setupChatStreams(Socket socket) throws IOException {
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void listenForMessages() {
        new Thread(() -> {
            try {
                String incomingMessage;
                while ((incomingMessage = reader.readLine()) != null) {
                    textArea.append("Friend: " + incomingMessage + "\n");
                }
            } catch (IOException e) {
                textArea.append("Connection lost.\n");
            }
        }).start();
    }

    // This thread listens for incoming audio packets all the time
	private void startCallReceiver() {
		boolean isServer = this.getTitle().equalsIgnoreCase("CN2 - SERVER");
		int receivePort = isServer ? CALL_PORT2 : CALL_PORT1;
	
		try {
			callSocket = new DatagramSocket(receivePort);
			AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
			DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
			SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
			speakers.open(format);
	
			Thread receiveThread = new Thread(() -> {
				byte[] receiveBuffer = new byte[1024];
				while (true) {
					try {
						DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
						callSocket.receive(receivePacket);
	
						if (!isCallActive) {
							int response = JOptionPane.showConfirmDialog(
								null,
								"Incoming call. Do you want to connect?",
								"Incoming Call",
								JOptionPane.YES_NO_OPTION
							);
	
							if (response == JOptionPane.YES_OPTION) {
								isCallActive = true;
								textArea.append("Call connected.\n");
								speakers.start(); // Start audio playback
							} else {
								textArea.append("Call rejected.\n");
								continue; // Ignore the packet
							}
						}
	
						if (isCallActive) {
							speakers.write(receivePacket.getData(), 0, receivePacket.getLength());
						}
					} catch (IOException e) {
						textArea.append("Error during call (receiving): " + e.getMessage() + "\n");
						break;
					}
				}
				speakers.close();
			});
	
			receiveThread.start();
		} catch (LineUnavailableException | SocketException e) {
			textArea.append("Error initializing call receiver: " + e.getMessage() + "\n");
		}
	}
	

	private void startCall() {
		boolean isServer = this.getTitle().equalsIgnoreCase("CN2 - SERVER");
		int sendPort = isServer ? CALL_PORT1 : CALL_PORT2;
	
		try {
			AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
			DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
			TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
			microphone.open(format);
	
			Thread sendThread = new Thread(() -> {
				byte[] buffer = new byte[1024];
				try {
					InetAddress receiverAddress = InetAddress.getByName("localhost"); // Replace with actual receiver's IP
					DatagramSocket sendSocket = new DatagramSocket(); // Use ephemeral port for sending
					microphone.start();
	
					while (isCallActive) {
						int bytesRead = microphone.read(buffer, 0, buffer.length);
						DatagramPacket packet = new DatagramPacket(buffer, bytesRead, receiverAddress, sendPort);
						sendSocket.send(packet);
					}
					sendSocket.close();
				} catch (IOException e) {
					textArea.append("Error during call (sending): " + e.getMessage() + "\n");
				} finally {
					microphone.close();
				}
			});
	
			isCallActive = true;
			textArea.append("Call started.\n");
			sendThread.start();
		} catch (LineUnavailableException e) {
			textArea.append("Error during call setup: " + e.getMessage() + "\n");
		}
	}
	

	private void endCall() {
		if (isCallActive) {
			isCallActive = false;
			if (callSocket != null && !callSocket.isClosed()) {
				callSocket.close();
			}
			textArea.append("Call ended.\n");
			callButton.setText("Call");
		}
	}
	


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton) {
            // Send button clicked
            String message = inputTextField.getText().trim();
            if (!message.isEmpty() && writer != null) {
                textArea.append("You: " + message + "\n");
                writer.println(message); // Send message to the other client
                inputTextField.setText(""); // Clear the input field
            }
        } else if (e.getSource() == callButton) {
            if (!isCallActive) {
                isCallActive = true;
                callButton.setText("End Call");
                startCall();
            } else {
                callButton.setText("Call");
                endCall();
            }
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        dispose();
        System.exit(0);
    }

    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowOpened(WindowEvent e) {}
}
