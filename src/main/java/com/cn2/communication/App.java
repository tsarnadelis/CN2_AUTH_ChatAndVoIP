package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.*;
import java.awt.event.*;
import java.lang.Thread;

public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;				
	
	private boolean isCallActive = false; // Flag to track call state
    private static final int PORT = 8080; // Port number for communication
    private static Socket socket;              // Socket for client communication
	private static PrintWriter writer;         // Writer for sending messages
	private static BufferedReader reader;      // Reader for receiving messages
			
			/**
			 * Construct the app's frame and initialize important parameters
			 */
			public App(String title) {
				
				/*
				 * 1. Defining the components of the GUI
				 */
				
				// Setting up the characteristics of the frame
				super(title);									
				gray = new Color(254, 254, 254);		
				setBackground(gray);
				setLayout(new FlowLayout());			
				addWindowListener(this);	
				
				// Setting up the TextField and the TextArea
				inputTextField = new TextField();
				inputTextField.setColumns(20);
				
				// Setting up the TextArea.
				textArea = new JTextArea(10,40);			
				textArea.setLineWrap(true);				
				textArea.setEditable(false);			
				JScrollPane scrollPane = new JScrollPane(textArea);
				scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				
				//Setting up the buttons
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
			
			/**
			 * The main method of the application. It continuously listens for
			 * new messages.
			 */
			public static void main(String[] args){
			
				/*
				 * 1. Create the app's window
				 */
				App app = new App("CN2 - AUTH");																	  
				app.setSize(500,250);				  
				app.setVisible(true);				  
		
				/*
				 * 2. If server is not found, open a new socket
				 */
				try (ServerSocket serverSocket = new ServerSocket(PORT)){	
					textArea.append("Socket not found, opening new socket at " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort() + "\n");
					while (true) {
						Socket clientSocket = serverSocket.accept();
						textArea.append("Client connected.\n");
						writer = new PrintWriter(clientSocket.getOutputStream(), true);
						reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	
						// Continuously listen for messages
						String incomingMessage;
						while ((incomingMessage = reader.readLine()) != null) {
							textArea.append("Friend: " + incomingMessage + "\n");
						}
					}
				} catch (Exception e) {
					textArea.append("Cannot open socket. Looking...\n");
				}
				
				/*
				 * 3. If server is found, connect to the server
				 */
				try {	
					textArea.append("Socket found, connecting...\n");
					while (true) {
						socket = new Socket("localhost", PORT);
						writer = new PrintWriter(socket.getOutputStream(), true);
						reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						textArea.append("Connected to the server at " + socket.getInetAddress() + ":" + socket.getPort() + "\n");

						// Continuously listen for messages
						String incomingMessage;
						while ((incomingMessage = reader.readLine()) != null) {
                    	textArea.append("Friend: " + incomingMessage + "\n");
                	}
				}
				} catch (Exception e) {
					textArea.append("Cannot open socket.\n");
				}
	}
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
	

		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton) {
            // Send button clicked
            String message = inputTextField.getText().trim();
            if (!message.isEmpty() && writer != null) {
                textArea.append("You: " + message + "\n");
                writer.println(message); // Send message to the other client
                inputTextField.setText(""); // Clear the input field
            }
        } else if (e.getSource() == callButton) {
             // Simulate voice call
			 if (!isCallActive) {
                textArea.append("Voice call started...\n");
                isCallActive = true;
                callButton.setText("End Call");
            } else {
                textArea.append("Voice call ended...\n");
                isCallActive = false;
                callButton.setText("Call");
            }
		}
	}

	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
