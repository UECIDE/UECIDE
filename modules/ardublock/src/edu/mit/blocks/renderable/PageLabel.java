package edu.mit.blocks.renderable;

import java.awt.Color;

import edu.mit.blocks.workspace.Workspace;

class PageLabel extends BlockLabel {

    PageLabel(Workspace workspace, String initLabelText, BlockLabel.Type labelType, boolean isEditable, long blockID) {
        super(workspace, initLabelText, labelType, isEditable, blockID, false, Color.yellow);
    }

    void update() {
        int x = 5;
        int y = 5;

        RenderableBlock rb = workspace.getEnv().getRenderableBlock(getBlockID());
        if (rb != null) {
            x += descale(rb.getControlLabelsWidth());
        }

        this.setPixelLocation(rescale(x), rescale(y));
    }
}
