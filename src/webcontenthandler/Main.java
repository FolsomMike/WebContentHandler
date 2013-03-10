/******************************************************************************
* Title: Main.java - Main Source File for Temperature Monitor
* Author: Mike Schoonover
* Date: 11/15/12
*
* Purpose:
*
* This application is used to control devices and create files for use with
* communicating with the subconscious brain.
*
* The following is a list of some of the uses of the program:
*
* generate binaural audio files to be played on a separate program to help
*  the listener achieve alpha, theta, and delta states
* control via network of devices which provide physical, visual, aural stimulus
*  to the subject
* monitoring via network of devices which read various types of signals from
*  the subject
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package webcontenthandler;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MainWindow
//
// Creates a main window JFrame.  Creates all other objects needed by the
// program.
//
// Listens for events generated by the main window.  Calls clean up functions
// on program exit.
//

class MainWindow{

    MainFrame mainFrame;

//-----------------------------------------------------------------------------
// MainWindow::MainWindow (constructor)
//

public MainWindow()
{

}//end of MainWindow::MainWindow (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainWindow::init
//
// Call this after construction to setup the object
//

public void init()
{

    //create the program's main window
    mainFrame = new MainFrame("Needs a Title");
    mainFrame.init();

}//end of MainWindow::init
//-----------------------------------------------------------------------------

}//end of class MainWindow
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------s

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Main
//

public class Main{

static MainWindow mainWindow;

//-----------------------------------------------------------------------------
// Main::createAndShowGUI
//
// Create the GUI and show it. For thread safety, this method should be invoked
// from the event-dispatching thread.  This is usually done by using
// invokeLater to schedule this funtion to be called from inside the event-
// dispatching thread.  This is necessary because the main function is not
// operating in the event-dispatching thread.  See the main function for more
// info.
//

private static void createAndShowGUI()
{

    //instantiate an object to create and handle the main window JFrame
    mainWindow = new MainWindow();
    mainWindow.init();

}//end of Main::createAndShowGUI
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Main::main
//

public static void main(String[] args)
{

    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.

    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() { createAndShowGUI(); } });

}//end of Main::main
//-----------------------------------------------------------------------------

}//end of class Main
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// Useful debugging code

//displays message on bottom panel of IDE
//System.out.println("File not found");

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
