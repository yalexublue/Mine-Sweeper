import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

public class Timer extends JFrame implements ActionListener, Runnable
{
    private long startTime = 1000*1000+System.currentTimeMillis();
    private JTextField name = new JTextField();
    private JLabel instruction = new JLabel("Please input your name");
    private long elapsed = 0;
    //private final static java.text.SimpleDateFormat timerFormat = new java.text.SimpleDateFormat("ss");
    private final JButton startStopButton= new JButton("Confirm");
    private Thread updater;
    private boolean isRunning= false;
    private final Runnable displayUpdater= new Runnable()
    {
        public void run()
        {
            displayElapsedTime(Timer.this.startTime - System.currentTimeMillis());
        }
    };
    public void actionPerformed(ActionEvent ae)
    {
        if(isRunning)
        {
            elapsed = startTime - System.currentTimeMillis() ;
            long elapsedSecond = elapsed / 1000;
            System.out.println(elapsed);

            isRunning= false;
            try
            {
                updater.join();
                // Wait for updater to finish
            }
            catch(InterruptedException ie) {}
            displayElapsedTime(elapsedSecond);
            // Display the end-result
        }
        else
        {
            startTime= 1000*1000+System.currentTimeMillis();
            isRunning= true;
            updater= new Thread(this);
            updater.start();
        }
    }
    private void displayElapsedTime(long elapsedTime)
    {
        startStopButton.setText(String.valueOf(elapsedTime / 1000));
    }
    public void run()
    {
        try
        {
            while(isRunning)
            {
                SwingUtilities.invokeAndWait(displayUpdater);
                Thread.sleep(50);
            }
        }
        catch(java.lang.reflect.InvocationTargetException ite)
        {
            ite.printStackTrace(System.err);
            // Should never happen!
        }
        catch(InterruptedException ie) {}
        // Ignore and return!
    }
    public Timer()
    {
        startStopButton.addActionListener(this);
        getContentPane().add(startStopButton, BorderLayout.SOUTH);
        getContentPane().add(instruction, BorderLayout.CENTER);
        getContentPane().add(name, BorderLayout.NORTH);
        setSize(200,100);
        setVisible(true);
    }
    public static void main(String[] arg)
    {
        new Timer().addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
    }
}