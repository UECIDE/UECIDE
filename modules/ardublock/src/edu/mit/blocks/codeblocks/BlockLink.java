package edu.mit.blocks.codeblocks;

import edu.mit.blocks.codeblockutil.Sound;
import edu.mit.blocks.codeblockutil.SoundManager;
import edu.mit.blocks.renderable.RenderableBlock;
import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;

/**
 * A class that stores information about a potential block connection.
 * 
 * Each BlockLink instance stores the Block IDs of the "plug" and "socket"
 * and two block connectors, one from each, with a possible connection.  
 * 
 * In block linking, a "plug" can be either a before or plug connector, 
 * while a "socket" can either be a after or socket connector.  Plugs can only 
 * connect to other socket connectors, while before connectors can connect to 
 * after and command socket connectors.  
 */
public class BlockLink {

    private final Workspace workspace;
    
    private static Sound clickSound;
    private Long plugBlockID;
    private Long socketBlockID;
    private Long lastPlugBlockID;
    private BlockConnector plug;
    private BlockConnector socket;
    //information regarding the last BlockLink instance
    private static Long lastPlugID;
    private static Long lastSocketID;
    private static BlockConnector lastPlug;
    private static BlockConnector lastSocket;
    private static BlockLink lastLink;

    /**
     * Private constructor to (somewhat) limit object creation
     * @param workspace The corresponding workspace
     * @param block1
     * @param block2
     * @param socket1
     * @param socket2
     */
    private BlockLink(Workspace workspace, Block block1, Block block2, BlockConnector socket1, BlockConnector socket2) {
        this.workspace = workspace;
        boolean isPlug1 = (block1.hasPlug() && block1.getPlug() == socket1)
                || (block1.hasBeforeConnector() && block1.getBeforeConnector() == socket1);
        boolean isPlug2 = (block2.hasPlug() && block2.getPlug() == socket2)
                || (block2.hasBeforeConnector() && block2.getBeforeConnector() == socket2);
        if (!(isPlug1 ^ isPlug2)) {
            // bad news... there should be only one plug
            assert (false);
        } else if (isPlug1) {
            plug = socket1;
            socket = socket2;
            plugBlockID = block1.getBlockID();
            socketBlockID = block2.getBlockID();
        } else {
            plug = socket2;
            socket = socket1;
            plugBlockID = block2.getBlockID();
            socketBlockID = block1.getBlockID();
        }
        lastPlugID = plugBlockID;
        lastSocketID = socketBlockID;
        lastPlug = plug;
        lastSocket = socket;
        lastPlugBlockID = Block.NULL;
    }

    static {
        //load the sound for connecting blocks, but only once
        try {
            clickSound = SoundManager.loadSound("/edu/mit/blocks/codeblocks/click.wav");
        } catch (Exception e) {
            System.out.println("Error initializing sounds.  Continuing...");
        }
    }

    /**
     *
     * @return the BlockConnector representing the plug side of the link
     */
    public BlockConnector getPlug() {
        return plug;
    }

    /**
     *
     * @return the BlockConnector representing the socket side of the link
     */
    public BlockConnector getSocket() {
        return socket;
    }

    /**
     *
     * @return the Block ID of the Block containing the plug side of the link
     */
    public Long getPlugBlockID() {
        return plugBlockID;
    }

    /**
     *
     * @return the Block ID of the Block containing the socket side of the link
     */
    public Long getSocketBlockID() {
        return socketBlockID;
    }

    public Long getLastBlockID() {
        return lastPlugBlockID;
    }

    /**
     * This method actually connects the two blocks stored in this BlockLink object.
     *
     */
    public void connect() {

        /* Make sure to disconnect any connections that are going to be overwritten
         * by this new connection.  For example, if inserting a block between two
         * others, make sure to break that original link.*/
        if (socket.hasBlock()) {
            // save the ID of the block previously attached to (in) this
            // socket.  This is used by insertion rules to re-link the replaced
            // block to the newly-inserted block.
            lastPlugBlockID = socket.getBlockID();

            // break the link between the socket block and the block in that socket
            Block plugBlock = workspace.getEnv().getBlock(lastPlugBlockID);
            BlockConnector plugBlockPlug = BlockLinkChecker.getPlugEquivalent(plugBlock);
            if (plugBlockPlug != null && plugBlockPlug.hasBlock()) {
                Block socketBlock = workspace.getEnv().getBlock(plugBlockPlug.getBlockID());
                BlockLink link = BlockLink.getBlockLink(workspace, plugBlock, socketBlock, plugBlockPlug, socket);
                link.disconnect();
                //don't tell the block about the disconnect like we would normally do, because
                // we don't actually want it to have a chance to remove any expandable sockets
                // since the inserted block will be filling whatever socket was vacated by this
                // broken link.
                //NOTIFY WORKSPACE LISTENERS OF DISCONNECTION (not sure if this is great because the connection is immediately replaced)
                workspace.notifyListeners(new WorkspaceEvent(workspace, workspace.getEnv().getRenderableBlock(socketBlock.getBlockID()).getParentWidget(), link, WorkspaceEvent.BLOCKS_DISCONNECTED));
            }
        }
        if (plug.hasBlock()) {
            // in the case of insertion, breaking the link above will mean that
            // the plug shouldn't be connected by the time we reach here.  This
            // exception will only be thrown if the plug is connected even
            // after any insertion-esq links were broken above
            throw new RuntimeException("trying to link a plug that's already connected somewhere.");
        }

        // actually form the connection
        plug.setConnectorBlockID(socketBlockID);
        socket.setConnectorBlockID(plugBlockID);

        //notify renderable block of connection so it can redraw with stretching
        RenderableBlock socketRB = workspace.getEnv().getRenderableBlock(socketBlockID);
        socketRB.blockConnected(socket, plugBlockID);

        if (clickSound != null) {
            //System.out.println("playing click sound");
            clickSound.play();
        }
    }

    public void disconnect() {
        plug.setConnectorBlockID(Block.NULL);
        socket.setConnectorBlockID(Block.NULL);
    }

    /**
     * Factory method for creating BlockLink objects
     * @param workspace The current workspace
     * @param block1 one of the Block objects in the potential link
     * @param block2 the other Block object
     * @param socket1 the BlockConnector from block1
     * @param socket2 the BlockConnector from block2
     * @return a BlockLink object storing the potential link between block1 and block2
     */
    public static BlockLink getBlockLink(Workspace workspace, Block block1, Block block2, BlockConnector socket1, BlockConnector socket2) {
        // If these arguments are the same as the last call to getBlockLink, return the old object instead of creating a new one
        if (!((block1.getBlockID().equals(lastPlugID) && block2.getBlockID().equals(lastSocketID)
                && socket1.equals(lastPlug) && socket2.equals(lastSocket))
                || (block2.getBlockID().equals(lastPlugID) && block1.getBlockID().equals(lastSocketID)
                && socket2.equals(lastPlug) && socket1.equals(lastSocket)))) {
            lastLink = new BlockLink(workspace, block1, block2, socket1, socket2);
        }
        return lastLink;
    }

    public String toString() {
        return "BlockLink(Plug: " + plugBlockID + ", Socket: " + socketBlockID + ")";
    }
}
