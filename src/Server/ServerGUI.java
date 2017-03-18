package Server;

import javax.swing.*;

import org.postgresql.translation.messages_bg;

import messengerUI.CreateUserDialog;

import java.awt.*;
import java.awt.event.*;

/**
 * Created by mnt_x on 22/02/2017.
 */
public class ServerGUI extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 1L;
    private JButton stopStart;
    private JTextArea activity;
    private JTextArea messages;
    private JTextField portField;
    private Server server;
    private int port;


    // server constructor that receive the port to listen to for connection as parameter
    ServerGUI(int port) {
    	
        super("Notsapp chat Server");
        this.port = port;
        server = null;   
        // create the interface
        createInterface();
    }
    
    public int getPort(){
    	return this.port;
    }
    
    public void createInterface(){
    	
        Font standardFont = new Font("Ariel", Font.PLAIN, 14);
        Color keyColor = new Color(51, 102, 153);
        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        
        // Whatsapp server title
		JPanel titleContainer = new JPanel();
		JLabel title = new JLabel("Notsapp Server");
		title.setFont(new Font("Ariel", Font.PLAIN, 28));
		title.setForeground(Color.WHITE);
		title.setBounds(34, 25, 61, 16);
		titleContainer.add(title);
		titleContainer.setBackground(keyColor);
		north.add(titleContainer);
		
		// server controls
		JPanel controlContainer = new JPanel();
		controlContainer.setBackground(Color.white);
		// port label
		JLabel portLabel = new JLabel("Port number: ");
		portLabel.setBounds(117, 192, 91, 29);
		controlContainer.add(portLabel);
		// port text box
		portField = new JTextField("" + getPort());
		portField.setPreferredSize(new Dimension(80, 30));
		portField.setFont(standardFont);
		controlContainer.add(portField);
		// start and stop button
		stopStart = new JButton("Start");
		stopStart.setBackground(keyColor);
		stopStart.setPreferredSize(new Dimension(80, 30));
		stopStart.setForeground(Color.white);
		stopStart.setFont(standardFont);
		stopStart.setBounds(34, 172, 91, 29);
		stopStart.addActionListener(this);
		
		// add the panels
		controlContainer.add(stopStart);		
		north.add(controlContainer);        
		add(north, BorderLayout.NORTH);
		
        
		// The center of the GUI containing two text areas displaying server messages
        JPanel center = new JPanel(new GridLayout(2,1));        
        GridBagConstraints c1 = new GridBagConstraints();
        GridBagConstraints c2 = new GridBagConstraints(); 
        JPanel centerTop = new JPanel(new GridBagLayout());
        // activity title
        JLabel aLabel = new JLabel("Server activity");
        aLabel.setFont(standardFont);
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.weightx = 1;
        c2.weighty = 0;
        c2.anchor = GridBagConstraints.PAGE_START;
        c2.insets = new Insets(10,5,5,0);
        c2.gridwidth = 2;
        c2.gridx = 0;
        c2.gridy = 0;        
        centerTop.add(aLabel, c2);
        centerTop.setBackground(Color.white);
        // activity text area
        activity = new JTextArea(80,160);
        activity.setEditable(false);
        c2.fill = GridBagConstraints.BOTH;
        c2.weighty = 1.0; 
        c2.gridx = 0;
        c2.gridy = 1;
        c2.insets = new Insets(0,0,0,0);
        centerTop.add(new JScrollPane(activity), c2);
        center.add(centerTop);
        center.setBackground(Color.white);
        // messages title
        JPanel centerBottom = new JPanel(new GridBagLayout());
        JLabel mLabel = new JLabel("Client messages");
        mLabel.setFont(standardFont);
        c1.fill = GridBagConstraints.HORIZONTAL;
        c1.weightx = 1;
        c1.weighty = 0; 
        c1.anchor = GridBagConstraints.PAGE_START;
        c1.insets = new Insets(10,5,5,0);
        c1.gridwidth = 2;
        c1.gridx = 0;
        c1.gridy = 0;
        centerBottom.setBackground(Color.white);
        centerBottom.add(mLabel, c1);
        // messages text area
        messages = new JTextArea(80, 160);
        messages.setEditable(false);  
        c1.fill = GridBagConstraints.BOTH;
        c1.weighty = 1.0; 
        c1.gridx = 0;
        c1.gridy = 1;
        c1.insets = new Insets(0,0,0,0);
        centerBottom.add(new JScrollPane(messages), c1);
        center.add(centerBottom);
        add(center);

        // need to be informed when the user click the close button on the frame
        addWindowListener(this);
        setDefaultLookAndFeelDecorated(true);
        setSize(400, 600);
        setVisible(true);
    }

    /**
     * append activity to the end
     * @param str
     */
    void appendActivity(String str) {
    	
        activity.append(str);
        activity.setCaretPosition(activity.getText().length() - 1);

    }

    /**
     * append activity to the end
     * @param str
     */
    void appendMessages(String str) {
    	
    	messages.append(str);
    	messages.setCaretPosition(messages.getText().length() - 1);
    }
    
    // start or stop where clicked
    public void actionPerformed(ActionEvent e) {
    	
        // if running we have to stop
        if(server != null) {
            server.stop();
            server = null;
            portField.setEditable(true);
            stopStart.setText("Start");
            return;
        }
        // OK start the server
        int port;
        try {
        	
            port = Integer.parseInt(portField.getText().trim());
        }
        catch(Exception er) {
        	
            appendActivity("Invalid port number");
            return;
        }
        // create Server and run it on a thread
        server = new Server(port, this);
        new ServerRunning().start();
        // change button to stop
        stopStart.setText("Stop");
        appendActivity("Server started\n");
        // set port textfield to not be editable
        portField.setEditable(false);
    }

    // entry point to start the Server
    public static void main(String[] arg) {
    	
        // default port 1080
        new ServerGUI(54321);
    }

    /**
     * Close server properly on window close
     *
     * @param e
     */
    public void windowClosing(WindowEvent e) {
        // if the server exists
        if(server != null) {
            try {
                server.stop();
            }
            catch(Exception eClose) {
            }
            server = null;
        }
        // dispose the frame
        dispose();
        System.exit(0);
    }
    // Ignore other listner methods for the window
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    /*
     * A thread to run the Server
     */
    class ServerRunning extends Thread {
    	
        public void run() {
        	
            server.run();
            // the server failed
            stopStart.setText("Start");
            portField.setEditable(true);
            server = null;
        }
    }
}
