CREATE TABLE chats(
	chat_id SERIAL NOT NULL,
	PRIMARY KEY(chat_id)
);

CREATE TABLE users(
	user_id SERIAL NOT NULL,
	user_name VARCHAR(40) NOT NULL,
	user_name_edited BOOLEAN NOT NULL,
	password VARCHAR(40) NOT NULL,
	password_edited BOOLEAN NOT NULL,
	bio TEXT,
	bio_edited BOOLEAN,
	most_recent_chat INT,
	PRIMARY KEY(user_id),
	CONSTRAINT most_recent_chat
		FOREIGN KEY(most_recent_chat)
		REFERENCES chats(chat_id)
		ON DELETE SET DEFAULT
);

CREATE TABLE users2chats(
	user_id INT NOT NULL,
	chat_id INT NOT NULL,
	CONSTRAINT user_id
		FOREIGN KEY(user_id)
		REFERENCES users(user_id)
		ON DELETE CASCADE,
	CONSTRAINT chat_id
		FOREIGN KEY(chat_id)
		REFERENCES chats(chat_id)
		ON DELETE CASCADE
);

CREATE TABLE users2users(
	user_id1 INT NOT NULL,
	user_id2 INT NOT NULL,
	CONSTRAINT user_id1
		FOREIGN KEY(user_id1)
		REFERENCES users(user_id)
		ON DELETE CASCADE,
	CONSTRAINT user_id2
		FOREIGN KEY(user_id2)
		REFERENCES users(user_id)
		ON DELETE CASCADE
);

CREATE TABLE messages(
	message_id SERIAL NOT NULL,
	app_target INT,
	metadata TEXT,
	message_content TEXT,
	timemark TIMESTAMP NOT NULL,
	message_creator INT,
	chat_id INT NOT NULL,
	PRIMARY KEY(message_id),
	CONSTRAINT message_creator
		FOREIGN KEY(message_creator)
		REFERENCES users(user_id)
		ON DELETE NO ACTION,
	CONSTRAINT chat_id
		FOREIGN KEY(chat_id)
		REFERENCES chats(chat_id)
		ON DELETE CASCADE
);

CREATE TABLE users2messages(
	user_id INT NOT NULL,
	message_id INT NOT NULL,
	CONSTRAINT user_id
		FOREIGN KEY(user_id)
		REFERENCES users(user_id)
		ON DELETE CASCADE,
	CONSTRAINT message_id
		FOREIGN KEY(message_id)
		REFERENCES messages(message_id)
		ON DELETE CASCADE
);
