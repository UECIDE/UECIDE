package edu.mit.blocks.codeblocks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.mit.blocks.codeblocks.BlockConnector.PositionType;

import edu.mit.blocks.renderable.RenderableBlock;
import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;

/**
 * <code>BlockStub</code> are a special form of blocks that provide a particular
 * reference to its "parent" block.  These references can set, get, or increment
 * the value of its "parent" block.  References may also get the value for a 
 * particular agent.  Finally, for a procedure block, its reference is a call
 * block, which executes the procedure.  
 * 
 * The parent instance for a set of stubs is not permanent.  The parent intance
 * may change if the original parent it removed and then a new one with the 
 * same parent name is added to the block canvas. BlockStub manages the mapping 
 * between stubs and their parent.
 */
public class BlockStub extends Block {

    /**
     * Temporary mapping for parent type (caller plugs). 
     * TODO remove once BlockUtilities cloneBlock() is finished
     */
    private static Map<String, String> parentToPlugType = new HashMap<String, String>();

    //stub type string constants
    private static final String GETTER_STUB = "getter";
    private static final String SETTER_STUB = "setter";
    private static final String CALLER_STUB = "caller";
    private static final String AGENT_STUB = "agent";

    //this particular stub type is unique to Starlogo TNG - may choose to remove it
    private static final String INC_STUB = "inc";
    private String parentName;
    private final String parentGenus;
    private final String stubGenus;

    /**
     * mySocketToParentSocket maps the sockets of this stubs to the sockets of its parent
     * this mapping is used to help in the maintanence this stub's sockets with respect to its parent
     */
    //private HashMap<BlockConnector,BlockConnector> mySocketToParentSocket = new HashMap<BlockConnector, BlockConnector>();
    /**
     * Constructs a new <code>BlockStub</code> instance using the specified
     * genus name of its parent block, the block id of its parent, the block name of parent
     * and its stub genus.  The exact reference to the parent through the specified initParentID 
     * is needed, in addition to the other specified parameters, to completely construct a new block
     * stub.
     * @param initParentID the Long block ID of its initial parent
     * @param parentGenus the BlockGenus String name of its initial parent
     * @param parentName 
     * @param stubGenus
     */
    public BlockStub(Workspace workspace, Long initParentID, String parentGenus, String parentName, String stubGenus) {
        super(workspace, stubGenus);

        assert initParentID != Block.NULL : "Parent id of stub should not be null";

        this.parentGenus = parentGenus;
        this.parentName = parentName;
        this.stubGenus = stubGenus;

        //initial parent of this
        Block parent = workspace.getEnv().getBlock(initParentID);
        //has parent block label
        this.setBlockLabel(parent.getBlockLabel());
        //initialize stub properties based on stubGenus such as sockets, plugs, and labels
        //this initialization assumes that nothing is connected to the parent yet
        //note: instead of modifying the stub blocks currect sockets, we replace them with whole new ones
        //such that the initkind of the stub blocks connectors are the same as their parents
        if (stubGenus.startsWith(GETTER_STUB)) {
            //set plug to be the single socket of parent or plug if parent has no sockets
            if (parent.getNumSockets() > 0) {
                this.setPlug(parent.getSocketAt(0).getKind(), this.getPlug().getPositionType(), this.getPlugLabel(), this.getPlug().isLabelEditable(), Block.NULL);
            } else {
                this.setPlug(parent.getPlugKind(), this.getPlug().getPositionType(), this.getPlugLabel(), this.getPlug().isLabelEditable(), Block.NULL);
            }

        } else if (stubGenus.startsWith(SETTER_STUB)) {
            BlockConnector mySoc = this.getSocketAt(0);
            //set socket type to be parent socket type or plug if parent has no sockets
            if (parent.getNumSockets() > 0) {
                this.setSocketAt(0, parent.getSocketAt(0).getKind(), mySoc.getPositionType(),
                        mySoc.getLabel(), mySoc.isLabelEditable(), mySoc.isExpandable(), mySoc.getBlockID());
            } else {
                this.setSocketAt(0, parent.getPlugKind(), mySoc.getPositionType(),
                        mySoc.getLabel(), mySoc.isLabelEditable(), mySoc.isExpandable(), mySoc.getBlockID());
            }
        } else if (stubGenus.startsWith(CALLER_STUB)) {
            //if parent has socket block connected to its first socket or has multiple sockets (parent may have blocks connected to its other sockets)
            if (parent.getSocketAt(0).getBlockID() != Block.NULL || parent.getNumSockets() > 1) {
                //retrieve sockets from parent and set sockets accordingly
                Iterator<BlockConnector> sockets = parent.getSockets().iterator();
                for (int i = 0; sockets.hasNext(); i++) {
                    BlockConnector socket = sockets.next();
                    //socket labels should correspond with the socket blocks of parent
                    if (socket.getBlockID() != Block.NULL) {
                        addSocket(socket.getKind(), BlockConnector.PositionType.SINGLE, workspace.getEnv().getBlock(socket.getBlockID()).getBlockLabel(), false, false, Block.NULL);
                    }
                }
            }

            //TODO: remove the following once BlockUtilities.cloneBlock() is finished
            // If our parent already has a plug type, we want to update 
            // Note that we don't need to call renderables, since we are still
            // in the constructor
            String kind = parentToPlugType.get(parent.getBlockLabel() + parent.getGenusName());
            if (kind != null) {
                removeBeforeAndAfter();
                //TODO ria commented code relates to creating mirror plugs for caller stubs that have no sockets
                //if(this.getNumSockets() == 0){
                //	setPlug(kind, PositionType.MIRROR, "", false, Block.NULL);
                //} else {
                setPlug(kind, PositionType.SINGLE, "", false, Block.NULL);
                //}
            }

        } else if (stubGenus.startsWith(AGENT_STUB)) {
            //getter for specific who
            //set plug to be parent single socket kind or plug kind if parent has no sockets
            if (parent.getNumSockets() > 0) {
                setPlug(parent.getSocketAt(0).getKind(), this.getPlug().getPositionType(), this.getPlugLabel(), this.getPlug().isLabelEditable(), this.getPlugBlockID());
            } else {
                setPlug(parent.getPlugKind(), this.getPlug().getPositionType(), this.getPlugLabel(), this.getPlug().isLabelEditable(), this.getPlugBlockID());
            }

        } else if (stubGenus.startsWith(INC_STUB)) {
            //only included for number variables
            //do nothing for now
        }

        //has  page label of parent if parent has page label
        this.setPageLabel(parent.getPageLabel());
        //add new stub to hashmaps
        //parent should have existed in hashmap before this stub was created
        //(look at main Block constructor)
        //thus no problem should occur with following line
        workspace.getEnv().getBlockStubs(parentName + parentGenus).add(this.getBlockID());

    }

    /**
     * Constructs a new BlockStub instance.  This contructor is protected as it should only be called 
     * while Block loads its information from the save String
     * @param workspace The workspace this stub should be created in
     * @param blockID the Long block ID of this
     * @param stubGenus the BlockGenus of this
     * @param label the Block label of this
     * @param parentName the String name of its parent
     * @param parentGenus the String BlockGenus name of its parent
     */
    protected BlockStub(Workspace workspace, Long blockID, String stubGenus, String label, String parentName, String parentGenus) {
        super(workspace, blockID, stubGenus, label, true);   //stubs may have stubs...
        //unlike the above constructor, the blockID specified should already
        //be referencing a fully loaded block with all necessary information
        //such as sockets, plugs, labels, etc.
        //the only information we need to handle is the stub information here.
        this.stubGenus = stubGenus;
        this.parentName = parentName;
        this.parentGenus = parentGenus;

        //there's a chance that the parent for this has not been added to parentNameToBlockStubs mapping
        String key = parentName + parentGenus;
        if (workspace.getEnv().containsBlockStubs(key)) {
            workspace.getEnv().getBlockStubs(parentName + parentGenus).add(this.getBlockID());
        } else {
            ArrayList<Long> stubs = new ArrayList<Long>();
            stubs.add(this.getBlockID());
            workspace.getEnv().putBlockStubs(key, stubs);
        }
    }

    /**
     * Returns a list of the block ids of the specified parent's stubs
     * @param blockID
     */
    public static Iterable<Long> getStubsOfParent(Workspace workspace, Block block) {
        ArrayList<Long> stubs = workspace.getEnv().getBlockStubs(block.getBlockLabel() + block.getGenusName());
        if (stubs != null) {
            return stubs;
        } else {
            return new ArrayList<Long>();
        }
    }

    /**
     * Saves the parent block information with the specified blockID in the Stub Map
     * @param blockID
     */
    public static void putNewParentInStubMap(Workspace workspace, Long blockID) {
        String key = workspace.getEnv().getBlock(blockID).getBlockLabel() + workspace.getEnv().getBlock(blockID).getGenusName();
        workspace.getEnv().putParentBlock(key, blockID);

        if (workspace.getEnv().getBlockStubs(key) == null) {
            workspace.getEnv().putBlockStubs(key, new ArrayList<Long>());
        }

        //notify dangling stubs and update their renderables
        //dangling stubs will be waiting to have a parent assigned to them
        //and reflect that graphically
        for (Long stubID : workspace.getEnv().getBlockStubs(key)) {
            BlockStub stub = (BlockStub) workspace.getEnv().getBlock(stubID);
            stub.notifyRenderable();
        }

    }

    /**
     * Updates BlockStub hashmaps and the BlockStubs of the parent of its new name
     * @param oldParentName
     * @param newParentName
     * @param parentID
     */
    public static void parentNameChanged(Workspace workspace, String oldParentName, String newParentName, Long parentID) {
        String oldKey = oldParentName + workspace.getEnv().getBlock(parentID).getGenusName();
        String newKey = newParentName + workspace.getEnv().getBlock(parentID).getGenusName();

        //only update if parents name really did "change" meaning the new parent name is
        //different from the old parent name
        if (!oldKey.equals(newKey)) {
            workspace.getEnv().putParentBlock(newKey, parentID);

            //update the parent name of each stub
            ArrayList<Long> stubs = workspace.getEnv().getBlockStubs(oldKey);
            for (Long stub : stubs) {
                BlockStub blockStub = ((BlockStub) workspace.getEnv().getBlock(stub));
                blockStub.parentName = newParentName;
                //update block label of each
                blockStub.setBlockLabel(newParentName);
                blockStub.notifyRenderable();
            }

            //check if any stubs already exist for new key
            ArrayList<Long> existingStubs = workspace.getEnv().getBlockStubs(newKey);
            if (existingStubs != null) {
                stubs.addAll(existingStubs);
            }

            workspace.getEnv().putBlockStubs(newKey, stubs);

            //remove old parent name from hash maps
            workspace.getEnv().removeParentBlock(oldKey);
            workspace.getEnv().removeBlockStubs(oldKey);
        }
    }

    /**
     * Updates the BlockStubs associated with the parent of its new page label
     * @param newPageLabel
     * @param parentID
     */
    public static void parentPageLabelChanged(Workspace workspace, String newPageLabel, Long parentID) {
        String key = workspace.getEnv().getBlock(parentID).getBlockLabel() + workspace.getEnv().getBlock(parentID).getGenusName();

        //update each stub
        ArrayList<Long> stubs = workspace.getEnv().getBlockStubs(key);
        for (Long stub : stubs) {
            BlockStub blockStub = ((BlockStub) workspace.getEnv().getBlock(stub));
            blockStub.setPageLabel(newPageLabel);
            blockStub.notifyRenderable();
        }

    }

    /**
     * Updates the BlocksStubs associated with the parent of its new page label
     * @param parentID
     */
    public static void parentConnectorsChanged(Workspace workspace, Long parentID) {
        String key = workspace.getEnv().getBlock(parentID).getBlockLabel() + workspace.getEnv().getBlock(parentID).getGenusName();

        //update each stub only if stub is a caller (as callers are the only type of stubs that 
        //can change its connectors after being created)
        ArrayList<Long> stubs = workspace.getEnv().getBlockStubs(key);
        for (Long stub : stubs) {
            BlockStub blockStub = ((BlockStub) workspace.getEnv().getBlock(stub));
            if (blockStub.stubGenus.startsWith(CALLER_STUB)) {
                blockStub.updateConnectors();
                //System.out.println("updated connectors of: "+blockStub);
                blockStub.notifyRenderable();
            }
        }
    }

    /**
     * Updates the plug on caller stubs associated with the given parent.
     * @param kind the new plug kind that callers should set
     */
    public static void parentPlugChanged(Workspace workspace, Long parentID, String kind) {
        String key = workspace.getEnv().getBlock(parentID).getBlockLabel() + workspace.getEnv().getBlock(parentID).getGenusName();

        // Update our type mapping.
        if (kind == null) {
            parentToPlugType.remove(key);
        } else {
            parentToPlugType.put(key, kind);
        }

        // update each stub only if stub is a caller
        ArrayList<Long> stubs = workspace.getEnv().getBlockStubs(key);
        for (Long stub : stubs) {
            BlockStub blockStub = ((BlockStub) workspace.getEnv().getBlock(stub));
            if (blockStub.stubGenus.startsWith(CALLER_STUB)) {
                if (kind == null) {
                    blockStub.restoreInitConnectors();
                } else {
                    blockStub.updatePlug(kind);
                }
            }
        }
    }

    ////////////////////////////////////
    // PARENT INFORMATION AND METHODS //
    ////////////////////////////////////
    /**
     * Returns the parent name of this stub
     * @return the parent name of this stub
     */
    public String getParentName() {
        return parentName;
    }

    /**
     * Returns the parent block of this stub
     * @return the parent block of this stub
     */
    public Block getParent() {
        String key = parentName + parentGenus;
        if (!workspace.getEnv().containsParentBlock(key)) {
            return null;
        }
        return workspace.getEnv().getBlock(workspace.getEnv().getParentBlockID(key));
    }

    /**
     * Returns the parent block genus of this stub
     * @return the parent block genus of this stub
     */
    public String getParentGenus() {
        return parentGenus;
    }

    /**
     *
     */
    public boolean doesParentExist() {
        //TODO ria: needs to check BlockCanvas if parent is "alive"

        return true;
    }

    ///////////////////////////////////
    // METHODS OVERRIDDEN FROM BLOCK //
    ///////////////////////////////////
    /**
     * Overriden from Block.  Can not change the genus of a Stub.
     */
    public void changeGenusTo(String genusName) {
        //return null;
    }

    //////////////////////////////////////////////////
    //BLOCK STUB CONNECTION INFORMATION AND METHODS //
    //////////////////////////////////////////////////
    /**
     * Updates the conenctors of this stub according to its parent.
     * For now only caller stubs should update their connector information after 
     * being created.  
     */
    private void updateConnectors() {
        Block parent = getParent();
        if (parent != null) {
            //retrieve sockets from parent and set sockets accordingly
            Iterator<BlockConnector> parentSockets = parent.getSockets().iterator();
            int i; //socket index
            //clear all sockets TODO temporary solution
            for (BlockConnector socket : getSockets()) {
                removeSocket(socket);
            }
            //add parent sockets
            for (i = 0; parentSockets.hasNext(); i++) {
                BlockConnector parentSocket = parentSockets.next();
                if (parentSocket.getBlockID() != Block.NULL) {
                    //may need to add more sockets if parent has > 1 sockets
                    if (i > this.getNumSockets() - 1) {
                        //socket labels should correspond with the socket blocks of parent
                        if (parentSocket.getBlockID() != Block.NULL) {
                            addSocket(parentSocket.getKind(), BlockConnector.PositionType.SINGLE, workspace.getEnv().getBlock(parentSocket.getBlockID()).getBlockLabel(), false, false, Block.NULL);
                        }
                    } else {
                        BlockConnector con = getSocketAt(i);
                        this.setSocketAt(i, parentSocket.getKind(), con.getPositionType(), workspace.getEnv().getBlock(parentSocket.getBlockID()).getBlockLabel(), con.isLabelEditable(),
                                con.isExpandable(), con.getBlockID());
                    }
                }
            }
        }
    }

    /**
     * Restores the initial state of the before, after, and plug. Disconnects
     * any invalid blocks. Only caller stubs should use this method.
     */
    private void restoreInitConnectors() {
        if (!hasPlug()) {
            return;     // Already in original state
        }
        // We have to check for a plug connector.
        Long id = getPlugBlockID();
        if (id != null && !id.equals(Block.NULL)) {
            disconnectBlock(id);
        }

        // Always synchronize! We can't have both a plug and a before.
        removePlug();
        resetBeforeAndAfter();
        workspace.getEnv().getRenderableBlock(getBlockID()).updateConnectors();
        notifyRenderable();
    }

    /**
     * Updates the plug type. Disconnects any invalid blocks. Only caller
     * stubs should use this method.
     * @param kind must not be null
     */
    private void updatePlug(String kind) {
        if (hasPlug() && getPlugKind().equals(kind)) {
            return;
        }

        // We have to check for a before and after block.
        Long id = getBeforeBlockID();
        if (id != null && !id.equals(Block.NULL)) {
            disconnectBlock(id);
        }

        id = getAfterBlockID();
        if (id != null && !id.equals(Block.NULL)) {
            disconnectBlock(id);
        }

        // We also need to check the plug, because it may be connected to
        // the wrong type.
        id = getPlugBlockID();
        if (id != null && !id.equals(Block.NULL)) {
            disconnectBlock(id);
        }

        // Always synchronize! We can't have both a plug and a before.
        removeBeforeAndAfter();
        setPlug(kind, PositionType.SINGLE, kind, false, Block.NULL);
        workspace.getEnv().getRenderableBlock(getBlockID()).updateConnectors();
        notifyRenderable();
    }

    /**
     * Disconnect the given block from us. Must have a valid id.
     */
    private void disconnectBlock(Long id) {
        Block b2 = workspace.getEnv().getBlock(id);
        BlockConnector conn2 = b2.getConnectorTo(getBlockID());
        BlockConnector conn = getConnectorTo(id);
        BlockLink link = BlockLink.getBlockLink(workspace, this, b2, conn, conn2);
        RenderableBlock rb = workspace.getEnv().getRenderableBlock(link.getSocketBlockID());
        link.disconnect();
        rb.blockDisconnected(link.getSocket());
        workspace.notifyListeners(
                new WorkspaceEvent(workspace, rb.getParentWidget(), link, WorkspaceEvent.BLOCKS_DISCONNECTED));

    }

    ////////////////////////////////////////
    // METHODS FROM BLOCK GENUS           //
    ////////////////////////////////////////
    /**
     * Returns the Color of this; May return Color.Black if color was unspecified.
     * @return the Color of this; May return Color.Black if color was unspecified.
     */
    public Color getColor() {
        if (getParent() == null) {
            return super.getColor();
        }
        return getParent().getColor();
    }

    /**
     * @return current information about block
     */
    public String toString() {
        return "Block Stub +" + getBlockID() + ": " + getBlockLabel() + " with sockets: " + getSockets() + " and plug: " + getPlug();
    }

    @Override
    public boolean isCommandBlock() {
        return hasAfterConnector() && hasBeforeConnector();
    }

    @Override
    public boolean isDataBlock() {
        return !hasAfterConnector() && !hasBeforeConnector();
    }

    @Override
    public boolean isFunctionBlock() {
        return hasPlug() && (this.getNumSockets() > 0);
    }

    ////////////////////////
    // SAVING AND LOADING //
    ////////////////////////
    public Node getSaveNode(Document document, int x, int y, Node commentNode, boolean collapsed) {
    	Element stubElement = document.createElement("BlockStub");
    	
    	Element parentNameElement = document.createElement("StubParentName");
    	parentNameElement.appendChild(document.createTextNode(parentName));
    	stubElement.appendChild(parentNameElement);
    	
    	Element parentGenusElement = document.createElement("StubParentGenus");
    	parentGenusElement.appendChild(document.createTextNode(parentGenus));
    	stubElement.appendChild(parentGenusElement);
    	
    	Node blockNode = super.getSaveNode(document, x, y, commentNode, collapsed);
    	stubElement.appendChild(blockNode);
    	
    	return stubElement;
    }
    
}
