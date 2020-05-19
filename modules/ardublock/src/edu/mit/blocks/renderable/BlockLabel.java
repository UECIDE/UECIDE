package edu.mit.blocks.renderable;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblocks.BlockConnector;
import edu.mit.blocks.codeblocks.BlockStub;
import edu.mit.blocks.codeblockutil.LabelWidget;
import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;

/**
 * BlockLabel is a region on a block in which text is displayed and possibly edited.  The
 * location and font of a BlockLabel is specified in BlockShape and the text displayed is specified
 * by a Block, BlockLabel is the gateway for text to be rendered and modified.
 *
 * The key nature of a BlockLabel is that it is a JLabel when being viewed, and a JTextField
 * when it is being edited.
 *
 * During mouse move, entered and exited events a white border is toggled around the label
 * for particular blocks. This white border helps to suggest editable labels for blocks that
 * have this enabled.
 */
public class BlockLabel implements MouseListener, MouseMotionListener, KeyListener {

    /**Enum for the differnt types of labels in codeblocks */
    public enum Type {
        NAME_LABEL, PAGE_LABEL, PORT_LABEL, DATA_LABEL
    }
    public final static Font blockFontSmall_Bold = new Font("Monospaced", Font.BOLD, 7);
    public final static Font blockFontMedium_Bold = new Font("Monospaced", Font.BOLD, 10);
    public final static Font blockFontLarge_Bold = new Font("Monospaced", Font.BOLD, 12);
    public final static Font blockFontSmall_Plain = new Font("Monospaced", Font.PLAIN, 7);
    public final static Font blockFontMedium_Plain = new Font("Monospaced", Font.PLAIN, 10);
    public final static Font blockFontLarge_Plain = new Font("Monospaced", Font.PLAIN, 12);
    private LabelWidget widget;

    /** These keys inputs are delegated back to renderable block */
    private final char[] validOperators = {'-', '+', '/', '*', '=', '<', '>', 'x', 'X'};

    private Long blockID;

    private BlockLabel.Type labelType;

    private double zoom = 1.0;

    protected final Workspace workspace;

    /**
     * BlockLabel Constructor
     * NOTE: A true boolean passed into the isEditable parameter does not necessarily make the label
     * editable, but a false boolean will make the label uneditable.
     */
    public BlockLabel(Workspace workspace, String initLabelText, BlockLabel.Type labelType, boolean isEditable, Color tooltipBackground) {
        //call other constructor
        this(workspace, initLabelText, labelType, isEditable, -1, false, tooltipBackground);
    }

    public BlockLabel(Workspace workspace, String initLabelText, BlockLabel.Type labelType, boolean isEditable, long blockID, boolean hasComboPopup, Color tooltipBackground) {
        this.workspace = workspace;
        if (Block.NULL.equals(blockID)) {
            throw new RuntimeException("May not pass a null block instance as the parent of a block label");
        }
        if (initLabelText == null) {
            initLabelText = "";
        }
        this.blockID = blockID;
        this.labelType = labelType;
        widget = new LabelWidget(initLabelText, workspace.getEnv().getBlock(blockID).getColor().darker(), tooltipBackground) {

            private static final long serialVersionUID = 328149080424L;

            protected void fireTextChanged(String text) {
                textChanged(text);
            }

            protected void fireGenusChanged(String genus) {
                genusChanged(genus);
            }

            protected void fireDimensionsChanged(Dimension value) {
                dimensionsChanged(value);
            }

            protected boolean isTextValid(String text) {
                return textValid(text);
            }
        };
        widget.setNumeric(workspace.getEnv().getBlock(this.blockID).getGenusName().equals("number"));

        // Only editable if the isEditable parameter was true, the label is either a Block's name or
        // socket label, the block can edit labels, and the block is not in the factory.
        widget.setEditable(
                isEditable
                && (labelType == BlockLabel.Type.NAME_LABEL || labelType == BlockLabel.Type.PORT_LABEL)
                && workspace.getEnv().getBlock(blockID).isLabelEditable()
                && !(workspace.getEnv().getRenderableBlock(blockID) instanceof FactoryRenderableBlock));
        if (labelType == null || labelType.equals(BlockLabel.Type.NAME_LABEL)) {
            widget.setFont(BlockLabel.blockFontLarge_Bold);
        } else if (labelType.equals(BlockLabel.Type.PAGE_LABEL)) {
            widget.setFont(BlockLabel.blockFontMedium_Bold);
        } else if (labelType.equals(BlockLabel.Type.PORT_LABEL)) {
            widget.setFont(BlockLabel.blockFontMedium_Bold);
        } else if (labelType.equals(BlockLabel.Type.DATA_LABEL)) {
            widget.setFont(BlockLabel.blockFontMedium_Bold);
        }
        if (workspace.getEnv().getBlock(blockID).hasSiblings()) {
            //Map<String, String> siblings = new HashMap<String, String>();
            List<String> siblingsNames = workspace.getEnv().getBlock(blockID).getSiblingsList();
            String[][] siblings = new String[siblingsNames.size() + 1][2];
            siblings[0] = new String[]{workspace.getEnv().getBlock(blockID).getGenusName(), workspace.getEnv().getBlock(blockID).getInitialLabel()};
            for (int i = 0; i < siblingsNames.size(); i++) {
                siblings[i + 1] = new String[]{siblingsNames.get(i), workspace.getEnv().getGenusWithName(siblingsNames.get(i)).getInitialLabel()};
            }
            widget.setSiblings(hasComboPopup && workspace.getEnv().getBlock(blockID).hasSiblings(), siblings);
        }

        widget.addMouseListenerToLabel(this);
        widget.addMouseMotionListenerToLabel(this);
        widget.addKeyListenerToTextField(this);

        //set initial text
        widget.updateLabelText(initLabelText);
        //add and show the textLabel initially
        widget.setEditingState(false);
    }

    public void setZoomLevel(double newZoom) {
        this.zoom = newZoom;
        widget.setZoomLevel(newZoom);
    }

    public int getAbstractWidth() {
        if (widget.hasSiblings()) {
            return (int) (widget.getWidth() / zoom) - LabelWidget.DROP_DOWN_MENU_WIDTH;
        } else {
            return (int) (widget.getWidth() / zoom);
        }
    }

    public int getAbstractHeight() {
        return (int) (widget.getHeight() / zoom);
    }

    public int getPixelWidth() {
        return widget.getWidth();
    }

    public int getPixelHeight() {
        return widget.getHeight();
    }

    public Point getPixelLocation() {
        return widget.getLocation();
    }

    public void setEditable(boolean isEditable) {
        widget.setEditable(isEditable);
    }

    public boolean editingText() {
        return widget.editingText();
    }

    public void highlightText() {
        widget.highlightText();
    }

    public void setPixelLocation(int x, int y) {
        widget.setLocation(x, y);
    }

    public String getText() {
        return widget.getText();
    }

    public void setText(String text) {
        widget.setText(text);
    }

    public void setText(boolean text) {
        widget.setText(text);
    }

    public void setText(double text) {
        widget.setText(text);
    }

    public void setToolTipText(String text) {
        widget.assignToolTipToLabel(text);
    }

    public void showMenuIcon(boolean show) {
        widget.showMenuIcon(show);
    }

    public JComponent getJComponent() {
        return widget;
    }

    public void setEditingState(boolean editing) {
        widget.setEditingState(editing);
    }

    protected int rescale(int x) {
        return (int) (x * zoom);
    }

    protected int rescale(double x) {
        return (int) (x * zoom);
    }

    protected int descale(int x) {
        return (int) (x / zoom);
    }

    protected int descale(double x) {
        return (int) (x / zoom);
    }

    /** returns the blockID for this BlockLabel */
    Long getBlockID() {
        return blockID;
    }

    protected void textChanged(String text) {
        if ((this.labelType.equals(BlockLabel.Type.NAME_LABEL) || this.labelType.equals(BlockLabel.Type.PORT_LABEL))
                && workspace.getEnv().getBlock(blockID).isLabelEditable()) {
            if (this.labelType.equals(BlockLabel.Type.NAME_LABEL)) {
                workspace.getEnv().getBlock(blockID).setBlockLabel(text);
            }
            BlockConnector plug = workspace.getEnv().getBlock(blockID).getPlug();
            // Check if we're connected to a block. If we are and the the block we're connected to
            // has stubs, update them.
            if (plug != null && plug.getBlockID() != Block.NULL) {
                if (workspace.getEnv().getBlock(plug.getBlockID()) != null) {
                    if (workspace.getEnv().getBlock(plug.getBlockID()).isProcedureDeclBlock()
                            && workspace.getEnv().getBlock(plug.getBlockID()).hasStubs()) {
                        // Blocks already store their socket names when saved so it is not necessary
                        // nor desired to call the connectors changed event again.
                        if (workspace.getEnv().getRenderableBlock(plug.getBlockID()).isLoading()) {
                            BlockStub.parentConnectorsChanged(workspace, plug.getBlockID());
                        }
                    }
                }
            }
            RenderableBlock rb = workspace.getEnv().getRenderableBlock(blockID);
            if (rb != null) {
                workspace.notifyListeners(new WorkspaceEvent(workspace, rb.getParentWidget(), blockID, WorkspaceEvent.BLOCK_RENAMED));
            }
        }
    }

    protected void genusChanged(String genus) {
        if (widget.hasSiblings()) {
            Block oldBlock = workspace.getEnv().getBlock(blockID);
            oldBlock.changeGenusTo(genus);
            RenderableBlock rb = workspace.getEnv().getRenderableBlock(blockID);
            rb.repaintBlock();
            workspace.notifyListeners(new WorkspaceEvent(workspace, rb.getParentWidget(), blockID, WorkspaceEvent.BLOCK_GENUS_CHANGED));
        }
    }

    protected void dimensionsChanged(Dimension value) {
        if (workspace.getEnv().getRenderableBlock(blockID) != null) {
        	workspace.getEnv().getRenderableBlock(blockID).repaintBlock();
        }
    }

    protected boolean textValid(String text) {
        return !text.equals("")
                && BlockUtilities.isLabelValid(workspace, blockID, text);
    }

    @Override
	public void mouseClicked(MouseEvent e) {
        if (!((e.getClickCount() == 1) && widget.isEditable())) {
            workspace.getEnv().getRenderableBlock(blockID).processMouseEvent(SwingUtilities.convertMouseEvent(widget, e, widget.getParent()));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (widget.getParent() != null && widget.getParent() instanceof MouseListener) {
            workspace.getEnv().getRenderableBlock(blockID).processMouseEvent(SwingUtilities.convertMouseEvent(widget, e, widget.getParent()));
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (widget.getParent() != null && widget.getParent() instanceof MouseListener) {
            workspace.getEnv().getRenderableBlock(blockID).processMouseEvent(SwingUtilities.convertMouseEvent(widget, e, widget.getParent()));
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (widget.getParent() != null && widget.getParent() instanceof MouseListener) {
            workspace.getEnv().getRenderableBlock(blockID).processMouseEvent(SwingUtilities.convertMouseEvent(widget, e,
                    widget.getParent()));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (widget.getParent() != null && widget.getParent() instanceof MouseListener) {
            workspace.getEnv().getRenderableBlock(blockID).processMouseEvent(SwingUtilities.convertMouseEvent(widget, e, widget.getParent()));
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (widget.getParent() != null && widget.getParent() instanceof MouseMotionListener) {
            ((MouseMotionListener) widget.getParent()).mouseDragged(SwingUtilities.convertMouseEvent(widget, e, widget.getParent()));
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                workspace.getEnv().getRenderableBlock(blockID).requestFocus();
                return;
            case KeyEvent.VK_ENTER:
                workspace.getEnv().getRenderableBlock(blockID).requestFocus();
                return;
            case KeyEvent.VK_TAB:
                workspace.getEnv().getRenderableBlock(blockID).processKeyPressed(e);
                return;
        }
        if (workspace.getEnv().getBlock(this.blockID).getGenusName().equals("number")) {
            if (e.getKeyChar() == '-' && widget.canProcessNegativeSign()) {
                return;
            }
            for (char c : validOperators) {
                if (e.getKeyChar() == c) {
                    workspace.getEnv().getRenderableBlock(blockID).processKeyPressed(e);
                    return;
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
