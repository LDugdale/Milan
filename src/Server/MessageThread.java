package Server;

import communications.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Laurie Dugdale
 */
public class MessageThread extends Thread {

    private Server server;
    private Queue<Memo> messageQueue;
    private Set<Integer> blockedChats;
    private boolean isStopped;

    /**
     * Constructor
     *
     * @param server
     */
    public MessageThread(Server server){
    	
    	isStopped = false;
        this.server = server;
        messageQueue = new LinkedBlockingQueue<>();
        this.blockedChats = new HashSet<Integer>();
    }

    /*
     * Getters and setters
     */
    public Queue<Memo> getMessageQueue() {

        return messageQueue;fg
    }
    
    public void addToBlockedChats(int chatID){
    	
    	this.blockedChats.add(chatID);
    }

    public synchronized Memo dequeueMessageQueue(){

        return this.messageQueue.remove();
    }

    public synchronized void enqueueMessageQueue(Memo m){
    	System.out.println(m.getClass().getName());
        messageQueue.offer(m);
    }

    @Override
    /**
     * The main run loop
     */
    public void run() {
	
        try {
        	
        	while (!isStopped){
        		while(!getMessageQueue().isEmpty()) {

        			handleMessage(dequeueMessageQueue());
        		}
        	}
        } catch (IOException e) {
        	
        	server.activityCommunication("Mesage thread has stopped. Error: " + e.getMessage() + "\n");
//            e.printStackTrace();
        }        
    }

    /**
     * Parses the Memo object that has been collected and decides what to do with it depending on the class.
     * 
     * @param m Represents the memory object to check
     * @throws IOException Theows IOException to the run method
     */
    public void handleMessage(Memo m) throws IOException {
    	
        if(m.getClass() == Message.class) {
        	
        	
            // dequeue message and assign
            Message message = (Message) m;
            
            // Check that the chat isn't blocked
            if(!blockedChats.contains(message.getChatID())){
	           	
	            // for each user id in message
	            for (int id : message.getUserIDs()) {
	            	
	                // Check user is on server before sending to client
	                if (server.inThread(id)) {
	
	                    // get the thread the message is intended for and send it to the client
	                    server.getThread(id).packetToClient(new Packet(m));
	                }
	            }
            }
        } else if (m.getClass() == AddFriend.class){
        	
        	AddFriend friend = (AddFriend)m;
            int id = friend.getUserID();

            // Check user is on server before sending to client
            if (server.inThread(id)) {
            	
                // get the thread the message is intended for and send it to the client
                server.getThread(id).packetToClient( new Packet(friend));
            }
        } else if (m.getClass() == History.class){

            History h = (History)m;

            // for each user id in History
            for (int id : h.getUserIDs()) {

                // Check user is on server before sending to client
                if (server.inThread(id)) {

                    // get the thread the message is intended for and send it to the client
                    server.getThread(id).packetToClient(new Packet(h));
                }
            }
            
            // remove from blocked list
            blockedChats.remove(h.getChatID());
        }
    }
    
    /**
     * Stops the messageThread safely
     */
    public void messageThreadStop(){
    	
    	this.isStopped = true;
    	server.activityCommunication("Message Thread has stopped!\n");
    	messageQueue = new LinkedBlockingQueue<>();
    	this.interrupt();    	
    }
}
