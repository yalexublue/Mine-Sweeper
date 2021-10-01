
import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MS_server extends JFrame implements Runnable{
    private final int BOARD_SIZE_ROW = 16;
    private final int BOARD_SIZE_COL = 16;
    //add padding to deal with edge condition of the board
    public final int PADDING = 1;
    final int PADDED_ROW = BOARD_SIZE_ROW + PADDING * 2;
    final int PADDED_COL = BOARD_SIZE_COL + PADDING * 2;

    private ObjectOutputStream outputObjToClient;
    private ObjectInputStream inputObjFromClient;
    //private DataInputStream inputFromClient;
    private JTextArea ta;
    //Set<Socket> connections;

    // Number a client
    private int clientNo = 0;

    ArrayList<DataSave> temp_save = new ArrayList<DataSave>();

    public MS_server(){
        ta = new JTextArea(10,10);
        //connections = new HashSet<>();
        JScrollPane sp = new JScrollPane(ta);
        this.add(sp);
        this.setTitle("Mine Sweeper Server");
        this.setSize(400,200);
        Thread t = new Thread(this);
        t.start();
    }
    public void run() {
        try {
            // Create a server socket
            ServerSocket serverSocket = new ServerSocket(8000);
            ta.append("MultiThreadServer started at "
                    + new java.util.Date() + '\n');

            while (true) {
                // Listen for a new connection request
                Socket socket = serverSocket.accept();

                // Increment clientNo
                clientNo++;

                ta.append("Starting thread for client " + clientNo +
                        " at " + new Date() + '\n');

                // Find the client's host name, and IP address
                InetAddress inetAddress = socket.getInetAddress();
                ta.append("Client " + clientNo + "'s host name is "
                        + inetAddress.getHostName() + "\n");
                ta.append("Client " + clientNo + "'s IP Address is "
                        + inetAddress.getHostAddress() + "\n");

                // Create and start a new thread for the connection
                new Thread(new HandleAClient(socket, clientNo)).start();
            }
        }
        catch(IOException ex) {
            System.err.println(ex);
        }
    }
    // Define the thread class for handling new connection
    class HandleAClient implements Runnable {
        private Socket socket; // A connected socket
        private int clientNum;
        InetAddress inetAddress;
        // Construct a thread
        public HandleAClient(Socket socket, int clientNum) {
            this.socket = socket;
            this.clientNum = clientNum;
            //this.inetAddress = socket.getInetAddress();
        }

        // Run a thread
        public void run() {
            try {
                // Create data input and output streams
                inputObjFromClient = new ObjectInputStream(
                        socket.getInputStream());
                outputObjToClient = new ObjectOutputStream(
                        socket.getOutputStream());
                //ServerSocket serverSocket = new ServerSocket(8000);
                //ta.append("MultiThreadServer started at " + new Date() + '\n');

                // Continuously serve the client
                Object obj = inputObjFromClient.readObject();
                DataSave s = (DataSave)obj;
                temp_save.add(s);
                System.out.println("Save complete");
                //MS_client.BlocksData s1 = (MS_client.BlocksData)obj;
                ta.append("col is: " + s.AllBlockData[3][2].isFlagSave);
                //ta.append("remain flag is: " + remainFlag);
                outputObjToClient.writeObject(temp_save.get(0));
                System.out.println("Load complete");




            }
            catch(IOException | ClassNotFoundException ex) {
                System.err.println(ex);
            }
        }
    }
    public static void main(String[] args) {
        MS_server mts = new MS_server();
        mts.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mts.setVisible(true);
    }
}

