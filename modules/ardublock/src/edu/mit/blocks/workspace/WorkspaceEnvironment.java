package edu.mit.blocks.workspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblocks.BlockGenus;
import edu.mit.blocks.renderable.RenderableBlock;

/**
 *
 * For a given {@link Workspace}, a {@link WorkspaceEnvironment} stores a reference to all instances of :
 * {@link Block}
 * {@link RenderableBlock}
 * {@link BlockStub}
 * {@link BlockGenus}
 *
 * This enables to separate the components of each workspace, and so to use several at the same time.
 * @author laurentschall
 *
 */

public class WorkspaceEnvironment {

	private final Map<Long, RenderableBlock> allRenderableBlocks = new HashMap<Long, RenderableBlock>();

    // RenderableBlock

    /**
     * Returns the Renderable specified by blockID; null if RenderableBlock does not exist
     * @param blockID the block id of the desired RenderableBlock
     * @return the Renderable specified by blockID; null if RenderableBlock does not exist
     */
    public RenderableBlock getRenderableBlock(Long blockID) {
        return this.allRenderableBlocks.get(blockID);
    }

    public void addRenderableBlock(RenderableBlock block) {
    	this.allRenderableBlocks.put(block.getBlockID(), block);
    }


    // Block

    private final Map<Long, Block> allBlocks = new HashMap<Long, Block>();
    private long nextBlockID = 1;

    public Block getBlock(Long blockID) {
        return this.allBlocks.get(blockID);
    }

    public void addBlock(Block block) {

    	long id = block.getBlockID();

        if (this.allBlocks.containsKey(id)) {
            Block dup = this.allBlocks.get(id);
            System.out.println("pre-existing block is: " + dup + " with genus " + dup.getGenusName() + " and label " + dup.getBlockLabel());
            assert !this.allBlocks.containsKey(id) : "Block id: " + id + " already exists!  BlockGenus " + block.getGenusName() + " label: " + block.getBlockLabel();
        }

    	this.allBlocks.put(id, block);
    }

    public long getNextBlockID() {
    	return this.nextBlockID++;
    }
    
    public void setNextBlockID(long blockID) {
    	nextBlockID = blockID;
    }

    // BlockStubs

    /** STUB HASH MAPS
     * key: parentName + parentGenus
     *
     * Key includes both parentName and parentGenus because the names of two parents
     * may be the same if they are of different genii
     * blockids of parents are not used as a reference because parents and stubs are
     * connected by the parentName+parentGenus information not the blockID.  This
     * connection is more apparent when the parent Block is removed/deleted.  The stubs
     * become dangling references. These stubs are resolved when a new parent block is
     * created with the previous parent's name.
     * */
    private final HashMap<String, Long> parentNameToParentBlock = new HashMap<String, Long>();
    private final HashMap<String, ArrayList<Long>> parentNameToBlockStubs = new HashMap<String, ArrayList<Long>>();

    public Long getParentBlockID(String parentName) {
    	return this.parentNameToParentBlock.get(parentName);
    }

    public void putParentBlock(String parentName, Long parentBlockID) {
    	this.parentNameToParentBlock.put(parentName, parentBlockID);
    }

    public void removeParentBlock(String parentName) {
    	this.parentNameToParentBlock.remove(parentName);
    }

    public boolean containsParentBlock(String parentName) {
    	return this.parentNameToParentBlock.containsKey(parentName);
    }

    public ArrayList<Long> getBlockStubs(String parentName) {
    	return this.parentNameToBlockStubs.get(parentName);
    }

    public void putBlockStubs(String parentName, ArrayList<Long> blockStubs) {
    	this.parentNameToBlockStubs.put(parentName, blockStubs);
    }

    public boolean containsBlockStubs(String parentName) {
    	return this.parentNameToBlockStubs.containsKey(parentName);
    }

    public void removeBlockStubs(String parentName) {
    	this.parentNameToBlockStubs.remove(parentName);
    }

    public void resetAll() {

        //RenderableBlock.reset();
        this.allRenderableBlocks.clear();

        //BlockUtilities.reset();

        //Block.reset();
        this.allBlocks.clear();
        this.nextBlockID = 1;

        //BlockStub.reset();
        this.parentNameToParentBlock.clear();
        this.parentNameToBlockStubs.clear();
    }

    // BlockGenuses
    
    private Map<String, BlockGenus> nameToGenus = new HashMap<String, BlockGenus>();
    
    /**
     * Returns the BlockGenus with the specified name; null if this name does not exist
     * @param name the name of the desired BlockGenus  
     * @return the BlockGenus with the specified name; null if this name does not exist
     */
    
    public BlockGenus getGenusWithName(String name) {
        return nameToGenus.get(name);
    }
    
    public void addBlockGenus(BlockGenus genus) {
    	nameToGenus.put(genus.getGenusName(), genus);
    }
    
    /**
     * Resets all the Block Genuses of current language.
     */
    public void resetAllGenuses() {
        nameToGenus.clear();
    }

    //add by HE Qichen 20120126
    public Iterable<RenderableBlock> getRenderableBlocks()
    {
    	return allRenderableBlocks.values();
    }
}
