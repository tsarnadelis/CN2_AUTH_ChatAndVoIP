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

    private boolean isCallActive = false;               // Flag to track call state
    private static final int CHAT_PORT = 8080;          // Port number for chat
	private static final int CALL_PORT = 8081;          // Port number for voice call
    private static DatagramSocket socket;               // Socket for client communicationges
    private static DatagramSocket callSocket;           // UDP socket for voice call
    private static String receiverIP = "localhost";     // IP address of the receiver
    

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

        // Start message listener
        chatListener();

        // Start call listener
        callListener();
    }

    // This listens for incoming messages all the time
    private static void chatListener() {
        new Thread(() -> {
            byte[] receiveBuffer = new byte[1024];

            while (true) {
                try {
                    socket = new DatagramSocket(CHAT_PORT);
                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(receivePacket);
                        String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        SwingUtilities.invokeLater(() -> textArea.append("Friend: " + message + newline));
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> textArea.append("Error receiving message: " + e.getMessage() + newline));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // This thread listens for incoming audio packets all the time
	private static void callListener() {
		new Thread(() -> {
            byte[] receiveCallBuffer = new byte[1024];
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
			DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
			
            while (true) {
                try {
                    SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
			        speakers.open(format);
                    callSocket = new DatagramSocket(CALL_PORT);
                    while (true) {
                        DatagramPacket receiveCallPacket = new DatagramPacket(receiveCallBuffer, receiveCallBuffer.length);
                        callSocket.receive(receiveCallPacket);
                        speakers.start(); // Start audio playback
                        speakers.write(receiveCallPacket.getData(), 0, receiveCallPacket.getLength());
                        // speakers.close(); // Speakers are always open. Uncommenting this makes the speakers not work
                    }
                } catch (IOException | LineUnavailableException e) {
                    if (e.getMessage().equals("Socket closed")) continue; // Ignore error when closing call
                    textArea.append("Error receiving call: " + e.getMessage() + newline);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }).start();
	}
	
    private void startCall() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
                microphone.open(format);
        
                byte[] buffer = new byte[1024];
                InetAddress receiverAddress = InetAddress.getByName(receiverIP);
                DatagramSocket sendSocket = new DatagramSocket();
                microphone.start();
                
                while(isCallActive) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);
                    DatagramPacket packet = new DatagramPacket(buffer, bytesRead, receiverAddress, CALL_PORT);
                    sendSocket.send(packet);
                }
                
                microphone.close();
                sendSocket.close();
    
            } catch (IOException | LineUnavailableException e) {
                textArea.append("Error during call: " + e.getMessage() + "\n");
            }
        }).start();
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
            if (!message.isEmpty()) {
                textArea.append("You: " + message + newline);
                try {
                    InetAddress receiverAddress = InetAddress.getByName(receiverIP);
                    byte[] sendBuffer = message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, receiverAddress, CHAT_PORT);
                    socket.send(sendPacket);
                    inputTextField.setText("");
                } catch (IOException ex) {
                    textArea.append("Error sending message: " + ex.getMessage() + newline);
                }
            } 
        }else if (e.getSource() == callButton) {
            if (!isCallActive) {
                textArea.append("Starting call...\n");
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
