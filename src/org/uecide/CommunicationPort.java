package org.uecide;

// A generic "Communication Port" interface which defines how
// to communicate in various ways over different channels, such
// as Serial Ports, Network Connections, etc.

public interface CommunicationPort {

    // This is the "pretty" name of the channel. It can include both the name of the port
    // and what the port represents, etc.  Used for menus.
    public String getName();

    // A textual representation of the address/port combination.  Used for status and display.
    public String toString();

    // These two define what is needed to connect to the console of the board.  If it is remote
    // then the getConsoleAddress() should return the IP address or hostname.  If it's local then
    // getConsoleAddress() should return null.  The getConsolePort() would either be the remote
    // port number or name, or the local port device name.
    public String getConsoleAddress();
    public String getConsolePort();

    // Similar to the above functions, but these return the details needed to connect to the
    // board for programming.  This may be the same as above, or may be somewhat different
    // for remote boards.
    public String getProgrammingAddress();
    public String getProgrammingPort();

    // If the board at the other end of this communication port can be positively identified
    // then this function should return that board - otherwise it returns null.
    public Board getBoard();

    // Attempt to open the console channel.  Returns true on success, false on error.
    public boolean openPort();

    // Close the console port if it's open. If it's not open then do nothing.
    public void closePort();

    // Standard functions for sending data to an open console port.
    public boolean print(String data);
    public boolean println(String data);
    public boolean write(byte[] data);
    public boolean write(byte data);

    // Reception is done via a standard communication interface class.
    // You need to make calls to the "commsDataReceived(byte[])" function
    // of the provided CommsListener object.
    // Other kinds of events can be sent through the commsEventReceived(CommsEvent)
    // function of tge CommsListener class.
    public void addCommsListener(CommsListener listener);
    public void removeCommsListener();

    // Get the last error message from any operation that failed.
    public String getLastError();

    // On interfaces that support speed changes this will set that speed.
    public boolean setSpeed(int speed);

    // This should return an array of CommsSpeed objects to provide the
    // speeds supported by this channel.
    public CommsSpeed[] getSpeeds();

    // This function will "pulse" the line (when supported).  This could
    // do many things, such as cycle the DTR and RTS lines to invoke a reset, etc.
    public void pulseLine();

    // A function to get the "base" name of a port.
    public String getBaseName();
}
