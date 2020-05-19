package edu.mit.blocks.codeblocks;

import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;
import edu.mit.blocks.workspace.WorkspaceListener;

public class CommandRule implements LinkRule, WorkspaceListener {
    
    private final Workspace workspace;

    public CommandRule(Workspace workspace) {
        this.workspace = workspace;
    }

    public boolean canLink(Block block1, Block block2, BlockConnector socket1, BlockConnector socket2) {
        if (!BlockConnectorShape.isCommandConnector(socket1) || !BlockConnectorShape.isCommandConnector(socket2)) {
            return false;
        }
        // We want exactly one before connector
        if (socket1 == block1.getBeforeConnector()) {
            return !socket1.hasBlock();
        } else if (socket2 == block2.getBeforeConnector()) {
            return !socket2.hasBlock();
        }
        return false;
    }

    public boolean isMandatory() {
        return false;
    }

    public void workspaceEventOccurred(WorkspaceEvent e) {
        // TODO Auto-generated method stub
        if (e.getEventType() == WorkspaceEvent.BLOCKS_CONNECTED) {
            BlockLink link = e.getSourceLink();
            if (link.getLastBlockID() != null && link.getLastBlockID() != Block.NULL
                    && BlockConnectorShape.isCommandConnector(link.getPlug()) && BlockConnectorShape.isCommandConnector(link.getSocket())) {
                Block top = workspace.getEnv().getBlock(link.getPlugBlockID());
                while (top.hasAfterConnector() && top.getAfterConnector().hasBlock()) {
                    top = workspace.getEnv().getBlock(top.getAfterBlockID());
                }
                Block bottom = workspace.getEnv().getBlock(link.getLastBlockID());

                // For safety: if either the top stack is terminated, or
                // the bottom stack is not a starter, don't try to force a link
                if (!top.hasAfterConnector() || !bottom.hasBeforeConnector()) {
                    return;
                }

                link = BlockLink.getBlockLink(workspace, top, bottom, top.getAfterConnector(), bottom.getBeforeConnector());
                link.connect();
            }
        }
    }
}
