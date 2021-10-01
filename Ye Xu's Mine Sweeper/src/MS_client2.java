import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.Random;

//TODO: Make sure game over when timer is up

public class MS_client2 extends JFrame{
    Timer t = new Timer();
    private long elapsed = 0;

    DataOutputStream toServer = null;
    ObjectOutputStream toServerObj = null;
    DataInputStream fromServer = null;
    ObjectInputStream fromServerObj = null;
    Socket socket = null;

    String player_name = "Player";
    DataSave load;
    boolean isLoadedGame = false;
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

    //All required images loaded into ImageIcon to be later applied quickly.
    ImageIcon icon0 = new ImageIcon("0.png");
    ImageIcon icon1 = new ImageIcon("1.png");
    ImageIcon icon2 = new ImageIcon("2.png");
    ImageIcon icon3 = new ImageIcon("3.png");
    ImageIcon icon4 = new ImageIcon("4.png");
    ImageIcon icon5 = new ImageIcon("5.png");
    ImageIcon icon6 = new ImageIcon("6.png");
    ImageIcon icon7 = new ImageIcon("7.png");
    ImageIcon icon8 = new ImageIcon("8.png");
    ImageIcon icon9 = new ImageIcon("9.png");
    ImageIcon icon10 = new ImageIcon("10.png");
    ImageIcon icon11 = new ImageIcon("11.png");
    ImageIcon icon12 = new ImageIcon("12.png");

    //This is mine grid, used to contain the array of mine button.
    Container grid = new Container();
    Blocks [][] field = new Blocks[PADDED_ROW][PADDED_COL];
    public MS_client2() throws IOException {
        JPanel boardPanel = new JPanel(new GridLayout(BOARD_SIZE_ROW,BOARD_SIZE_COL));
        grid.setLayout(new GridLayout(BOARD_SIZE_ROW,BOARD_SIZE_COL));

        for (int i = 0; i < PADDED_ROW; i += 1){
            for (int j = 0; j < PADDED_COL; j += 1){
                field[i][j] = new Blocks(i, j, icon10);
                field[i][j].setEnabled(false);
                if (i != 0 && i != PADDED_ROW - 1 && j != 0 && j != PADDED_COL - 1)
                    grid.add(field[i][j]);
            }
        }

        //Set mines into their place.
        mine_location = setMine();
        for (int i = 0; i < MINE_COUNT; i += 1){
            field[mine_location[i][0]][mine_location[i][1]].is_mine = true;
        }
        countDisplay = new JLabel(String.valueOf(flag_number));
        JMenuBar menu = createMenu();
        this.add(menu, BorderLayout.NORTH);
        this.add(grid, BorderLayout.CENTER);
        this.add(countDisplay, BorderLayout.SOUTH);
        this.setSize(300, 350);
        //this.setResizable(false);
    }

    //create File Menu
    public JMenuBar createMenu(){
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu menu = new JMenu("File");

        //New game button
        JMenuItem newGame = new JMenuItem("New");
        class NewGameListener implements ActionListener
        {
            public void actionPerformed(ActionEvent e) {
                try {
                    reset();
                    mine_location = setMine();
                    for (int i = 0; i < MINE_COUNT; i += 1){
                        field[mine_location[i][0]][mine_location[i][1]].is_mine = true;
                    }
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        }
        NewGameListener listener = new NewGameListener();
        newGame.addActionListener(listener);
        //Load from data base
        JMenuItem open = new JMenuItem("Load");

        class OpenFileListener implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                try {
                    socket = new Socket("localhost", 8001);
                    fromServerObj = new ObjectInputStream(socket.getInputStream());
                    load = (DataSave) fromServerObj.readObject();
                    System.out.println("load complete");
                    System.out.println(load.player + " now back in game with score: " + load.remainTime);
                    isLoadedGame = true;
                    t.instruction.setText("Welcome back " + load.player);
                    t.startStopButton.setText("Resume");
                    remaining_mine = load.remainMine;
                    flag_number = load.remainFlag;
                    countDisplay.setText(String.valueOf(flag_number));
                    player_name = load.player;
                    //carefully restore all the blocks to its former states depending on save.
                    for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1){
                        for (int j = PADDING; j < PADDED_COL - PADDING; j += 1) {
                            if (load.AllBlockData[i][j].isMineSave) {
                                field[i][j].is_mine = true;
                            }

                            if (load.AllBlockData[i][j].isRevealedSave){
                                field[i][j].is_revealed = true;
                                field[i][j].mine_update();
                                block_handling(field[i][j]);
                            }
                            else
                                field[i][j].setIcon(icon10);
                            if (load.AllBlockData[i][j].isFlagSave){
                                field[i][j].is_flag = true;
                                field[i][j].setIcon(icon11);
                            }
                            if (load.remainMine != 0 && load.AllBlockData[i][j].isMineSave &&
                            !load.AllBlockData[i][j].isFlagSave)
                                field[i][j].setIcon(icon9);
                        }
                    }
                    if (!load.isComplete){
                        for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1){
                            for (int j = PADDING; j < PADDED_COL - PADDING; j += 1){
                                field[i][j].setEnabled(true);
                            }
                        }
                    }
                    t.startStopButton.setEnabled(true);
                    t.name.setText(load.player);
                } catch (IOException | ClassNotFoundException e1) {
                    e1.printStackTrace();
                }
            }
        }
        OpenFileListener open_listener = new OpenFileListener();
        open.addActionListener(open_listener);

        //Save game into data base. Like load game will open up a designated port to server
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

                    //Use object output stream to transport DataSave object
                    toServerObj = new ObjectOutputStream(socket.getOutputStream());
                    DataSave save1 = new DataSave();
                    player_name = t.name.getText();
                    System.out.println("Display time is: " + t.startStopButton.getText());
                    long temp_savetime = Long.parseLong(t.startStopButton.getText());
                    System.out.println("save time is: " + temp_savetime);
                    save1.update(player_name, flag_number, remaining_mine, temp_savetime, is_complete, s1);
                    toServerObj.writeObject(save1);
                    toServerObj.flush();
                    System.out.println("connected");
                } catch (IOException e1) {
                    e1.printStackTrace();
                    System.out.println("connection Failure");
                }
            }
        }
        SaveFileListener save_listener = new SaveFileListener();
        save.addActionListener(save_listener);

        //exit as it is
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
    //check if win or not, if so, stop the timer.
    public void check_win() throws InterruptedException {
        if (remaining_mine == 0 && flag_number == 0) {
            countDisplay.setText("You win!");
            is_complete = true;
            t.isRunning = false;
            t.updater.join();
        }
    }
    //get non-diagonal neighbors
    public Blocks[] get_neighbours(int row, int col){
        Blocks[] out_nei = new Blocks[4];
        out_nei[0] = field[row - 1][col];
        out_nei[1] = field[row][col - 1];
        out_nei[2] = field[row + 1][col];
        out_nei[3] = field[row][col + 1];

        return out_nei;
    }
    //recursively display nearby cells with no mine around
    public void reveil_cell(int row, int col){
        if (row == 0 || row == PADDED_ROW - PADDING ||
                col == 0 || col == PADDED_COL - PADDING){
            return;
        }
        field[row][col].mine_update();
        if (field[row][col].mine_around != 0){
            block_handling(field[row][col]);
        }
        else {
            block_handling(field[row][col]);
            for (Blocks c : get_neighbours(row, col)) {
                if (!c.is_mine && !c.is_revealed) {
                    reveil_cell(c.row, c.col);
                }
            }
        }
    }
    //trigger when game ends, mostly likely tragically ends
    public void reveal_all() throws InterruptedException {
        for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1){
            for (int j = PADDING; j < PADDED_COL - PADDING; j += 1){
                if (field[i][j].is_mine && field[i][j].is_flag)
                    field[i][j].setIcon(icon11);
                else if (!field[i][j].is_mine && field[i][j].is_flag)
                    field[i][j].setIcon(icon12);
                else if (field[i][j].is_mine)
                    field[i][j].setIcon(icon9);

                field[i][j].setEnabled(false);
                field[i][j].is_revealed = true;
            }
        }
        countDisplay.setText("Boom!");
        t.startStopButton.setEnabled(false);
        t.isRunning = false;
        t.updater.join();
        is_complete = true;
        System.out.println(elapsed);
    }
    //handle printing each blocks depend on mine around it.
    public void block_handling(Blocks target){
        if (target.mine_around == 0){
            target.setIcon(icon0);
        }
        else {
            switch(target.mine_around){
                case 1:
                    target.setIcon(icon1);
                    break;
                case 2:
                    target.setIcon(icon2);
                    break;
                case 3:
                    target.setIcon(icon3);
                    break;
                case 4:
                    target.setIcon(icon4);
                    break;
                case 5:
                    target.setIcon(icon5);
                    break;
                case 6:
                    target.setIcon(icon6);
                    break;
                case 7:
                    target.setIcon(icon7);
                    break;
                case 8:
                    target.setIcon(icon8);
                    break;
            }
        }
        target.is_revealed = true;
    }

    //class for each block display.
    public class Blocks extends JButton{
        int row;
        int col;
        int mine_around = 0;
        private Image img;
        boolean is_mine = false;
        boolean is_flag = false;
        boolean is_revealed = false;

        //The most important mine block class, left click (button) listener and
        //right click (mouse listener) are all here.
        public Blocks(int in_row, int in_col, Icon icon) {
            super(icon);
            this.setMargin(new Insets(0, 0, 0, 0));
            this.addMouseListener(new block_clicked());
            this.addActionListener(new block_leftclicked());
            row = in_row;
            col = in_col;
        }
        //button listener, since Blocks class itself is JButton extends
        public class block_leftclicked implements ActionListener{
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Blocks.this.is_mine) {
                    Blocks.this.setIcon(icon9);
                    try {
                        reveal_all();
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                } else
                    reveil_cell(Blocks.this.row, Blocks.this.col);
            }
        }
        //mouse listener for right click.
        public class block_clicked extends MouseAdapter{
            @Override
            public void mouseClicked(MouseEvent e) {
                if(!Blocks.this.is_revealed) {
                    //mouse right click, plant flag
                    if (e.getModifiers() == MouseEvent.BUTTON3_MASK && e.getClickCount() == 1) {

                        if (flag_number == 0)
                            countDisplay.setText("No flag is available");
                        else{
                            flag_number -= 1;
                            countDisplay.setText(String.valueOf(flag_number));
                            Blocks.this.setIcon(icon11);
                            Blocks.this.is_flag = true;
                            Blocks.this.is_revealed = true;
                            if (Blocks.this.is_mine)
                                remaining_mine -= 1;
                            try {
                                check_win();
                            } catch (InterruptedException interruptedException) {
                                interruptedException.printStackTrace();
                            }
                        }
                    }
                }
                else if(Blocks.this.is_flag){
                    if (e.getModifiers() == MouseEvent.BUTTON3_MASK && e.getClickCount() == 1) {
                        Blocks.this.setIcon(icon10);
                        Blocks.this.is_flag = false;
                        flag_number += 1;
                        countDisplay.setText(String.valueOf(flag_number));
                        if (Blocks.this.is_mine)
                            remaining_mine += 1;
                        Blocks.this.is_revealed = false;
                    }
                }
            }
        }
        //Exclusively update the mine_around value of this variable, to show the mine
        //count in all its 8 directions.
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
    //method to generate an array storing all mine's x and y location
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
    //reset the game to a blank states.
    public void reset() throws InterruptedException {
        t.isRunning = false;
        for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1){
            for (int j = PADDING; j < PADDED_COL - PADDING; j += 1){
                field[i][j].setIcon(icon10);
                field[i][j].is_revealed = false;
                field[i][j].is_mine = false;
                field[i][j].is_flag = false;
                remaining_mine = MINE_COUNT;
                flag_number = MINE_COUNT;
                is_complete = false;
                countDisplay.setText(String.valueOf(flag_number));

                t.instruction.setText("Please input your name");
                t.startStopButton.setText("Let's go again!");
                t.startStopButton.setEnabled(true);
                t.name.setText("");
                field[i][j].setEnabled(false);
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

    //timer method, running on independent thread
    public class Timer extends JFrame implements ActionListener, Runnable {
        private long startTime;
        private JTextField name = new JTextField();
        private JLabel instruction = new JLabel("Please input your name");
        private final JButton startStopButton = new JButton("Confirm");
        private Thread updater;
        //volatile since this needs to be changed in order to stop the thread
        //since currently no effective method in java can stop a thread
        volatile private boolean isRunning = false;

        private final Runnable displayUpdater = new Runnable() {
            public void run() {
                displayElapsedTime(Timer.this.startTime - System.currentTimeMillis());
            }
        };

        public void actionPerformed(ActionEvent ae) {
            //This is used to pause the timer
            if (isRunning) {
                elapsed = startTime - System.currentTimeMillis();
                startStopButton.setEnabled(false);
                for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1){
                    for (int j = PADDING; j < PADDED_COL - PADDING; j += 1){
                        field[i][j].setEnabled(false);
                    }
                }
                isRunning = false;
                instruction.setText("Save or not?");
                // Display the end-result
                displayElapsedTime(elapsed);

            }
            //this is used to start the timer
            else {
                if (!name.getText().equals(""))
                    player_name = name.getText();
                isRunning = true;
                if (!isLoadedGame)
                    startTime = 1000 * 1000 + System.currentTimeMillis();
                else {
                    startTime = load.remainTime + System.currentTimeMillis();
                    isLoadedGame = false;
                }
                updater = new Thread(this);
                updater.start();
                instruction.setText("Game start! No pause!");
                for (int i = PADDING; i < PADDED_ROW - PADDING; i += 1){
                    for (int j = PADDING; j < PADDED_COL - PADDING; j += 1){
                        field[i][j].setEnabled(true);
                    }
                }
            }
        }
        private void displayElapsedTime(long elapsedTime) {
            startStopButton.setText(String.valueOf(elapsedTime));
        }
        public void run() {
            try {
                //terminate the game when the time is up
                while (isRunning) {
                    if (Timer.this.startTime - System.currentTimeMillis()==0) {
                        reveal_all();
                        isRunning = false;
                        is_complete = true;
                    }
                    SwingUtilities.invokeAndWait(displayUpdater);
                    Thread.sleep(50);

                }
            } catch (java.lang.reflect.InvocationTargetException ite) {
                ite.printStackTrace(System.err);
            } catch (InterruptedException ie) {
            }
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

    public static void main(String[] args) throws IOException {
        MS_client2 c = new MS_client2();

        c.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        c.setVisible(true);
        c.t.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        c.t.setVisible(true);
    }

}
