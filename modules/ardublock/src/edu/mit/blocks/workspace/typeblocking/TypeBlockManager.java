package edu.mit.blocks.workspace.typeblocking;

import java.awt.Container;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblocks.BlockConnector;
import edu.mit.blocks.codeblocks.BlockLink;
import edu.mit.blocks.codeblocks.BlockLinkChecker;
import edu.mit.blocks.renderable.BlockNode;
import edu.mit.blocks.renderable.BlockUtilities;
import edu.mit.blocks.renderable.RenderableBlock;
import edu.mit.blocks.renderable.TextualFactoryBlock;
import edu.mit.blocks.workspace.BlockCanvas;
import edu.mit.blocks.workspace.PageChangeEventManager;
import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;
import edu.mit.blocks.workspace.WorkspaceWidget;

/**
 * The TypeBlockManager primary serves to help users drop 
 * blocks manually into the bock canvas through the keyboard.  
 * To achieve this, the TypeBlockManager commands three 
 * distinct phases: Interfacing, Searching, Dropping.
 */
public class TypeBlockManager {
    
    private final Workspace workspace;
    
    /**Directional Pad values*/
    protected static enum Direction {

        UP, DOWN, LEFT, RIGHT, ESCAPE, ENTER
    };
    /**TypeBlockmanager graphical view*/
    private final AutoCompletePanel autoCompletePanel;
    /**Helper Controller that manages the transition between blocks D-PAD*/
    private FocusTraversalManager focusManager;
    /**Current canvas with focus*/
    private BlockCanvas blockCanvas;
    /** plus operations string constants**/
    static final String PLUS_OPERATION_LABEL = "+";
    static final String NUMBER_PLUS_OPERATION_LABEL = "+ [number]";
    static final String TEXT_PLUS_OPERATION_LABEL = "+ [text]";
    /**empty string for labels that already exist and shouldn't be altered to user's preference**/
    static final String EMPTY_LABEL_NAME = "";
    /**quote string for string blocks**/
    static final String QUOTE_LABEL = "\"";
    JFrame frame;
    
    /** Whether keyboard support is enabled or not */
    private boolean enabled = false;

    /**
     * TypeBlockManager Constructor
     */
    public TypeBlockManager(Workspace workspace, BlockCanvas component) {
        this.workspace = workspace;
        
        // turned off the automated block placements
        KeyInputMap.enableDefaultKeyMapping(false);
        this.autoCompletePanel = new AutoCompletePanel(workspace);
        this.blockCanvas = component;
        this.focusManager = workspace.getFocusManager();
    }
    
    /**
     * Enables/disables the keyboard support
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            blockCanvas.getCanvas().addMouseListener(focusManager);
            blockCanvas.getCanvas().addKeyListener(focusManager);
            workspace.addWorkspaceListener(focusManager);
        }
        else {
            blockCanvas.getCanvas().removeMouseListener(focusManager);
            blockCanvas.getCanvas().removeKeyListener(focusManager);
            workspace.removeWorkspaceListener(focusManager);
        }
    }
    
    /**
     * Whether keyboard support is enabled or not
     * @return {@code true}/{@code false}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /*----------------------------------------------------------*
     * Convenience Methods										*
    -----------------------------------------------------------*/
    /**
     * @return true if and only if block is invalid (null or ID==-1)
     */
    private static boolean invalidBlockID(Long blockID) {
        if (blockID == null) {
            return true;
        } else if (blockID.equals(Block.NULL)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isNullBlockInstance(Long blockID) {
        if (blockID == null) {
            return true;
        } else if (blockID.equals(Block.NULL)) {
            return true;
        } else if (workspace.getEnv().getBlock(blockID) == null) {
            return true;
        } else if (workspace.getEnv().getBlock(blockID).getBlockID() == null) {
            return true;
        } else if (workspace.getEnv().getBlock(blockID).getBlockID().equals(Block.NULL)) {
            return true;
        } else if (workspace.getEnv().getRenderableBlock(blockID) == null) {
            return true;
        } else if (workspace.getEnv().getRenderableBlock(blockID).getBlockID() == null) {
            return true;
        } else if (workspace.getEnv().getRenderableBlock(blockID).getBlockID().equals(Block.NULL)) {
            return true;
        } else {
            return false;
        }
    }

    ///////////////////////
    //Automation Handlers//
    ///////////////////////
    /**
     * @requires the current block with focus must exist with non-null
     * 			 ID in a non-null widget with a non-null parent
     * @modifies the current block with focus
     * @effects  removes the current block with focus and all
     * 			 its children from the GUI and destroys the link
     * 			 between the block with focus and it's parent
     * 			 block if one exists
     */
    protected void automateBlockDeletion(Workspace workspace) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateBlockDeletion invoked but typeBlockManager is disabled.");
            return;
        }
        if (!isNullBlockInstance(typeBlockManager.focusManager.getFocusBlockID())) {
            typeBlockManager.deleteBlockAndChildren();
            PageChangeEventManager.notifyListeners();
        }
    }

    /**
     * @requires the current block with focus must exist with non-null
     * 			 ID in a non-null widget with a non-null parent
     * @modifies the current block with focus
     * @effects  removes the current block with focus and children
     * 			 from the GUI and destroys the link
     * 			 between the block with focus and it's parent
     * 			 block if one exist and children blocks
     * 			 if it has childrens.
     */
    private void deleteBlockAndChildren() {
//		====================>>>>>>>>>>>>>>>>>>>>>>>>>
//		====================focus coming in>>>>>>>>>>TODO
//		====================>>>>>>>>>>>>>>>>>>>>>>>>>

        //Do not delete null block references.  Otherwise, get Block and RenderableBlock instances.
        if (isNullBlockInstance(focusManager.getFocusBlockID())) {
            throw new RuntimeException("TypeBlockManager: deleting a null block references.");
        }
        Block block = workspace.getEnv().getBlock(focusManager.getFocusBlockID());
        RenderableBlock renderable = workspace.getEnv().getRenderableBlock(block.getBlockID());

        //get workspace widget associated with current focus
        WorkspaceWidget widget = renderable.getParentWidget();
        //do not delete block instances in null widgets
        if (widget == null) {
            throw new RuntimeException("TypeBlockManager: do not delete blocks with no parent widget.");
            //return;
        }
        //get parent container of this graphical representation
        Container container = renderable.getParent();
        //do not delete block instances in null parents
        if (container == null) {
            throw new RuntimeException("TypeBlockManager: do not delete blocks with no parent container.");
            //return;
        }
        //get the Block's location on the canvas
        Point location = SwingUtilities.convertPoint(
                renderable, new Point(0, 0), this.blockCanvas.getCanvas());

        //for every valid and active connection, disconnect it.
        Long parentID = null;
        if (validConnection(block.getPlug())) {
            parentID = block.getPlugBlockID();
            this.disconnectBlock(block, widget);
            if (validConnection(block.getAfterConnector())) {
                disconnectBlock(workspace.getEnv().getBlock(block.getAfterBlockID()), widget);
            }
        } else if (validConnection(block.getBeforeConnector())) {
            parentID = block.getBeforeBlockID();
            BlockConnector parentConnectorToBlock = workspace.getEnv().getBlock(parentID).getConnectorTo(block.getBlockID());
            this.disconnectBlock(block, widget);
            if (validConnection(block.getAfterConnector())) {
                Long afterBlockID = block.getAfterBlockID();
                disconnectBlock(workspace.getEnv().getBlock(afterBlockID), widget);
                if (parentID != null) {
                    BlockLink link = BlockLinkChecker.canLink(
                            workspace,
                            workspace.getEnv().getBlock(parentID),
                            workspace.getEnv().getBlock(afterBlockID),
                            parentConnectorToBlock,
                            workspace.getEnv().getBlock(afterBlockID).getBeforeConnector());
                    if (link != null) {
                        link.connect();
                        workspace.notifyListeners(new WorkspaceEvent(
                                workspace,
                                workspace.getEnv().getRenderableBlock(link.getPlugBlockID()).getParentWidget(),
                                link, WorkspaceEvent.BLOCKS_CONNECTED));
                        workspace.getEnv().getRenderableBlock(link.getPlugBlockID()).repaintBlock();
                        workspace.getEnv().getRenderableBlock(link.getPlugBlockID()).repaint();
                        workspace.getEnv().getRenderableBlock(link.getPlugBlockID()).moveConnectedBlocks();
                        workspace.getEnv().getRenderableBlock(link.getSocketBlockID()).repaintBlock();
                        workspace.getEnv().getRenderableBlock(link.getSocketBlockID()).repaint();

                    }
                }
            }
        } else if (validConnection(block.getAfterConnector())) {
            parentID = block.getAfterBlockID();
        }

        //remove form widget and container
        this.removeChildrenBlock(renderable, widget, container);

//		<<<<<<<<<<<<<<<<<<<<<<<<<<==========================
//		<<<<<<<<<<<<<<<<<<<<<<<<<<focus changing, coming out TODO
//		<<<<<<<<<<<<<<<<<<<<<<<<<<==========================
        //If the deleted block had a parent, give the parent the focus,
        //Otherwise, give the focus to the canvas (NOT BLOCK CANVAS)
        if (invalidBlockID(parentID)) {
            this.focusManager.setFocus(location, Block.NULL);
            this.blockCanvas.getCanvas().requestFocus();
            return;
        } else {
            this.focusManager.setFocus(parentID);
            this.blockCanvas.getCanvas().requestFocus();
            return;
        }
    }

    private void removeChildrenBlock(RenderableBlock renderable, WorkspaceWidget widget, Container container) {
        widget.removeBlock(renderable);
        container.remove(renderable);
        container.validate();
        container.repaint();
        renderable.setParentWidget(null);
        //Workspace.getInstance().notifyListeners(new WorkspaceEvent(widget, renderable.getBlockID(), WorkspaceEvent.BLOCK_REMOVED));
        for (BlockConnector child : workspace.getEnv().getBlock(renderable.getBlockID()).getSockets()) {
            if (child == null || child.getBlockID().equals(Block.NULL)) {
                continue;
            }
            RenderableBlock childRenderable = workspace.getEnv().getRenderableBlock(child.getBlockID());
            if (childRenderable == null) {
                continue;
            }
            removeBlock(childRenderable, widget, container);
        }
        // If it is a procedure block, we want to delete the entire stack
        if (workspace.getEnv().getBlock(renderable.getBlockID()).isProcedureDeclBlock()) {
            if (workspace.getEnv().getBlock(renderable.getBlockID()).getAfterBlockID() != Block.NULL) {
                removeAfterBlock(workspace.getEnv().getRenderableBlock(workspace.getEnv().getBlock(renderable.getBlockID()).getAfterBlockID()),
                        widget, container);
                this.disconnectBlock(workspace.getEnv().getBlock(workspace.getEnv().getBlock(renderable.getBlockID()).getAfterBlockID()), widget);
            }
        }
        if (renderable.hasComment()) {
            renderable.removeComment();
        }
        workspace.notifyListeners(new WorkspaceEvent(workspace, widget, renderable.getBlockID(), WorkspaceEvent.BLOCK_REMOVED));
    }

    /**
     * Helper method that recursively finds and removes all the blocks connected to the bottom of
     * this block, including this block.
     *
     * @param afterBlock - RenderableBlock we start removing at
     * @param widget - WorkspaceWidget that the block is using
     * @param container - Container the block is stored in
     *
     */
    private void removeAfterBlock(RenderableBlock afterBlock, WorkspaceWidget widget, Container container) {
        if (workspace.getEnv().getBlock(afterBlock.getBlockID()).getAfterBlockID() != Block.NULL) {
            removeAfterBlock(workspace.getEnv().getRenderableBlock(workspace.getEnv().getBlock(afterBlock.getBlockID()).getAfterBlockID()),
                    widget, container);
        }
        removeChildrenBlock(afterBlock, widget, container);
    }

    /**
     * Checks if a connection is a valid and ACTIVE connection.
     *
     * @param connection - BlockConnector in question
     *
     * @requires none
     * @return true if and only if connection != null && connection.hasBlock() == true
     */
    private boolean validConnection(BlockConnector connection) {
        if (connection != null) {
            Long blockID = connection.getBlockID();
            if (!isNullBlockInstance(blockID)) {
                if (connection.hasBlock()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     * @param renderable
     * @param widget
     * @param container
     *
     * @requires renderable != null && renderable.blockID != null && renderable.blockID != Block.NULL
     * 			 && widget != null && container != null
     * @modifies renderable && children blocks connected to renderable
     * @effects removes renderable from container and widget and re-renders
     * 			renderable block, widget, and container appropriately.
     * 			Repeats for all of renderable's children.
     */
    private void removeBlock(RenderableBlock renderable, WorkspaceWidget widget, Container container) {
        widget.removeBlock(renderable);
        container.remove(renderable);
        container.validate();
        container.repaint();
        renderable.setParentWidget(null);
        //Workspace.getInstance().notifyListeners(new WorkspaceEvent(widget, renderable.getBlockID(), WorkspaceEvent.BLOCK_REMOVED));
        for (BlockConnector child : BlockLinkChecker.getSocketEquivalents(workspace.getEnv().getBlock(renderable.getBlockID()))) {
            if (child == null || child.getBlockID().equals(Block.NULL)) {
                continue;
            }
            RenderableBlock childRenderable = workspace.getEnv().getRenderableBlock(child.getBlockID());
            if (childRenderable == null) {
                continue;
            }
            removeBlock(childRenderable, widget, container);
        }
        if (renderable.hasComment()) {
            renderable.removeComment();
        }
        workspace.notifyListeners(new WorkspaceEvent(workspace, widget, renderable.getBlockID(), WorkspaceEvent.BLOCK_REMOVED));
    }

    /**
     * @param childBlock
     * @param widget
     *
     * @requires widget != null
     * @modifies
     * @effects Does nothing if: childBlock is invalid (null)
     * 			Otherwise, remove childBlock from it's parent block
     * 			if the childBlock has a parent.  If it does not have
     * 			a parent, do nothing.
     */
    private void disconnectBlock(Block childBlock, WorkspaceWidget widget) {
        if (childBlock == null || invalidBlockID(childBlock.getBlockID())) {
            return;
        }
        BlockConnector childPlug = BlockLinkChecker.getPlugEquivalent(childBlock);
        if (childPlug == null || !childPlug.hasBlock() || isNullBlockInstance(childPlug.getBlockID())) {
            return;
        }
        Block parentBlock = workspace.getEnv().getBlock(childPlug.getBlockID());
        BlockConnector parentSocket = parentBlock.getConnectorTo(childBlock.getBlockID());
        if (parentSocket == null) {
            return;
        }
        //disconector if child connector exists and has a block connected to it
        BlockLink link = BlockLink.getBlockLink(workspace, childBlock, parentBlock, childPlug, parentSocket);
        if (link == null) {
            return;
        }

        link.disconnect();

        RenderableBlock parentRenderable = workspace.getEnv().getRenderableBlock(parentBlock.getBlockID());
        if (parentRenderable == null) {
            throw new RuntimeException("INCONSISTANCY VIOLATION: "
                    + "parent block was valid, non-null, and existed.\n\tBut yet, when we get it's renderable"
                    + "representation, we recieve a null instance.\n\tIf the Block instance of an ID is non-null"
                    + "then its graphical RenderableBlock should be non-null as well");
        }
        parentRenderable.blockDisconnected(parentSocket);
        workspace.notifyListeners(new WorkspaceEvent(workspace, widget, link, WorkspaceEvent.BLOCKS_DISCONNECTED));
    }
    /**
     * @requires none
     * @modifies bufferedBlock (the block that is copied)
     * @effects change bufferedBlock such that it points
     * 			to the block with current focus
     */
    private BlockNode bufferedBlock = null;

    public static void copyBlock(Workspace workspace) {
        TypeBlockManager.automateCopyBlock(workspace);
    }

    public static void pasteBlock(Workspace workspace) {
        TypeBlockManager.automatePasteBlock(workspace);
    }

    protected static void automateCopyBlock(Workspace workspace) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateCopyBlock invoked but typeBlockManager is disabled.");
            return;
        }
        typeBlockManager.bufferedBlock =
                BlockUtilities.makeNodeWithChildren(workspace, typeBlockManager.focusManager.getFocusBlockID());
    }

    protected static void automateCopyAll(Workspace workspace) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMatePasteBlock invoked but typeBlockManager is disabled.");
            return;
        }
        typeBlockManager.bufferedBlock =
                BlockUtilities.makeNodeWithStack(workspace, typeBlockManager.focusManager.getFocusBlockID());
    }

    /**
     * @param workspace
     * @requires whatever is requires for AutomatedBlockInsertion
     *
     */
    protected static void automatePasteBlock(Workspace workspace) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMatePasteBlock invoked but typeBlockManager is disabled.");
            return;
        }

        typeBlockManager.pasteStack(typeBlockManager.bufferedBlock);
    }

    private void pasteStack(BlockNode node) {
//		====================>>>>>>>>>>>>>>>>>>>>>>>>>
//		====================focus coming in>>>>>>>>>> TODO
//		====================>>>>>>>>>>>>>>>>>>>>>>>>>
        if (node == null) {
            return;
        }
        WorkspaceWidget widget = null;
        Iterable<WorkspaceWidget> widgets = null;
        Point spot = null;
        if (invalidBlockID(focusManager.getFocusBlockID())) {
            //canvas has focus
            Point location = SwingUtilities.convertPoint(
                    this.blockCanvas.getCanvas(),
                    this.focusManager.getCanvasPoint(),
                    workspace);
            widget = workspace.getWidgetAt(location);
            spot = SwingUtilities.convertPoint(
                    this.blockCanvas.getCanvas(),
                    this.focusManager.getCanvasPoint(),
                    widget.getJComponent());
        } else {
            RenderableBlock focusRenderable = workspace.getEnv().getRenderableBlock(focusManager.getFocusBlockID());
            widget = focusRenderable.getParentWidget();
            spot = focusRenderable.getLocation();
        }

        if (widget == null) {
            // TODO: To be examined and fixed, occurs on macs
            JOptionPane.showMessageDialog(frame, "Please click somewhere on the canvas first.",
                    "Error", JOptionPane.PLAIN_MESSAGE);
            //throw new RuntimeException("Why are we adding a block to a null widget?");
        } else {
            // checks to see if the copied block still exists
            if (BlockUtilities.blockExists(workspace, node)) {
                //create mirror block and mirror childrens
                spot.translate(10, 10);
                RenderableBlock mirror = BlockUtilities.makeRenderable(workspace, node, widget);
                mirror.setLocation(spot);
                mirror.moveConnectedBlocks(); // make sure the childrens are placed correctly
            } else {
                //TODO: future version, allow them to paste
                JOptionPane.showMessageDialog(frame, "You cannot paste blocks that are currently NOT on the canvas."
                        + "\nThis function will be available in a future version.\n", "Error", JOptionPane.PLAIN_MESSAGE);
            }

        }
    }

    /**
     * Traverses the block tree structure to move
     * in the direction of the input argument.
     * @param workspace
     * @param dir
     */
    protected static void automateFocusTraversal(Workspace workspace, Direction dir) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateFocusTraversal invoked but typeBlockManager is disabled.");
            return;
        }
        typeBlockManager.traverseFocus(dir);
    }

    private void traverseFocus(Direction dir) {
        if (isNullBlockInstance(focusManager.getFocusBlockID())) {
            if (dir == Direction.UP) {
                blockCanvas.getVerticalModel().setValue(blockCanvas.getVerticalModel().getValue() - 5);
            } else if (dir == Direction.DOWN) {
                blockCanvas.getVerticalModel().setValue(blockCanvas.getVerticalModel().getValue() + 5);
            } else if (dir == Direction.LEFT) {
                blockCanvas.getHorizontalModel().setValue(blockCanvas.getHorizontalModel().getValue() - 5);
            } else if (dir == Direction.RIGHT) {
                blockCanvas.getHorizontalModel().setValue(blockCanvas.getHorizontalModel().getValue() + 5);
            } else if (dir == Direction.ESCAPE) {
                //according to the focus manager, the canvas already
                //has focus. So, just request focus again.
                this.blockCanvas.getCanvas().requestFocus();
            } else if (dir == Direction.ENTER) {
            }
        } else {
            if (dir == Direction.UP) {
                focusManager.focusBeforeBlock();
            } else if (dir == Direction.DOWN) {
                focusManager.focusAfterBlock();
            } else if (dir == Direction.LEFT) {
                focusManager.focusPrevBlock();
            } else if (dir == Direction.RIGHT) {
                focusManager.focusNextBlock();
            } else if (dir == Direction.ESCAPE) {
                RenderableBlock block = workspace.getEnv().getRenderableBlock(
                        focusManager.getFocusBlockID());
                Point location = SwingUtilities.convertPoint(block, new Point(0, 0), this.blockCanvas.getCanvas());
                this.focusManager.setFocus(location, Block.NULL);
                this.blockCanvas.getCanvas().requestFocus();
            } else if (dir == Direction.ENTER) {
            	workspace.getEnv().getRenderableBlock(focusManager.getFocusBlockID()).switchToLabelEditingMode(true);
            }
        }
    }

    /**
     * Displays an assisting AutoCompletePanel.
     * @param workspace
     * @param character
     */
    protected void automateAutoComplete(Workspace workspace, char character) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateAutoComplete invoked but typeBlockManager is disabled.");
            return;
        }
        typeBlockManager.displayAutoCompletePanel(character);
    }

    /**
     * @requires this.blockCanvas.getCanvas() != null
     * @param character
     */
    private void displayAutoCompletePanel(char character) {
//		====================>>>>>>>>>>>>>>>>>>>>>>>>>
//		====================focus coming in>>>>>>>>>> TODO
//		====================>>>>>>>>>>>>>>>>>>>>>>>>>
        if (invalidBlockID(focusManager.getFocusBlockID())) {
            //canvas has focus
            this.blockCanvas.getCanvas().add(autoCompletePanel, JLayeredPane.DRAG_LAYER);
            autoCompletePanel.setLocation(this.focusManager.getCanvasPoint());
            autoCompletePanel.setVisible(true);
            autoCompletePanel.requestFocus();
        } else {
            //renderableblock has focus
            this.blockCanvas.getCanvas().add(autoCompletePanel, JLayeredPane.DRAG_LAYER);
            RenderableBlock block = workspace.getEnv().getRenderableBlock(focusManager.getFocusBlockID());
            Point location = SwingUtilities.convertPoint(
                    block,
                    this.focusManager.getBlockPoint(),
                    this.blockCanvas.getCanvas());
            location.translate(10, 10);
            autoCompletePanel.setLocation(location);
            autoCompletePanel.setVisible(true);
            autoCompletePanel.requestFocus();
        }
        autoCompletePanel.setText(String.valueOf(character));
    }

    /**
     * assumes number and differen genus exist and number genus has ediitabel lable
     */
    protected void automateNegationInsertion(Workspace workspace) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateNegationInsertion invoked but typeBlockManager is disabled.");
            return;
        }

//		====================>>>>>>>>>>>>>>>>>>>>>>>>>
//		====================focus coming in>>>>>>>>>> TODO
//		====================>>>>>>>>>>>>>>>>>>>>>>>>>

        //get focus block
        Long parentBlockID = typeBlockManager.focusManager.getFocusBlockID();
        if (isNullBlockInstance(parentBlockID)) {
            //focus on canvas
            automateBlockInsertion(workspace, "number", "-");

        } else {
            Block parentBlock = workspace.getEnv().getBlock(parentBlockID);
            if (parentBlock.isDataBlock()) {
                //focus on a data block
                automateBlockInsertion(workspace, "difference", null);
            } else {
                //focus on a non-data block
                automateBlockInsertion(workspace, "number", "-");
            }
        }
    }

    protected void automateMultiplication(Workspace workspace, char character) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateMultiplication invoked but typeBlockManager is disabled.");
            return;
        }
        if (!isNullBlockInstance(typeBlockManager.focusManager.getFocusBlockID())) {
            Block parentBlock = workspace.getEnv().getBlock(typeBlockManager.focusManager.getFocusBlockID());
            if (parentBlock.getGenusName().equals("number")) {
                automateBlockInsertion(workspace, "product", null);
                return;
            }
        }
        automateAutoComplete(workspace, character);
        return;
    }

    protected void automateAddition(Workspace workspace, char character) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateMultiplication invoked but typeBlockManager is disabled.");
            return;
        }
        //get focus block
        Long parentBlockID = typeBlockManager.focusManager.getFocusBlockID();
        if (isNullBlockInstance(parentBlockID)) {
            //focus on canvas
            automateBlockInsertion(workspace, "sum", null);
        } else {
            Block parentBlock = workspace.getEnv().getBlock(parentBlockID);
            if (parentBlock.getGenusName().equals("string")) {
                //focus on string block
                automateBlockInsertion(workspace, "string-append", null);
            } else if (parentBlock.getGenusName().equals("string-append")) {
                //focus on string append block
                automateBlockInsertion(workspace, "string-append", null);
            } else {
                //focus on any other block
                automateBlockInsertion(workspace, "sum", null);
            }
        }
    }

    /**
     * @param workspace The workspace in use
     * @param genusName
     * @param label
     *
     * @requires if (label != null) then associated block.isLabelEditable() should return true
     * @modifies 	focusManager.focusblock &&
     * 				focusManager.focuspoint &&
     * 				blockCanvas
     * @effects Do nothing if "genusName" does not map to a valid block.
     * 			Otherwise, create and add a new block with matching genus
     * 			and label properties to one of the following:
     * 				1. the current block with focus at (0,0)
     * 				   relative to that block.
     * 				2. the current block with focus at next
     * 				   applicable socket location
     * 				3. the canvas at the last mouse click point.
     * 			Then update any focus and block connections.
     */
    protected void automateBlockInsertion(Workspace workspace, String genusName, String label) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateBlockInsertion invoked but typeBlockManager is disabled.");
            return;
        }
        //if genus is null, DO NOT insert a new block, DO NOT change the focus
        if (genusName == null) {
            return;
        }
        //get matching textual Block
        RenderableBlock createdRB = BlockUtilities.getBlock(workspace, genusName, null);
        if (createdRB == null) {
            return;
        } else {
            //change name of block IF AN DONLY IFF a label was passed
            //and the block's label was editable and the block
            //does not need to have a unique label
            if (label != null && workspace.getEnv().getBlock(createdRB.getBlockID()).isLabelEditable() && !workspace.getEnv().getBlock(createdRB.getBlockID()).labelMustBeUnique()) {
            	workspace.getEnv().getBlock(createdRB.getBlockID()).setBlockLabel(label);
            }
            //add block
            typeBlockManager.addBlock(createdRB);
        }
    }

    /**
     * @requires none
     * @modifies 	focusManager.focusblock &&
     * 				focusManager.focuspoint &&
     * 				blockCanvas
     * @effects Do nothing if "genusName" does not map to a valid block.
     * 			Otherwise, create and add a new block with matching genus
     * 			and label properties to one of the following:
     * 				1. the current block with focus at (0,0)
     * 				   relative to that block.
     * 				2. the current block with focus at next
     * 				   applicable socket location
     * 				3. the canvas at the last mouse click point.
     * 			Then update any focus and block connections.
     */
    protected void automateBlockInsertion(Workspace workspace, TextualFactoryBlock block) {
        /*Passing in an empty label name means that the block should already have
        a predetermined label name that does not need to be altered to the user's preference*/
        automateBlockInsertion(workspace, block, EMPTY_LABEL_NAME);
    }

    /**
     * @requires none
     * @modifies 	focusManager.focusblock &&
     * 				focusManager.focuspoint &&
     * 				blockCanvas
     * @effects Do nothing if "genusName" does not map to a valid block.
     * 			Otherwise, create and add a new block with matching genus
     * 			and label properties to one of the following:
     * 				1. the current block with focus at (0,0)
     * 				   relative to that block.
     * 				2. the current block with focus at next
     * 				   applicable socket location
     * 				3. the canvas at the last mouse click point.
     * 			If label is not an empty string, then set the block label
     * 			to that string.
     * 			Then update any focus and block connections.
     */
    protected void automateBlockInsertion(Workspace workspace, TextualFactoryBlock block, String label) {
        TypeBlockManager typeBlockManager = workspace.getTypeBlockManager();
        if (!typeBlockManager.isEnabled()) {
            System.err.println("AutoMateBlockInsertion invoked but typeBlockManager is disabled.");
            return;
        }
        RenderableBlock createdRB = createRenderableBlock(block);
        // sets the label of the block to whatever the user typed (should only be numbers)
        if (label != EMPTY_LABEL_NAME) {
            createdRB.getBlock().setBlockLabel(label);
        }
        // changes the plus number labels back to +
        if (label.equals(NUMBER_PLUS_OPERATION_LABEL)) {
            createdRB.getBlock().setBlockLabel(PLUS_OPERATION_LABEL);
        }
        // changes the plus text labels back to +
        if (label.equals(TEXT_PLUS_OPERATION_LABEL)) {
            createdRB.getBlock().setBlockLabel(PLUS_OPERATION_LABEL);
        }
        if (createdRB == null) {
            return;
        } else {
            typeBlockManager.addBlock(createdRB);
        }
    }

    /**
     * @param block - the textual block from which a new RenderableBlock will be constructed
     *
     * @requires
     * @modifies nothing
     * @effects none
     * @return new RenderableBlock instance from the TextualFactoryBlock
     * 		   or null if not possible.
     */
    private RenderableBlock createRenderableBlock(TextualFactoryBlock block) {
        //if textual wrapper is null, return a null instance of RenderableBlock.
        if (block == null) {
            return null;
        }
        //if FactoryBlock wrapped in textual wrapper is invalid, return null RenderableBlock instance.
        if (block.getfactoryBlock() == null || block.getfactoryBlock().getBlockID().equals(Block.NULL)) {
            return null;
        }
        //create and get the RenderableBloc instance associated with the Textual wrapper's FactoryBlock
        RenderableBlock createdRB = block.getfactoryBlock().createNewInstance();
        //if the above instance of RenderableBlock is invalid (null or points to null)
        //then DO NOT insert a new block, DO NOT change the focus.
        if (createdRB == null || isNullBlockInstance(createdRB.getBlockID())) {
            throw new RuntimeException("Invariant Violated:"
                    + "May not drop null instances of Renderable Blocks");
        }
        //Please keep the above check rep because it does not
        //make any sense to have an exisitn valid
        //FactoryRenderableBlock point to some non-existing
        //block.  In other words, why would you have a factory
        //that churns out invalid products?
        return createdRB;
    }

    /**
     * @param block
     *
     * @requires 	block must be a valid block.  That is, block may not be such that
     * 				block == null || block.getBlockID() == null ||
     *				block.getBlockID() == Block.NULL || block.getBlockID() == -1 ||
     *				Block.getBlock(block.getBlockID()) == null ||
     *				Block.getBlock(block.getBlockID()).getGenusName() == null ||
     *				Block.getBlock(block.getBlockID()).getGenusName().length() == 0 ||
     *				Block.getBlock(block.getBlockID()).getBlockLabel() == null
     * @modifies Objects modified by this method is undefined
     * @effects The effects of this method is unknown
     */
    private void addBlock(RenderableBlock block) {
        //check invariant
        if (block == null || block.getBlockID() == null
                || block.getBlockID().equals(Block.NULL)
                || workspace.getEnv().getBlock(block.getBlockID()) == null
                || workspace.getEnv().getBlock(block.getBlockID()).getGenusName() == null
                || workspace.getEnv().getBlock(block.getBlockID()).getGenusName().length() == 0
                || workspace.getEnv().getBlock(block.getBlockID()).getBlockLabel() == null) {
            throw new RuntimeException("Invariant Violated: may not pass an invalid instance of renderabel block");
        }

        //ignore default arguments
        block.ignoreDefaultArguments();
        this.blockCanvas.getCanvas().add(block, 0);
        block.setLocation(0, 0);
        Long parentBlockID = this.focusManager.getFocusBlockID();
        if (invalidBlockID(parentBlockID)) {
            new BlockDropAnimator(
                    workspace,
                    this.focusManager.getCanvasPoint(),
                    block,
                    workspace.getEnv().getRenderableBlock(parentBlockID));
        } else {
            RenderableBlock parentBlock = workspace.getEnv().getRenderableBlock(parentBlockID);
            new BlockDropAnimator(
                    workspace,
                    SwingUtilities.convertPoint(parentBlock,
                    this.focusManager.getBlockPoint(),
                    this.blockCanvas.getCanvas()),
                    block,
                    workspace.getEnv().getRenderableBlock(parentBlockID));
        }
        this.focusManager.setFocus(block.getBlockID());
    }
}
