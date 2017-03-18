package Server.database;

import Server.Server;
import communications.ChatInfo;
import communications.Message;
import communications.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

/**
 *
 */
public interface Database {

    // add user to the database return false if user exists
    boolean addUser(String username, String password, byte[] salt);
    
    boolean ifExists(String username);
    
    boolean updateProfile(int userID, boolean[] edited, String[] fields);
    
    User getUser(int userID);

    // check user credentials
    boolean checkCredentials(String username, String password);

    // get the user ID
    int getUserID(String username);

    // get a list of chats the user belongs too
    List<Integer> getChats(String username);

    // get the users last active chat
    List<Message> getActiveChat(String username);

    /// create delete method

    // possible delete from db entry here
    // update history and resend if deleted

    // create chat with these userIDs
    int createChat(int[] userIDs);

    // get the ID of the users last active chat
    int getActiveChatID(String username);

    // add chat history this method should delete all history for the chatID and appTarget and add the new history in its place
    boolean addHistory(int chatID, int appTarget, List<Message> messages);

    // get chat history
    // Note this method has been changed we now need to get this specified number of messages from the server
    List<Message> getHistory(int chatID, int appTarget, int numberOfMessages);

    // get usernames belonging to a specific chat
    User [] getChatUsers(int chatID);

    // get friends belonging to a user
    List<User> getFriends(int userID);

    // add a message to the database
    boolean setMessage(int senderID, int chatID, int appTarget, String message, String meta);

    // get a single message from the database
    // TODO parameters are wrong discuss the needed parameters to get a specific message
    String getMessage(int chatID, int appTarget, String message, String meta);

    // Close connection
    void close();

    boolean addFriend(int tooID, int fromID);
    
    List<ChatInfo> getUserChats(int userID);
    
    String getPasword(int userID);
    
    byte[] getSalt(int userID);
    
    boolean setPassword(String username, String password);
    
    boolean setSalt(String username, byte[] salt);
    
    User[] deleteChat(int chatID);
}
