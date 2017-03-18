import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import applications.AppUtilities;
import communications.*;
import java.io.*;

public class ClientTestServer {
	
	public static void main(String[] args) {
		
		ServerSocket serverSocket = null;
		
		int portNum = 54321;

		try {
			serverSocket = new ServerSocket(portNum);
		} catch (IOException e) {
			System.err.println("Couldn't listen on port: " + portNum + ".");
			System.exit(-1);
		}

		
		Socket client = null;
		try {
			while(true) {
				System.out.println("Waiting for client.");
				client = serverSocket.accept();
				System.out.println("Found client.");
				
				(new ClientTestServer().new ClientThread(client)).start();
			}
			
		} catch (IOException e) {
			System.out.println("Something went wrong. Ending service to client...");
			try {
				serverSocket.close();
			} catch (IOException e1) {
				System.out.println("Could not close client socket.");
			}
			return;
		}
	}
	
	static int nextCID = 1000;
	static int nextChatNumber() {
		nextCID++;
		return nextCID;
	}
	static int nextUID = 2000;
	static int nextUserID() {
		nextUID++;
		return nextUID;
	}
	static Map<Integer, User> usersByID;
	static {
		usersByID = new TreeMap<Integer, User>();
		usersByID.put(29, new User(29, "tdc222"));
		usersByID.put(22, new User(22, "Tom"));
		usersByID.put(51, new User(51, "John"));
		usersByID.put(52, new User(52, "Jazz"));
		usersByID.put(53, new User(53, "Tim"));
		usersByID.put(54, new User(54, "Leah"));
		usersByID.put(55, new User(55, "Carl"));
		usersByID.put(56, new User(56, "Chavonne"));
		usersByID.put(57, new User(57, "Walter"));
		usersByID.put(58, new User(58, "Jess"));
		usersByID.put(59, new User(59, "Jack"));
		usersByID.put(60, new User(60, "Darwin"));
	}
	static User getUser(int userID) {
		User user;
		if((user = usersByID.get(userID)) != null) {
			return user;
		}
		else return new User(userID, userID + "_username");
	}
	static User addUser(String username) {
		int uid = nextUserID();
		usersByID.put(uid, new User(uid, username));
		return getUser(uid);
	}
	static List<User> getFriends() {
		List<User> friends = new ArrayList<User>();
		for(Entry<Integer, User> entry : usersByID.entrySet()) {
			friends.add(entry.getValue());
		}
		return friends;
	}
	
	class ClientThread extends Thread {
		
		Socket client;
		
		int counter;
		
		public ClientThread(Socket client) {
			this.client = client;
			
			counter = 0;
		}
		
		public void run() {
			ObjectOutputStream toClient;
			ObjectInputStream fromClient;
			try {			
				toClient = new ObjectOutputStream(client.getOutputStream());
				fromClient = new ObjectInputStream(client.getInputStream());
				
			} catch (IOException e) {
				System.out.println("Something went wrong. Ending service to client...");
				try {
					client.close();
				} catch (IOException e1) {
					System.out.println("Could not close client socket.");
				}
				return;
			}
			
			try {
				while (true) {
					
					System.out.println("Waiting for input.");
					
					Object datum = null;
					
					if ((datum = fromClient.readObject()) != null) {
						Packet packet = (Packet)datum;
						
						if(packet.getType() == 10 || packet.getType() == 4) {
							
							String username = "";
							String password = "";
							
							if(packet.getType() == 10) {
								Login login = (Login)packet.getMemo();
							
								username = login.getUsername();
								password = login.getPassword();
								
								System.out.println("Login received. Username: " + username + ", password: " + password);
							}
							else {
								SignUp signup = (SignUp)packet.getMemo();
								
								username = signup.getUsername();
								password = signup.getPassword();
								
								System.out.println("SignUp received. Username: " + username + ", password: " + password);
							}
							
						/*	try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// Do nothing
							}*/
							
							if(username.equals("TDC222")) {
								toClient.writeObject(new Packet(new Setup()));
							}
							else {
								int tempUserID = Character.getNumericValue(username.charAt(0));
								
								List<ChatInfo> myChats = new ArrayList<ChatInfo>();
								for(int i=1; i<=10; i++) {
									User[] users = new User[2];
									users[0] = getUser(tempUserID);
									users[1] = getUser(i + 50);
									
									myChats.add(new ChatInfo(i, users));
								}
								
								User[] users = new User[3];
								users[0] = getUser(tempUserID);
								users[1] = new User(1, "Laurie");
								users[2] = new User(2, "Rosie");
								
								List<Message> messageHistory = new ArrayList<Message>();
								messageHistory.add(new Message(users, 1, AppUtilities.chatTargetID, 1, users[1].getUserID(), "meta", "This is a message."));
								messageHistory.add(new Message(users, 1, AppUtilities.chatTargetID, 2, users[2].getUserID(), "meta", "This is another message."));
								
								History myHistory = new History(users, 1, AppUtilities.chatTargetID, messageHistory);
								Setup mySetup = new Setup(tempUserID, getUser(tempUserID), getFriends(), myChats, myHistory);
								
								toClient.writeObject(new Packet(mySetup));
							}
						}
						else if(packet.getType() == 0) {
							
							System.out.println("Message received.");
							
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// Do nothing
							}
							
							Message message = (Message)packet.getMemo();
							
							System.out.println("Pass message for cid " + message.getChatID() + ". Message reads: " + message.getMessage());
							
							if(message.getAppTarget() == -585576737) {
								
								System.out.println("Tic tac toe message.");
								
								String responseText = "";
								if(counter == 0) responseText = "o 0 0";
								else if(counter == 1) responseText = "o 1 1";
								else if(counter == 2) responseText = "o 2 2";
								else if(counter == 3) responseText = "o 0 0";
								else if(counter == 4) responseText = "o 2 0";
								else if(counter == 5) responseText = "o 2 2";
								counter++;
								counter = counter%6;
								
								System.out.println("Respond with: " + responseText);
								
								User fromUser = null;
								if((MemoUtilities.getUser(message.getSenderID(), message.getUsers())).equals(getUser(message.getUserIDs()[0]))) {
									fromUser = usersByID.get(message.getUserIDs()[1]);
								}
								else fromUser = usersByID.get(message.getUserIDs()[0]);
								System.out.println(Arrays.toString(message.getUsernames()));
								System.out.println(message.getUserIDs()[0] + ": " + getUser(message.getUserIDs()[0]));
								System.out.println(message.getUserIDs()[1] + ": " + getUser(message.getUserIDs()[1]));
								
								message.getUsers();
								message.getChatID();
								message.getAppTarget();
								fromUser.getUserID();
								Message tictacMessage = new Message(message.getUsers(), message.getChatID(), message.getAppTarget(), -1, fromUser.getUserID(), "move", responseText);
								
								toClient.writeObject(new Packet(tictacMessage));
							}
							else toClient.writeObject(new Packet(message));
						}
						else if(packet.getType() == 2) {
							
							System.out.println("HistoryRequest received.");
							
							HistoryRequest request = (HistoryRequest)packet.getMemo();
							
							System.out.println("History request for cid: " + request.getChatID() + ", appTarget: " + request.getAppTarget());
							
							User[] users = new User[2];
							users[0] = getUser(request.getUserID());
							users[1] = getUser(request.getChatID() + 50);
							
							List<Message> messageHistory = new ArrayList<Message>();
							
							if(request.getAppTarget()==0) {
								messageHistory.add(new Message(users, request.getChatID(), request.getAppTarget(), 1, users[0].getUserID(), "meta", "History for chat " + request.getChatID() + "."));
								messageHistory.add(new Message(users, request.getChatID(), request.getAppTarget(), 2, users[1].getUserID(), "meta", request.getChatID() + " history."));
							}
							else if(request.getAppTarget()==-96231851) {
								messageHistory.add(new Message(users, request.getChatID(), request.getAppTarget(), 1, getUser(request.getUserID()).getUserID(), "new", users[0].getUserID() + " w " + 2 + " " + 3));
							}
							toClient.writeObject(new Packet(new History(users, request.getChatID(), request.getAppTarget(), messageHistory)));
						}
						else if(packet.getType() == 1) {
							
							History history = (History)packet.getMemo();
							
							System.out.println("History received from " + history.getHistory().get(0).getSenderUsername() + ".");
							for(int i=0; i<history.size(); i++) {
								System.out.println("\t" + history.get(i).getMeta() + " " + history.get(i).getMessage());
							}
							
							toClient.writeObject(new Packet(history));
						}
						else if(packet.getType() == 6) {
							
							CreateChat createChat = (CreateChat)packet.getMemo();
							
							User[] users = new User[createChat.getUserIDs().length];
							
							for(int i=0; i<users.length; i++) {
								users[i] = getUser(createChat.getUserIDs()[i]);
							}
							
							toClient.writeObject(new Packet(new History(users, nextChatNumber(), 0, new ArrayList<Message>())));
							
						}
						else if(packet.getType() == 7) {
							
							FriendRequest request = (FriendRequest)packet.getMemo();
							
							if(request.getUsername().equals("TDC222")) {
								toClient.writeObject(new Packet(new AddFriend(request.getUserID(), new User(-1, request.getUsername()))));
							}
							else {
								toClient.writeObject(new Packet(new AddFriend(request.getUserID(), addUser(request.getUsername()))));
							}
						}
					}
					
				}
			}
			catch (IOException | ClassNotFoundException e) {
				System.out.println("Network failure - attempt to close client thread.");
			}
			catch (Exception e) {
				System.out.println("Other exception in client thread.");
				e.printStackTrace();
			}
			finally {
				try {
					toClient.close();
					fromClient.close();
				} catch (IOException e) {
					System.out.println("Could not close client streams.");
				}
				
				try {
					client.close();
				} catch (IOException e) {
					System.out.println("Could not close client.");
				}
			}
		}
	}
	
}
