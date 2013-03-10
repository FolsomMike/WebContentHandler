/******************************************************************************
* Title: MainFrame.java
* Author: Mike Schoonover
* Date: 11/15/12
*
* Purpose:
*
* This class creates the canvas where the world is drawn and handles all the
* action.
*
* Communications with networked modules occurs in a separate thread so that
* the display components don't freeze during lengthy network transactions.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
* Notes for Binaural Beats:
*
* When two different frequencies below about 1000 Hz are played in each ear,
* (a different frequency in each ear), the brain perceives a beating sound at
* the frequency equal to the difference between the two frequencies. The
* listener's brain waves will entrain to this beat frequency, providing a
* method of inducing states of relaxation.
*
*  > 40 Hz 	Gamma waves     Higher mental activity, including perception,
*                                problem solving, fear, and consciousness
* 13–39 Hz 	Beta waves 	Active, busy or anxious thinking and active
*                                concentration, arousal, cognition, and or
*                                paranoia
* 7–13 Hz 	Alpha waves 	Relaxation (while awake), pre-sleep and pre-wake
*                                drowsiness, REM sleep, Dreams
* 8–12 Hz 	Mu waves 	Sensorimotor rhythm Mu_rhythm,
*                                Sensorimotor_rhythm
* 4–7 Hz 	Theta waves 	Deep meditation/relaxation, NREM sleep
* < 4 Hz 	Delta waves 	Deep dreamless sleep, loss of body awareness
*
* When the perceived beat frequency corresponds to the delta, theta, alpha,
* beta, or gamma range of brainwave frequencies, the brainwaves entrain to or
* move towards the beat frequency.[29] For example, if a 315 Hz sine wave is
* played into the right ear and a 325 Hz one into the left ear, the brain is
* entrained towards the beat frequency 10 Hz, in the alpha range. Since alpha
* range is associated with relaxation, this has a relaxing effect or if in the
* beta range, more alertness. An experiment with binaural sound stimulation
* using beat frequencies in the Beta range on some participants and Delta/Theta
* range in other participants, found better vigilance performance and mood in
* those on the awake alert state of Beta range stimulation.
*  source: Wikipedia, November 2012
*
*/

//-----------------------------------------------------------------------------

package webcontenthandler;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.TextAttribute;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MainFrame
//

class MainFrame extends JFrame implements WindowListener, ActionListener,
                                                                    Runnable {

    JPanel mainPanel;

    Options options;

    MainMenu mainMenu;

    GuiUpdater guiUpdater;

    javax.swing.Timer mainTimer;

    Font blackSmallFont, redSmallFont;

    Font redLargeFont, greenLargeFont, yellowLargeFont, blackLargeFont;

    JLabel statusLabel, infoLabel;

    Boolean blinkStatusLabel = false;

    String errorMessage;

    Log log;
    ThreadSafeLogger tsLog;
    Help help;
    About about;

    SwingWorker workerThread;

    DecimalFormat decimalFormat1 = new DecimalFormat("#.0");

    JLabel progressLabel;
    Font tSafeFont;
    String tSafeText;

    int displayUpdateTimer = 0;

    String XMLPageFromRemote;

    boolean shutDown = false;

    final JFileChooser fileChooser = new JFileChooser();

    static private final String newline = "\n";

//-----------------------------------------------------------------------------
// MainFrame::MainFrame (constructor)
//

public MainFrame(String pTitle)
{

    super(pTitle);

}//end of MainFrame::MainFrame (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::init
//
// Initializes the object.  Must be called immediately after instantiation.
//

public void init()
{

    setupMainFrame();

    //create and load the program options
    options = new Options();

    //create a window for displaying messages and an object to handle updating
    //it in threadsafe manner
    log = new Log(this); log.setLocation(230, 0);

    tsLog = new ThreadSafeLogger(log.textArea);

    //add a menu to the main form, passing this as the action listener
    setJMenuBar(mainMenu = new MainMenu(options, this));

    //create an object to handle thread safe updates of GUI components
    guiUpdater = new GuiUpdater(this);
    guiUpdater.init();

    //create various fonts for use by the program
    createFonts();

    //create user interface: buttons, displays, etc.
    setupGui();

    //arrange all the GUI items
    pack();

    //display the main frame
    setVisible(true);

    tsLog.appendLine("Hello"); tsLog.appendLine("");

    //start the control thread
    new Thread(this).start();

    setupAndStartMainTimer();

}// end of MainFrame::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::setupMainFrame
//
// Sets various options and styles for the main frame.
//

public void setupMainFrame()
{

    //add a JPanel to the frame to provide a familiar container
    mainPanel = new JPanel();
    getContentPane().add(mainPanel);

    //set the min/max/preferred sizes of the panel to set the size of the frame
    setSizes(mainPanel, 200, 300);

    addWindowListener(this);

    //turn off default bold for Metal look and feel
    UIManager.put("swing.boldMetal", Boolean.FALSE);

    //force "look and feel" to Java style
    try {
        UIManager.setLookAndFeel(
            UIManager.getCrossPlatformLookAndFeelClassName());
        }
    catch (Exception e) {
        System.out.println("Could not set Look and Feel");
        }

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    //    setLocation((int)screenSize.getWidth() - getWidth(), 0);

}// end of MainFrame::setupMainFrame
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::setupAndStartMainTimer
//
// Prepares and starts a Java Swing timer.
//

public void setupAndStartMainTimer()
{

    //main timer has 2 second period
    mainTimer = new javax.swing.Timer (2000, this);
    mainTimer.setActionCommand ("Timer");
    mainTimer.start();

}// end of MainFrame::setupAndStartMainTimer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::createFonts
//
// Creates fonts for use by the program.
//

public void createFonts()
{

    //create small and large red and green fonts for use with display objects
    HashMap<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();

    blackSmallFont = new Font("Dialog", Font.PLAIN, 12);

    map.put(TextAttribute.FOREGROUND, Color.RED);
    redSmallFont = blackSmallFont.deriveFont(map);

    //empty the map to use for creating the large fonts
    map.clear();

    blackLargeFont = new Font("Dialog", Font.PLAIN, 20);

    map.put(TextAttribute.FOREGROUND, Color.GREEN);
    greenLargeFont = blackLargeFont.deriveFont(map);

    map.put(TextAttribute.FOREGROUND, Color.RED);
    redLargeFont = blackLargeFont.deriveFont(map);

    map.put(TextAttribute.FOREGROUND, Color.YELLOW);
    yellowLargeFont = blackLargeFont.deriveFont(map);

}// end of MainFrame::createFonts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::setupGUI
//
// Sets up the user interface on the mainPanel: buttons, displays, etc.
//

private void setupGui()
{

    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    mainPanel.add(Box.createRigidArea(new Dimension(0,20))); //vertical spacer

    //create a label to display good/warning/bad system status
    statusLabel = new JLabel("Status");
    mainPanel.add(statusLabel);

    mainPanel.add(Box.createRigidArea(new Dimension(0,20))); //vertical spacer

    //create a label to display miscellaneous info
    infoLabel = new JLabel("Info");
    mainPanel.add(infoLabel);

    mainPanel.add(Box.createRigidArea(new Dimension(0,20))); //vertical spacer

    //add button
    JButton unused1 = new JButton("Unused 1");
    unused1.setActionCommand("Unused 1");
    unused1.addActionListener(this);
    unused1.setToolTipText("Unused 1.");
    mainPanel.add(unused1);

    mainPanel.add(Box.createRigidArea(new Dimension(0,10))); //vertical spacer

    //add a button
    JButton unused2 = new JButton("Unused 2");
    unused2.setActionCommand("Unused 2");
    unused2.addActionListener(this);
    unused2.setToolTipText("Unused 2");
    mainPanel.add(unused2);

    mainPanel.add(Box.createRigidArea(new Dimension(0,10))); //vertical spacer

    progressLabel = new JLabel("Progress");
    mainPanel.add(progressLabel);

    mainPanel.add(Box.createRigidArea(new Dimension(0,10))); //vertical spacer

}// end of MainFrame::setupGui
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::actionPerformed
//
// Responds to events.
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Timer".equals(e.getActionCommand())) {doTimerActions();}

    if ("Display Log".equals(e.getActionCommand())) {displayLog();}

    if ("Display Help".equals(e.getActionCommand())) {displayHelp();}

    if ("Display About".equals(e.getActionCommand())) {displayAbout();}

    if ("New File".equals(e.getActionCommand())) {doSomething1();}

    if ("Open File".equals(e.getActionCommand())) {
        doSomething2();
    }

}//end of MainFrame::actionPerformed
//-----------------------------------------------------------------------------

/*
//-----------------------------------------------------------------------------
// MainFrame::paintComponent
//

@Override
public void paintComponent (Graphics g)
{

}// end of MainFrame::paintComponent
//-----------------------------------------------------------------------------

*/

//-----------------------------------------------------------------------------
// MainFrame::doTimerActions
//
// Performs actions driven by the timer.
//
// Not used for accessing network -- see run function for details.
//

public void doTimerActions()
{


}//end of MainFrame::doTimerActions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::displayLog
//
// Displays the log window. It is not released after closing as the information
// is retained so it can be viewed the next time the window is opened.
//

private void displayLog()
{

    log.setVisible(true);

}//end of MainFrame::displayLog
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::displayHelp
//
// Displays help information.
//

private void displayHelp()
{

    help = new Help(this);
    help = null;  //window will be released on close, so point should be null

}//end of MainFrame::displayHelp
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::displayAbout
//
// Displays about information.
//

private void displayAbout()
{

    about = new About(this);
    about = null;  //window will be released on close, so point should be null

}//end of MainFrame::displayAbout
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::doSomething1
//

private void doSomething1()
{


}//end of MainFrame::doSomething1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::doSomethingInWorkerThread
//
// Does nothing right now -- modify it to call a function which takes a long
// time to finish. It will be run in a background thread so the GUI is still
// responsive.
// -- CHANGE THE NAME TO REFLECT THE ACTION BEING DONE --
//

private void doSomethingInWorkerThread()
{

    //define and instantiate a worker thread to create the file


    //----------------------------------------------------------------------
    //class SwingWorker
    //

    workerThread = new SwingWorker<Void, String>() {
        @Override
        public Void doInBackground() {

            //do the work here by calling a function

            return(null);

        }//end of doInBackground

        @Override
        public void done() {

            //clear in progress message here if one is being displayed

            try {

                //use get(); function here to retrieve results if necessary
                //note that Void type here and above would be replaced with
                //the type of variable to be returned

                Void v = get();

            } catch (InterruptedException ignore) {}
            catch (java.util.concurrent.ExecutionException e) {
                String why;
                Throwable cause = e.getCause();
                if (cause != null) {
                    why = cause.getMessage();
                } else {
                    why = e.getMessage();
                }
                System.err.println("Error creating file: " + why);
            }//catch

        }//end of done

        @Override
        protected void process(java.util.List <String> pairs) {

            //this method is not used by this application as it is limited
            //the publish method cannot be easily called outside the class, so
            //messages are displayed using a ThreadSafeLogger object and status
            //components are updated using a GUIUpdater object

        }//end of process

    };//end of class SwingWorker
    //----------------------------------------------------------------------

}//end of MainFrame::doSomethingInWorkerThread
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::doSomething2
//

private void doSomething2()
{


}//end of MainFrame::doSomething2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::run
//
// This is the part which runs as a separate thread.  The actions of accessing
// remote devices occur here.  If they are done in a timer call instead, then
// buttons and displays get frozen during the sometimes lengthy calls to access
// the network.
//
// NOTE:  All functions called by this thread must wrap calls to alter GUI
// components in the invokeLater function to be thread safe.
//

@Override
public void run()
{

    //call the control method repeatedly
    while(true){

        control();

        //sleep for 2 seconds -- all timing is based on this period
        threadSleep(2000);

    }

}//end of MainFrame::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::threadSleep
//
// Calls the Thread.sleep function. Placed in a function to avoid the
// "Thread.sleep called in a loop" warning -- yeah, it's cheezy.
//

public void threadSleep(int pSleepTime)
{

    try {Thread.sleep(pSleepTime);} catch (InterruptedException e) { }

}//end of MainFrame::threadSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::control
//
// Performs all display and control.  Call this from a thread.
//

public void control()
{

    //update the display every 30 seconds
    if (displayUpdateTimer++ == 14){
        displayUpdateTimer = 0;
        //call function to update stuff here
    }


    //If a shut down is initiated, clean up and exit the program.

    if(shutDown){
        //exit the program
        System.exit(0);
    }

}//end of MainFrame::control
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

public void displayErrorMessage(String pMessage)
{

    JOptionPane.showMessageDialog(this, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of MainFrame::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::shutDown
//
// Disables chassis power and performs any other appropriate shut down
// operations.
//
// This is done by setting a flag so that this class's thread can do the
// actual work, thus avoiding thread contention.
//

public void shutDown()
{

    shutDown = true;

}//end of MainFrame::shutDown
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::windowClosing
//
// Handles actions necessary when the window is closing
//

@Override
public void windowClosing(WindowEvent e)
{

    //perform all shut down procedures

    shutDown();

}//end of MainFrame::windowClosing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::(various window listener functions)
//
// These functions are implemented per requirements of interface WindowListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

@Override
public void windowActivated(WindowEvent e){}
@Override
public void windowDeactivated(WindowEvent e){}
@Override
public void windowOpened(WindowEvent e){}
//@Override
//public void windowClosing(WindowEvent e){}
@Override
public void windowClosed(WindowEvent e){}
@Override
public void windowIconified(WindowEvent e){}
@Override
public void windowDeiconified(WindowEvent e){}

//end of MainFrame::(various window listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pWidth and pHeight.
//

static void setSizes(Component pComponent, int pWidth, int pHeight)
{

    pComponent.setMinimumSize(new Dimension(pWidth, pHeight));
    pComponent.setPreferredSize(new Dimension(pWidth, pHeight));
    pComponent.setMaximumSize(new Dimension(pWidth, pHeight));

}//end of MainFrame::setSizes
//-----------------------------------------------------------------------------


}//end of class MainFrame
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
