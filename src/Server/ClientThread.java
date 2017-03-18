package Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import communications.*;

/**
 * TODO if logout user correctly if connection drops
 * TODO setup outgoing queue
 */
public class ClientThread extends Thread implements PacketHandler {

    private Server server; // The server object, should be an instance of the main server object
    private ObjectInputStream input; // the input to the client thread
    private ObjectOutputStream output; // the output to the client thread
    private Queue<Packet> tempPacketsToProcess = new LinkedList<Packet>(); // The queue holding the packets to process
    private boolean isStopped; // if this is true the loop in the run method will stop
    private int userID; // Represents the userID of the client thats communicating with the thread
    private String username; // Represents the username of the client thats communicating with the thread
    private Socket clientSocket; // The client socket

    /**
     * Constructor setup input output stream and assign socket to field variable.
     * 
     * @param clientSocket
     * @param server
     */
    public ClientThread(Server server, Socket clientSocket) {

        this.server = server;
        this.isStopped = false;
        this.clientSocket = clientSocket;
        try {

            input = new ObjectInputStream(clientSocket.getInputStream());
            output = new ObjectOutputStream(clientSocket.getOutputStream());

        } catch (IOException e) {

            e.printStackTrace();
        } 
    }

    /*
     * Getters & setters
     */
    /**
     * Getter for username
     *
     * @return the username field
     */
    public String getUsername() {

        return this.username;
    }

    /**
     * Setter for username
     *
     * @param username the username to add to the field variable
     */
    public void setUsername(String username){

        this.username = username;
    }

    /**
     * Setter for userID
     *
     * @param userID The user id to add to the field variable
     */
    public void setUserID(int userID){

        this.userID = userID;
    }

    /**
     * Getter for userID
     *
     * @return the userID field
     */
    public int getUserID() {

        return this.userID;
    }

    /**
     * Adds a packet to the queue
     * 
     * @param packet The packet to be added
     */
    public synchronized void addPacketToProcess(Packet packet) {

        tempPacketsToProcess.add(packet);
    }

    /**
     * Removes a packet from the queue 
     * 
     * @return The packet that's been removed ... of course!
     */
    public synchronized Packet dequeuePacketToProcess() {

        return tempPacketsToProcess.poll();
    }

    /*
     * Main clientThread operations
     */
    /**
     * The main client thread loop waiting for message from the client.
     *
     */
    public synchronized void run() {

        try {
	        while(!isStopped) {
	
	        	addPacketToProcess((Packet) input.readObject());    
	        	
	            Packet item;
	            while (!tempPacketsToProcess.isEmpty()) {

	                if ((item = dequeuePacketToProcess()) != null) {

	                    PacketReader.readPacket(item, this);
	                }
	            }
	        }
        } catch (EOFException e) {
        	
        	server.activityCommunication("Client thread has stopped for " + getUsername() + " stopped. Error message:"+ e.getMessage() + "\n");
//            e.printStackTrace();
        } catch (ClassNotFoundException e) {
        	
        	server.activityCommunication("Client thread has stopped for " + getUsername() + " stopped. Error message:"+ e.getMessage() + "\n");
//            e.printStackTrace();
        } catch (IOException e) {
        	
        	server.activityCommunication("Client thread has stopped for " + getUsername() + " stopped. Error message:"+ e.getMessage() + "\n");
//          e.printStackTrace();
        }
        
        clientStop();
    	server.activityCommunication("Thread : " + this.getName() + " has stopped. This thread belonged too " + username + ".\n");

    }
    
    

    /**
     * Stop the client safely
     *
     */
    public void clientStop(){
    	
        server.removeClientThread(getUserID());

        this.isStopped = true;
        
        try {
        	
            input.close();
            output.close();
			clientSocket.close();
		} catch (IOException e) {
			
			server.activityCommunication("Problem when stopping Client Thread for " + getUsername() + ". Error message:"+ e.getMessage() + "\n");
//			e.printStackTrace();
		}
        this.interrupt();
    }

    /**
     * Send a packet to the connected client
     *
     * @param packet
     * @throws IOException
     */
    public void packetToClient(Packet packet) throws IOException {

        output.writeObject(packet);
    }

    /*
     * Handlers
     */
    @Override
    public boolean handleMessage(Message message) {

        server.receivedMessage(message);
        return true;
    }

    @Override
    public boolean handleHistory(History history) {

        // should not be received by server
        // TODO when history request received block messages to that chat id
    	server.activityCommunication("Received history from " + username + " this wipes and replaces app: " + history.getAppTarget() + ".\n");

        server.receivedHistory(history);
        return true;
    }

    @Override
    public boolean handleHistoryRequest(HistoryRequest historyRequest) {

        try {
        	// server GUI message
        	server.getSG().appendActivity(getUsername() + " requested history for chat ID " + historyRequest.getChatID() + ". \n");
        	
			packetToClient(new Packet(server.history(historyRequest.getChatID(), historyRequest.getAppTarget(), historyRequest.getSize())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return true;
    }

    @Override
    public boolean handleServerMessage(ServerMessage serverMessage) {
    	
    	String[] messageInfo = serverMessage.getMessage().split(" ");
    	if (messageInfo[0].equals("delete")){
    		
    		server.receivedDeleteChat(Integer.parseInt(messageInfo[1]), userID);    		
    		return true;
    	}
    	 	
    	// "delete chatID"
    	return false;
    }

    @Override
    public boolean handleSignUp(SignUp signUp) {
    	
        String username = signUp.getUsername();
        String password = signUp.getPassword();
        setUsername(username);
        
        try {
        	
            if (server.addUser(username, password)) {
            	
            	server.activityCommunication(username + " has signed up. \n");        
            	int id = server.getUserID(username);
                server.addThread(id, this);
                setUserID(id);
                
                output.writeObject( new Packet( new Setup(id, new User(id, username, ""))));                
                return true;
            }

            output.writeObject(new Packet( new Setup()));            
        } catch (IOException e) {
        	clientStop();
            e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public boolean handleLogout(Logout logout) {
    	server.activityCommunication(username + " has logged out. \n");
        this.clientStop();
        return true;
    }

    @Override
    public boolean handleCreateChat(CreateChat createChat) {

        server.receivedCreateChat(createChat);
        return false;
    }

    @Override
    public boolean handleFriendRequest(FriendRequest friendRequest) {
    	
        server.receivedFriendRequest(friendRequest, getUserID(), getUsername());
        return true;
    }

    @Override
    public boolean handleAddFriend(AddFriend addFriend) {

        // should not be received by server
        return false;
    }

    @Override
    public boolean handleSetup(Setup setup) {

        // should not be received by server
        return false;
    }

    @Override
    public boolean handleLogin(Login login) {
    	
    	// get username and password from login object
        String username = login.getUsername();
        String password = login.getPassword();
        
        // get ID from database given the username and set it
        int id = server.getUserID(username);
        setUserID(id);
        
        // set username
        setUsername(username);
        System.out.println("login username " + username);
        // add the thread to the server map
        server.addThread(id, this);

        try {

            output.writeObject(new Packet(server.setup(id, username, password)));
        } catch (IOException e) {     
        	
        	clientStop();
            e.printStackTrace();
        }
        return true;
    }

	@Override
	public boolean handleEditUser(EditUser editUser) {
		
		if (server.editUser(editUser)){
			
			server.activityCommunication(username + " has edited their profile. \n");
            try {
            	
				output.writeObject(new Packet(editUser));
			} catch (IOException e) {
				
				e.printStackTrace();
			}

			return true;
		}
		return false;
	}
}
