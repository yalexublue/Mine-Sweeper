import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Random;


//block with 0 marked will also unlock other blocks with 0.
// will stop upon a number (mine nearby) block is met
//A time count should be saved
//current game states should be saved to a data base, it means the game can save load!
//a menu is also needed, (drop down) should have save option in it.
//open button should open a game saved in database, to overlap the current game,
//this is done to a database in server, the server should also be multi thread,
//it should connecting to an open port in server to save to the data base,
//the thread each time should be unique.
//saving highest score is optional

public class MS_client extends JFrame{
    Timer t = new Timer();
    private long elapsed = 0;

    DataOutputStream toServer = null;
    ObjectOutputStream toServerObj = null;
    DataInputStream fromServer = null;
    ObjectInputStream fromServerObj = null;
    Socket socket = null;
    int saveID = 0;

    String player_name = "Player";
    DataSave load;
    final int MINE_COUNT = 40;
    int remaining_mine = MINE_COUNT;
    int flag_number = MINE_COUNT;
    boolean is_complete = false;
    int[][] mine_location;

    JLabel countDisplay;
    private final int BOARD_SIZE_ROW = 16;
    private final int BOARD_SIZE_COL = 16;
    //add padding to deal with edge condition of the board
    public final int PADDING = 1;
    final int PADDED_ROW = BOARD_SIZE_ROW + PADDING * 2;
    final int PADDED_COL = BOARD_SIZE_COL + PADDING * 2;
    //init the board
    Blocks [][] field = new Blocks[PADDED_ROW][PADDED_COL];
    public MS_client(){
        JPanel boardPanel = new JPanel(new GridLayout(BOARD_SIZE_ROW,BOARD_SIZE_COL));

        for (int i = 0; i < PADDED_ROW; i += 1){
            for (int j = 0; j < PADDED_COL; j += 1){
                field[i][j] = new Blocks("10.png", i, j);
                if (i != 0 && i != PADDED_ROW - 1 && j != 0 && j != PADDED_COL - 1)
                    boardPanel.add(field[i][j]);
            }
        }
        mine_location = setMine();
        for (int i = 0; i < MINE_COUNT; i += 1){
            field[mine_location[i][0]][mine_location[i][1]].is_mine = true;
        }
        //flagCount = 40;
        countDisplay = new JLabel(String.valueOf(flag_number));
        JMenuBar menu = createMenu();
        this.add(menu, BorderLayout.NORTH);
        this.add(boardPanel);
        this.add(countDisplay, BorderLayout.SOUTH);
        this.setSize(400, 450);
        this.setResizable(false);
        /*
        try {
            socket = new Socket("localhost", 8000);
            System.out.println("connected");
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            System.out.println("connection Failure");
        }

         */
    }
    public JMenuBar createMenu(){
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu menu = new JMenu("File");
        //new
        JMenuItem newGame = new JMenuItem("New");
        class NewGameListener implements ActionListener
        {
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        }
        NewGameListener listener = new NewGameListener();
        newGame.addActionListener(listener);
        //open
        JMenuItem open = new JMenuItem("Open");

        class OpenFileListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                //TODO: how to open a file from save
                try {
                    socket = new Socket("localhost", 8001);
                    fromServerObj = new ObjectInputStream(socket.getInputStream());
                    load = (DataSave) fromServerObj.readObject();
                    System.out.println("load complete");
                    System.out.println(load.remainFlag);

                    //field = fromServer.readByte();
                    //textArea.append("connected");
                } catch (IOException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                    //textArea.append("connection Failure");
                }
            }
        }
        OpenFileListener open_listener = new OpenFileListener();
        open.addActionListener(open_listener);
        JMenuItem save = new JMenuItem("Save");
        class SaveFileListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    socket = new Socket("localhost", 8000);

                    BlockData[][] s1 = new BlockData[PADDED_ROW][PADDED_COL];
                    for (int i = 0; i < PADDED_ROW; i+=1){
                        for (int j = 0; j < PADDED_COL; j+=1){
                            Blocks temp = field[i][j];
                            s1[i][j] = new BlockData();
                            //System.out.println(temp.row);
                            s1[i][j].update(temp.row, temp.col, temp.mine_around, temp.is_mine,
                                    temp.is_flag, temp.is_revealed);
                        }
                    }
                    //BlockData s2 = new BlockData();
                    //s2.update(3, 2, 4, false, false, false);

                    toServerObj = new ObjectOutputStream(socket.getOutputStream());
                    DataSave save1 = new DataSave();
                    player_name = t.name.getText();
                    //elapsed = Long.parseLong(t.startStopButton.getText());
                    System.out.println("save time is: " + elapsed);
                    System.out.println("Display time is: " + t.startStopButton.getText());
                    //TODO : add player name here
                    save1.update(player_name, flag_number, remaining_mine, MS_client.this.elapsed, is_complete, s1);
                    toServerObj.writeObject(save1);
                    toServerObj.flush();
                    //create output object
                    //textArea.append("connected");
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    //textArea.append("connection Failure");
                }
            }
        }
        SaveFileListener save_listener = new SaveFileListener();
        save.addActionListener(save_listener);


        //exit
        JMenuItem exit = new JMenuItem("Exit");
        class ExitItemListener implements ActionListener
        {
            public void actionPerformed(ActionEvent event)
            {
                System.exit(0);
            }
        }
        ActionListener exit_listener = new ExitItemListener();
        exit.addActionListener(exit_listener);

        menu.add(newGame);
        menu.add(open);
        menu.add(save);
        menu.add(exit);
        menuBar.add(menu);

        return menuBar;
    }
    public void check_win() throws InterruptedException {
        if (remaining_mine == 0 && flag_number == 0) {
            countDisplay.setText("You win!");
            is_complete = true;
            t.isRunning = false;
            t.updater.join();

        }

    }
    /*
    public void reveal_neighbor_nw(Blocks target, Blocks[][] mine_field){
        target.mine_update(mine_field);
        if (target.mine_around != 0 || target.row == 1 || target.col == 1){
            block_handling(target, true);
        }
        else {
            block_handling(target, false);
            reveal_neighbor_nw(mine_field[target.row - 1][target.col], mine_field);
            reveal_neighbor_nw(mine_field[target.row][target.col - 1], mine_field);
        }
    }
    public void reveal_neighbor_ne(Blocks target, Blocks[][] mine_field){
        target.mine_update(mine_field);
        if (target.mine_around != 0 || target.row == 1 || target.col == PADDED_COL - PADDING * 2){
            block_handling(target, true);
        }
        else {
            block_handling(target, false);
            reveal_neighbor_ne(mine_field[target.row - 1][target.col], mine_field);
            reveal_neighbor_ne(mine_field[target.row][target.col + 1], mine_field);
        }
    }
    public void reveal_neighbor_sw(Blocks target, Blocks[][] mine_field){
        target.mine_update(mine_field);
        if (target.mine_around != 0 || target.row == PADDED_ROW - PADDING * 2 || target.col == 1){
            block_handling(target, true);
        }
        else {
            block_handling(target, false);
            reveal_neighbor_sw(mine_field[target.row + 1][target.col], mine_field);
            reveal_neighbor_sw(mine_field[target.row][target.col - 1], mine_field);
        }
    }
    public void reveal_neighbor_se(Blocks target, Blocks[][] mine_field){
        target.mine_update(mine_field);
        if (target.mine_around != 0 || target.row == PADDED_ROW - PADDING * 2
                || target.col == PADDED_COL - PADDING * 2){
            block_handling(target, true);
        }
        else {
            block_handling(target, false);
            reveal_neighbor_se(mine_field[target.row + 1][target.col], mine_field);
            reveal_neighbor_se(mine_field[target.row][target.col + 1], mine_field);
        }
    }

     */
    public Blocks[] get_neighbours(int row, int col){
        Blocks[] out_nei = new Blocks[4];
        out_nei[0] = field[row - 1][col];
        out_nei[1] = field[row][col - 1];
        out_nei[2] = field[row + 1][col];
        out_nei[3] = field[row][col + 1];

        return out_nei;
    }

    public void reveil_cell(int row, int col){
        if (row == 0 || row == PADDED_ROW - PADDING ||
                col == 0 || col == PADDED_COL - PADDING){
            return;
        }
        field[row][col].mine_update();
        if (field[row][col].mine_around != 0){
            block_handling(field[row][col], true);
        }
        /*
        else if (row == PADDING || row == PADDED_ROW - 2 * PADDING ||
                col == PADDING || col == PADDED_COL - 2 * PADDING){
            block_handling(field[row][col], false);
        }

         */

         /*
        else if ((row == PADDING && col == PADDING) || (row == PADDED_ROW - 2 * PADDING
        && col == PADDING) || (row == PADDING && col == PADDED_COL - 2 * PADDING) ||
                (row == PADDED_ROW - 2 * PADDING && col == PADDED_COL - 2 * PADDING)){
            block_handling(field[row][col], false);
        }


        else if (row == PADDING || row == PADDED_ROW - 2 * PADDING){
            block_handling(field[row][col], false);
            for (Blocks c : get_neighbours_horizontal(row, col)) {
                if (!c.is_mine && !c.is_revealed) {
                    reveil_cell(c.row, c.col);
                }
            }
        }


        else if (col == PADDING || col == PADDED_COL - 2 * PADDING){
            block_handling(field[row][col], false);
            for (Blocks c : get_neighbours_horizontal(row, col)) {
                if (!c.is_mine && !c.is_revealed) {
                    reveil_cell(c.row, c.col);
                }
            }
        }

          */
        else {
            block_handling(field[row][col], false);
            for (Blocks c : get_neighbours(row, col)) {
                if (!c.is_mine && !c.is_revealed) {
                    reveil_cell(c.row, c.col);
                }
            }
        }
    }
    //TODO: repaint, another loop repainting
    public void reveal_all() throws InterruptedException {
        for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1){
            for (int j = PADDING; j < PADDED_COL - PADDING; j += 1){
                if (field[i][j].is_mine && field[i][j].is_flag)
                    field[i][j].update("11.png");
                else if (!field[i][j].is_mine && field[i][j].is_flag)
                    field[i][j].update("12.png");
                else if (field[i][j].is_mine)
                    field[i][j].update("9.png");

                field[i][j].is_revealed = true;
            }
        }
        for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1) {
            for (int j = PADDING; j < PADDED_COL - PADDING; j += 1) {
                field[i][j].repaint();
            }
        }
        countDisplay.setText("Boom!");
        t.isRunning = false;
        t.updater.join();
        is_complete = true;
        System.out.println(elapsed);
    }

    public void block_handling(Blocks target, boolean has_mine_around){
        if (!has_mine_around){
            target.update("0.png");
        }
        else {
            switch(target.mine_around){
                case 1:
                    target.update("1.png");
                    break;
                case 2:
                    target.update("2.png");
                    break;
                case 3:
                    target.update("3.png");
                    break;
                case 4:
                    target.update("4.png");
                    break;
                case 5:
                    target.update("5.png");
                    break;
                case 6:
                    target.update("6.png");
                    break;
                case 7:
                    target.update("7.png");
                    break;
                case 8:
                    target.update("8.png");
                    break;
            }
        }
        target.is_revealed = true;
        target.repaint();
    }

    //class for each block display.
    public class Blocks extends JPanel{
        int row;
        int col;
        int mine_around = 0;
        private Image img;
        boolean is_mine = false;
        boolean is_flag = false;
        boolean is_revealed = false;
        public Blocks(String img, int in_row, int in_col) {
            this(new ImageIcon(img).getImage());
            this.addMouseListener(new block_clicked());
            row = in_row;
            col = in_col;
        }

        public Blocks(Image img) {
            this.img = img;
            Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
            setPreferredSize(size);
            setLayout(null);
        }
        public void update(String img) {
            this.img = new ImageIcon(img).getImage();
        }

        public void paintComponent(Graphics g) {
            g.drawImage(img, 0, 0,25, 25, null);
        }
        public class block_clicked extends MouseAdapter{
            @Override
            public void mouseClicked(MouseEvent e) {
                if(!Blocks.this.is_revealed) {
                    if (e.getModifiers() == MouseEvent.BUTTON3_MASK && e.getClickCount() == 1) {

                        if (flag_number == 0)
                            countDisplay.setText("No flag is available");
                        else{
                            flag_number -= 1;
                            countDisplay.setText(String.valueOf(flag_number));
                            Blocks.this.update("11.png");
                            Blocks.this.is_flag = true;
                            Blocks.this.is_revealed = true;
                            Blocks.this.repaint();
                            if (Blocks.this.is_mine)
                                remaining_mine -= 1;
                            try {
                                check_win();
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                        }
                    }
                    else if (e.getModifiers() == MouseEvent.BUTTON1_MASK && e.getClickCount() == 1) {
                         if (Blocks.this.is_mine) {
                            Blocks.this.update("9.png");
                            Blocks.this.repaint();
                             try {
                                 reveal_all();
                             } catch (InterruptedException interruptedException) {
                                 interruptedException.printStackTrace();
                             }
                         } else
                            reveil_cell(Blocks.this.row, Blocks.this.col);
                    }
                }
                else if(Blocks.this.is_flag){
                    if (e.getModifiers() == MouseEvent.BUTTON3_MASK && e.getClickCount() == 1) {
                        Blocks.this.update("10.png");
                        Blocks.this.is_flag = false;
                        flag_number += 1;
                        countDisplay.setText(String.valueOf(flag_number));
                        if (Blocks.this.is_mine)
                            remaining_mine += 1;
                        Blocks.this.is_revealed = false;
                        Blocks.this.repaint();
                    }
                }
            }
        }
        public void mine_update(){
            this.mine_around = 0;
            for (int r_offset = -1; r_offset <= 1; r_offset += 1) {
                for (int c_offset = -1; c_offset <= 1; c_offset += 1){
                    if (r_offset == 0 && c_offset == 0)
                        continue;
                    else {
                        Blocks curr_block = field[row + r_offset][col + c_offset];
                        if (curr_block.is_mine) {
                            this.mine_around += 1;
                        }
                    }
                }
            }
        }
    }
    //tested, array correctly generated
    public int[][] setMine(){
        int [][] mine_index = new int[MINE_COUNT][2];
        for (int i = 0; i < MINE_COUNT; i += 1){
            //generate mine only within the non-padded region
            mine_index[i][0] = generateRandom(true, PADDING, PADDED_ROW - PADDING * 2);
            mine_index[i][1] = generateRandom(true, PADDING, PADDED_COL - PADDING * 2);
        }
        for (int i = 0; i < MINE_COUNT; i += 1){
            field[mine_index[i][0]][mine_index[i][1]].is_mine = true;
        }

        return mine_index;
    }
    //TODO: repaint(), update model then repaint()
    public void reset(){
        for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1){
            for (int j = PADDING; j < PADDED_COL - PADDING; j += 1){
                field[i][j].update("10.png");
                //field[i][j].repaint();
                field[i][j].is_revealed = false;
                field[i][j].is_mine = false;
                field[i][j].is_flag = false;
            }
        }
        for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1) {
            for (int j = PADDING; j < PADDED_COL - PADDING; j += 1) {
                field[i][j].repaint();
            }
        }
        mine_location = setMine();
        flag_number = MINE_COUNT;
        countDisplay.setText(String.valueOf(flag_number));

    }
    public int generateRandom(boolean is_row, int min, int max){
        //designed to generate according to either row or column of the board
        //increase code usability.
        Random rand = new Random();
        if (is_row)
            return (int)Math.floor(Math.random()*(max-min+1)+min);
        else
            return (int)Math.floor(Math.random()*(max-min+1)+min);
    }


    public class Timer extends JFrame implements ActionListener, Runnable {
        private long startTime = 1000 * 1000 + System.currentTimeMillis();
        private JTextField name = new JTextField();
        private JLabel instruction = new JLabel("Please input your name");

        //private final static java.text.SimpleDateFormat timerFormat = new java.text.SimpleDateFormat("ss");
        private final JButton startStopButton = new JButton("Confirm");
        private Thread updater;
        private boolean isRunning = false;
        private final Runnable displayUpdater = new Runnable() {
            public void run() {
                displayElapsedTime(Timer.this.startTime - System.currentTimeMillis());
            }
        };

        public void actionPerformed(ActionEvent ae) {
            if (isRunning) {
                elapsed = startTime - System.currentTimeMillis();
                System.out.println(elapsed);
                isRunning = false;
                instruction.setText("Save or not?");
                /*
                try {
                    updater.join();
                    // Wait for updater to finish
                } catch (InterruptedException ie) {
                }

                 */
                displayElapsedTime(elapsed);
                // Display the end-result
            } else {
                player_name = name.getText();
                isRunning = true;
                updater = new Thread(this);
                updater.start();
                instruction.setText("Game start! No pause!");
            }
        }

        private void displayElapsedTime(long elapsedTime) {
            startStopButton.setText(String.valueOf(elapsedTime));
        }

        public void run() {
            try {
                while (isRunning) {
                    SwingUtilities.invokeAndWait(displayUpdater);
                    Thread.sleep(50);
                }
            } catch (java.lang.reflect.InvocationTargetException ite) {
                ite.printStackTrace(System.err);
                // Should never happen!
            } catch (InterruptedException ie) {
            }
            // Ignore and return!
        }

        public Timer() {
            startStopButton.addActionListener(this);
            getContentPane().add(startStopButton, BorderLayout.SOUTH);
            getContentPane().add(instruction, BorderLayout.CENTER);
            getContentPane().add(name, BorderLayout.NORTH);
            setSize(200, 100);
            setVisible(true);
        }
    }

    public static void main(String[] args){
        MS_client c = new MS_client();

        c.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        c.setVisible(true);
        c.t.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        c.t.setVisible(true);
    }

}
