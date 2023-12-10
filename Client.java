// Java implementation for multithreaded chat client 
// Save file as Client.java 

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
	static int serverPort;
	static InetAddress serverIP;

	public static void main(String args[]) throws UnknownHostException, IOException {
		Scanner scn = new Scanner(System.in);

		// Get the user name first
		System.out.println("Please enter your user name");
		String userName = scn.nextLine();

		// We would only allow the user to enter %connect or %logout command
		while (true) {
			System.out.println("Please use %connect command to connect to the message server.");
			String received = scn.nextLine();

			// if the command entered is "%connect ip serverport"
			if (received.startsWith("%connect")) {
				try {
					String rest = received.split(" ", 2)[1];
					String ip = rest.split(" ")[0];
					serverIP = InetAddress.getByName(ip);
					serverPort = Integer.parseInt(rest.split(" ")[1]);
					break;
				} catch (Exception e) {
					System.out.println("The %connect command you have entered might be in wrong format.\n" +
							"The correct format is %connect [ip] [port]");
				}
			} else if (received.equals("%logout")) { // The command is %logout
				System.exit(0);
			} else {
				System.out.println(
						"The command you have entered is invalid currently. Only these commands are valid at the moment:\n"
								+
								"%connect [ip] [port]\n%logout");
			}

		}
		// establish the connection
		Socket s = new Socket(serverIP, serverPort);
		System.out.println("Connection is done! %help for more commands on server");

		// obtaining input and out streams
		DataInputStream dis = new DataInputStream(s.getInputStream());
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());

		// Send the username to the server
		dos.writeUTF(userName);

		// sendMessage thread
		Thread sendMessage = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {

					// read the message to deliver.
					String msg = scn.nextLine();

					try {
						// write on the output stream
						dos.writeUTF(msg);
						if (msg.equals("%logout")) {
							System.exit(0);
						}
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
			}
		});

		// readMessage thread
		Thread readMessage = new Thread(new Runnable() {
			@Override
			public void run() {

				while (true) {
					try {
						// read the message sent to this client
						String msg = dis.readUTF();
						System.out.println(msg);
					} catch (IOException e) {
						// e.printStackTrace();
						break;
					}
				}
			}
		});

		sendMessage.start();
		readMessage.start();

	}
}