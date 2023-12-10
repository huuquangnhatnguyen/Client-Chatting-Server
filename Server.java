import java.io.*;
import java.util.*;
import java.net.*;
import java.time.LocalDate;

// Server class
public class Server {

	// Vector to store active clients. Vector is also fexible in size.
	static Vector<ClientHandler> activeClients = new Vector<>();

	// Vector to store the server messages.
	static Vector<Message> msgs = new Vector<>();

	// Hashmap to store 5 private boards
	static final Map<String, PrivateMessageBoard> privateBoards = new HashMap<>();

	// create 5 groups for clients to join
	static {
		// Initialize 5 private boards
		for (int i = 1; i <= 5; i++) {
			String boardName = "PrivateBoard" + i;
			PrivateMessageBoard privateBoard = new PrivateMessageBoard(boardName);
			// Put the PrivateMessageBoard object to the map with their name as keys
			privateBoards.put(boardName, privateBoard);
		}
	}

	// counter for message number
	static int messageID = 0;

	public static void main(String[] args) throws IOException {
		// server is listening on port 1234
		ServerSocket serverSocket = new ServerSocket(1234);
		Socket clientSocket;
		// running infinite loop for getting
		// client request
		while (true) {
			// Accept the incoming request
			clientSocket = serverSocket.accept();
			System.out.println("New client request received : " + clientSocket);

			System.out.println("Creating a new handler for this client...");
			// Create a new handler object for handling this request.
			ClientHandler mtch = new ClientHandler(clientSocket);

			// Create a new Thread with this object.
			Thread clientThread = new Thread(mtch);
			System.out.println("Adding this client to active client list...");

			// add this client to active clients list
			activeClients.add(mtch);

			// start the thread.
			clientThread.start();

		}
	}
}

// ClientHandler class
class ClientHandler implements Runnable {
	Scanner scn = new Scanner(System.in);
	private String username;
	private DataInputStream dis;
	private DataOutputStream dos;
	Socket clientSocket;
	boolean isloggedin;
	boolean onBoard;
	private Map<String, PrivateMessageBoard> privateBoards;

	// constructor
	public ClientHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
		this.privateBoards = new HashMap<>();
		try {
			// obtain input and output streams
			this.dis = new DataInputStream(clientSocket.getInputStream());
			this.dos = new DataOutputStream(clientSocket.getOutputStream());

			// take username from the client
			this.username = dis.readUTF();

		} catch (IOException e) {
			e.printStackTrace();
		}
		// set a boolean value to show that the client is logged in and active.
		// This will be turned to be false when the client logs out
		this.isloggedin = true;
		// set this to be false since the the client has not join in the public chat
		// yet.
		// the client cannot post or retrieve message if not onboard.
		this.onBoard = false;
	}

	@Override
	public void run() {

		String received;
		while (true) {
			try {
				// receive the string
				received = dis.readUTF();
				System.out.println(received);

				if (received.equals("%logout")) {
					for (ClientHandler mc : Server.activeClients) {
						mc.dos.writeUTF(this.username + " has logged out!");
					}
					//Remove this clienthandler from the list
					Server.activeClients.remove(this);
					this.isloggedin = false;
					this.clientSocket.close();
					break;
				}

				// detect the command and run accordingly
				String command = received.split(" ", 2)[0];
				String rest;
				try {
					rest = received.split(" ", 2)[1];
				} catch (Exception e) {
					rest = "";
				}

				switch (command) {
					case "%join":

						//Prints a list of users in the public board.
						printUsers();

						//Alerts other members
						for(ClientHandler mc : Server.activeClients){
							if(mc.onBoard){
								mc.dos.writeUTF(this.username + " has joined the public board!");
							}
						}	
						
						this.onBoard = true;
						// Prints the last 2 messages
						try {
							Message secondLast = Server.msgs.get(Server.msgs.size() - 2);
							dos.writeUTF(secondLast.messageInfo());
						} catch (Exception e) {
						}
						try {
							Message last = Server.msgs.lastElement();
							dos.writeUTF(last.messageInfo());
						} catch (Exception e) {
							dos.writeUTF("Message history is empty\n");
						}
						
						
						
						break;

					case "%users": // Prints a list of all users to the client
						printUsers();
						break;

					case "%post": // Posts a message to the message board
						try {
							if (this.onBoard) {
								postMessage(rest);
								break;
							} else {
								dos.writeUTF("You have to join the public board to post message!");
							}
						} catch (Exception e) {
						}

					case "%leave": // leave the public board
						this.onBoard = false;
						for (ClientHandler mc : Server.activeClients) {
							if (mc.onBoard) {
								mc.dos.writeUTF(this.username + " has left the public chat room!");
							}
						}
						break;

					case "%message": // Retrieves the contents of the message
						try {
							if (this.onBoard) {
								retrieveMessage(rest);
								break;
							} else {
								dos.writeUTF("You have to join the public board to retrieve message!");
							}
						} catch (Exception e) {
						}
						break;

					case "%help": // Provides a list of commands.
						printHelp();
						break;

					case "%groups":
						showAllGroups();
						break;

					case "%groupjoin":
						joinPrivateBoard(rest);
						break;

					case "%grouppost":
						postMessageToGroup(rest);
						break;

					case "%groupusers":
						printGroupUsers(rest);
						break;

					case "%groupleave":
						leaveGroup(rest);
						break;

					case "%groupmessage":
						retrieveGroupMessage(rest);
						break;

					default: // Invalid command
						dos.writeUTF(command + "is inavlid. type %help to see the list of commands available");
						break;

				}

			} catch (IOException e) {

				//e.printStackTrace();
			}

		}
		try {
			// closing resources
			this.dis.close();
			this.dos.close();

		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

	// public board commands and functions
	// %users to print all usernames
	public void printUsers() {
		try {
			if (Server.activeClients.size() == 0) { // First to join
				dos.writeUTF("You are the first user in the public board!");
			} else { // Otherwise print the users currently in the board
				String listOfUsers = "\nThe list of users in the public board: \n";
				for (ClientHandler mc : Server.activeClients) {
					// only print the users is on public board
					if (mc.onBoard) {
						listOfUsers += mc.username + "\n";
					}
				}
				dos.writeUTF(listOfUsers);
			}

		} catch (Exception e) {
		}

	}

	// %post to post message to the public board
	public void postMessage(String inlineMessage) {
		try {
			String subject = inlineMessage.split(" : ", 2)[0];
			String message = inlineMessage.split(" : ", 2)[1];
			Message m = new Message(subject, message, this.username, Server.messageID);
			Server.messageID++;
			Server.msgs.add(m);

			for (ClientHandler mc : Server.activeClients) {
				if (mc.onBoard) {
					mc.dos.writeUTF(m.messageInfo());
				}
			}
		} catch (Exception e) {
		}
	}

	// %message to retrieve message from the public Board with message ID
	public void retrieveMessage(String messageID) {
		try {
			boolean messageFound = false;
			for (Message msg : Server.msgs) {
				if (messageID.equals(String.valueOf(msg.messageID))) {
					dos.writeUTF("Public: " + msg.subject + ": " + msg.message);
					messageFound = true;
					break;
				}
			}
			if (!messageFound)// The message has not been found
				dos.writeUTF(messageID
						+ " is either not a number or there isn't any message with that value as an ID!");
		} catch (Exception e) {
		}
	}

	// %help to show more information about the groups
	public void printHelp() {
		try {
			String commands = "\nList of Commands:\n" +
					"%join - Join the public message board\n" +
					"%post [subject] : [message] - posts the message to the public message board\n" +
					"%users - Provides a list of users inside the public group\n" +
					"%leave - leave the public group\n" +
					"%message [message number (ex: 1, 100, etc.)] - retrieves message whose ID is provided\n"
					+
					"%groups - retrieve a list of all groups that can be joined\n" +
					"%groupjoin [Group name] - Provides a list of active users\n" +
					"%grouppost [Group name] [subject] : [message] - Provides a list of active users\n" +
					"%groupusers  [Group name]- Provides a list of active users\n" +
					"%groupleave  [Group name] - Provides a list of active users\n" +
					"%groupmessage [Group name] [MessageID] - Provides a list of active users\n" +
					"%logout - logs you out of the server.\n";
			dos.writeUTF(commands);
		} catch (Exception e) {
		}
	}

	// private boards commands
	// %groupjoin to join a private messaging board
	public void joinPrivateBoard(String privateBoardName) {
		try {
			if (Server.privateBoards.containsKey(privateBoardName)) {
				// Logic to join a predefined private board
				PrivateMessageBoard privateBoard = Server.privateBoards.get(privateBoardName);
				// use addMember method inside the PrivateMessageBoard class to add a new member
				// to the list of member of the group
				privateBoard.addMember(this.username);
				this.privateBoards.put(privateBoardName, privateBoard);
				// Notify the client about the successful join
				groupBroadcasting(("Welcome " + this.username + " to " + privateBoardName + " !"), privateBoard);
				;
				// print last 2 message
				try {
					Message secondLast = (Message) privateBoard.getMessages()
							.get(privateBoard.getMessages().size() - 2);
					dos.writeUTF(secondLast.messageInfo());
				} catch (Exception e) {
				}
				try {
					Message last = (Message) privateBoard.getMessages().get(privateBoard.getMessages().size() - 1);
					dos.writeUTF(last.messageInfo());
				} catch (Exception e) {
					dos.writeUTF("\nMessage history is empty\n");
				}
			} else {
				// Notify the client that the private board does not exist
				dos.writeUTF("Board does not exist! Please try again.");
			}

		} catch (Exception e) {
		}
	}

	// %groups to show all groups inside the server
	public void showAllGroups() {
		try {
			// create a new StringBuilder object that will store all the available private
			// messaging board
			StringBuilder boardList = new StringBuilder("Available Private Boards:\n");
			// iterate through the keySet of the privateBoards hashmap to collects all
			// boards' names
			// then append to boardList before converting it to String and output on the
			// screen
			for (String groupName : Server.privateBoards.keySet()) {
				boardList.append(groupName).append("\n");
			}
			dos.writeUTF(boardList.toString());
		} catch (Exception e) {
		}
	}

	// %grouppost to post a message to a private group
	public void postMessageToGroup(String inlineMessage) {
		try {
			// %grouppost [Group name] [subject] : [message]
			// we need to split the arguments into sentoGroupName, subject of the message
			// and the messageID inside the group
			String sentoGroup = inlineMessage.split(" ", 2)[0];
			String rest = inlineMessage.split(" ", 2)[1];
			String subject = rest.split(" : ", 2)[0];
			String message = rest.split(" : ", 2)[1];
			// First, iterate through all the available boards and find the board that the
			// client want to send to
			for (String groupName : privateBoards.keySet()) {
				if (sentoGroup.equals(groupName)) {
					// Logic to post a message to a group
					PrivateMessageBoard privateBoard = Server.privateBoards.get(sentoGroup);
					// We also need to check if the username is a member of this private group or
					// not?
					// if yes, we need to create a new message
					if (privateBoard.getMembers().contains(this.username)) {
						Message m = new Message(subject, message, this.username, privateBoard.getGroupMessageID());
						// then add the message to the list of message and increase the group messageID
						// by 1
						privateBoard.increaseMessageID(m);
						privateBoard.addMessage(m);
						// output the message on screen
						groupBroadcasting(groupName + " : " + m.messageInfo(), privateBoard);
						// if no, we inform the client that he/she cant post here!
					} else {
						dos.writeUTF("You need to join this board first!");
					}
					break;
				}
			}
		} catch (Exception e) {
		}
	}

	// %groupleave [GroupName] is to leave a private group
	public void leaveGroup(String groupToLeave) {
		try {
			// find the group name in the hashmap
			for (String groupName : privateBoards.keySet()) {
				if (groupName.equals(groupToLeave)) {
					// check if the client is in the board or not.
					PrivateMessageBoard privateBoard = Server.privateBoards.get(groupToLeave);
					if (privateBoard.getMembers().contains(this.username)) {
						privateBoard.removeMember(this.username);
						groupBroadcasting((this.username + " has left the " + groupToLeave +" chat!"), privateBoard);
					} else {
						// if not, just inform the client
						dos.writeUTF("You are not in this group!");
					}
				}
			}
		} catch (Exception e) {
		}
	}

	// %groupmessage [GroupName] [GroupMessageID] will retrieve the message inside
	// the private group with the group message ID.
	public void retrieveGroupMessage(String groupMessageID) {
		try {
			// set a boolean value to manage if the message is found or not?
			boolean messageFound = false;
			// split the arguments
			String sentoGroup = groupMessageID.split(" ", 2)[0];
			String messageID = groupMessageID.split(" ", 2)[1];
			// find the groupName in the hashmap of private messaging boards
			for (String groupName : privateBoards.keySet()) {
				if (sentoGroup.equals(groupName)) {
					PrivateMessageBoard privateBoard = Server.privateBoards.get(sentoGroup);
					// We also need to check if the username is a member of this private group or
					// not?
					// if yes, we continue on retrieving the message
					if (privateBoard.getMembers().contains(this.username)) {
						// temporarily store the list of message in the private Board in
						List<Message> tempListOfMessages = privateBoard.getMessages();
						// find the message with the matched message ID
						for (Message msg : tempListOfMessages) {
							// output the message if found
							if (messageID.equals(String.valueOf(msg.messageID))) {
								dos.writeUTF(groupName + " : " + msg.subject + ": " + msg.message);
								messageFound = true;
								break;
							}
						}
						// if no, we inform the client that he/she cant post here!
					} else {
						dos.writeUTF("You need to join this board first!");
						break;
					}

				}
			}

			if (!messageFound)// The message has not been found
				dos.writeUTF(messageID
						+ " is either not a number or there isn't any message with that value as an ID!");
		} catch (Exception e) {
		}

	}

	public void printGroupUsers(String targetGroup) {
		try {
			for (String groupName : privateBoards.keySet()) {
				if (targetGroup.equals(groupName)) {
					PrivateMessageBoard privateBoard = Server.privateBoards.get(targetGroup);
					if (!privateBoard.getMembers().isEmpty()) {// First to join privateBoard.getMembers().size() == 0
						String listOfUsers = "\nThe list of users in this message board: \n";
						List<String> tempListOfMembers = privateBoard.getMembers();
						for (String member : tempListOfMembers) {
							listOfUsers += member + "\n";
						}
						dos.writeUTF(listOfUsers);
					} else {
						dos.writeUTF("There is no one in this chat board!");
					}
					break;
				}
			}
		} catch (Exception e) {
		}
	}

	public void groupBroadcasting(String message, PrivateMessageBoard board) {
		try {
			for (ClientHandler mc : Server.activeClients) {
				if (board.getMembers().contains(mc.username)) {
					mc.dos.writeUTF(message);
				}
			}
		} catch (Exception e) {
		}
	}
}

// Message Class
class Message {
	String subject;
	String message;
	String sender;
	LocalDate dateCreated;
	int messageID;

	public Message(String s, String m, String u, int id) {
		subject = s;
		message = m;
		sender = u;
		messageID = id;
		dateCreated = LocalDate.now();
	}

	public String messageInfo() {
		return "#" + this.messageID + ", " + this.sender + ", " + this.dateCreated + ", " + this.subject + ".";
	}
}

class PrivateMessageBoard {
	private String boardName;
	private List<String> members;
	private List<Message> messages;
	private int groupMessageID = 0;

	public PrivateMessageBoard(String boardName) {
		this.boardName = boardName;
		this.members = new ArrayList<>();
		this.messages = new ArrayList<>();
	}

	// Add getters and setters as needed
	public String getBoardName() {
		return this.boardName;
	}

	public List<String> getMembers() {
		return this.members;
	}

	public List<Message> getMessages() {
		return this.messages;
	}

	public int getGroupMessageID() {
		return this.groupMessageID;
	}

	public void addMember(String username) {
		// Logic to add a member to the private board
		// Check if the member is not already in the list
		if (!this.members.contains(username)) {
			// Add the member to the list
			this.members.add(username);
		}
	}

	public void removeMember(String username) {
		// Logic to remove a member from the private board
		// Remove the member from the list
		this.members.remove(username);
	}

	public void addMessage(Message message) {
		// Logic to add a message to the private board
		// Add the message to the list
		this.messages.add(message);
	}

	public void increaseMessageID(Message message) {
		// Logic to add a message to the private board
		// Add the message to the list
		this.groupMessageID++;
	}
}
