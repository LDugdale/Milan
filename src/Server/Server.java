package Server;

import communications.*;
import Server.database.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Laurie Dugdale
 */
public class Server implements Runnable {

    private int port; // the port to be opened
    private ServerGUI sg; // instance over the server GUI
    private ServerSocket serverSocket; // The server socket
    private boolean isStopped; // if true the server while loop will stop
    protected ExecutorService threadPool; // Thread pool
    private Map<Integer, ClientThread> clientThreads; // Stores all the ClientThreads so they can be accessed easily
    private MessageThread messageThread; // Message thread is used for sending messages to clients
    private Database db; // instance of the database

    /*
     * Constructors
     */
    /**
     * Constructor for command line
     *
     * @param port
     */
    public Server(int port) {
    	
    	this.threadPool = Executors.newCachedThreadPool();
        this.serverSocket = null;
        this.port = port;
        this.isStopped = false;
        this.db = new MessengerDatabase();
        this.clientThreads = new HashMap<>();
        this.messageThread = new MessageThread(this);
    }

    /**
     * Constructor for GUI
     *
     * @param port
     * @param sg
     */
    public Server(int port, ServerGUI sg) {
    	
    	this.threadPool = Executors.newCachedThreadPool();
        this.serverSocket = null;
        this.port = port;
        this.isStopped = false;
        this.db = new MessengerDatabase();
        this.clientThreads = new HashMap<>();
        this.messageThread = new MessageThread(this);
        this.sg = sg;
    }

    /*
     * Getters & setters
     */
    /**
     * Adds a ClientThread to the clientThread map
     * 
     * @param key The user ID of the thread so it can be located easily
     * @param thread the ClientThread to be added.
     */
    public void addThread(int key, ClientThread thread){

        clientThreads.put(key, thread);
    }

    /**
     * Gets a client thread from the clientThreads map
     * 
     * @param key They key which represents the User ID associated with the ClientThread
     * @return ClientThread
     */
    public ClientThread getThread(int key){

        return clientThreads.get(key);
    }
    
    
    /**
     * Getter for the sg field variable
     * 
     * @return the ServerGUI field variable
     */
    public ServerGUI getSG(){
    	
    	return this.sg;
    }

    /**
     * checks value of isStopped field
     * 
     * @return isStopped field variable
     */
    private synchronized boolean isStopped() {

        return this.isStopped;
    }

    /*
     * Server operations
     */
    /**
     * Server pool creating new Client thread
     */
    public void run(){

        openServerSocket();

        // start messageThread to handle all messages.
        messageThread.start();
        try {

	        while (!isStopped()) {
	        	
                // Accept socket connection
                Socket clientSocket = this.serverSocket.accept();	
                // Create the client thread
                threadPool.execute( new ClientThread(this, clientSocket));	
	        }
        } catch (IOException e) {

        	activityCommunication("Error accepting client connection " + e.getMessage() + "\n");
//            throw new RuntimeException( "Error accepting client connection", e);
        }
    }

    /**
     * Stop server - used by GUI
     */
    public synchronized void stop(){
    	activityCommunication("Server stopped.\n");
        this.isStopped = true;
        try {

            terminateThreads();
            messageThread.messageThreadStop();
            messageThread = null;
            this.serverSocket.close();
            db.close();
        } catch (IOException e) {

            throw new RuntimeException("Error closing server", e);
        }
    }

    /**
     * terminate threads in the map here
     */
    public void terminateThreads(){

        for(ClientThread c : clientThreads.values()){
            c.clientStop();
        }
    }

    /**
     * Opens the server socket
     */
    private void openServerSocket() {

        try {

            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
        	activityCommunication("Cannot open the specified port. Error:" + e.getMessage() + "\n");
        	this.isStopped = true;
//            throw new RuntimeException("Cannot open port", e);
        }
    }

    /*
     * server to thread operations
     */
    /**
     * Gets a user ID from a username out of the database
     * 
     * @param username
     * @return
     */
    public int getUserID(String username) {
    	
        return db.getUserID(username);
    }

    /**
     * Creates a Setup object for a user who is logging in or signing up
     * 
     * @param id
     * @param username
     * @param password
     * @return
     */
    public Setup setup(int id, String username, String password){
    	
    	activityCommunication(username + " requested setup. \n");    
    	// If user doesnt exist
    	
    	if (checkHash(id, username, password)){
    		
    		// Create setup object for the users client
    		activityCommunication(username + " has signed in. \n");
            return new Setup(id, db.getUser(id), db.getFriends(id), db.getUserChats(id), history(db.getActiveChatID(username), 0, Integer.MAX_VALUE));
    	} else {
    		
    		// Send an empty Setup object signifying that the sign in has failed to the client.
    		activityCommunication(username + " failed sign in. \n");    
            return new Setup();
    	}
    }
    
    public boolean editUser(EditUser eu){
    	
    	if(eu.getEdited()[1] == true){
    		eu.getFields()[1] = get_SHA_1_SecurePassword(eu.getPassword(), db.getSalt(eu.getUserID()));
    	}
    	
    	return db.updateProfile(eu.getUserID(), eu.getEdited(), eu.getFields());
    }

    /**
     * Queries the database and returns a history object with the specified parameters.
     * 
     * @param chatID the chatID of the history needed.
     * @param appTarget The app target of the history needed.
     * @param The number of messages needed in the history object.
     * @return History object
     */
    public History history(int chatID, int appTarget, int numberOfMessages){
    	    	
        return new History(db.getChatUsers(chatID), chatID, appTarget, db.getHistory(chatID, appTarget, numberOfMessages));
    }
    
    /**
     * Sets a message in the database, prints it to the server GUI and passes it to the messageThread
     * 
     * @param m message to be added to the database and sent back to clients
     */
    public void receivedMessage(Message m){
    	
    	// add message to the database
        db.setMessage(m.getSenderID(), m.getChatID(), m.getAppTarget(), m.getMessage(), m.getMeta());
        
        // print the messag	e in the server GUI
        messageCommunication(m.getSenderUsername() + " sent : " + m.getMessage() + " apptarget : " + m.getAppTarget() + " chatID : " + m.getChatID() +  " meta : " + m.getMeta() + "\n");
        
        // pass the message to the messageThread to be processed
        messageThread.enqueueMessageQueue(m);
    }

    /**
     * Processes a friend request object by adding it to the database, 
     * and passing it to the messageThread to be sent out to other clients
     * 
     * @param fr The FriendRequest object to be processed
     * @param from The ID of the person who sent the request
     */
    public void receivedFriendRequest(FriendRequest fr, int from, String fromUsername){
    	
    	
    	activityCommunication(fromUsername + " is sending a friend request too " + fr.getUsername() + ". \n");
    	int friendID = db.getUserID(fr.getUsername());
    	
    	// pass FriendRequest object to messageThread
        messageThread.enqueueMessageQueue(new AddFriend(from, new User(friendID, fr.getUsername())));
        messageThread.enqueueMessageQueue(new AddFriend(friendID, new User(from, fromUsername)));

        // add the friend relationship to the database
        db.addFriend(friendID, from);
    }

    /**
     * Creates a database entry for a chat with the specified users. 
     * Passes it to the messageThread to be sent out to other clients
     * 
     * @param cc The CreateChat object to be processed
     */
    public void receivedCreateChat(CreateChat cc){
    	
    	// server GUI message
    	activityCommunication(clientThreads.get(cc.getUserID()).getUsername() + " has created a chat. \n");
    	// enqueue CreateChat Object in the messageThread
        messageThread.enqueueMessageQueue(history(db.createChat(cc.getUserIDs()), 0, Integer.MAX_VALUE));
    }

    /**
     * Processes a History object.
     * Blocks the chat that the History object is changing by adding to the addToBlockedChats Set
     * Passes it to the messageThread to be sent out to other clients.
     * Notifies the database the a chat History needs to be changed.
     * 
     * @param h The history object to be processed
     */
    public void receivedHistory(History h){
    	
    	activityCommunication("The app " + h.getAppTarget() + " is changing the history for chat ID " + h.getChatID() +". \n");

    	// add to blocked hats in message thread
    	messageThread.addToBlockedChats(h.getChatID());
    	// enqueue to list in message thread
        messageThread.enqueueMessageQueue(h);
        // add history to the database
        db.addHistory(h.getChatID(), h.getAppTarget(),h.getHistory());
    }
    
    public void receivedDeleteChat(int chatID, int senderID){
    	
    	activityCommunication(clientThreads.get(senderID).getUsername() +  " is deleting chat ID " + chatID +". \n");
    	
    	User [] usersToInform = db.deleteChat(chatID);
    	
    	messageThread.enqueueMessageQueue( new Message(usersToInform, chatID, 0, 0, senderID, "delete", ""));
    }
    
    /**
     * wrapper for activity messages to make sure the serverGUI is active
     * 
     * @param msg message to be added to activity pane
     */
    public void activityCommunication(String msg){
    	
    	if (sg == null){
    		
    		return;
    	} else {
    		
        	sg.appendActivity(msg);

    	}
    }
    
    /**
     * wrapper for messages to make sure the serverGUI is active
     * 
     * @param msg message to be added to message pane
     */
    public void messageCommunication(String msg){
    	
    	if (sg == null){
    		
    		return;
    	} else {
    		
        	sg.appendMessages(msg);
    	}
    }

    /**
     * Checks if a user is logged in by checking the clientThreads map
     * 
     * @param id Represents the userID
     * @return True if the user is in the map.
     */
    public boolean inThread(int id){

        return this.clientThreads.containsKey(id);
    }

    /**
     * Uses the db.addUser method to add a user to the database
     * 
     * @param username Username of the user
     * @param password Password of the user
     * @return returns true if successful
     */
    public boolean addUser(String username, String password){
    	
    	String userHash = "";

		try {
			
			byte[] usersSalt = getSalt();
			userHash = get_SHA_1_SecurePassword(password, usersSalt);
		    return db.addUser(username, userHash, usersSalt);
		} catch (NoSuchAlgorithmException e) {
			
			e.printStackTrace();
		}
		return false;
    }

    /**
     * Removed a Thread the from ClientThread map
     * 
     * @param id The UserID to remove
     */ 
    public void removeClientThread(int id){

        this.clientThreads.remove(id);
    }
    
    /*
     * Secure passwords in database
     */
    /**
     * 
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] getSalt() throws NoSuchAlgorithmException
    {
        //Always use a SecureRandom generator
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        //Create array for salt
        byte[] salt = new byte[16];
        //Get a random salt
        sr.nextBytes(salt);
        //return salt
        return salt;
    }
    
    private static String get_SHA_1_SecurePassword(String passwordToHash, byte[] salt) {
    	
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(salt);
            byte[] bytes = md.digest(passwordToHash.getBytes());
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        } 
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return generatedPassword;
    }

    public boolean checkHash(int userID, String username, String password){
    	
    	if (db.ifExists(username)) {
			byte[] usersSalt = db.getSalt(userID);
	    	String userHash = get_SHA_1_SecurePassword(password, usersSalt);    
	    	return db.checkCredentials(username, userHash);
    	} 
    	return false;
    	
    }
    
    /*
     * Main method for starting server on command line
     */
    /**
     * TODO possibly start the GUI from the command line
     * @param argschatID
     */
    public static void main(String[] args) {

        Server test = new Server(54321);
        test.run();
    }
}

