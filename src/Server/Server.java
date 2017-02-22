package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Laurie Dugdale
 */
public class Server implements Runnable {

    private int port;
    private ServerGUI sg;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped = false;
    protected Thread runningThread= null;
    protected ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public Server(int port, ServerGUI sg) {

        this.port = port;

        this.sg = sg;

    }

    public void run(){

        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()){

            Socket clientSocket = null;
            try {

                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {

                if(isStopped()) {

                    System.out.println("Server Stopped.") ;
                    break;
                }

                throw new RuntimeException( "Error accepting client connection", e);
            }
            this.threadPool.execute( new Packets(clientSocket));
        }
        this.threadPool.shutdown();
        System.out.println("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {

        return this.isStopped;
    }

    public synchronized void stop(){

        this.isStopped = true;
        try {

            this.serverSocket.close();
        } catch (IOException e) {

            throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {

        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port", e);
        }
    }
}
