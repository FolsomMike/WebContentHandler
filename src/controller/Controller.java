/******************************************************************************
* Title: Controller.java
* Author: Mike Schoonover
* Date: 11/15/12
*
* Purpose:
*
* This class is the Controller in a Model-View-Controller architecture.
* It creates the Model and the View.
* It tells the View to update its display of the data in the model.
* It handles user input from the View (button pushes, etc.)*
* It tells the Model what to do with its data based on these inputs and tells
*   the View when to update or change the way it is displaying the data.
*
* In this implementation:
*   the Model knows only about itself
*   the View knows only about the Model and can get data from it
*   the Controller about the Model and the View and interacts with both
*
* In this specific MVC implementation, the Model does not send messages to
* the View -- it expects the Controller to trigger the View to request data
* from the Model when necessary.
*
* The View does send messages to the Controller indirectly as the screen GUI
* components such as buttons send action messages to an ActionListener -- in
* this case the Controller is designated as the ActionListener for all GUI
* components.
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

package controller;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.DecimalFormat;
import javax.swing.*;
import model.Options;
import view.View;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Controller
//

public class Controller implements WindowListener, ActionListener, Runnable
{

    View view;

    Options options;

    javax.swing.Timer mainTimer;

    Boolean blinkStatusLabel = false;

    String errorMessage;

    SwingWorker workerThread;

    DecimalFormat decimalFormat1 = new DecimalFormat("#.0");

    Font tSafeFont;
    String tSafeText;

    int displayUpdateTimer = 0;

    String XMLPageFromRemote;

    boolean shutDown = false;

    final JFileChooser fileChooser = new JFileChooser();

    static private final String newline = "\n";

//-----------------------------------------------------------------------------
// Controller::Controller (constructor)
//

public Controller()
{

}//end of Controller::Controller (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::init
//
// Initializes the object.  Must be called immediately after instantiation.
//

public void init()
{

    view = new View(this, this);
    view.init();

    //create and load the program options
    options = new Options();

    //start the control thread
    new Thread(this).start();

    setupAndStartMainTimer();

}// end of Controller::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::setupAndStartMainTimer
//
// Prepares and starts a Java Swing timer.
//

public void setupAndStartMainTimer()
{

    //main timer has 2 second period
    mainTimer = new javax.swing.Timer (2000, this);
    mainTimer.setActionCommand ("Timer");
    mainTimer.start();

}// end of Controller::setupAndStartMainTimer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::actionPerformed
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

}//end of Controller::actionPerformed
//-----------------------------------------------------------------------------

/*
//-----------------------------------------------------------------------------
// Controller::paintComponent
//

@Override
public void paintComponent (Graphics g)
{

}// end of Controller::paintComponent
//-----------------------------------------------------------------------------

*/

//-----------------------------------------------------------------------------
// Controller::doTimerActions
//
// Performs actions driven by the timer.
//
// Not used for accessing network -- see run function for details.
//

public void doTimerActions()
{


}//end of Controller::doTimerActions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::displayLog
//
// Displays the log window. It is not released after closing as the information
// is retained so it can be viewed the next time the window is opened.
//

private void displayLog()
{

    view.displayLog();

}//end of Controller::displayLog
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::displayHelp
//
// Displays help information.
//

private void displayHelp()
{

    view.displayHelp();

}//end of Controller::displayHelp
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::displayAbout
//
// Displays about information.
//

private void displayAbout()
{

    view.displayAbout();

}//end of Controller::displayAbout
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::doSomething1
//

private void doSomething1()
{


}//end of Controller::doSomething1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::doSomethingInWorkerThread
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

}//end of Controller::doSomethingInWorkerThread
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::doSomething2
//

private void doSomething2()
{


}//end of Controller::doSomething2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::run
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

}//end of Controller::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::threadSleep
//
// Calls the Thread.sleep function. Placed in a function to avoid the
// "Thread.sleep called in a loop" warning -- yeah, it's cheezy.
//

public void threadSleep(int pSleepTime)
{

    try {Thread.sleep(pSleepTime);} catch (InterruptedException e) { }

}//end of Controller::threadSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::control
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

}//end of Controller::control
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

public void displayErrorMessage(String pMessage)
{

    view.displayErrorMessage(pMessage);

}//end of Controller::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::shutDown
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

}//end of Controller::shutDown
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::windowClosing
//
// Handles actions necessary when the window is closing
//

@Override
public void windowClosing(WindowEvent e)
{

    //perform all shut down procedures

    shutDown();

}//end of Controller::windowClosing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Controller::(various window listener functions)
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

//end of Controller::(various window listener functions)
//-----------------------------------------------------------------------------


}//end of class Controller
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
