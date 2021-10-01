
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import javax.xml.crypto.Data;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.Date;

//TODO: Showing highest score
//multithreads server to handle multiple clients
public class MS_serverAlt2 extends JFrame implements Runnable{
    private final int BOARD_SIZE_ROW = 16;
    private final int BOARD_SIZE_COL = 16;
    //add padding to deal with edge condition of the board
    public final int PADDING = 1;
    final int PADDED_ROW = BOARD_SIZE_ROW + PADDING * 2;
    final int PADDED_COL = BOARD_SIZE_COL + PADDING * 2;

    //used to input from client and write out to client
    private ObjectOutputStream outputObjToClient;
    private ObjectInputStream inputObjFromClient;

    //GUI element to display connection
    private JTextPane ta;
    Connection connection =null;

    // Number a client
    static private int clientNo = 0;
    PreparedStatement insertStatement;

    public MS_serverAlt2(){
        ta = new JTextPane();
        JScrollPane sp = new JScrollPane(ta);
        this.add(sp);
        this.setTitle("Mine Sweeper Server");
        this.setSize(400,200);
        Thread t = new Thread(this);
        t.start();
    }
    public void run() {
        try {
            appendString("MultiThreadServer started at "
                    + new Date() + '\n');

        // Increment clientNo

        appendString("Starting thread for client " + clientNo +
                " at " + new Date() + '\n');

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection
                    ("jdbc:sqlite:MineSweeperSave.db");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(0);;
        }
        System.out.println("Database connected");
        String insertSQL = "Insert Into saves (ID, Data, Names, Score)" +
                "Values (?,?,?,?)";
        try {
            insertStatement = connection.prepareStatement(insertSQL);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        // Two different ports and threads are used to handle save and load from
        //one client thread.
        new Thread(new HandleAClientSave()).start();
        new Thread(new HandleAClientLoad()).start();

    }

    // This class is specifically used to handle save, which is client's
    //data input
    class HandleAClientSave implements Runnable {
        ServerSocket serverSocket;
        Socket socket;
        // Construct a thread, this thread will only run when port 8000 is requested
        //which will be sent out when user hit save button on client side.
        public void run() {
            try {
                clientNo++;
                serverSocket = new ServerSocket(8000);

                while (true) {
                    socket = serverSocket.accept();
                    // Find the client's host name, and IP address
                    InetAddress inetAddress = socket.getInetAddress();
                    appendString("Client " + clientNo + "'s host name is "
                            + inetAddress.getHostName() + "\n");
                    appendString("Client " + clientNo + "'s IP Address is "
                            + inetAddress.getHostAddress() + "\n");
                    // Create data input and output streams
                    inputObjFromClient = new ObjectInputStream(socket.getInputStream());
                    // Continuously serve the client
                    Object obj = inputObjFromClient.readObject();
                    DataSave s = (DataSave) obj;
                    String temp_name = s.player;
                    //shorten the time number to save into database.
                    //only second will be saved.
                    int temp_score = (int)s.remainTime/1000;

                    //These steps are used to convert the input object into a
                    //byte stream to store in data base.
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream out = null;
                    out = new ObjectOutputStream(bos);
                    out.writeObject(s);
                    out.flush();
                    byte[] saveBytes = bos.toByteArray();

                    //write save data into database. The core is only the
                    //DataSave object. Others are used as reference for
                    //easier access
                    insertStatement.setInt(1, clientNo);
                    insertStatement.setBytes(2, saveBytes);
                    insertStatement.setString(3, temp_name);
                    insertStatement.setInt(4, temp_score);
                    insertStatement.execute();

                    System.out.println("Save complete");
                }
            }
            catch(IOException | ClassNotFoundException | BadLocationException | SQLException ex) {
                System.err.println(ex);
            }
        }
    }

    //This class like save class, will only start to run when port 8001
    //receive a request from client, which will be sent out by pressing the
    //load button.
    class HandleAClientLoad implements Runnable {
        ServerSocket serverSocket;
        Socket socket;
        // Construct a thread
        public void run() {
            try {
                clientNo++;
                serverSocket = new ServerSocket(8001);
                while (true) {
                    socket = serverSocket.accept();
                    // Find the client's host name, and IP address
                    InetAddress inetAddress = socket.getInetAddress();
                    appendString("Client " + clientNo + "'s host name is "
                            + inetAddress.getHostName() + "\n");
                    appendString("Client " + clientNo + "'s IP Address is "
                            + inetAddress.getHostAddress() + "\n");

                    outputObjToClient = new ObjectOutputStream(
                            socket.getOutputStream());

                    //A new pop up frame will be initiate here to load game
                    JFrame frame = new JFrame("All Saves");
                    Statement stm = connection.createStatement();
                    ResultSet rs = stm.executeQuery("SELECT * from saves");
                    //Using this array list is simply to organize database object into
                    //a GUI element. Here I used JComboBox
                    ArrayList<DataSave> temp_save = new ArrayList<DataSave>();

                    //These are used to transform the ByteArray back to the original
                    //Object to be sent back to client.
                    while (rs.next()){
                        byte[] buf = rs.getBytes(2);
                        ObjectInputStream objectIn = null;
                        if (buf != null)
                            objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
                        Object deSerializaedObject = objectIn.readObject();
                        DataSave receivedObject = (DataSave) deSerializaedObject;
                        temp_save.add(receivedObject);
                    }
                    rs.close();
                    //first sort, to sort out the clearing games saves from unfinished
                    //and blowing up game saves
                    for (int i = 0; i < temp_save.size() - 1; i += 1){
                        if (temp_save.get(i).remainMine == 0)
                            continue;
                        for (int j = i + 1; j < temp_save.size(); j += 1) {
                            if (temp_save.get(j).remainMine == 0) {
                                Collections.swap(temp_save, i, j);
                                break;
                            }
                        }
                    }
                    //second sort, make sure the save with a cleared data and
                    //maximum time remaining stays on top.
                    for (int i = 0; i < temp_save.size() - 1; i += 1){
                        if (temp_save.get(i).remainMine != 0)
                            break;
                        int max_idx = i;
                        for (int j = i + 1; j < temp_save.size(); j += 1) {
                            if (temp_save.get(j).remainMine != 0) {
                                break;
                            }
                            if (temp_save.get(j).remainTime > temp_save.get(max_idx).remainTime)
                                max_idx = j;
                        }
                        Collections.swap(temp_save, i, max_idx);
                    }
                    // create a combo box
                    JComboBox comboBox = new JComboBox(temp_save.toArray());
                    comboBox.setEditable(true);
                    // create a button; when it's pressed, print out
                    // the selection in the list
                    JButton button = new JButton("Load Data");
                    button.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            Object[] selection = comboBox.getSelectedObjects();
                            try {
                                outputObjToClient.writeObject((DataSave)selection[0]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Load complete");
                        }
                    });
                    Container c = frame.getContentPane();
                    JLabel message = new JLabel("Highest completion score is displayed on top");
                    JPanel comboPanel = new JPanel();
                    comboPanel.add(comboBox);
                    comboPanel.add(message);
                    c.add(comboPanel, BorderLayout.CENTER);
                    //c.add(new JScrollPane(list), BorderLayout.CENTER);
                    c.add(button, BorderLayout.SOUTH);

                    frame.setSize(500, 300);
                    frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
                    frame.setVisible(true);

                }
            }
            catch(IOException | BadLocationException | SQLException | ClassNotFoundException ex) {
                System.err.println(ex);
            }
        }
    }
    public void appendString(String str) throws BadLocationException
    {
        StyledDocument document = (StyledDocument) ta.getDocument();
        document.insertString(document.getLength(), str, null);
    }
    public static void main(String[] args) {
        MS_serverAlt2 mts = new MS_serverAlt2();
        mts.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mts.setVisible(true);
    }
}

