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
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
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
    WAVFile wavFile;

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

    if ("View WAV Audio File".equals(e.getActionCommand())) {displayWAVFile();}

    if ("Create Binaural WAV Audio File".equals(e.getActionCommand())) {
        createBinauralWAVFile();
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
// MainFrame::displayWAVFile
//
// Allows the user to select a WAV file which is then partially displayed in
// the log window.
//
// All of the control and format data is displayed and a portion of the sound
// data is displayed.
//

private void displayWAVFile()
{

    int returnVal = fileChooser.showOpenDialog(this);

    if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        tsLog.appendLine("Opening: " + file.getName() + ".");
        tsLog.appendLine("");
        wavFile = new WAVFile(
                        file.getName(), tsLog, guiUpdater, progressLabel);
        wavFile.init();
        wavFile.loadFile();

    }
    else {
        tsLog.appendLine("Open command cancelled by user.");
    }

}//end of MainFrame::displayWAVFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::createBinauralWAVFile
//
// Allows the user to select a text configuration file which is then used to
// generate a custom binaural WAV audio file.
//
// The new file will have the same name as the configuration file but with a
// .wav extension.
//

private void createBinauralWAVFile()
{

    int returnVal = fileChooser.showOpenDialog(this);

    if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        tsLog.appendLine(
                    "Opening configuration file: " + file.getName() + ".");
        tsLog.appendLine(
                    "Creating Binaural WAV file: " + file.getName() + ".");
        tsLog.appendLine("");

        //create the audio file waveform data
        createBinauralWAVFileInWorkerThread(file.getPath());

        //start the thread
        workerThread.execute();

    }
    else {
        tsLog.appendLine("Open command cancelled by user.");
    }

}//end of MainFrame::createBinauralWAVFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::createBinauralWAVFileInWorkerThread
//
// Starts a worker thread to create a binaural wav file. The worker thread is
// used so the work is done in the background and the GUI can continue to
// function and display progress messages.
//
// See createBinauralWAVFile for details
//

private void createBinauralWAVFileInWorkerThread(final String pFilename)
{

    //define and instantiate a worker thread to create the file


    //----------------------------------------------------------------------
    //class SwingWorker
    //

    workerThread = new SwingWorker<Void, String>() {
        @Override
        public Void doInBackground() {

            createBinauralWAVFileHelper(pFilename);
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

}//end of MainFrame::createBinauralWAVFileInWorkerThread
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::createBinauralWAVFileHelper
//
// This method does the actual work of creating the binaural file.
// The configuration info will be read from pFilename
// The resulting audio file will be named pFilename but with the extension
// ".wav".
//
// Returns true if no error, returns false on error.
//

private boolean createBinauralWAVFileHelper(String pFilename)
{

    //create wavFile object to hold the new audio data
    String outFilename = switchExtensionToWAV(pFilename);
    wavFile = new WAVFile(outFilename, tsLog, guiUpdater, progressLabel);
    wavFile.init();

    //open the configuration file selected by the user

    String fileFormat = Charset.defaultCharset().displayName();
    IniFile inFile;

    try{
        tsLog.appendLine("Reading audio configuration file...");
        inFile = new IniFile(pFilename, fileFormat);
    }
    catch(IOException e){
        tsLog.appendLine("Error -- Could not open configuration file.");
        return(false);
    }

    //set various settings read from the configuration file

    int sampleRate = inFile.readInt("General", "samples per second", 44100);
    tsLog.appendLine("Sample Rate: " + sampleRate);
    int sampleValueRange = inFile.readInt("General",
                "sample value range (1 = signed word, 2 = unsigned byte)", 1);
    tsLog.appendLine("SampleValueRange: "
            + (sampleValueRange == 1 ? "signed word" : "unsigned byte"));

    wavFile.setAudioParameters(sampleRate, sampleValueRange, 0x0001);

    if (!calculateTotalSampleSize(inFile)){
        tsLog.appendLine(errorMessage);
        return(false);
    }

    guiUpdater.addUpdate(
             progressLabel, blackSmallFont,"" + wavFile.getTotalSampleSize());

    //create and prepare the WAV file for data
    if(!wavFile.startFileSave()) {return(false);}

    if (!createAudio(inFile)){
        tsLog.appendLine(errorMessage);
        return(false);
    }

    //finish up and close the audio file
    wavFile.endFileSave();

    return(true);

}//end of MainFrame::createBinauralWAVFileHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::calculateTotalSampleSize
//
// Sums the time durations in seconds for each section in the configuration
// file and multiplies the total by the sample rate (samples per second) to
// get the total number of samples to be generated.
//
// When the first section is reached for which the time duration is not set,
// which could be due to the section being non-existent or the duration being
// set blank, that section and all subsequently numbered sections are ignored.
// Thus the user can easily truncate the section list by setting any one of
// them to have a blank time duration.
//
// Sets the total sample size value in the wavFile.
//
// Returns true if no error, returns false on error.
//

private boolean calculateTotalSampleSize(IniFile pInFile)
{

    int timeDuration;
    int totalTimeDuration = 0;
    int sectionIndex = 1;
    String section;

    //add up all the time durations in the sections until the first one reached
    //which has an unset time duration

    while(true){

        section = "section " + sectionIndex++;

        timeDuration =
                pInFile.readInt(section, "time duration in seconds", -1);

        //stop when the first unset time duration is encountered
        if(timeDuration == -1) {break;}

        tsLog.appendLine(section + " time duration: " + timeDuration);

        totalTimeDuration += timeDuration;

    }

    //limit total time for the audio to 10 hours
    if (totalTimeDuration < 0 || totalTimeDuration > 36000){
        errorMessage =
                  "Total time duration cannot exceed 10 hours (36000 seconds)";
        return(false);
    }

    tsLog.appendLine("Total Time Duration: " + totalTimeDuration);

    //calculate the total number of samples required for the audio
    long numberOfSamples = wavFile.getSampleRate() * totalTimeDuration;

    //limit to 10 hours at 44100 samples per second
    if (numberOfSamples > 1587600000){
        errorMessage =
            "Total number of samples cannot exceed 44100 samples per second"
             + "over 10 hours (36000 seconds)";
        return(false);
    }

    tsLog.appendLine("Total Number of Samples for Each Channel: "
                                                          + numberOfSamples);

    //initialize the channels
    wavFile.initChannels((int)numberOfSamples);

    return(true);

}//end of MainFrame::calculateTotalSampleSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::createAudio
//
// Creates the audio segment by combining sections specified in the
// configuration file.
//
// When the first section is reached for which the time duration is not set,
// which could be due to the section being non-existent or the duration being
// set blank, that section and all subsequently numbered sections are ignored.
// Thus the user can easily truncate the section list by setting any one of
// them to have a blank time duration.
//
// Returns true if no error, returns false on error.
//

private boolean createAudio(IniFile pInFile)
{

    int leftStartingFrequency, leftEndingFrequency, leftAmplitude;
    int rightStartingFrequency, rightEndingFrequency, rightAmplitude;
    int timeDuration;
    int sectionIndex = 1;
    String section;

    tsLog.appendLine("Creating audio...");

    //process all sections in numerical order until the first one reached
    //which has an unset time duration

    while(true){

        //read the audio parameters from each section

        section = "section " + sectionIndex++;

        leftStartingFrequency =
             pInFile.readInt(section, "left channel starting frequency Hz", 1);
        leftEndingFrequency =
             pInFile.readInt(section, "left channel ending frequency Hz", 1);
        leftAmplitude =
             pInFile.readInt(section, "left channel amplitude", 10000);

        rightStartingFrequency =
             pInFile.readInt(section, "right channel starting frequency Hz", 1);
        rightEndingFrequency =
             pInFile.readInt(section, "right channel ending frequency Hz", 1);
        rightAmplitude =
             pInFile.readInt(section, "right channel amplitude", 10000);

        timeDuration =
                pInFile.readInt(section, "time duration in seconds", -1);

        //stop when the first unset time duration is encountered
        if(timeDuration == -1) {break;}

        //create the audio signals

        tsLog.appendLine("---- processing " + section + "...");

        if (!wavFile.createAudioSection(
            leftStartingFrequency, leftEndingFrequency, leftAmplitude,
            rightStartingFrequency, rightEndingFrequency, rightAmplitude,
            timeDuration, false)){

            errorMessage = "Error creating the file.";
            return(false);

        }

        //display configuration info about each channel

        logChannelInfo("--- Left Channel", WAVFile.LEFT,
                    leftStartingFrequency, leftEndingFrequency, leftAmplitude);

        logChannelInfo("--- Right Channel", WAVFile.RIGHT,
                 rightStartingFrequency, rightEndingFrequency, rightAmplitude);

    }

    tsLog.appendLine("-------------------------------------------------------");
    tsLog.appendLine("");

    return(true);

}//end of MainFrame::createAudio
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::logChannelInfo
//
// Displays information about pWhichChannel (WAVFile.LEFT or WAVFile.RIGHT) in
// the log window.
//

private void logChannelInfo(String pDescription, int pWhichChannel,
                   int pStartingFrequency, int pEndingFrequency, int pAmplitude)
{

    tsLog.appendLine(pDescription);

    tsLog.appendLine("beginning frequency: " + pStartingFrequency + "...");
    tsLog.appendLine("ending frequency: " + pEndingFrequency + "...");

    //as the frequency is ramped from beginning to ending values, a
    //rounding error is introduced -- display the actual ending frequency
    tsLog.appendLine("actual ending frequency (with rounding error): "
                             + wavFile.getFrequency(pWhichChannel) + "...");
    tsLog.appendLine("amplitude: " + pAmplitude + "...");

}//end of MainFrame::logChannelInfo
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::switchExtensionToWAV
//
// Returns a filename identical to pFilename but with the extension ".wav".
//

private String switchExtensionToWAV(String pFilename)
{

    //get position of dot separator for filename extension
    int dot = pFilename.lastIndexOf('.');

    //if extension found, then extract everything up to the dot, else just
    //use the entire name

    String result;

    if (dot != -1) {
        result = pFilename.substring(0, dot);
    }
    else {
        result = pFilename;
    }

    //add the new extension
    result = result + ".wav";

    return(result);

}//end of MainFrame::switchExtensionToWAV
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::sendGetRequest
//
// Sends an HTTP GET request to a url
//
// @param endpoint - The URL of the server.
// @param requestParameters - all the request parameters
//  (Example: "param1=val1&param2=val2").
// Note: This method will add the question mark (?) to the request -
//      DO NOT add it yourself
// @return - The response from the end point
//

public String sendGetRequest(String pNetURL, String pRequestParameters)
{

    String result = "";

    URL url;

    try{

        url = new URL(pNetURL);

        Object o = ( url.getContent() );

        //get the response
        BufferedReader rd = new BufferedReader(
                            new InputStreamReader((InputStream)o));

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = rd.readLine()) != null){
            sb.append(line);
        }

        rd.close();
        result = sb.toString();

    }
    catch(MalformedURLException me){

    }//catch
    catch (IOException e){

    }//catch

    return(result);

}//end of MainFrame::sendGetRequest
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainFrame::getXMLValueForKey
//
// Reads the value associated with pKey from the class member String
//    temperatureMonitorXMLPage.
//
// If the key is not found, the default value is returned.
//
// Example format for key/value pairs: <units>F</units>.
//

public String getXMLValueForKey(String pKey, String pDefault)
{

    //find start of opening key
    int start = XMLPageFromRemote.indexOf("<" + pKey + ">");
    if (start == -1) {return(pDefault);} //key not found, return
    //find closing bracket of opening key -- value is text after that
    start = XMLPageFromRemote.indexOf(">", start);
    if (start == -1) {return(pDefault);} //closing bracket not found, return

    int end = XMLPageFromRemote.indexOf("</" + pKey + ">", start);
    if (end == -1) {return(pDefault);} //if closing key not found, return

    //return the value between the opening and closing key delimiters
    return(XMLPageFromRemote.substring(start + 1, end));

}//end of MainFrame::getXMLValueForKey
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
