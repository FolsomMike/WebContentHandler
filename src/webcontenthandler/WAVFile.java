/******************************************************************************
* Title: WAVFile.java - Main Source File for Temperature Monitor
* Author: Mike Schoonover
* Date: 11/17/12
*
* Purpose:
*
* This class loads a WAV file and displays its meta data and part of its
* sound data in the log. The data can be modified and saved.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package webcontenthandler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class WAVFile
//
// The following is the Microsoft defined structure WAVEFORMATEX. For this
// program, the structure is not used as is -- classes are defined using the
// necessary parts. It is shown here as it helps to explain the values saved
// to the WAV file.
//
// Microsoft defines:
//
//    typedef struct {
//      WORD  wFormatTag; (not saved to the WAV file -- used by Windows)
//      WORD  nChannels;
//      DWORD nSamplesPerSec;
//      DWORD nAvgBytesPerSec;
//      WORD  nBlockAlign;
//      WORD  wBitsPerSample;
//      WORD  cbSize;
//    } WAVEFORMATEX;
//
// typedef unsigned short WORD; (a 16-bit unsigned integer)
// typedef unsigned long DWORD; (a 32-bit unsigned integer)
//
// Members:
//
// wFormatTag
//
//  Waveform-audio format type. Format tags are registered with Microsoft
//  Corporation for many compression algorithms. A complete list of format tags
//  can be found in the Mmreg.h header file. For one- or two-channel Pulse Code
//  Modulation (PCM) data, this value should be WAVE_FORMAT_PCM.
//
//  If wFormatTag equals WAVE_FORMAT_EXTENSIBLE, the structure is interpreted
//  as a WAVEFORMATEXTENSIBLE structure.
//  If wFormatTag equals WAVE_FORMAT_MPEG, the structure is interpreted as an
//  MPEG1WAVEFORMAT structure.
//  If wFormatTag equals WAVE_FORMAT_MPEGLAYER3, the structure is interpreted
//  as an MPEGLAYER3WAVEFORMAT structure.
//
//  Before reinterpreting a WAVEFORMATEX structure as one of these extended
//  structures, verify that the actual structure size is sufficiently large and
//  that the cbSize member indicates a valid size.
//
// nChannels
//
//  Number of channels in the waveform-audio data. Monaural data uses one
//  channel and stereo data uses two channels.
//
// nSamplesPerSec
//
//  Sample rate, in samples per second (hertz). If wFormatTag is
//  WAVE_FORMAT_PCM, then common values for nSamplesPerSec are 8.0 kHz,
//  11.025 kHz, 22.05 kHz, and 44.1 kHz. For non-PCM formats, this member must
//  be computed according to the manufacturer's specification of the format tag.
//
// nAvgBytesPerSec
//
//  Required average data-transfer rate, in bytes per second, for the format
//  tag. If wFormatTag is WAVE_FORMAT_PCM, nAvgBytesPerSec must equal
//  nSamplesPerSec × nBlockAlign. For non-PCM formats, this member must be
//  computed according to the manufacturer's specification of the format tag.
//
// nBlockAlign
//
//  Block alignment, in bytes. The block alignment is the minimum atomic unit
//  of data for the wFormatTag format type. If wFormatTag is WAVE_FORMAT_PCM,
//  nBlockAlign must equal (nChannels × wBitsPerSample) / 8. For non-PCM
//  formats, this member must be computed according to the manufacturer's
//  specification of the format tag.
//
//  Software must process a multiple of nBlockAlign bytes of data at a time.
//  Data written to and read from a device must always start at the beginning
//  of a block. For example, it is illegal to start playback of PCM data in the
//  middle of a sample (that is, on a non-block-aligned boundary).
//
// wBitsPerSample
//
//  Bits per sample for the wFormatTag format type. If wFormatTag is
//  WAVE_FORMAT_PCM, then wBitsPerSample should be equal to 8 or 16. For
//  non-PCM formats, this member must be set according to the manufacturer's
//  specification of the format tag. If wFormatTag is WAVE_FORMAT_EXTENSIBLE,
//  this value can be any integer multiple of 8.
//
//  Some compression schemes do not define a value for wBitsPerSample, so this
//  member can be zero.
//
// cbSize
//
//  Size, in bytes, of extra format information appended to the end of the
//  WAVEFORMATEX structure. This information can be used by non-PCM formats to
//  store extra attributes for the wFormatTag. If no extra information is
//  required by the wFormatTag, this member must be set to zero.
//  For WAVE_FORMAT_PCM formats (and only WAVE_FORMAT_PCM formats), this member
//  is ignored. However it is still recommended to set the value.
//  (MKS note: this value is not saved to the WAV file if it is 0)
//
//
// Java does not implement unsigned values, so they are stored in the next
// larger variable type which can hold the entire value as a positive number.
// Thus, WORD values (16 bit) are stored as integers (32 bit) and
// DWORD values (32 bit) are stored as longs (64 bits).
//
// When loaded as bytes, the unsigned values are not sign extended as they
// are concatenated into the larger variable type. This retains their unsigned
// nature.
//
// Thus all the values in the structure are unsigned.
//
// Search www.microsoft.com for:
//  "Writing to a WAV File"
//  "WAVEFORMATEX structure (Windows)"
//
//

class WAVFile extends Object{

    String filename;
    ThreadSafeLogger tsLog;
    GuiUpdater guiUpdater;

    JLabel progressLabel;

    //calculate 2 * PI once for use later in loops
    double twoPI = 2 * Math.PI;

    static private final String newline = "\n";

    private HashMap<Integer, String> compressionNames;

    int totalSampleSize = 0;

    FileOutputStream out = null;

    int [][] viewChannels;

    int sampleIndex;
    int prevSampleIndex;
    int sectionSize;

    class Channel{

        //frequency is a double because it ramps up or down fractionally
        //as it moves from the starting frequency to the ending frequency
        //for a section

        double frequency;
        double freqStep;
        // no longer used -- data saved straight to file -- int samples[];
        int amplitude;

    }

    Channel leftChannel, rightChannel;

    //see notes at top of Class declaration for info on the Chunk classes

    class Chunk{

        //all values are unsigned so they are stored in the next largest
        //variable size in order to accomodate the max positive values

        long chunkID;
        String chunkIDText;
        long chunkDataSize;

    }//end of class Chunk

    class RIFFLISTTypeChunk extends Chunk{

        //contains variables used by RIFF and LIST chunks

        //all values are unsigned so they are stored in the next largest
        //variable size in order to accomodate the max positive values

        long type;
        String typeText;

    }//end of class RIFFTypeChunk


    class RIFFTypeChunk extends RIFFLISTTypeChunk{

    }//end of class RIFFTypeChunk

    class FormatChunk extends Chunk{

        //all values are unsigned so they are stored in the next largest
        //variable size in order to accomodate the max positive values

        int compressionCode;            //2 unsigned bytes from file
        int numberOfChannels;           //2 unsigned bytes from file
        long sampleRate;                //4 unsigned bytes from file
        long averageBytesPerSecond;     //4 unsigned bytes from file
        int blockAlign;                 //2 unsigned bytes from file
        int significantBitsPerSample;   //2 unsigned bytes from file
        int extraFormatBytes;           //2 unsigned bytes from file
                                        //(or none if compressionCode is 0)
                                        //this is the number of format bytes
                                        //needed to define compression when
                                        //used

        int valueRange;                 //used when creating audio

        //to handle compression, need to add an array of integers here
        //to hold extra format bytes

    }//end of class FormatChunk

    class DataChunk extends Chunk{

        //all values are unsigned so they are stored in the next largest
        //variable size in order to accomodate the max positive values

        int numberOfSamples;

    }//end of class DataChunk

    class ListChunk extends RIFFLISTTypeChunk{

        //all values are unsigned so they are stored in the next largest
        //variable size in order to accomodate the max positive values

    }//end of class ListChunk

    class ICOPChunk extends Chunk{

        //all values are unsigned so they are stored in the next largest
        //variable size in order to accomodate the max positive values

        String text;

    }//end of class ICOPChunk

    Chunk chunk;
    RIFFTypeChunk riffTypeChunk;
    FormatChunk formatChunk;
    DataChunk dataChunk;
    ListChunk listChunk;
    ICOPChunk icopChunk;
    Chunk unknownChunk;

    boolean riffTypeChunkHandled = false;
    boolean formatChunkHandled = false;
    boolean dataChunkHandled = false;

    static int RIFF_TYPE_CHUNK = 0;
    static int FORMAT_CHUNK = 1;
    static int DATA_CHUNK = 2;
    static int LIST_CHUNK = 3;

    static int SIGNED_WORD = 1;
    static int UNSIGNED_BYTE = 2;

    static int LEFT = 0;
    static int RIGHT = 1;


//-----------------------------------------------------------------------------
// WAVFile::WAVFile (constructor)
//

public WAVFile(String pFilename, ThreadSafeLogger pTSLog,
                                 GuiUpdater pGuiUpdater, JLabel pProgressLabel)
{

    filename = pFilename;
    tsLog = pTSLog;
    guiUpdater = pGuiUpdater;
    progressLabel = pProgressLabel;

}//end of WAVFile::WAVFile (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::init
//

public void init()
{
    //create objects to hold data
    chunk = new Chunk();
    riffTypeChunk = new RIFFTypeChunk();
    formatChunk = new FormatChunk();
    dataChunk = new DataChunk();
    listChunk = new ListChunk();
    unknownChunk = new Chunk();

    compressionNames = new HashMap<Integer, String>();

    compressionNames.put(0x0000, "Unknown");
    compressionNames.put(0x0001, "PCM/uncompressed");
    compressionNames.put(0x0002, "Microsoft ADPCM");
    compressionNames.put(0x0006, "ITU G.711 a-law");
    compressionNames.put(0x0007, "ITU G.711 Âµ-law");
    compressionNames.put(0x0011, "IMA ADPCM");
    compressionNames.put(0x0016, "ITU G.723 ADPCM (Yamaha)");
    compressionNames.put(0x0031, "GSM 6.10");
    compressionNames.put(0x0040, "ITU G.721 ADPCM");
    compressionNames.put(0x0050, "MPEG");
    compressionNames.put(0xFFFF, "Experimental");

}//end of WAVFile::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::loadFile
//
// Loads a WAV file.
//

public void loadFile()
{

    FileInputStream in = null;

    try {

        in = new FileInputStream(filename);

        //scan through the file processing any RIFF chunks found
        scanFileForChunks(in);

        //even though an int is used here, only one byte at a time is loaded
        int c;

        //display any remaining data in the file as raw hex
        while ((c = in.read()) != -1) {
            tsLog.appendLine("0x" + Integer.toString(c, 16));
        }

    }//try
    catch(IOException e){
        logError("Error opening or reading file: ", e.getMessage());
    }
    finally {
        if (in != null) {
            try{
                in.close();
            }
            catch(IOException e){
                logError("Error closing file: ", e.getMessage());
            }
        }//if (in...
    }//finally

}//end of WAVFile::loadFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::scanFileForChunks
//
// Scans pIn for RIFF chunks and processes any which are found.
//
// Throws IOException if an I/O error occurs or if the end of the file is
// reached unexpectedly.
//

private void scanFileForChunks(FileInputStream pIn) throws IOException
{

    //scan through the file reading chunk ID and data sizes
    while(true){

        try{
            //attempt to read the Chunk ID and Chunk Data Size
            readIDAndChunkSize(pIn, chunk);

            if (chunk.chunkIDText.equals("RIFF")) {
                handleRIFFTypeChunk(pIn, chunk);
            }
            else if (chunk.chunkIDText.equals("fmt ")) {
                handleFormatChunk(pIn, chunk);
            }
            else if (chunk.chunkIDText.equals("data")) {
                handleDataChunk(pIn, chunk);
            }
            else if (chunk.chunkIDText.equals("LIST")) {
                handleLISTChunk(pIn, chunk);
            }
            else {
                handleUnknownTypeChunk(pIn, chunk);
            }

        }
        catch(IOException e){

            //exceptions are caught for readIDAndChunkSize because if the end of
            //the file is reached and exception will be thrown, but when
            //searching for new chunks the EOF will be reached when there are
            //no more chunks but it is not unexpected so catch that case but
            //rethrow all other exceptions

            if(e.getMessage().equals("End of file reached unexpectedly.")) {
                return;
            }
            else {
                throw(e);
            }

        }//catch(IOException e)
    }//while(true)

}//end of WAVFile::scanFileForChunks
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::handleRIFFTypeChunk
//
// Finishes reading a RIFF chunk.
// The data from pChunk is transferred to the RIFF chunk.
// Displays the ID as a hex value and a string.
// Displays the Chunk Data Size as a decimal value.
// Displays the RIFF Type as a hex value and a string.
// Sets the riffTypeChunkHandled flag.
//
// If there is an unsupported format error, throws an IOException.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void handleRIFFTypeChunk(FileInputStream pIn, Chunk pChunk)
                                                            throws IOException
{

    //display the header
    tsLog.appendLine("-- RIFF Type Chunk ------------------------------------");

    //copy values already read into the actual chunk type
    riffTypeChunk.chunkID = pChunk.chunkID;
    riffTypeChunk.chunkIDText = pChunk.chunkIDText;
    riffTypeChunk.chunkDataSize = pChunk.chunkDataSize;

    //display the Chunk ID and Chunk Data Size
    displayIDAndChunkSize(riffTypeChunk);

    //read and display the type
    readType(pIn, riffTypeChunk, "File Type: ");

    if(!riffTypeChunk.typeText.equals("WAVE")){
        logError("Error -- WAVE type RIFF file expected: ", "");
        logFileRequirements();
        throw new IOException("WAVE type RIFF file expected.");
        }

    //flag that the RIFF Type Chunk has been found and processed
    riffTypeChunkHandled = true;

}//end of WAVFile::handleRIFFTypeChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::handleFormatChunk
//
// Finishes reading a Format chunk.
// The data from pChunk is transferred to the Format chunk.
// Displays the ID as a hex value and a string.
// Displays the Chunk Data Size as a decimal value.
// Sets the formatChunkHandled flag.
//
// If there is an unsupported format error, throws an IOException.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void handleFormatChunk(FileInputStream pIn, Chunk pChunk)
                                                            throws IOException
{

    //display the header
    tsLog.appendLine("-- Format Chunk ---------------------------------------");

    //make sure all necessary support chunks have been handled
    checkForChunkSequencingOrder(FORMAT_CHUNK);

    //copy values already read into the actual chunk type
    formatChunk.chunkID = pChunk.chunkID;
    formatChunk.chunkIDText = pChunk.chunkIDText;
    formatChunk.chunkDataSize = pChunk.chunkDataSize;

    //display the Chunk ID and Chunk Data Size
    displayIDAndChunkSize(formatChunk);

    //read and display the compression type

    formatChunk.compressionCode = readUnsignedShort(pIn);
    tsLog.appendString("Compression Type: ");
    tsLog.appendString("" + formatChunk.compressionCode);
    tsLog.appendLine(" (" + compressionNames.get(formatChunk.compressionCode)
                                                                        + ")");
    if (formatChunk.compressionCode != 1){
        logError("Error -- Compression Code not Supported: ", "");
        logFileRequirements();
        throw new IOException("Compression Code not Supported.");
    }

    //read and display the number of audio channels

    formatChunk.numberOfChannels = readUnsignedShort(pIn);
    tsLog.appendString("Number of Channels: ");
    tsLog.appendLine("" + formatChunk.numberOfChannels);

    formatChunk.sampleRate = readUnsignedInt(pIn);
    tsLog.appendString("Sample Rate: ");
    tsLog.appendLine("" + formatChunk.sampleRate);

    formatChunk.averageBytesPerSecond = readUnsignedInt(pIn);
    tsLog.appendString("Average Bytes Per Second: ");
    tsLog.appendLine("" + formatChunk.averageBytesPerSecond);

    formatChunk.blockAlign = readUnsignedShort(pIn);
    tsLog.appendString("Block Align: ");
    tsLog.appendLine("" + formatChunk.blockAlign);

    formatChunk.significantBitsPerSample = readUnsignedShort(pIn);
    tsLog.appendString("Significant Bits per Sample: ");
    tsLog.appendLine("" + formatChunk.significantBitsPerSample);

    //check for values not supported by this application

    if(formatChunk.numberOfChannels != 1
            && formatChunk.numberOfChannels != 2){
        logError("Error -- Specified Number of Channels not supported: ", "");
        logFileRequirements();
        throw new IOException("Specified Number of Channels not supported.");
    }

    if(formatChunk.significantBitsPerSample != 8
            && formatChunk.significantBitsPerSample != 16){
        logError(
          "Error -- Specified Significant Bits per Sample not supported: ", "");
        logFileRequirements();
        throw new IOException(
                        "Specified Significant Bits per Sample not supported.");
    }

    //if other compressions are supported in the future, there will be more
    //information bytes to read here -- in that case chunkDataSize will also
    //be bigger to specify the number of bytes to read to finish the chunk

    //flag that the Format Chunk has been found and processed
    formatChunkHandled = true;

}//end of WAVFile::handleFormatChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::handleDataChunk
//
// Finishes reading a Format chunk.
// The data from pChunk is transferred to the Data chunk.
// Displays the Chunk Data Size as a decimal value.
// Sets the dataChunkHandled flag.
//
// If there is an unsupported format error, the function returns false,
// otherwise it returns true.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void handleDataChunk(FileInputStream pIn, Chunk pChunk)
                                                            throws IOException
{

    //display the header
    tsLog.appendLine("-- Data Chunk -----------------------------------------");

    //make sure all necessary support chunks have been handled
    checkForChunkSequencingOrder(DATA_CHUNK);

    //copy values already read into the actual chunk type
    dataChunk.chunkID = pChunk.chunkID;
    dataChunk.chunkIDText = pChunk.chunkIDText;
    dataChunk.chunkDataSize = pChunk.chunkDataSize;

    //display the Chunk ID and Chunk Data Size
    displayIDAndChunkSize(dataChunk);

    long numberOfSamples =
        dataChunk.chunkDataSize / formatChunk.numberOfChannels
            / (formatChunk.significantBitsPerSample / 8);

    tsLog.appendString("Number of Samples per Channel: ");
    tsLog.appendLine("" + numberOfSamples);

    if(numberOfSamples > Integer.MAX_VALUE){
        logError("Error -- " + "Too many Samples per Channel: ", "");
        logFileRequirements();
        throw new IOException("Too many Samples per Channel.");
    }

    dataChunk.numberOfSamples = (int) numberOfSamples;

    //initialize the array to hold the data samples, load and display the data
    readDataSamples(pIn, true);

    //flag that the Data Chunk has been found and processed
    dataChunkHandled = true;

}//end of WAVFile::handleDataChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::handleLISTChunk
//
// Finishes reading a LIST type of chunk.
// The data from pChunk is transferred to the Unknown chunk.
// Displays the ID as a hex value and a string.
// Displays the Chunk Data Size as a decimal value.
//
// LIST chunks contain sub-chunks, the sub-chunks are handled. The end of the
// sub-chunks is recognized by the Chunk Data Size of the LIST chunk.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void handleLISTChunk(FileInputStream pIn, Chunk pChunk)
                                                            throws IOException
{

    //display the header
    tsLog.appendLine("-- LIST Chunk -----------------------------------------");

    //copy values already read into the actual chunk type
    listChunk.chunkID = pChunk.chunkID;
    listChunk.chunkIDText = pChunk.chunkIDText;
    listChunk.chunkDataSize = pChunk.chunkDataSize;

    //display the Chunk ID and Chunk Data Size
    displayIDAndChunkSize(listChunk);

    //read and display the list type
    readType(pIn, listChunk, "List Type: ");

    //process all sub-chunks in the LIST chunk
    scanFileForLISTSubChunks(pIn, listChunk.chunkDataSize);

}//end of WAVFile::handleLISTChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::scanFileForLISTSubChunks
//
// Scans pIn for LIST sub-chunks and processes any which are found.
// When the number of bytes processed while handling the sub-chunks reaches
// pLISTChunkDataSize, the funtion exits.
//
// Throws IOException if an I/O error occurs or if the end of the file is
// reached unexpectedly.
//

private void scanFileForLISTSubChunks(FileInputStream pIn,
                                    long pLISTChunkDataSize) throws IOException
{

    //the 4 LIST type bytes have already been read
    long bytesProcessed = 4;

    //scan through the file reading chunk ID and data sizes
    //stop when the number of bytes processed equals the data chunk size
    //specified in the LIST chunk -- that's the end of the list of sub-chunks
    //for that LIST chunk

    while(bytesProcessed < pLISTChunkDataSize){

        //attempt to read the Chunk ID and Chunk Data Size
        readIDAndChunkSize(pIn, chunk);

        bytesProcessed += 8; //count bytes for id and size

        //add in the number of data bytes for the chunk
        bytesProcessed += chunk.chunkDataSize;

        if (chunk.chunkIDText.equals("ICOP")) {
            handleLISTINFOSubChunkICOP(pIn, chunk);
        }
        else {
            handleUnknownTypeChunk(pIn, chunk);
        }

    }//while(true)

}//end of WAVFile::scanFileForLISTSubChunks
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::handleLISTINFOSubChunkICOP
//
// Finishes reading a LIST of INFO type subchunk ICOP.
// The data from pChunk is transferred to the LIST INFO chunk.
// Displays the ID as a hex value and a string.
// Displays the Chunk Data Size as a decimal value.
// Reads the rest of the data as specified by the Chunk Data Size value from
// the chunk and displays it.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void handleLISTINFOSubChunkICOP(FileInputStream pIn, Chunk pChunk)
                                                            throws IOException
{

    //display the header
    tsLog.appendLine("-- LIST/INFO sub-chunk ICOP ---------------------------");

    //create this type of chunk as needed as it may not be present in the file
    icopChunk = new ICOPChunk();

    //copy values already read into the actual chunk type
    icopChunk.chunkID = pChunk.chunkID;
    icopChunk.chunkIDText = pChunk.chunkIDText;
    icopChunk.chunkDataSize = pChunk.chunkDataSize;

    //display the Chunk ID as a key -- the string will be displayed as the value
    //the INFO strings are somewhat like key/value pairs
    tsLog.appendString(icopChunk.chunkIDText + ": ");

    icopChunk.text = "";

    //read the chunk data as a string
    int c;
    for (int i=0; i<icopChunk.chunkDataSize; i++){

        c = readByte(pIn);
        icopChunk.text += Character.toString ((char) (c & 0xff));

    }

    //display the string to complete the key/value line
    tsLog.appendLine(icopChunk.text);

    //if the number of data bytes is odd, there will be a padding byte at the
    //end; read and discard to complete the chunk and position the file pointer
    //to read the next chunk
    if ((icopChunk.chunkDataSize % 2) != 0){
         tsLog.appendString("Padding byte: "); logInteger(readByte(pIn));
     }

}//end of WAVFile::handleLISTINFOSubChunkICOP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::handleUnknownTypeChunk
//
// Finishes reading an unknown type of chunk.
// The data from pChunk is transferred to the Unknown chunk.
// Displays the ID as a hex value and a string.
// Displays the Chunk Data Size as a decimal value.
// Reads the rest of the data as specified by the Chunk Data Size value from
// the chunk and displays it.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void handleUnknownTypeChunk(FileInputStream pIn, Chunk pChunk)
                                                            throws IOException
{

    //display the header
    tsLog.appendLine("-- Unknown Type Chunk ---------------------------------");

    //copy values already read into the actual chunk type
    unknownChunk.chunkID = pChunk.chunkID;
    unknownChunk.chunkIDText = pChunk.chunkIDText;
    unknownChunk.chunkDataSize = pChunk.chunkDataSize;

    //display the Chunk ID and Chunk Data Size
    displayIDAndChunkSize(unknownChunk);

    //read and display the data bytes -- readByte will throw an exception if
    //the EOF file is reached unexpectedly

    int c;
    for (int i=0; i<unknownChunk.chunkDataSize; i++){
        c = readByte(pIn);
        tsLog.appendLine("0x" + Integer.toString(c, 16));
    }

    //if the number of data bytes is odd, there will be a padding byte at the
    //end; read and discard to complete the chunk and position the file pointer
    //to read the next chunk
    if ((unknownChunk.chunkDataSize % 2) != 0){
         tsLog.appendString("Padding byte: "); logInteger(readByte(pIn));
     }

}//end of WAVFile::handleUnknownTypeChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readIDAndChunkSize
//
// Reads eight bytes as the Chunk ID and the Chunk Data Size.
// The values are stored in pChunk.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void readIDAndChunkSize(FileInputStream pIn, Chunk pChunk)
                                                            throws IOException
{

    //read and display the Chunk ID
    readID(pIn, pChunk);

    //read the Chunk Data Size
    pChunk.chunkDataSize = readUnsignedInt(pIn);

}//end of WAVFile::readIDAndChunkSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::displayIDAndChunkSize
//
// Displays pChunk.ID as a hex value and a string.
// Displays the pChunk.chunkDataSize as a decimal value.
//

private void displayIDAndChunkSize(Chunk pChunk)
{

    //display the description and the ID value as a hex string and a text string
    tsLog.appendString("Chunk ID: ");
    logUnsignedInteger(pChunk.chunkID);
    tsLog.appendString("  as a text string: ");
    tsLog.appendLine(pChunk.chunkIDText);

    tsLog.appendString("Chunk Data Size: ");
    tsLog.appendLine("" + pChunk.chunkDataSize);

}//end of WAVFile::displayIDAndChunkSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readID
//
// Reads four bytes into an integer, Big-Endian order as would be proper
// for an ASCII string.
// Also converts the value to a string.
// Uses pDescription as a description for the displayed values.
// The values are stored in pChunk.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void readID(FileInputStream pIn, Chunk pChunk) throws IOException
{

    //read the chunk ID
    pChunk.chunkID = read4Bytes(pIn);

    //convert the ASCII values to a text string
    pChunk.chunkIDText = intASCIIToString(pChunk.chunkID);

}//end of WAVFile::readID
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readType
//
// RIFF and LIST chunks list the file type or list type after the chunk data
// size. This method can be used to read and display that value.
//
// Reads four bytes into an integer and displays them as a hex value.
// Also converts them to a string and displays that as well.
// Uses pDescription as a description for the displayed values.
// The values are stored in pChunk.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private void readType(FileInputStream pIn, RIFFLISTTypeChunk pChunk,
                                        String pDescription) throws IOException
{

    //read the RIFF chunk file type or LIST chunk list type

    pChunk.type = read4Bytes(pIn);

    //convert the ASCII values to a text string
    pChunk.typeText = intASCIIToString(pChunk.type);

    //display the description and the value as a hex string and a text string
    tsLog.appendString(pDescription);
    logUnsignedInteger(pChunk.type);
    tsLog.appendString("  as a text string: ");
    tsLog.appendLine(pChunk.typeText);

}//end of WAVFile::readType
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readByte
//
// Reads byte from pIn. This wrapper is used so it can thrown an IOException
// if the end of the file is passed. The  file should specify the number of
// data bytes available, so if the end is reached unexpectedly, this application
// treats it as an exception.
//
// The byte is returned as an integer without sign extension, so the resulting
// value is unsigned.
//
// Throws IOException if an I/O error occurs or if an attempt is made to read
// past the end of the file.
//

private int readByte(FileInputStream pIn) throws IOException
{
    int r = pIn.read();

    if ( r== -1) {throw new IOException("End of file reached unexpectedly.");}

    return(r);

}//end of WAVFile::readByte
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::read4Bytes
//
// Reads four bytes from file pIn and concatenates them into an integer
// which is then returned. It is expected that four bytes are available in
// the file.
//
// Mainly used to load ASCII strings -- numeric values in WAV files must be
// read in Little-Endian order.
//
// NOTE: the bytes are read in Big-Endian order as would be used for an ASCII
// text string for a chunk ID.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private int read4Bytes(FileInputStream pIn) throws IOException
{
    int r =
       (int)(
            ((readByte(pIn)<<24) & 0xff000000) +
            ((readByte(pIn)<<16) & 0xff0000) +
            ((readByte(pIn)<<8) & 0xff00) +
            (readByte(pIn) & 0xff));

    return(r);

}//end of WAVFile::read4Bytes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readInt
//
// Reads four bytes from file pIn and concatenates them into an integer
// which is then returned. It is expected that sufficient bytes are available in
// the file.
//
// The value will be sign extended.
//
// NOTE: the bytes are read in Little-Endian order as would be used for a
// chunk data size value.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private int readInt(FileInputStream pIn) throws IOException
{

    int r =
       (int)(
            (readByte(pIn) & 0xff) +
            ((readByte(pIn)<<8) & 0xff00) +
            ((readByte(pIn)<<16) & 0xff0000) +
            ((readByte(pIn)<<24) & 0xff000000)
            );

    return(r);

}//end of WAVFile::readInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readUnsignedInt
//
// Reads four bytes from file pIn and concatenates them into an long value
// which is then returned. It is expected that sufficient bytes are available in
// the file.
//
// The value will be not be sign extended and will be returned as a long value
// so that it can reflect the full range of an unsigned integer. This method
// is used because Java does not implement unsigned variables.
//
// NOTE: the bytes are read in Little-Endian order as would be used for a
// chunk data size value.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private long readUnsignedInt(FileInputStream pIn) throws IOException
{

    //the bytes read from the file (returned as integers by the read function)
    //are cast as long values to prevent sign extension in the final value

    long r =
        (
         ((long)readByte(pIn) & 0xff) +
         (((long)readByte(pIn)<<8) & 0xff00) +
         (((long)readByte(pIn)<<16) & 0xff0000) +
         (((long)readByte(pIn)<<24) & 0xff000000)
         );



    return(r);

}//end of WAVFile::readUnsignedInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readShort
//
// Reads two bytes from file pIn and concatenates them into an integer
// which is then returned. It is expected that sufficient bytes are available in
// the file.
//
// The value will be sign extended.
//
// NOTE: the bytes are read in Little-Endian order as would be used for a
// chunk data size value.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private int readShort(FileInputStream pIn) throws IOException
{

    //cast to short used to force sign extension in the int variable

    int r =
       (short)(
            (readByte(pIn) & 0xff) +
            ((readByte(pIn)<<8) & 0xff00)
            );

    return(r);

}//end of WAVFile::readShort
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readUnsignedShort
//
// Reads two bytes from file pIn and concatenates them into an int value
// which is then returned. It is expected that sufficient bytes are available in
// the file.
//
// The value will be not be sign extended and will be returned as an int value
// so that it can reflect the full range of an unsigned short. This method
// is used because Java does not implement unsigned variables.
//
// NOTE: the bytes are read in Little-Endian order as would be used for a
// chunk data size value.
//
// If there are too few bytes available in the file or if there is an error
// in reading the file, throws IOException.
//

private int readUnsignedShort(FileInputStream pIn) throws IOException
{

    //the bytes read from the file (returned as integers by the read function)
    //are cast as long values to prevent sign extension in the final value

    int r =
            (
            ((int)readByte(pIn) & 0xff) +
            (((int)readByte(pIn)<<8) & 0xff00)
            );

    return(r);

}//end of WAVFile::readUnsignedShort
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::initViewSampleArray
//
// Instantiates the sample arrays used to view a WAV file. This consists of a
// multi-dimensional array for which the first index is the channel and the
// second is the time slot.
//
// The number of channels is read from formatChunk.numberOfChannels.
// The number of samples is read from dataChunk.numberOfsamples.
//

private void initViewSampleArray()
{

    viewChannels =
            new int [formatChunk.numberOfChannels][dataChunk.numberOfSamples];

}//end of WAVFile::initViewSampleArray
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readDataSamples
//
// Instantiates the sample array and reads the data samples into them.
// If pDisplay is true, the samples are also displayed in the log window.

private void readDataSamples(FileInputStream pIn, boolean pDisplay)
                                                            throws IOException
{

    //create the viewing sample array
    initViewSampleArray();

    for (int x=0; x<viewChannels[0].length; x++){

        //read one sample for each channel
        for (int y=0; y<viewChannels.length; y++) {
            viewChannels[y][x] = readDataSample(pIn);
        }

    }

    //display the values if pDisplay is true
    if (pDisplay){

        tsLog.appendLine("");
        tsLog.appendLine("Data Samples (channel 1 or channel 1,channel 2):");

        for (int x=0; x<viewChannels[0].length; x++){

            for (int y=0; y<viewChannels.length; y++){
                if (y == 0) {tsLog.appendString("\n" + viewChannels[y][x]);}
                if (y == 1) {tsLog.appendString(" , " + viewChannels[y][x]);}
            }
        }
    }

    tsLog.appendLine("");

    //if the number of data bytes is odd, there will be a padding byte at the
    //end; read and discard to complete the chunk and position the file pointer
    //to read the next chunk if there is one
     if ((dataChunk.chunkDataSize % 2) != 0){
         tsLog.appendString("Padding byte: "); logInteger(readByte(pIn));
     }

}//end of WAVFile::readDataSamples
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::readDataSample
//
// Reads one data sample.
//
// If formatChunk.significantBitsPerSample = 8, one unsigned byte is read.
// If formatChunk.significantBitsPerSample = 16, one signed word is read.
//
// WAV files use unsigned values for 8 bit samples and signed values for 16
// bit samples.
//
// The value of the sample is returned as an integer.
//

private int readDataSample(FileInputStream pIn) throws IOException
{

    int s;

    if(formatChunk.significantBitsPerSample == 8){
        s = readByte(pIn); //return as an unsigned byte
        return(s);
    }

    if(formatChunk.significantBitsPerSample == 16){
        s = readShort(pIn); //return as a signed integer
        return(s);
    }

    //invalid bits per sample so return zero
    return(0);

}//end of WAVFile::readDataSample
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::intASCIIToString
//
// Converts the four ASCII bytes in pValue to a string and returns it.
//

private String intASCIIToString(long pValue)
{

    String s = "";

    s += Character.toString ((char) ((pValue >> 24) & 0xff));
    s += Character.toString ((char) ((pValue >> 16) & 0xff));
    s += Character.toString ((char) ((pValue >> 8) & 0xff));
    s += Character.toString ((char) (pValue & 0xff));

    return(s);

}//end of WAVFile::intASCIIToString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::logInteger
//
// Appends the integer pValue to the log as a four byte hex value.
//

private void logInteger(int pValue)
{

    tsLog.appendLine("0x" + Log.toHex8String(pValue));

}//end of WAVFile::logInteger
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::logUnsignedInteger
//
// Appends the unsigned integer pValue to the log as a four byte hex value.
//
// Since Java does not implement unsigned variables, the unsigned integer is
// transferred as a long value which is large enough to contain the full
// positive value of an unsigned integer
//

private void logUnsignedInteger(long pValue)
{

    tsLog.appendLine("0x" + Log.toUnsignedHex8String(pValue));

}//end of WAVFile::logUnsignedInteger
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::logFileRequirements
//
// Displays an explanation of the format requirements for a file to be
// displayed by this application. This is useful after an error occurs so the
// user can discern what issue caused the error.
//

private void logFileRequirements()
{

    tsLog.appendLine("--------------------------------------------------------");
    tsLog.appendLine("WAV File Format Requirements");

    tsLog.appendLine("");
    tsLog.appendLine("This application can view WAV files with the following");
    tsLog.appendLine("specifications:");
    tsLog.appendLine("");
    tsLog.appendLine("Format Chunk must immediately follow the RIFF Type Chunk.");
    tsLog.appendLine("Format Chunk must not have any Extra Format Bytes");
    tsLog.appendLine("The DATA Chunk must immediately follow the Format Chunk.");
    tsLog.appendLine("All other Chunk types will be ignored.");
    tsLog.appendLine("Compression types allowed:");
    tsLog.appendLine("    1 (0x0001) 	PCM/uncompressed");
    tsLog.appendLine("Number of channels allowed: 1 or 2");
    tsLog.appendLine("Valid samples per second values: any");
    tsLog.appendLine("Valid block align values: any");
    tsLog.appendLine("Valid bits per sample values: 8 or 16");
    tsLog.appendLine("Maximum number of channels: 65535");
    tsLog.appendLine("Maximum number of samples per channel: 2147483647");

    tsLog.appendLine("");
    tsLog.appendLine("------------------------------------------------------");
    tsLog.appendLine("");

}//end of WAVFile::logFileRequirements
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::checkForSequencingError
//
// Checks to see if the chunk currently being processed has been preceded
// by all necessary chunks.
//
// The RIFF Type chunk must have been read before any other chunk.
// The Format chunk must have been read before the Data chunk.
//

private void checkForChunkSequencingOrder(int pCurrentChunk) throws IOException
{

    //make sure that the RIFF Type chunk has been read if any other chunk is
    //currently being handled

    if (pCurrentChunk != RIFF_TYPE_CHUNK && !riffTypeChunkHandled) {
        throw new IOException(
            "The RIFF Type Chunk must be located before any other chunk type.");
    }

    if (pCurrentChunk == DATA_CHUNK && !formatChunkHandled) {
        throw new IOException(
                    "The Format Chunk must be located before the Data Chunk.");
    }

}//end of WAVFile::checkForChunkSequencingOrder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::setAudioParameters
//
// Sets various audio parameters used in the creation of signals.
//
// pSampleRate is the sampling frequency to be used for the data points.
// pValueRange is the type of value to be used for the data points:
//   SIGNED_WORD or UNSIGNED_BYTE
//

public void setAudioParameters(int pSampleRate, int pValueRange,
                                                        int pCompressionCode)
{

    formatChunk.sampleRate = pSampleRate;

    formatChunk.valueRange = pValueRange;

    if (pValueRange == SIGNED_WORD) {
        formatChunk.significantBitsPerSample = 16;
    }
    else if (pValueRange == UNSIGNED_BYTE) {
        formatChunk.significantBitsPerSample = 8;
    }

    //set the RIFF chunk ID and the file type
    setChunkIDFromASCII("RIFF", riffTypeChunk);
    setTypeFromASCII("WAVE", riffTypeChunk);

    //set the Format chunk ID
    setChunkIDFromASCII("fmt ", formatChunk);

    formatChunk.compressionCode = pCompressionCode;

    //currently the program always uses 2 channels for audio creation
    formatChunk.numberOfChannels = 2;

    //NOTE: the following for blockAlign and averageBytesPerSecond is only
    // valid for compression code "0x0001 PCM/uncompressed"

    //number of bytes for one sample from all channels -- this is the amount
    //to make a complete sample set for recreating sound for all channels at
    //a single instant of time

    formatChunk.blockAlign = (
       formatChunk.numberOfChannels * formatChunk.significantBitsPerSample) / 8;

    //data transfer rate required to play the sound
    formatChunk.averageBytesPerSecond =
                            formatChunk.sampleRate * formatChunk.blockAlign;

    //set the Data chunk ID
    setChunkIDFromASCII("data", dataChunk);

}//end of WAVFile::setAudioParameters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::getSampleRate
//
// Gets the current Sample Rate.
//

public long getSampleRate()
{

    return(formatChunk.sampleRate);

}//end of WAVFile::getSampleRate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::getFrequency
//
// Gets the current Frequency for channel specified by pWhichChannel (LEFT or
// RIGHT).
//

public double getFrequency(int pWhichChannel)
{

    if (pWhichChannel == LEFT) {return(leftChannel.frequency);}
    if (pWhichChannel == RIGHT) {return(rightChannel.frequency);}

    return(0);

}//end of WAVFile::getFrequency
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::setChunkIDFromASCII
//
// Sets the chunkID and chunkIDText of pChunk to pIDText. The string pIDText is
// converted to ASCII values before storing in chunkID.
//

public void setChunkIDFromASCII(String pIDText, Chunk pChunk)
{

    pChunk.chunkIDText = pIDText;
    pChunk.chunkID = convertASCIIToInt(pIDText);

}//end of WAVFile::setChunkIDFromASCII
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::setTypeFromASCII
//
// Sets the RIFF or LIST type and typeText of pChunk to pTypeText. The string
// is converted to ASCII values before storing in the type variable.
//

public void setTypeFromASCII(String pTypeText, RIFFLISTTypeChunk pChunk)
{

    pChunk.typeText = pTypeText;
    pChunk.type = convertASCIIToInt(pTypeText);

}//end of WAVFile::setTypeFromASCII
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::convertASCIIToInt
//
// Converts first four characters in pString to ASCII values in a long variable
// and returns it.
//

public long convertASCIIToInt(String pText)
{

    return(
            (long)(
                (((byte)pText.charAt(0)<<24) & 0xff000000) +
                (((byte)pText.charAt(1)<<16) & 0xff0000) +
                (((byte)pText.charAt(2)<<8) & 0xff00) +
                ((byte)pText.charAt(3) & 0xff))
           );

}//end of WAVFile::convertASCIIToInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::setTotalSampleSize
//
// Sets the totalSampleSize variable to pSize. This should be multiplied
// by the number of channels and then by the number of bytes in each sample
// to get the total byte size.
//

public void setTotalSampleSize(int pSize)
{

    totalSampleSize = pSize;

}//end of WAVFile::setTotalSampleSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::getTotalSampleSize
//
// Gets the current size of the sample set.
//
// This should be multiplied by the number of channels and then by the number
// of bytes in each sample to get the total byte size.
//

public long getTotalSampleSize()
{

    return(totalSampleSize);

}//end of WAVFile::getTotalSampleSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::initChannels
//
// Creates two Channel objects to hold the samples and parameters for an audio
// segment created by the application rather than being loaded from a WAV file.
//
// Sets class member totalSampleSize to pTotalSampleSize.
//
// Returns true if no error, returns false on error.
//

public boolean initChannels(int pTotalSampleSize)
{

    //store the value in class member
    totalSampleSize = pTotalSampleSize;

    //create the Channel objects
    leftChannel = new Channel();
    rightChannel = new Channel();

    return(true);

}//end of WAVFile::initChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::createAudioSection
//
// Creates a section of audio for the left and right channels using the
// supplied parameters.
//
// If pDisplaySamples is true, the audio samples will be listed in the log
// window.
//
// The first section will begin at time 0, each successive call will be
// appended to the preceding section.
//
// The audio sections will aways begin at amplitude 0 and end at amplitude 0.
// If the specified time length would result in ending on a partial wave cycle,
// the wave will be completed to amplitude 0 even if it runs slightly beyond the
// specified time.
//
// The parameter list is for the left and right audio channels with the time
// duration in seconds applying to both channels.
// The amplitude is relative to the valueRange:
//      for signed word, the valid range is 0 to 32,767
//      for unsigned byte, the valid range is 0 to 127
//
// The waveform frequency will ramp linearly from the starting frequency to
// the ending frequency over the specified time duration.
//
// pDisplaySamples now does nothing -- need to display samples as they are
// written to disk as they are no longer stored in an array.
//
//
// Returns true if no error, returns false on error.
//

public boolean createAudioSection(
    int pLeftStartingFrequency, int pLeftEndingFrequency, int pLeftAmplitude,
    int pRightStartingFrequency, int pRightEndingFrequency, int pRightAmplitude,
    int pTimeDuration, boolean pDisplaySamples)
{

    //use shorter names
    Channel leftCh = leftChannel, rightCh = rightChannel;

    leftCh.amplitude = pLeftAmplitude;
    rightCh.amplitude = pRightAmplitude;

    //calculate number of samples required for the section
    sectionSize = (int)getSampleRate() * pTimeDuration;

    //the frequency of the signal will start at the starting frequency and
    //linearly shift to the ending frequency over the section's time duration
    //calculate the amount the frequency will need to shift for each sample
    // note -- if this is a very large number, it may distort the signal

    leftCh.freqStep = (double)(pLeftEndingFrequency - pLeftStartingFrequency)
                                                 / (double)sectionSize;

    rightCh.freqStep = (double)(pRightEndingFrequency - pRightStartingFrequency)
                                                 / (double)sectionSize;

    leftCh.frequency = pLeftStartingFrequency;
    rightCh.frequency = pRightStartingFrequency;

    //audio channels are created in separate loops because they may be at
    //slightly different index positions and may reach the end at different
    //times

    //remember where the index started for this section
    prevSampleIndex = sampleIndex;

    //create the audio samples for all channels

    try{
        processChannels();
    }
    catch(IOException e){
        logError("Error saving file: ", e.getMessage());

        if (out != null) {
            try{
                out.close();
            }
            catch(IOException e2){
                logError("Error closing file: ", e2.getMessage());
            }

            return(false);

        }//if (out...
    }

    return(true);

}//end of WAVFile::createAudioSection
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::processChannels
//
// Creates the audio signal for all channels based on the parameters stored in
// the Channel objects.
//

private void processChannels() throws IOException
{

    //process left audio channel until size of section reached or the total
    //calcuated sample size reached
    while (sampleIndex < (prevSampleIndex + sectionSize)
                                    && sampleIndex < totalSampleSize){

        if (formatChunk.valueRange == SIGNED_WORD){
            //write samples with channels interleaved, signed words
            writeShort(out, (long)calculateSample(leftChannel));
            writeShort(out, (long)calculateSample(rightChannel));
        }
        else
        if (formatChunk.valueRange == UNSIGNED_BYTE){
            //write samples with channels interleaved, unsigned bytes
            //xor with 0x80 to flip top bit -- easy way to convert between
            //signed and unsigned values
            out.write((byte)(calculateSample(leftChannel) ^ 0x80));
            out.write((byte)(calculateSample(leftChannel) ^ 0x80));
        }

        sampleIndex++;

        guiUpdater.addUpdate(progressLabel, null,
                   "Processing " + sampleIndex + " of " + getTotalSampleSize());

        //shift the frequencies toward the ending frequencies with each step
        leftChannel.frequency =
                        (double)leftChannel.frequency + leftChannel.freqStep;

        rightChannel.frequency =
                        (double)rightChannel.frequency + rightChannel.freqStep;

    }

}//end of WAVFile::processChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::calculateSample
//
// Calculates an audio data sample for pChannel and returns it
//

private int calculateSample(Channel pChannel)
{

    return(
            (int)Math.round(pChannel.amplitude *
                Math.sin((pChannel.frequency * twoPI * sampleIndex)
                                                            / getSampleRate()))
            );

}//end of WAVFile::calculateSample
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::finishWaveCycle
//
// Completes the wave cycle currently in progress in pChannel by extending it
// until its amplitude is as close to zero as possible, but below zero at the
// end of the wave cycle.
// By starting the next section at zero amplitude, the discontinuity between the
// sections is minimized because the below zero value from the previous section
// will smoothly connect to the first zero amplitude sample of the succeeding
// section.
//
// NOTE: Since the frequencies are in cycles per second and the time duration
// is always in whole seconds (no fractional portions), the cycles always end
// up complete. So this function doesn't actually finish out a wave as they
// all naturally finish out. It will however finish out a wave that misses
// ending slightly due to a rounding error -- rare as that may be.
//
// NOTE: No longer used now that the data points are saved directly to file.
//  Would need to buffer the samples in an array to make this work no so that
//  the cycles could be finished in the array before being saved.
//  Very complicated to do that.
//

private void finishWaveCycle(Channel pChannel)
{

    //stop if the calculated total sample size is reached
    while (sampleIndex < totalSampleSize){

        int nextSample = calculateSample(pChannel);

        //when the signal has crossed the zero line from below to above, the
        //end of the cycle has been reached, so exit without saving the last
        //sample which above or equal to zero

//        if (pChannel.samples[pChannel.sampleIndex - 1] < 0
//                                                        && nextSample >= 0)
            break;

        //save the sample since crossover not reached
//        pChannel.samples[pChannel.sampleIndex++] = nextSample;

    }

}//end of WAVFile::finishWaveCycle
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::startFileSave
//
// Opens a file, writes the RIFF Type chunk, Format chunk, and the header
// for the Data chunk in preparation to adding waveform data to complete the
// file.
//
// Returns true if no error, returns false on error.
//

public boolean startFileSave()
{

    try {

        out = new FileOutputStream(filename);

        //write the RIFF Type Chunk file as the first chunk
        writeRIFFTypeChunk(out);
        //write the Format Chunk before the Data Chunk
        writeFormatChunk(out);
        //write the Data chunk last
        writeDataChunk(out);

    }//try
    catch(IOException e){
        logError("Error saving file: ", e.getMessage());

        if (out != null) {
            try{
                out.close();
            }
            catch(IOException e2){
                logError("Error closing file: ", e2.getMessage());
            }

            return(false);

        }//if (out...
    }

    return(true);

}//end of WAVFile::startFileSave
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::endFileSave
//
// Closes the output file being used to save a WAV file.
//

public void endFileSave()
{

    if (out != null) {
        try{

            finishDataChunk(); //apply padding byte to Data chunk if required

            //write other chunks here, such as LIST INFO chunks

            out.close();
        }
        catch(IOException e){
            logError("Error closing file: ", e.getMessage());
        }
    }//if (out...

}//end of WAVFile::endFileSave
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::writeRIFFTypeChunk
//
// Writes the RIFF Type chunk to pOut.
//

private void writeRIFFTypeChunk(FileOutputStream pOut) throws IOException
{

    //declare a variable to calculate the chunk size -- does not include the
    //Chunk ID and Chunk Data Size bytes for the RIFF Type Chunk itself, but
    //does include the four bytes of the File Type, so init with value of 4
    //after all chunks sizes are added in, the final value should be the
    //file size in bytes minus 8 (the 8 being the Chunk ID and the Chunk Data
    //Size variables for the RIFF Type Chunk itself)

    int chunkSize = 4;

    //add in sizes of all the chunks which will be written

    chunkSize += calculateFormatChunkSize();
    chunkSize += calculateDataChunkSize();

    riffTypeChunk.chunkDataSize = chunkSize;

    //write Chunk ID as ASCII codes
    write4Bytes(pOut, riffTypeChunk.chunkID);
    //write Chunk Data Size
    writeInt(pOut, riffTypeChunk.chunkDataSize);
    //write File type as ASCII codes
    write4Bytes(pOut, riffTypeChunk.type);

}//end of WAVFile::writeRIFFTypeChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::writeFormatChunk
//
// Writes the Format chunk to pOut.
//

private void writeFormatChunk(FileOutputStream pOut) throws IOException
{


    //get the chunk's data size
    int dataSize = calculateFormatChunkDataSize();
    //do not include any padding byte in the size even if it is added later
    formatChunk.chunkDataSize = dataSize;

    //write Chunk ID as ASCII codes
    write4Bytes(pOut, formatChunk.chunkID);
    //write Chunk Data Size
    writeInt(pOut, formatChunk.chunkDataSize);
    //write the Compression code
    writeShort(pOut, formatChunk.compressionCode);
    //write the number of channels
    writeShort(pOut, formatChunk.numberOfChannels);
    //write the Sample Rate
    writeInt(pOut, formatChunk.sampleRate);
    //write the Average Bytes per Second
    writeInt(pOut, formatChunk.averageBytesPerSecond);
    //write the Block Align
    writeShort(pOut, formatChunk.blockAlign);
    //write the Significant Bits per Sample
    writeShort(pOut, formatChunk.significantBitsPerSample);

    if (formatChunk.extraFormatBytes != 0){
        //add code here to write format bytes when other compression codes
        //are supported

        //need to write the extra format bytes and padding byte if necessary
    }

}//end of WAVFile::writeFormatChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::writeDataChunk
//
// Writes the Data chunk to pOut.
//

private void writeDataChunk(FileOutputStream pOut) throws IOException
{

    //get the chunk's data size
    int dataSize = calculateDataChunkDataSize();
    //do not include any padding byte in the size even if it is added later
    dataChunk.chunkDataSize = dataSize;

    //write Chunk ID as ASCII codes
    write4Bytes(pOut, dataChunk.chunkID);
    //write Chunk Data Size
    writeInt(pOut, dataChunk.chunkDataSize);

}//end of WAVFile::writeDataChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::finishDataChunk
//
// Completes the writing of the data chunk by appending a padding byte if
// the data size was odd.
//

public void finishDataChunk() throws IOException
{

    if (dataChunk.chunkDataSize % 2 != 0) {out.write(0);}

}//end of WAVFile::finishDataChunk
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::write4Bytes
//
// Writes 4 bytes of pValue to pOut in Big-Endian order. This is used mainly
// to write ASCII codes.
//

private void write4Bytes(FileOutputStream pOut, long pValue) throws IOException
{

    pOut.write((byte)((pValue>>24) & 0xff));
    pOut.write((byte)((pValue>>16) & 0xff));
    pOut.write((byte)((pValue>>8) & 0xff));
    pOut.write((byte)(pValue & 0xff));

}//end of WAVFile::write4Bytes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::writeInt
//
// Writes 4 bytes of pValue to pOut in Little-Endian order.
//

private void writeInt(FileOutputStream pOut, long pValue) throws IOException
{

    pOut.write((byte)(pValue & 0xff));
    pOut.write((byte)((pValue>>8) & 0xff));
    pOut.write((byte)((pValue>>16) & 0xff));
    pOut.write((byte)((pValue>>24) & 0xff));

}//end of WAVFile::writeInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::writeShort
//
// Writes 2 bytes of pValue to pOut in Little-Endian order.
//

private void writeShort(FileOutputStream pOut, long pValue) throws IOException
{

    pOut.write((byte)(pValue & 0xff));
    pOut.write((byte)((pValue>>8) & 0xff));

}//end of WAVFile::writeShort
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::calculateFormatChunkSize
//
// Returns the size in bytes of the Format chunk.
//

private int calculateFormatChunkSize()
{

    int size = calculateFormatChunkDataSize();

    //if the data size ends up odd, an extra padding byte will be added later
    //to make the data word-aligned, so account for that here as the chunk's
    //total size is used by the RIFF chunk and includes the byte

    if (size % 2 != 0){size++;}

    //add in the Chunk ID and the Chunk Data Size variables
    size += 8;

    return(size);

}//end of WAVFile::calculateFormatChunkSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::calculateFormatChunkDataSize
//
// Returns the size in bytes of the Format chunk's data.
//

private int calculateFormatChunkDataSize()
{

    //currently, the Format chunk's data size is always 16 (this excludes the
    //8 bytes for the Chunk ID and the Chunk Data Size variables)
    //the Extra Format Bytes value is also not included as it is not saved
    //for PCM/uncompressed
    //the size is constant because the application only handles the
    //PCM/uncompressed format which does not use any extra format bytes

    return(16);

}//end of WAVFile::calculateFormatChunkDataSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::calculateDataChunkSize
//
// Returns the size in bytes of the Data chunk
//

private int calculateDataChunkSize()
{

    //calculate the size of all the sample bytes for all channels
    int size = calculateDataChunkDataSize();

    //if the data size ends up odd, an extra padding byte will be added later
    //to make the data word-aligned, so account for that here as the chunk's
    //total size is used by the RIFF chunk and includes the byte

    if (size % 2 != 0) {size++;}

    //add in the Chunk ID and the Chunk Data Size variables
    size += 8;

    return(size);

}//end of WAVFile::calculateDataChunkSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::calculateDataChunkDataSize
//
// Returns the size in bytes of the Data chunk's data. Does NOT include a
// padding byte if it needs to be added due to an odd numbered size to make
// the chunk word aligned.
//

private int calculateDataChunkDataSize()
{

    //this app always creates audio with 2 channels
    int numberOfChannels = 2;

    int sampleByteSize = 0;

    if (formatChunk.significantBitsPerSample == 16) {sampleByteSize = 2;}
    if (formatChunk.significantBitsPerSample == 8) {sampleByteSize = 1;}

    //calculate the size of all the sample bytes for all channels
    int size = numberOfChannels * sampleByteSize * totalSampleSize;

    return(size);

}//end of WAVFile::calculateDataChunkDataSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WAVFile::logError
//
// Displays error message in the log window and adds it to the error log file.
//
// Appends error message pErrorMessage along with the filename and then appends
// pErrorDetails on the next line.
//

private void logError(String pErrorMessage, String pErrorDetails)
{

    tsLog.appendLine(pErrorMessage + filename);
    tsLog.appendToErrorLogFile(pErrorMessage + filename);
    tsLog.appendLine(pErrorDetails);
    tsLog.appendToErrorLogFile(pErrorDetails);
    tsLog.appendLine("");
    tsLog.appendToErrorLogFile("");

}//end of WAVFile::logError
//-----------------------------------------------------------------------------

}//end of class WAVFile
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
