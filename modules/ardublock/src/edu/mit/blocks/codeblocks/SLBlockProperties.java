package edu.mit.blocks.codeblocks;

import edu.mit.blocks.renderable.RenderableBlock;
import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblocks.BlockStub;
//import static slcodeblocks.SLCommand.*;

/**
 * <code>SLBlockProperties</code> holds final static String constants of named Starlogo TNG
 * specific Block properties.  
 */
public class SLBlockProperties {
	
	private static Workspace workspace;
	
    public final static String YES = "yes";

    /** Value corresponds to the vm command name its corresponding Block */
    public final static String VM_COMMAND_NAME = "vm-cmd-name";
    
    public final static String CMD_EVAL_PROCEDURE = "eval-procedure";
    
    /** The inline-arg property. */
    public final static String INLINE_ARG = "inline-arg";

    /** The socket that contains an ask-agent command. */
    public final static String ASK_AGENT_ARG = "ask-arg";
    
    /** Denotes the scope of its corresponding Block
     * kinds of scope: agent, global, patch */
    public final static String TYPE = "type";
    public final static String SCOPE = "scope";
    public final static String SCOPE_AGENT = "agent";
    public final static String SCOPE_GLOBAL = "global";
    public final static String SCOPE_PATCH = "patch";
    public final static String SCOPE_LOCAL = "local";
    
    /** the String breed name associated with a block*/
    public final static String BREED_NAME = "breed-name";
    
    public final static String INCLUDE_BREED = "include-breed";
    public final static String INCLUDE_BREED_SHAPE = "include-breed-shape";
    
    /** The floating point minimum value of RuntimeSliders. */
    public final static String BOUNDING_MIN = "bounding-min";
    
    /** The floating point maximum value of RuntimeSliders. */
    public final static String BOUNDING_MAX = "bounding-max";
    
    /** The floating point abstract value of RuntimeSliders. */
    public final static String BOUNDING_VALUE = "bounding-value";
    
    public final static String STACK_TYPE = "stack-type";
    public final static String STACK_MONITOR = "monitor";
    public final static String STACK_SETUP = "setup";
    public final static String STACK_BREED = "breed";
    public final static String STACK_BREED_FOREVER = "breed-forever";
    
    public final static String SPECIAL_VAL = "special-value";
    
    public final static String KIND_CMD = "cmd";
    public final static String KIND_NUMBER = "number";
    public final static String KIND_STRING = "string";
    
    /* SPECIAL BLOCK IDENTIFIERS ======================== */
    
    public final static String IS_MONITORABLE = "is-monitorable";
    public final static String IS_SETUP = "is-setup";
    public final static String IS_COLLISION = "is-collision";
    public final static String IS_MONITOR = "is-monitor";
    public final static String IS_SLIDER = "is-slider";
    public final static String IS_BAR_GRAPH = "is-bar-graph";
    public final static String IS_LINE_GRAPH = "is-line-graph";
    public final static String IS_TABLE = "is-table";
    public final static String IS_RUNTIME_BAR_GRAPH = "is-runtime-bar-graph";
    public final static String IS_RUNTIME_LINE_GRAPH = "is-runtime-line-graph";
    public final static String IS_RUNTIME_TABLE = "is-runtime-table";
    public final static String RUNTIME_TYPE = "runtime-type";
    public final static String HAS_RUNTIME_EQUIVALENT = "has-runtime-equiv"; 
    public final static String IS_SPECIAL_VAR = "is-special-variable";
    
    /** Flag set to determine if a block is assoicated with a particular breed. */
    public final static String IS_OWNED_BY_BREED = "is-owned-by-breed";
    
    /** Flag set to determine if a block's label is determined by the page the 
     * block was originally dropped  */
    public final static String IS_BREED_SET_BY_CANVAS = "is-breed-set-by-canvas";
    
    /**
     * The possible block runtime types.
     */
    public static enum RuntimeType {
        FOREVER ("forever"), 
        RUNONCE ("runonce"), 
        RUNFORSOMETIME ("runforsometime"),
        NULL_RUNTIME ("");  // used as a marker to prevent NPEs
        
        private final String myString;
        private RuntimeType(String s) { myString = s; }
        public String getString() { return myString; }
        
        public static RuntimeType getRuntime(String s) {
            for (RuntimeType t : values()) {
                if (t.myString.equals(s)) {
                    return t;
                }
            }
            
            return NULL_RUNTIME;
        }
    }
    
    /* Helper methods ============================== */
    
    /** Returns true if "vm-cmd-name" is given cmd */
    public static boolean isCmd(String cmd, Block b) {
        return b != null && cmd.equals(b.getProperty(VM_COMMAND_NAME));
    }
    
    /** Returns true if "stack-type" property = "monitor" */
    public static boolean isMonitor(Block b) {
        return b != null && STACK_MONITOR.equals(b.getProperty(STACK_TYPE));
    }
    
    /** returns true if "vm-command-name" property = "eval-procedure" */
    public static boolean isProcedureCall(Block b) {
        return b != null && 
               CMD_EVAL_PROCEDURE.equals(b.getProperty(VM_COMMAND_NAME)) &&
               b instanceof BlockStub;
    }
    
    /** Returns true if this is a collision block. */
    public static boolean isCollision(Long blockID) {
        return hasProperty(workspace.getEnv().getBlock(blockID), IS_COLLISION);
    }
    
    /** Returns true if the given property is set in this block */
    public static boolean hasProperty(Block b, String prop) {
        return b != null && b.getProperty(prop) != null;
    }
    
    /** Return the runtime type of the block. */
    public static RuntimeType getRuntimeType(Block b) {
        return RuntimeType.getRuntime(b.getProperty(RUNTIME_TYPE));
    }
    
/*    *//** 
     * Returns the breed type of the current block. If the breed is NOT 
     * currently registered with BreedManager, throws a CompilerException. 
     *//*
    public static String getBreed(Block b) throws CompilerException {
        String breed = b.getProperty(SLBlockProperties.BREED_NAME);
        //TODO this is a hack until the breed property returns the 
        // right breed for breed-string blocks
        //TODO Remove the next two lines once it works
        if (isCmd(CMD_NOP, b))
            breed = b.getBlockLabel().substring(7);
        
        if (!BreedManager.isExistingBreed(breed)) { 
            throw new CompilerException(CompilerException.Error.CUSTOM,
                b.getBlockID(),
                "Unrecognized breed: " + breed);
        }
        return breed;
    }*/
    
    /** Returns the inline-arg property, or -1 if not set. */
    public static int getInlineArg(Block b) {
        String inlineArg = b.getProperty(INLINE_ARG);
        return (inlineArg == null) ? -1 : Integer.parseInt(inlineArg);
    }
    
    /** Returns parent of stub, or null if parent does not exist or if b is not a stub. */
    public static Block getParent(Block b) {
        if (b instanceof BlockStub) {
            Block parent = ((BlockStub) b).getParent();
            if (parent != null && 
            		workspace.getEnv().getRenderableBlock(parent.getBlockID()).getParentWidget() != null) 
            {
                return parent;
            }
        }
            
        return null;
    }
    
    /**
     * Returns the top block in the stack, or null if this is not a
     * Forever, RunForSomeTime, or Procedure stack.
     */
    public static Long getTopBlockID(Long blockID) {
    	if (blockID == null || Block.NULL.equals(blockID)  || workspace.getEnv().getBlock(blockID) == null)
			return null;
        Block b = workspace.getEnv().getBlock(blockID);
        if (b.hasBeforeConnector()) return getTopBlockID(b.getBeforeBlockID());
        if (b.hasPlug()) return getTopBlockID(b.getPlugBlockID());
    
        // b is the top block, but is it a valid type?
        if (b.isProcedureDeclBlock() || hasProperty(b, IS_COLLISION)) return blockID;
        return isForeverRunBlock(blockID) ? blockID : null;
    }
    
    /**
     * Returns true if this is the last command block in a stack. DO NOT 
     * pass in a null block. DO NOT pass in a data block.
     */
    public static boolean terminatesStack(Block b) {
        if (!b.getAfterBlockID().equals(Block.NULL)) return false;
        
        // Otherwise, check that any enclosing parents are also the last
        // in their stacks.
        Block before = workspace.getEnv().getBlock(b.getBeforeBlockID());
        Long id = b.getBlockID();
        while (before != null) {
            if (!before.getAfterBlockID().equals(id)) {
                return terminatesStack(before);
            }
            id = before.getBlockID();
            before = workspace.getEnv().getBlock(before.getBeforeBlockID());
        }
        
        return true;
    }
    
    /**
     * Returns true if the given block is a forever-run block 
     * (forever, runforsometime, runonce).
     */
    public static boolean isForeverRunBlock(Long blockID) {
        if (blockID.equals(Block.NULL)) return false;
        RuntimeType rt = getRuntimeType(workspace.getEnv().getBlock(blockID));
        return rt == RuntimeType.FOREVER || rt == RuntimeType.RUNFORSOMETIME ||
               rt == RuntimeType.RUNONCE;
    }
}
