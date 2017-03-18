/**
 * 
 * @author Rosalie Butcher
 * @version 12/03/2017
 */
package Server.database;

import Server.database.Database;
import communications.ChatInfo;
import communications.Message;
import Server.Server;
import communications.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by mnt_x on 22/02/2017.
 */
public class MessengerDatabase implements Database
{
	
	private Connection conn;

	public Connection getConn()
	{
		return conn;
	}
	
	//------------------------------------------------------------------------------------------------
	//                                            CONSTRUCTOR
	//------------------------------------------------------------------------------------------------
	/**
	 * Constructor connects to the school database.
	 */
	public MessengerDatabase()
	{
		String username = "rxb365";
		String password = "8ge3a54e4q";
		
		try
		{
			Class.forName("org.postgresql.Driver").newInstance();
		}
		catch (InstantiationException | IllegalAccessException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		
		Properties info = new Properties();
		info.put("user", username);
		info.put("password", password);
		
		try
		{
			// Change this url to jdbc:postfresql://mod_fun_databases for use on lab machines.
			String url = "jdbc:postgresql://mod-fund-databases.cs.bham.ac.uk/rxb365";
			Connection connect = DriverManager.getConnection(url, info);
			if (connect != null)
			{
				this.conn = connect;
			}
			else
			{
				this.conn = null;
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	//------------------------------------------------------------------------------------------------
	//                                          ADD USER
	//------------------------------------------------------------------------------------------------
	/**
	 * Adds a new user to the database.
	 * 
	 * Returns true if the entry was successful, and false if it was not.
	 */
    @Override
    public boolean addUser(String username, String password, byte[] salt)
    {
		String exists = "";
		try
		{
			PreparedStatement addUser = conn.prepareStatement("INSERT INTO users(user_name, password, salt) VALUES (?, ?, ?)");
			PreparedStatement checkUserName = conn.prepareStatement("SELECT user_name FROM users WHERE user_name = ?");
			checkUserName.setString(1, username);
			ResultSet userNameExists = checkUserName.executeQuery();
			while(userNameExists.next())
			{
				exists = userNameExists.getString("user_name");
			}
			if (exists.equals(username))
			{
				System.out.println("The username entered already exists.");
				return false;
			}
			addUser.setString(1, username);
			addUser.setString(2, password);
			addUser.setBytes(3, salt);
			addUser.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to create a new user." + "\n");
			e.printStackTrace();
			return false;
		}
    }
    
	//------------------------------------------------------------------------------------------------
	//                                       CHECK CREDENTIALS
	//------------------------------------------------------------------------------------------------
    /**
     * Checks whether the given credentials for a user are correct.
     * If the user_name exists and the password is correct returns true, else the method returns false.
     */
    @Override
    public boolean checkCredentials(String username, String password)
    {
    	System.out.println("---------------------------------------------------");
    	System.out.println("The check credentials method is starting.");
    	System.out.println("---------------------------------------------------");
		String enteredPassword = "";
		try
		{
			PreparedStatement checkCredentials = conn.prepareStatement("SELECT password FROM users WHERE user_name = ?");
			checkCredentials.setString(1, username);
			ResultSet ccResults = checkCredentials.executeQuery();
			while(ccResults.next())
			{
				System.out.println("The given password was: " + password);
				enteredPassword = ccResults.getString("password");
				System.out.println("The password in the database for this user is: " + enteredPassword);
			}
			ccResults.close();
			if (enteredPassword.equals(password))
			{
				return true;
			}
			else
			{
				System.out.println("The password given didn't match the one in the database.");
				return false;
			}
			
		}
		catch (SQLException e)
		{
			System.out.println("There was an issue checking the credentials in the database." + "\n");
			e.printStackTrace();
			return false;
		}
    }

	//------------------------------------------------------------------------------------------------
	//                                        GET USER ID
	//------------------------------------------------------------------------------------------------
	/**
	 * Returns the corresponding user_id for a users user_name.
	 * 
	 * Currently returns 0 if that user_name does not exist.
	 */
    @Override
    public int getUserID(String username)
    {
		int result = 0;
		try
		{
			PreparedStatement getId = conn.prepareStatement("SELECT user_id FROM users WHERE user_name = ?");
			getId.setString(1, username);
			ResultSet iDResult = getId.executeQuery();
			while(iDResult.next())
			{
				result = iDResult.getInt("user_id");
			}
			iDResult.close();

		}
		catch (SQLException e)
		{
			System.out.println("Was unable to retrieve the user_id from the database." + "\n");
			e.printStackTrace();
		}
		return result;
    }

	//------------------------------------------------------------------------------------------------
	//                                          GET CHATS
	//------------------------------------------------------------------------------------------------
    /**
     * Returns the corresponding chat_ids in a list associated with a user.
     */
    @Override
    public List<Integer> getChats(String username)
    {
		List<Integer> result = new ArrayList<Integer>();
		try
		{
			PreparedStatement getChats = conn.prepareStatement("SELECT DISTINCT user_name,"
					+ " users2chats.user_id, users2chats.chat_id FROM users FULL JOIN users2chats"
					+ " ON users.user_id = users2chats.user_id WHERE user_name = ?");
			getChats.setString(1, username);
			ResultSet iDResult = getChats.executeQuery();
			while(iDResult.next())
			{
				result.add(iDResult.getInt("chat_id"));
			}
			iDResult.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		
		return result;
    }

	//------------------------------------------------------------------------------------------------
	//                                       GET ACTIVE CHATS
	//------------------------------------------------------------------------------------------------
    /**
     * IMPORTANTLY, this method returns a list which has the NEWEST MESSAGE FIRST.
     * 
     * Currently returns a list of messages with the newest message first.
     * 
     * If there is no most_recent_chat, the method returns null.
     */
    @Override
    public List<Message> getActiveChat(String username)
    {
    	List<Message> result = new ArrayList<Message>();
    	ArrayList<User> currentUsers = new ArrayList<User>();
    	ArrayList<Integer> messageids = new ArrayList<Integer>();
    	int chatId = 0;
    	
		try
		{
			PreparedStatement getChatId = conn.prepareStatement("SELECT most_recent_chat FROM users WHERE user_name = ?");
			getChatId.setString(1, username);
			ResultSet id = getChatId.executeQuery();
			while(id.next())
			{
				chatId = id.getInt("most_recent_chat");
			}
			if (chatId == 0){
				System.out.println("This user doesn't currently have an active chat.");
			}
			id.close();
			
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to find the most_recent_chat from the users table in the database." + "\n");
			e.printStackTrace();
		}
		
		try
		{
			PreparedStatement getMessageIds = conn.prepareStatement("SELECT message_id FROM messages WHERE chat_id = ?");
			getMessageIds.setInt(1, chatId);
			ResultSet ids = getMessageIds.executeQuery();
			while(ids.next())
			{
				messageids.add(ids.getInt("message_id"));
			}
			ids.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to find the message_id from the messages table in the database." + "\n");
			e.printStackTrace();
		}
		
		Iterator<Integer> iterator = messageids.iterator();
		while(iterator.hasNext())
		{
			int currentId = iterator.next();
			try
			{
				PreparedStatement getUsers = conn.prepareStatement("SELECT DISTINCT users2messages.user_id, message_id"
						+ ", users.user_name FROM users2messages LEFT JOIN users on "
						+ "users2messages.user_id=users.user_id WHERE message_id=?");
				getUsers.setInt(1, currentId);
				ResultSet userInfo = getUsers.executeQuery();
				while(userInfo.next())
				{
					User temp = new User(userInfo.getInt("user_id"), userInfo.getString("user_name"));
					currentUsers.add(temp);
				}
				userInfo.close();
			}
			catch (SQLException e)
			{
				System.out.println("Was unable to get the user_id and user_name from the users table in the database." + "\n");
				e.printStackTrace();
			}
			
			try
			{
				PreparedStatement getOtherInfo = conn.prepareStatement("SELECT chat_id, app_target, message_id"
						+ ", message_creator, metadata, message_content FROM messages WHERE message_id = ?");
				getOtherInfo.setInt(1, currentId);
				ResultSet otherInfo = getOtherInfo.executeQuery();
				while(otherInfo.next()){
					User[] users = new User[currentUsers.size()];
					users = currentUsers.toArray(users);
					Message temp = new Message(users, otherInfo.getInt("chat_id"), otherInfo.getInt("app_target"),
							otherInfo.getInt("message_id"), otherInfo.getInt("message_creator"), 
							otherInfo.getString("metadata"), otherInfo.getString("message_content"));
					result.add(temp);
				}
				otherInfo.close();
			}
			catch (SQLException e)
			{
				System.out.println("Was unable to get the message information from the messages table in the database." + "\n");
				e.printStackTrace();
			}
			
		}	
		Collections.reverse(result);
        return result;
    }
    
    // Lock object to prevent interleaving in the createChat method without
    // requiring threads to have a lock on the whole database
    private Object createChatLock = new Object();

    
	//------------------------------------------------------------------------------------------------
	//                                   CREATE A NEW CHAT
	//------------------------------------------------------------------------------------------------
    /**
     * Creates a new chat by:
     * - Creating a new chat in the chat table.
     * - Adding the users ids in the integer array to the users2chats table.
     * - Sets all the users most_recent-chat to the new chat.
     * 
     * Returns false if a user is entered that doesn't exist.
     * 
     * ADD SOMETHING THAT DOESN'T LET YOU CREATE A CHAT WITH YOURSELF
     */
	@Override
	public int createChat(int[] userIDs)
	{
		Set<Integer> userSet = new HashSet<Integer>();
		
		for (int user: userIDs)
		{
			userSet.add(user);
		}
		synchronized (createChatLock)
		{ // Synchronize statement for createChat method
		  // This ensures the chat_id variable will always be correct
			
			int chatId = -1;
			Iterator<Integer> userIterator = userSet.iterator();
			
			
			while(userIterator.hasNext())
			{
				try
				{
					int user = userIterator.next();
					PreparedStatement findUser = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE user_id = ?");
					findUser.setInt(1, user);
					ResultSet results = findUser.executeQuery();
					int exists = 0;
					while(results.next())
					{
						exists = results.getInt("count");
					}
					if (exists == 0)
					{
						return -1;
					}
					results.close();
				}
				catch (SQLException e)
				{
					System.out.println("Was unable to access user information from the users table of the database." + "\n");
					e.printStackTrace();
				}
			}
			
			// Create a new chat_id
			try
			{
				PreparedStatement createChat = conn.prepareStatement("INSERT INTO chats DEFAULT VALUES");
				createChat.executeUpdate();
			}
			catch (SQLException e)
			{
				System.out.println("Was unable to insert values into the chats table of the database." + "\n");
				e.printStackTrace();
			}
			
			// Find out the value of the created chat_id (It'll be the highest number.)
			try
			{
				PreparedStatement findChatId = conn.prepareStatement("SELECT * FROM chats ORDER BY chat_id DESC LIMIT 1");
				ResultSet chatIdR = findChatId.executeQuery();
				while(chatIdR.next())
				{
				chatId = chatIdR.getInt("chat_id");	
				}
				chatIdR.close();
			}
			catch (SQLException e)
			{
				System.out.println("Was unable to access chat information from the chats table of the database." + "\n");
				e.printStackTrace();
			}
			
			// Sets the users most_recent_chat to the new chat
			
			try
			{
				PreparedStatement setNewChatID = conn.prepareStatement("UPDATE users SET most_recent_chat = ? WHERE "
						+ "user_id = (?)");
				for (int user:userIDs)
				{
					setNewChatID.setInt(1, chatId);
					setNewChatID.setInt(2, user);
					setNewChatID.executeUpdate();
				}
			}
			catch (SQLException e1)
			{
				System.out.println("Was unable to update user information from the users table of the database." + "\n");
				e1.printStackTrace();
			}
			
			// Add the chat Ids and the user ids to the table.
			try
			{
				PreparedStatement addNewChat = conn.prepareStatement("INSERT INTO users2chats(user_id, chat_id) "
						+ "VALUES(?, ?)");
				for (int user:userIDs)
				{
					addNewChat.setInt(1, user);
					addNewChat.setInt(2, chatId);
					addNewChat.executeUpdate();
				}
				return chatId;
			}
			catch (SQLException e)
			{
				System.out.println("Was unable to access user and chat ids from the users2chats table of the"
						+ " database." + "\n");
				e.printStackTrace();
			}
			
			return -1;
		}
	}

	
	//------------------------------------------------------------------------------------------------
	//                                 GET ACTIVE CHAT ID
	//------------------------------------------------------------------------------------------------
	/**
	 * Returns the most_recent_chat corresponding to that user_name from the database.
	 * 
	 * Will return 0 if there is no most_recent_chat.
	 */
	@Override
    public int getActiveChatID(String username)
	{
        int result = 0;
        try
        {
			PreparedStatement getMostRecent = conn.prepareStatement("SELECT most_recent_chat FROM users WHERE user_name = ?");
			getMostRecent.setString(1, username);
			ResultSet results = getMostRecent.executeQuery();
			while(results.next())
			{
				result = results.getInt("most_recent_chat");
			}
			results.close();
		}
        catch (SQLException e)
        {
			System.out.println("Was unable to access the user information from the users table in the database." + "\n");
			e.printStackTrace();
		}
        return result;
    }
	
	
	//------------------------------------------------------------------------------------------------
	//                                 ADD A DIFFERENT HISTORY TO A CHAT
	//------------------------------------------------------------------------------------------------
	/**
	 * Deletes all of the old history associated with this chatID and appTarget.
	 * Creates a new history comprised of the new list of messages for that chatID and
	 * appTarget.
	 * 
	 * Returns true if this is successful, and false if not.
	 * 
	 * First message in input list is oldest.
	 */
	@Override
	public boolean addHistory(int chatID, int appTarget, List<Message> messages)
	{
		User[] users = null;
		List<Integer> messageId = new ArrayList<Integer>();
		List<Boolean> chatExistList = new ArrayList<Boolean>();
		int exists = 4;
		boolean chatExistBoolean = false;
		boolean delete = false;
		boolean newChat = false;
		boolean newMessages = false;
		boolean newIDsRetrieved = false;
		boolean users2Chats = false;
		boolean users2Messages = false;
		boolean mostRecent = false;
		
		// Delete the old history.
		try
		{
			PreparedStatement deleteOld = conn.prepareStatement("DELETE FROM messages WHERE chat_id = ? AND app_target = ?");
			deleteOld.setInt(1, chatID);
			deleteOld.setInt(2, appTarget);
			deleteOld.executeUpdate();
			delete = true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to delete information from the chats table in the database." + "\n");
			e.printStackTrace();
		}
		
		// See if anything with this chat_id still exists. 
		
		try
		{
			PreparedStatement anythingLeft = conn.prepareStatement("SELECT COUNT(*) FROM chats WHERE chat_id = ?");
			anythingLeft.setInt(1, chatID);
			ResultSet left = anythingLeft.executeQuery();
			while(left.next())
			{
				exists = left.getInt("count");
			}
			left.close();
		}
		catch (SQLException e2)
		{
			System.out.println("Was unable to count information from the chats table in the database." + "\n");
			e2.printStackTrace();
		}
		
		// If not, create a new chat.
		if (exists == 0)
		{
			try
			{
				PreparedStatement createChat = conn.prepareStatement("INSERT INTO chats(chat_id) VALUES(?)");
				createChat.setInt(1, chatID);
				createChat.executeUpdate();
				newChat = true;
			}
			catch (SQLException e1)
			{
				System.out.println("Was unable to insert information into the chats table in the database." + "\n");
				e1.printStackTrace();
			}
		}
		
		// Adds the new message information to the database. Automatically assigns message_ids by the database.
		try
		{
			PreparedStatement createNew = conn.prepareStatement("INSERT INTO messages(app_target, metadata,"
					+ " message_content, message_creator, chat_id) VALUES(?, ?, ?, ?, ?)");
			Iterator<Message> iterator = messages.iterator();
			while(iterator.hasNext())
			{
				Message currentMessage = iterator.next();
				int messageID = currentMessage.getMessageID();
				messageId.add(messageID);
				String metadata = currentMessage.getMeta();
				String content = currentMessage.getMessage();
				int messageCreator = currentMessage.getSenderID();
				createNew.setInt(1, appTarget);
				createNew.setString(2, metadata);
				createNew.setString(3, content);
				createNew.setInt(4, messageCreator);
				createNew.setInt(5, chatID);
				users = currentMessage.getUsers();
				createNew.executeUpdate();
			}
			newMessages = true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to insert information into the messages table in the database." + "\n");
			e.printStackTrace();
		}
		
		// Queries the database to get the new message_ids
		try {
			PreparedStatement retrieveIDs = conn.prepareStatement("SELECT message_id FROM messages ORDER BY timemark"
					+ " ASC LIMIT ?");
			int numberOfMessages = messages.size();
			retrieveIDs.setInt(1, numberOfMessages);
			ResultSet newIDs = retrieveIDs.executeQuery();
			while(newIDs.next())
			{
				int messageID = newIDs.getInt("message_id");
				messageId.add(messageID);
			}
			newIDsRetrieved = true;
			newIDs.close();
		}
		catch (SQLException e1)
		{
			System.out.println("Was unable to retrieve information from the messages table in the database." + "\n");
			e1.printStackTrace();
		}
		
		// Checks to see whether those relations are in the users2chats table.
		
		try
		{
			PreparedStatement checkUsers2Chats = conn.prepareStatement("SELECT COUNT(*) FROM users2chats WHERE user_id = ?"
					+ " AND chat_id = ?");
			for (User user: users)
			{
				int count = 0;
				checkUsers2Chats.setInt(1, user.getUserID());
				checkUsers2Chats.setInt(2, chatID);
				ResultSet checking = checkUsers2Chats.executeQuery();
				while(checking.next())
				{
					count = checking.getInt("count");
					if (count > 0)
					{
						chatExistList.add(true);
					}
					else
					{
						chatExistList.add(false);
					}
				}
				checking.close();
			}
			
			for (Boolean exist : chatExistList)
			{
				if (exist == true)
				{
					chatExistBoolean = true;
				}
			}
		}
		catch (SQLException e1)
		{
			System.out.println("Was unable to retrieve information from the users2chats table in the database." + "\n");
			e1.printStackTrace();
		}
		
		// Re-populates the users2chats table.
		// ITS MINUS 3
		try
		{
			if (chatExistBoolean == true)
			{
				PreparedStatement users2chats = conn.prepareStatement("INSERT INTO users2chats(user_id, chat_id) VALUES (?, ?)");
			
				for (User user: users)
				{
					users2chats.setInt(1, user.getUserID());
					users2chats.setInt(2, chatID);
					users2chats.executeUpdate();
				}
			}
			users2Chats = true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to insert information into the users2chats table in the database." + "\n");
			e.printStackTrace();
		}
		
		// Re-populates users2messages.
		try
		{
			PreparedStatement users2messages = conn.prepareStatement("INSERT INTO users2messages(user_id, message_id)"
					+ " VALUES(?, ?)");
			for (User user: users)
			{
				Iterator<Integer> iterator = messageId.iterator();
				while(iterator.hasNext())
				{
					int messageIDR = iterator.next();
					users2messages.setInt(1, user.getUserID());
					users2messages.setInt(2, messageIDR);
					users2messages.executeUpdate();
					users2messages.close();
				}
			}
			users2Messages = true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to insert information into the users2messages table in the database." + "\n");
			e.printStackTrace();
		}
		
		// Updates each users most_recent_chat field.
		try
		{
			PreparedStatement updateMostRecent = conn.prepareStatement("UPDATE users SET most_recent_chat = ? WHERE user_id = ?");
			for (User user: users)
			{
				updateMostRecent.setInt(1, chatID);
				updateMostRecent.setInt(2, user.getUserID());
				updateMostRecent.executeUpdate();
			}
			mostRecent = true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to update information in the users table in the database." + "\n");
			e.printStackTrace();
		}
		
		if (delete == true && newChat == true && newMessages == true && newIDsRetrieved && users2Chats == true
				&& users2Messages == true && mostRecent == true)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	//------------------------------------------------------------------------------------------------
	//                                   RETRIEVE CHAT HISTORY
	//------------------------------------------------------------------------------------------------
	/**
	 * Returns an arrayList of messages, containing message objects for each message in that chat.
	 * The arrayList has the most recent message first.
	 */
	@Override
	public List<Message> getHistory(int chatID, int appTarget, int numberOfMessages)
	{
		List<Integer> users = new ArrayList<Integer>();
		List<String> names = new ArrayList<String>();
		List<Message> result = new ArrayList<Message>();
		
		// Create an array list of users.
		try
		{
			PreparedStatement getUserIds = conn.prepareStatement("SELECT DISTINCT users2chats.user_id, users.user_name FROM users2chats "
					+ "INNER JOIN users ON users2chats.user_id=users.user_id WHERE chat_id = ?");
			getUserIds.setInt(1, chatID);
			ResultSet userIds = getUserIds.executeQuery();
			while(userIds.next()){
				int resultID = userIds.getInt("user_id");
				String nameResult = userIds.getString("user_name");
				users.add(resultID);
				names.add(nameResult);
			}
			userIds.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to acces user information from the users2chats and users tables in the database." + "\n");
			e.printStackTrace();
		}
		
		User[] userArray = new User[users.size()];
		for (int i = 0; i < users.size(); i++)
		{
			userArray[i] = new User(users.get(i), names.get(i));
		}
		
		// Create the list of messages.
		try
		{
			PreparedStatement getMessageInfo = conn.prepareStatement("SELECT message_id, message_creator, metadata, message_content"
					+ " FROM messages WHERE chat_id = ? AND app_target = ? ORDER BY timemark ASC LIMIT ?");
			getMessageInfo.setInt(1, chatID);
			getMessageInfo.setInt(2, appTarget);
			getMessageInfo.setInt(3, numberOfMessages);
			ResultSet messageInfo = getMessageInfo.executeQuery();
			
			while(messageInfo.next())
			{
				int message_id = messageInfo.getInt("message_id");
				int message_creator = messageInfo.getInt("message_creator");
				String metadata = messageInfo.getString("metadata");
				String message_content = messageInfo.getString("message_content");
				Message message = new Message(userArray, chatID, appTarget, message_id, message_creator, metadata, message_content);
				result.add(message);
			}
			messageInfo.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access message information from the messages table of the database." + "\n");
			e.printStackTrace();
		}
		return result;
	}

	//------------------------------------------------------------------------------------------------
	//                                   CREATES A NEW MESSAGE
	//------------------------------------------------------------------------------------------------
	/**
	 * Inserts a new row into the messages table in the database.
	 * 
	 * Links the corresponding users to the messages in the users2messages table.
	 * 
	 * Returns false if:
	 * - The chat_id doesn't exist.
	 * - It is unable to retrieve corresponding users2chats
	 * - It unsuccessfully inserts the new messages into the messages table
	 * - It unsuccessfully finds the chat_id of the new chat
	 * - It is unable to insert new values into the users2messages table
	 * 
	 */
	@Override
    public boolean setMessage(int senderID, int chatID, int appTarget, String message, String meta)
	{
		boolean result = false;
		int messageID = 0;
		Set<Integer> userIds = new HashSet<Integer>();
		boolean seeIfExist = false;
		boolean retrieveUser = false;
		boolean insertMessages = false;
		boolean retrieveID = false;
		boolean insertUser2Messages = false;
		
		// Check to see whether that chat exists.
		try
		{
			int no = 0;
			PreparedStatement seeIfExists = conn.prepareStatement("SELECT COUNT (*) FROM chats WHERE chat_id = ?");
			seeIfExists.setInt(1, chatID);
			ResultSet exists = seeIfExists.executeQuery();
			while(exists.next())
			{
				no = exists.getInt("count");
			}
			if (no == 0)
			{
				return false;
			}
			else
			{
				seeIfExist = true;
			}
			exists.close();
		}
		catch (SQLException e2)
		{
			System.out.println("Was unable to select chat information from the chats table.");
			e2.printStackTrace();
		}
		
		// Retrieve a set of users from the chat_id.
		try
		{
			PreparedStatement retrieveUsers = conn.prepareStatement("SELECT DISTINCT user_id FROM users2chats WHERE chat_id = ?");
			retrieveUsers.setInt(1, chatID);
			ResultSet userResult = retrieveUsers.executeQuery();
			while(userResult.next())
			{
				int userID = userResult.getInt("user_id");
				userIds.add(userID);
			}
			retrieveUser = true;
			userResult.close();
		}
		catch (SQLException e1)
		{
			System.out.println("Was unable to access user information from the users table." + "\n");
			e1.printStackTrace();
		}
		
		// Insert into the messages table.
		try
		{
			PreparedStatement insertMessage = conn.prepareStatement("INSERT INTO messages(app_target, metadata, message_content,"
					+ " message_creator, chat_id) VALUES(?, ?, ?, ?, ?)");
			insertMessage.setInt(1, appTarget);
			insertMessage.setString(2, meta);
			insertMessage.setString(3, message);
			insertMessage.setInt(4, senderID);
			insertMessage.setInt(5, chatID);
			insertMessage.executeUpdate();
			
			insertMessages = true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to insert information into the messages table in the database." + "\n");
			e.printStackTrace();
		}
		
		// Retrieve the messageID of the newly created message.
		try
		{
			PreparedStatement getMessageID = conn.prepareStatement("SELECT message_id FROM messages ORDER BY message_id DESC LIMIT 1");
			ResultSet messageIDResult = getMessageID.executeQuery();
			while(messageIDResult.next())
			{
				messageID = messageIDResult.getInt("message_id");
			}
			
			messageIDResult.close();
			retrieveID = true;
		}
		catch (SQLException e1)
		{
			System.out.println("Was unable to access message information from the messages table in the database." + "\n");
			e1.printStackTrace();
		}
		
		// Insert into the users2messages table.
		try
		{
			PreparedStatement insertUser2Message = conn.prepareStatement("INSERT INTO users2messages(user_id, message_id) VALUES(?, ?)");
			Iterator<Integer> iterator = userIds.iterator();
			while(iterator.hasNext())
			{
				insertUser2Message.setInt(1, iterator.next());
				insertUser2Message.setInt(2, messageID);
				insertUser2Message.executeUpdate();
				insertUser2Message.close();
			}
			
			insertUser2Messages = true;
			
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to insert information into the users2messages table in the database." + "\n");
			e.printStackTrace();
		}
		
		if (seeIfExist == true && retrieveUser == true && insertMessages == true && retrieveID == true && insertUser2Messages == true)
		{
			result = true;
		}
		return result;
	}

	
	//------------------------------------------------------------------------------------------------
	//                                  RETRIEVE MESSAGE CONTENT
	//------------------------------------------------------------------------------------------------
	/**
	 * Returns the message_content from the messages table for the specified message.
	 */
    @Override
    public String getMessage(int chatID, int appTarget, String message, String meta)
    {
    	String result = "";
    	
    	try 
    	{
			PreparedStatement getMessage = conn.prepareStatement("SELECT message_content FROM messages WHERE chat_id = ? AND "
					+ "app_target = ? AND message_content = ? AND metadata = ?");
			getMessage.setInt(1, chatID);
			getMessage.setInt(2, appTarget);
			getMessage.setString(3, message);
			getMessage.setString(4, meta);
			
			ResultSet messageResult = getMessage.executeQuery();
			while(messageResult.next())
			{
				result = messageResult.getString("message_content");
			}
			messageResult.close();
		}
    	catch (SQLException e)
    	{
			System.out.println("Was unable to access message information from the messages table." + "\n");
			e.printStackTrace();
		}	
        return result;
    }
    
	//------------------------------------------------------------------------------------------------
	//                                   CLOSE THE CONNECTION WITH THE DATABASE
	//------------------------------------------------------------------------------------------------
    /**
     * Closes the connection with the database.
     */
    @Override
    public void close()
    {
		try
		{
			conn.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to close the connection with the database." + "\n");
			e.printStackTrace();
		}
    }

	//------------------------------------------------------------------------------------------------
	//                                 ADDS A NEW FRIENDS FOR A USER
	//------------------------------------------------------------------------------------------------
    /**
     * Adds a relationship between two users in the users2users table.
     * Returns true if successful and false if not.
     */
	@Override
	public boolean addFriend(int tooID, int fromID)
	{
		boolean alreadyThere = false;
		int count1 = 0;
		int count2 = 0;
		
		// Check to see whether that relationship already exists
		try
		{
			PreparedStatement checkFriend = conn.prepareStatement("SELECT COUNT(*) FROM users2users WHERE user_id1 = ? AND user_id2 = ?");
			checkFriend.setInt(1, tooID);
			checkFriend.setInt(2, fromID);
			ResultSet doesItExist1 = checkFriend.executeQuery();
			while(doesItExist1.next())
			{
				count1 = doesItExist1.getInt("count");
				if (count1 > 0)
				{
					alreadyThere = true;
				}
			}
			doesItExist1.close();
		}
		catch (SQLException e1)
		{
			System.out.println("Was unable to count information from the users2users table of the database.");
			e1.printStackTrace();
		}
		
		try
		{
			PreparedStatement checkFriend1 = conn.prepareStatement("SELECT COUNT(*) FROM users2users WHERE user_id1 = ? AND user_id2 = ?");
			checkFriend1.setInt(1, fromID);
			checkFriend1.setInt(2, tooID);
			ResultSet doesItExist2 = checkFriend1.executeQuery();
			while(doesItExist2.next())
			{
				count2 = doesItExist2.getInt("count");
				if (count2 > 0)
				{
					alreadyThere = true;
				}
			}
			doesItExist2.close();
		}
		catch (SQLException e1)
		{
			System.out.println("Was unable to count the information from the users2users table of the database.");
			e1.printStackTrace();
		}
		
		// If not, inserts that relationship into the table
		try
		{
			if (alreadyThere == false)
			{
				PreparedStatement addFriend = conn.prepareStatement("INSERT INTO users2users(user_id1, user_id2) VALUES(?, ?)");
				addFriend.setInt(1, tooID);
				addFriend.setInt(2, fromID);
				addFriend.executeUpdate();
				return true;
			}
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to insert information into the users2users table in the database." + "\n");
			e.printStackTrace();
		}
		return false;
	}

	//------------------------------------------------------------------------------------------------
	//                               GET THE USERS ASSOCIATED WITH A CHAT
	//------------------------------------------------------------------------------------------------
	/**
	 * NEED TO FINISH TESTING THIS!!!
	 * 
	 * Returns an array list of users associated with a chat_id.
	 */
	@Override
	public User[] getChatUsers(int chatID)
	{	
		List<Integer> ids = new ArrayList<Integer>();
		try
		{
			PreparedStatement getUserIds = conn.prepareStatement("SELECT DISTINCT * FROM users2chats WHERE chat_id = ?");
			getUserIds.setInt(1, chatID);
			ResultSet iDResult = getUserIds.executeQuery();
			while(iDResult.next())
			{
				int id = iDResult.getInt("user_id");
				ids.add(id);
			}
			iDResult.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to retrieve information from the users2chats table in the database." + "\n");
			e.printStackTrace();
		}
		
		User[] result = new User[ids.size()];
		Iterator<Integer> iterator = ids.iterator();
		try
		{
			PreparedStatement getUserNames = conn.prepareStatement("SELECT user_name FROM users WHERE user_id = ?");
			int counter = 0;
			
			while(iterator.hasNext())
			{
				int id = iterator.next();
				getUserNames.setInt(1, id);
				ResultSet nameResult = getUserNames.executeQuery();
				while(nameResult.next())
				{
					User user = new User(id, nameResult.getString("user_name"));
					result[counter] = user;
				}
				counter++;
				nameResult.close();
			}
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to retrieve user information from the users table in the database." + "\n");
			e.printStackTrace();
		}
		return result;
	}

	
	//------------------------------------------------------------------------------------------------
	//                                   RETRIEVE USERS FRIENDS
	//------------------------------------------------------------------------------------------------
	/**
	 * Returns an array list of users corresponding to the friends of the user with the inputted id.
	 */
	@Override
	public List<User> getFriends(int userID)
	{
		Set<Integer> friendIds = new HashSet<Integer>();
		List<User> result = new ArrayList<User>();
		try
		{
			PreparedStatement userId1 = conn.prepareStatement("SELECT user_id2 FROM users2users WHERE user_id1 = ?");
			userId1.setInt(1, userID);
			ResultSet user1Result = userId1.executeQuery();
			while(user1Result.next())
			{
				friendIds.add(user1Result.getInt("user_id2"));
			}
			user1Result.close();
		} 
		catch (SQLException e)
		{
			System.out.println("Was unable to access user_id2 from the users2users table in the database." + "\n");
			e.printStackTrace();
		}
		
		try
		{
			PreparedStatement userId2 = conn.prepareStatement("SELECT user_id1 FROM users2users WHERE user_id2 = ?");
			userId2.setInt(1, userID);
			ResultSet user2Result = userId2.executeQuery();
			while(user2Result.next())
			{
				friendIds.add(user2Result.getInt("user_id1"));
			}
			user2Result.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access user_id1 from the users2users table in the database." + "\n");
			e.printStackTrace();
		}
		
		Iterator<Integer> iterator = friendIds.iterator();
		try
		{
			PreparedStatement getName = conn.prepareStatement("SELECT user_name, bio FROM users WHERE user_id = ?");
			while(iterator.hasNext())
			{
				int currentID = iterator.next();
				getName.setInt(1, currentID);
				ResultSet name = getName.executeQuery();
				while(name.next())
				{
					String username = name.getString("user_name");
					String bio = name.getString("bio");
					User user = new User(currentID, username, bio);
					result.add(user);
				}
				name.close();
			}
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access user_name from the users table in the database." + "\n");
			e.printStackTrace();
		}
		return result;
	}
	
	
	//------------------------------------------------------------------------------------------------
	//                                  RETRIEVE A USERS CHATS
	//------------------------------------------------------------------------------------------------
	/**
	 * Returns an array list of the chat info from each chat a user is involved in.
	 */
	@Override
	public List<ChatInfo> getUserChats(int userID) 
	{
		List<ChatInfo> result = new ArrayList();
		List<Integer> chatIDs = new ArrayList();
		List<User[]> users = new ArrayList();
		
		// Find all of the chatIDs associated with a user.
		try
		{
			PreparedStatement getChatIDs = conn.prepareStatement("SELECT DISTINCT chat_id FROM users2chats WHERE user_id = ?");
			getChatIDs.setInt(1, userID);
			ResultSet chatIDResult = getChatIDs.executeQuery();
			while(chatIDResult.next())
			{
				int chatID = chatIDResult.getInt("chat_id");
				chatIDs.add(chatID);
			}
			chatIDResult.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access chat_id from the users2chats table in the database.");
			e.printStackTrace();
		}
		
		// For each chatID, create a user array.
		try
		{
			PreparedStatement getUserIDs = conn.prepareStatement("SELECT DISTINCT users2chats.user_id, users.user_name FROM users2chats INNER JOIN"
					+ " users ON users2chats.user_id=users.user_id WHERE chat_id = ?");
			Iterator<Integer> iterator = chatIDs.iterator();
			while(iterator.hasNext())
			{
				List<String> userNames = new ArrayList();
				
				// Create dynamic array list of the users.
				List<Integer> dynamicUser = new ArrayList();
				getUserIDs.setInt(1, iterator.next());
				ResultSet userResult = getUserIDs.executeQuery();
				while(userResult.next())
				{
					int userID1 = userResult.getInt("user_id");
					String userName = userResult.getString("user_name");
					dynamicUser.add(userID1);
					userNames.add(userName);
				}
				userResult.close();
				
				// Convert the dynamic array into a regular array of users
				int sizeOfArray = dynamicUser.size();
				User[] regularUser = new User[sizeOfArray];
				for (int i = 0; i < sizeOfArray; i++)
				{
					int userID2 = dynamicUser.get(i);
					String username = userNames.get(i);
					User user = new User(userID2, username);
					regularUser[i] = user;
				}
				
				// Add the regular array to the array of integer arrays
				users.add(regularUser);
			}
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access user information from the tables users2chats and users from the database." + "\n");
			e.printStackTrace();
		}
		
		// Create a ChatInfo object for each chatID and user array and add to the return list.
		int sizeOfArray = users.size();
		for (int i = 0; i < sizeOfArray; i++)
		{
			ChatInfo chatInfo = new ChatInfo(chatIDs.get(i), users.get(i));
			result.add(chatInfo);
		}
		return result;
	}

	
	//------------------------------------------------------------------------------------------------
	//                                  UPDATE A PROFILE
	//------------------------------------------------------------------------------------------------
	/**
	 * 
	 */
	@Override
	public boolean updateProfile(int userID, boolean[] edited, String[] fields)
	{
		boolean result = false;
		boolean usernameDone = false;
		boolean passwordDone = false;
		boolean bioDone = false;
		boolean aiDone = false;
		
		for (int i = 0; i < 4; i++)
		{
			String exists = "";
			// Username
			if (i == 0)
			{
				if (edited[i] == true)
				{
					String username = fields[i];
						// Check to see if that username already exists
						try
						{
							System.out.println("Reached start of method.");
							PreparedStatement checkUsername = conn.prepareStatement("SELECT user_name FROM users WHERE "
									+ "user_name = ?");
							checkUsername.setString(1, username);
							ResultSet existsR = checkUsername.executeQuery();
							System.out.println("Checking to see if that username exists....");
							while(existsR.next())
							{
								exists = existsR.getString("user_name");
							}
							existsR.close();
						}
						catch (SQLException e)
						{
							System.out.println("Was unable to access information from the users table of the database." + "\n");
							e.printStackTrace();
						}
						if (exists.equals(username))
						{
							System.out.println("That username already exists." + "\n");
						}
						// If the username doesn't already exist, create a new one.
						else
						{
							// INCLUDE CHANGING usernameDone to true here.
							try
							{
								PreparedStatement updateUsername = conn.prepareStatement("UPDATE users SET user_name = ? WHERE"
										+ " user_id = ?");
								updateUsername.setString(1, username);
								updateUsername.setInt(2, userID);
								updateUsername.executeUpdate();
								usernameDone = true;
							}
							catch (SQLException e)
							{
								System.out.println("Was unable to update information from the users table of the database." + "\n");
								e.printStackTrace();
							}
						}
				}
				else
				{
					usernameDone = true;
				}
			}
			// Password
			else if (i == 1)
			{
				if (edited[i] == true)
				{
					String password = fields[i];
					try
					{
						PreparedStatement updatePassword = conn.prepareStatement("UPDATE users SET password = ? WHERE "
								+ "user_id = ?");
						updatePassword.setString(1, password);
						updatePassword.setInt(2, userID);
						updatePassword.executeUpdate();
						passwordDone = true;
					}
					catch (SQLException e)
					{
						System.out.println("Was unable to update information from the users table of the database." + "\n");
						e.printStackTrace();
					}
				}
				else
				{
					passwordDone = true;
				}
			}
			// User bio
			else if (i == 2)
			{
				if (edited[i] == true)
				{
					String bio = fields[i];
					try
					{
						PreparedStatement updateBio = conn.prepareStatement("UPDATE users SET bio = ? WHERE user_id = ?");
						updateBio.setString(1, bio);
						updateBio.setInt(2, userID);
						updateBio.executeUpdate();
						bioDone = true;
					}
					catch (SQLException e)
					{
						System.out.println("Was unable to update information from the users table of the database." + "\n");
						e.printStackTrace();
					}
				}
				else
				{
					bioDone = true;
				}
			}
			// Avatar_Index
			else if (i ==3)
			{
				if (edited[i] == true)
				{
					String ai = fields[i];
					
					try
					{
						PreparedStatement updateAI = conn.prepareStatement("UPDATE users SET avatar_index = ? WHERE user_id = ?");
						updateAI.setString(1, ai);
						updateAI.setInt(2, userID);
						updateAI.executeUpdate();
						aiDone = true;
					}
					catch (SQLException e)
					{
						System.out.println("Was unable to update information from the users table of the database." + "\n");
						e.printStackTrace();
					}
				}
				else
				{
					aiDone = true;
				}
			}
		}
		if (usernameDone == true && passwordDone == true && bioDone == true
				&& aiDone == true)
		{
			result = true;
		}
		return result;
	}

	//------------------------------------------------------------------------------------------------
	//                                      GET USER
	//------------------------------------------------------------------------------------------------
	@Override
	public User getUser(int userID)
	{
		int userId = 0;
		String userName = "";
		String userBio = "";
		try
		{
			PreparedStatement getUserInfo = conn.prepareStatement("SELECT user_id, user_name, bio FROM users WHERE user_id = ?");
			getUserInfo.setInt(1, userID);
			ResultSet userResult = getUserInfo.executeQuery();
			while(userResult.next())
			{
				userId = userResult.getInt("user_id");
				userName = userResult.getString("user_name");
				userBio = userResult.getString("bio");
			}
			userResult.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access information from the users table of the database.");
			e.printStackTrace();
		}
		User newUser = new User(userId, userName, userBio);
		return newUser;
	}

	
	//------------------------------------------------------------------------------------------------
	//                                      GET PASSWORD
	//------------------------------------------------------------------------------------------------
	/**
	 * Returns an empty string if unsuccessful.
	 */
	@Override
	public String getPasword(int userID)
	{
		String result = "";
		try
		{
			PreparedStatement getPassword = conn.prepareStatement("SELECT password FROM users WHERE user_id = ?");
			getPassword.setInt(1, userID);
			ResultSet passwordResult = getPassword.executeQuery();
			while(passwordResult.next())
			{
				result = passwordResult.getString("password");
			}
			passwordResult.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access information from the users table of the database." + "\n");
			e.printStackTrace();
		}
		return result;
	}
 
	
	//------------------------------------------------------------------------------------------------
	//                                      GET SALT
	//------------------------------------------------------------------------------------------------
	@Override
	public byte[] getSalt(int userID)
	{
		byte[] result;
		
		try
		{
			PreparedStatement getSalt = conn.prepareStatement("SELECT salt FROM users WHERE user_id = ?");
			getSalt.setInt(1, userID);
			ResultSet saltResult = getSalt.executeQuery();
			while(saltResult.next())
			{
				result = saltResult.getBytes("salt");
				return result;
			}
			saltResult.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access salt from the users table of the database." + "\n");
			e.printStackTrace();
		}
		return null;
	}

	
	//------------------------------------------------------------------------------------------------
	//                                      SET PASSWORD
	//------------------------------------------------------------------------------------------------
	@Override
	public boolean setPassword(String username, String password)
	{
		try
		{
			PreparedStatement changePassword = conn.prepareStatement("UPDATE users SET password = ? WHERE"
					+ " user_name = ?");
			changePassword.setString(1, password);
			changePassword.setString(2, username);
			changePassword.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to update the password in the users table of the database." + "\n");
			e.printStackTrace();
		}
		return false;
	}

	
	//------------------------------------------------------------------------------------------------
	//                                      SET SALT
	//------------------------------------------------------------------------------------------------
	@Override
	public boolean setSalt(String username, byte[] salt)
	{
		try
		{
			PreparedStatement setSalt = conn.prepareStatement("UPDATE users SET salt = ? WHERE user_name = ?");
			setSalt.setBytes(1, salt);
			setSalt.setString(2, username);
			setSalt.executeUpdate();
			return true;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to update the salt in the users table of the database." + "\n");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public User[] deleteChat(int chatID)
	{
		int arraySize = 0;
		int[] iDs = null;
		User[] result;
		
		// Get the size of the user array	
		PreparedStatement sizeQ;
		try
		{
			sizeQ = conn.prepareStatement("SELECT DISTINCT COUNT(*) FROM users2chats WHERE chat_id = ?");
			sizeQ.setInt(1, chatID);
			ResultSet sizeResult = sizeQ.executeQuery();
			while(sizeResult.next())
			{
				arraySize = sizeResult.getInt("count");
			}
			sizeResult.close();
		}
		catch (SQLException e1)
		{
			System.out.println("Was unable to count the users in the users2chats table of the database." + "\n");
			e1.printStackTrace();
		}

		
		// Get an array of the userIDs.
		try
		{
			iDs = new int[arraySize];
			int counter = 0;
			PreparedStatement findUsers = conn.prepareStatement("SELECT DISTINCT user_id FROM users2chats WHERE"
					+ " chat_id = ?");
			findUsers.setInt(1, chatID);
			ResultSet userResult = findUsers.executeQuery();
			while(userResult.next())
			{
				iDs[counter] = userResult.getInt("user_id");
				counter ++;
			}
			userResult.close();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to select the user_id from the users table of the database." + "\n");
			e.printStackTrace();
		}
		
		// Delete the chat
		// Return the user array
		try
		{
			PreparedStatement deleteChat = conn.prepareStatement("DELETE FROM chats WHERE chat_id = ?");
			deleteChat.setInt(1, chatID);
			deleteChat.executeUpdate();
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to delete chats from the chat table of the database." + "\n");
			e.printStackTrace();
		}

		
		// Get an array of users.
		try
		{
			int counter = 0;
			String name = "";
			result = new User[arraySize];
			PreparedStatement getUserName = conn.prepareStatement("SELECT user_name FROM users WHERE user_id = ?");
			for (int userIDs : iDs)
			{
				getUserName.setInt(1, userIDs);
				ResultSet nameResult = getUserName.executeQuery();
				while(nameResult.next())
				{
					name = nameResult.getString("user_name");
				}
				User newUser = new User(userIDs, name);
				result[counter] = newUser;
				counter ++;
				nameResult.close();
			}
			return result;
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to access user_name from the users table of the database.");
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public boolean ifExists(String username)
	{
		int exist = 0;
		try
		{
			PreparedStatement checkIfExists = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE user_name = ?");
			checkIfExists.setString(1, username);
			ResultSet count = checkIfExists.executeQuery();
			while(count.next())
			{
				exist = count.getInt("count");
			}
			
			if (exist > 0)
			{
				return true;
			}
		}
		catch (SQLException e)
		{
			System.out.println("Was unable to count information from the users table of the database." + "\n");
			e.printStackTrace();
		}
		return false;
	}


}