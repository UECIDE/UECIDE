package edu.mit.blocks.renderable;

import java.awt.event.MouseEvent;

import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblocks.BlockConnector;
import edu.mit.blocks.codeblocks.BlockConnectorShape;
import edu.mit.blocks.workspace.Workspace;

/**
 * CollapseLabel is a label that can be added to a renderable block that
 * will cause all blocks after this block to be hidden from view when
 * the isCollapsed parameter is true.
 *
 *
 */
class CollapseLabel extends BlockControlLabel {

    private static final long serialVersionUID = 1L;

    CollapseLabel(Workspace workspace, long blockID) {
        super(workspace, blockID);
    }

    /**
     * setup current visual state of button
     */
    @Override
    public void update() {
        RenderableBlock rb = workspace.getEnv().getRenderableBlock(getBlockID());

        if (rb != null) {
            int x = 0;
            int y = 0;

            y += rb.getBlockHeight() / rb.getZoom() - 22 + (isActive() ? BlockConnectorShape.CONTROL_PLUG_HEIGHT : 0);
            x += 12;
            x = rb.rescale(x);
            y = rb.rescale(y);

            setLocation(x, y);
            setSize(rb.rescale(14), rb.rescale(14));

            if (isActive()) {
                setText("+");
            } else {
                setText("-");
            }
        }
    }

    /**
     * Reverses visibility of afterBlocks and sockets of a procedure block (if present)
     * when collapse label clicked
     */
    void collapseBlockAndStack() {
        updateCollapse();
    }

    /**
     * Sets visibility of afterBlocks and sockets of a procedure block
     */
    void updateCollapse() {
        RenderableBlock rb = workspace.getEnv().getRenderableBlock(getBlockID());

        if (rb != null) {
            collapseAfterBlocks(rb.getBlockID());
            rb.repaintBlock();
            if (rb.getHighlightHandler() != null) {
                rb.getHighlightHandler().updateImage();
                if (rb.getHighlightHandler().getParent() != null && rb.getHighlightHandler().getParent().getParent() != null) {
                    rb.getHighlightHandler().getParent().getParent().repaint();  //force redraw to erase highlight
                }
            }

        }
    }

    /**
     * Toggles visibility of all afterBlocks and their sockets of the given blockID
     */
    void collapseAfterBlocks(long blockID) {
        Block block = workspace.getEnv().getBlock(blockID);

        if (block.getAfterBlockID() != Block.NULL) {
            do {
                block = workspace.getEnv().getBlock(block.getAfterBlockID());
                collapseBlock(block.getBlockID());
            } while (block.getAfterBlockID() != Block.NULL);
        }
    }

    /**
     * Collapse the block corresponding to @param blockID and the blocks
     * connect to its sockets.
     * @param blockID
     */
    void collapseBlock(long blockID) {
        RenderableBlock rBlock;
        rBlock = workspace.getEnv().getRenderableBlock(blockID);
        rBlock.setVisible(!isActive());
        if (rBlock.hasComment() && rBlock.getComment().getCommentLabel().isActive()) {
            rBlock.getComment().setVisible(!isActive());
        }
        rBlock.getHighlightHandler().updateImage();
        rBlock.repaintBlock();
        collapseSockets(blockID);
    }

    /**
     * Toggles visibility of all blocks connected to sockets
     * NB Sockets on procedure blocks do not have afterBlocks
     */
    void collapseSockets(Long block_id) {
        Block block = workspace.getEnv().getBlock(block_id);

        for (BlockConnector socket : block.getSockets()) {
            if (socket.getBlockID() != Block.NULL) {
                collapseBlock(socket.getBlockID());
                collapseAfterBlocks(socket.getBlockID());
            }
        }
    }

    /**
     * Implement MouseListener interface
     * toggle collapse state of block if button pressed
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        toggle();
        collapseBlockAndStack();
        update();
    }
}
