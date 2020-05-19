package edu.mit.blocks.codeblocks;

import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblocks.LinkRule;
import edu.mit.blocks.codeblocks.BlockConnector;

/**
 * LinkRule that prevents constants from linking as procedure parameters and prevents parameters
 * from linking as constants in other code.
 */
public class ParamRule implements LinkRule{

    public boolean canLink(Block block1, Block block2, BlockConnector socket1, BlockConnector socket2) {
        switch (socket1.getPositionType()) {
        case BOTTOM:
        case TOP:
            return true; // we're only checking params, so ignore top and bottom connectors
        }
        
        // first, if connecting to a proc decl, 
        // make sure you're a param
        if (block1.isProcedureDeclBlock())
            return block2.isProcedureParamBlock();
        else if (block2.isProcedureDeclBlock())
            return block1.isProcedureParamBlock();
        
        // second, if connecting to a param,
        // make sure you're a proc decl
        else if (block1.isProcedureParamBlock())
            return block2.isProcedureDeclBlock();
        else if (block2.isProcedureParamBlock())
            return block1.isProcedureDeclBlock();
        
        // and if you have nothing to do with procs,
        // go ahead and connect.
        else
            return true;
    }

    public boolean isMandatory() {
        return true;
    }
}
