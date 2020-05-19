package edu.mit.blocks.codeblocks;

import java.util.HashMap;

import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEvent;
import edu.mit.blocks.workspace.WorkspaceListener;
import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblocks.BlockConnector;
import edu.mit.blocks.codeblocks.BlockLink;
import edu.mit.blocks.codeblocks.BlockLinkChecker;
import edu.mit.blocks.codeblocks.LinkRule;

public class StackRule implements LinkRule, WorkspaceListener {

    private final Workspace workspace;
  
	private static HashMap<Long, Long> topBlock;
	private static HashMap<Long, String> stackType;
	
	public StackRule(Workspace workspace) {
		this.workspace = workspace;
		topBlock = new HashMap<Long, Long>();
		stackType = new HashMap<Long, String>();
	}
	
	public boolean canLink(Block block1, Block block2, BlockConnector socket1, BlockConnector socket2) {
		String type1 = stackType.get(getTopBlock(block1.getBlockID()));
		String type2 = stackType.get(getTopBlock(block2.getBlockID()));
		if ((socket1 == block1.getBeforeConnector() || (block1.getBeforeConnector() == null && socket1 == block1.getPlug())) && socket2 == getInlineStackSocket(block2))
			type2 = block2.getProperty("inline-stack-type");
		else if ((socket2 == block2.getBeforeConnector() || (block2.getBeforeConnector() == null && socket2 == block2.getPlug())) && socket1 == getInlineStackSocket(block1)) {
			type1 = block1.getProperty("inline-stack-type");
		}
		return (type1 == null || type2 == null || type1.startsWith(type2) || type2.startsWith(type1));
	}

	public boolean isMandatory() {
		return true;
	}

	public void workspaceEventOccurred(WorkspaceEvent e) {
		BlockLink link = e.getSourceLink();
		if (e.getEventType() == WorkspaceEvent.BLOCKS_CONNECTED) {
			makeStack(getTopBlock(link.getSocketBlockID()));
		}
		else if (e.getEventType() == WorkspaceEvent.BLOCKS_DISCONNECTED) {
			makeStack(getTopBlock(link.getSocketBlockID()));
			makeStack(link.getPlugBlockID());
		}
	}
	
	private static BlockConnector getInlineStackSocket(Block b) {
		if (b.getProperty("inline-stack-type") == null)
			return null;
		String inline = b.getProperty("inline-arg");
		if (inline == null)
			return null;
		return b.getSocketAt(Integer.parseInt(inline));
	}
	
	private void makeStack(Long blockID) {
		String type = getStackType(blockID);
		stackType.put(blockID, type);  
	}
	
	public String getStackType(Long blockID) {
		return findStackType(blockID, blockID, "");
	}

	private String findStackType(Long topID, Long startID, String currentType) {
		if (startID == Block.NULL)
			return "";
		topBlock.put(startID, topID);
		Block b = workspace.getEnv().getBlock(startID);
		String type = b.getProperty("stack-type");
		if (type != null && type.length() > currentType.length())
			currentType = type;
		BlockConnector inlineSocket = getInlineStackSocket(b);
		for (BlockConnector socket : BlockLinkChecker.getSocketEquivalents(b)) {
			if (socket == inlineSocket && socket.hasBlock()) {
				long blockID = socket.getBlockID();
				stackType.put(blockID, findStackType(blockID, blockID, b.getProperty("inline-stack-type")));
			}
			else {
				type = findStackType(topID, socket.getBlockID(), currentType);
				if (type.length() > currentType.length())
					currentType = type;
			}
		}
		return currentType;
	}
	
	private Long getTopBlock(Long blockID) {
		if (!topBlock.containsKey(blockID)) {
			makeStack(findTopBlock(blockID));
		}
		return topBlock.get(blockID);
	}
	
	private Long findTopBlock(Long blockID) {
		BlockConnector plug = BlockLinkChecker.getPlugEquivalent(workspace.getEnv().getBlock(blockID));
		if (plug == null || !plug.hasBlock())
			return blockID;
		return findTopBlock(plug.getBlockID());
	}
}
