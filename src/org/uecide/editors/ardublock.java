package org.uecide.editors;

import org.uecide.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import com.ardublock.core.*;
import com.ardublock.ui.*;
import com.ardublock.ui.listener.*;
import com.ardublock.translator.*;
import com.ardublock.translator.block.exception.*;
import edu.mit.blocks.workspace.*;
import edu.mit.blocks.controller.*;
import edu.mit.blocks.renderable.*;
import edu.mit.blocks.codeblocks.*;

public class ardublock extends JPanel implements EditorBase {

    Editor editor;
    File file;
    Sketch sketch;
    Workspace workspace;

    com.ardublock.core.Context context;

    public ardublock(Sketch s, File f, Editor e) {
        sketch = s;
        file = f;
        editor = e;
    
        this.setLayout(new BorderLayout());

        if (f == null) return;

        context = com.ardublock.core.Context.getContext();
        workspace = context.getWorkspace();
        add(workspace, BorderLayout.CENTER);

		workspace.addWorkspaceListener(new WorkspaceListener() {
            public void workspaceEventOccurred(WorkspaceEvent e) {
                context.setWorkspaceChanged(true);
                context.resetHightlightBlock();
            }
        });

        try {
            context.loadArduBlockFile(file);
            context.setWorkspaceChanged(false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    



    public boolean isModified() { return context.isWorkspaceChanged(); }

    public String getText() { return generateCode(context, editor); }

    public void setText(String text) {}

    public void setModified(boolean m) { context.setWorkspaceChanged(m); }

    public File getFile() { return file; }
    public void populateMenu(JMenu menu, int flags) {}
    public void populateMenu(JPopupMenu menu, int flags) {}

    public boolean save() { 
        try {
            String saveString = getArduBlockString();
            context.saveArduBlockFile(file, saveString);
            context.setWorkspaceChanged(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    String getArduBlockString() {
        return context.getWorkspaceController().getSaveString();
    }

    public boolean saveTo(File f) { 
        file = f;
        return save();
    }

    public void reloadFile() { 
        try {
            context.loadArduBlockFile(file);
            context.setWorkspaceChanged(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestFocus() {}
    public void insertAtCursor(String text) {}
    public void insertAtStart(String text) {}
    public void insertAtEnd(String text) {}
    public void refreshSettings() {}
    public String getSelectedText() { return ""; }
    public void setSelectedText(String text) {}
    public int getSelectionStart() { return 0; }
    public int getSelectionEnd() { return 0; }
    public void setSelection(int start, int end) {}
    public void highlightLine(int line, Color color) {}
    public void clearHighlights() {}
    public void gotoLine(int line) {}
    public int getCursorPosition() { return 0; }
    public void setCursorPosition(int pos) {}
    public void removeAllFlags() {}
    public void flagLine(int line, Icon icon, int group) {}
    public void removeFlag(int line) {}
    public void removeFlagGroup(int group) {}
    public void clearKeywords() {}
    public void addKeyword(String name, int type) {}
    public void repaint() {}
    public Component getContentPane() { return null; }
    public Rectangle getViewRect() { return null; }
    public void setViewPosition(Point p) {}
    public boolean getUpdateFlag() { return false; }

    public static String emptyFile() {
        StringBuilder o = new StringBuilder();
        o.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        o.append("<cb:CODEBLOCKS xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://education.mit.edu/openblocks/ns http://education.mit.edu/openblocks/codeblocks.xsd\" xmlns:cb=\"http://education.mit.edu/openblocks/ns\">\n");
        o.append("<Pages collapsible-pages=\"yes\" drawer-with-page=\"yes\">\n");
        o.append("<Page page-color=\"128 128 128\" page-drawer=\"Main\" page-infullview=\"yes\" page-name=\"Main\" page-width=\"2448\">\n");
        o.append("<PageBlocks>\n");
        o.append("<Block genus-name=\"loop\" id=\"476\">\n");
        o.append("<Location>\n");
        o.append("<X>100</X>\n");
        o.append("<Y>100</Y>\n");
        o.append("</Location>\n");
        o.append("<Sockets num-sockets=\"1\">\n");
        o.append("<BlockConnector connector-kind=\"socket\" connector-type=\"cmd\" init-type=\"cmd\" label=\"loop\" position-type=\"single\"/>\n");
        o.append("</Sockets>\n");
        o.append("</Block>\n");
        o.append("</PageBlocks>\n");
        o.append("</Page>\n");
        o.append("</Pages>\n");
        o.append("</cb:CODEBLOCKS>\n");

        return o.toString();
    }


    public static String generateCode(com.ardublock.core.Context context, Editor editor) {
		boolean success;
		success = true;
        Workspace workspace = context.getWorkspace();
		Translator translator = new Translator(workspace);
		translator.reset();
		
		Iterable<RenderableBlock> renderableBlocks = workspace.getRenderableBlocks();
		
		Set<RenderableBlock> loopBlockSet = new HashSet<RenderableBlock>();
		Set<RenderableBlock> subroutineBlockSet = new HashSet<RenderableBlock>();
		Set<RenderableBlock> scoopBlockSet = new HashSet<RenderableBlock>();
		Set<RenderableBlock> guinoBlockSet = new HashSet<RenderableBlock>();
		StringBuilder code = new StringBuilder();
		
		
		for (RenderableBlock renderableBlock:renderableBlocks) {
			Block block = renderableBlock.getBlock();
			
			if (block.getGenusName().equals("DuinoEDU_Guino_Read")) {
				translator.setGuinoProgram(true);
				
			}
			if ((block.getGenusName().equals("DuinoEDU_Guino_Title")) || (block.getGenusName().equals("DuinoEDU_Guino_Slider")) || (block.getGenusName().equals("DuinoEDU_Guino_column")) || (block.getGenusName().equals("DuinoEDU_Guino_switch"))|| (block.getGenusName().equals("DuinoEDU_Guino_pause")) ) {
				translator.setGuinoProgram(true);
				
			}
			
			
			if (!block.hasPlug() && (Block.NULL.equals(block.getBeforeBlockID()))) {
				
				if(block.getGenusName().equals("loop")) {
					loopBlockSet.add(renderableBlock);
				}
				if(block.getGenusName().equals("loop1")) {
					loopBlockSet.add(renderableBlock);
				}
				if(block.getGenusName().equals("loop2")) {
					loopBlockSet.add(renderableBlock);
				}
				if(block.getGenusName().equals("loop3")) {
					loopBlockSet.add(renderableBlock);
				}
				if(block.getGenusName().equals("program")) {
					loopBlockSet.add(renderableBlock);
				}
				if(block.getGenusName().equals("setup")) {
					loopBlockSet.add(renderableBlock);
				}
				if (block.getGenusName().equals("subroutine")) {
					String functionName = block.getBlockLabel().trim();
					try {
						translator.addFunctionName(block.getBlockID(), functionName);
					} catch (SubroutineNameDuplicatedException e1) {
						context.highlightBlock(renderableBlock);
						//find the second subroutine whose name is defined, and make it highlight. though it cannot happen due to constraint of OpenBlocks -_-
                        if (editor != null) editor.error(e1);
						return "";
					}
					subroutineBlockSet.add(renderableBlock);
				}
				if (block.getGenusName().equals("subroutine_var")) {
					String functionName = block.getBlockLabel().trim();
					try {
						translator.addFunctionName(block.getBlockID(), functionName);
					} catch (SubroutineNameDuplicatedException e1) {
						context.highlightBlock(renderableBlock);
						//find the second subroutine whose name is defined, and make it highlight. though it cannot happen due to constraint of OpenBlocks -_-
                        if (editor != null) editor.error(e1);
						return "";
					} subroutineBlockSet.add(renderableBlock);
				}
				if (block.getGenusName().equals("scoop_task")) {
					translator.setScoopProgram(true);
					scoopBlockSet.add(renderableBlock);
				}
				if (block.getGenusName().equals("scoop_loop")) {
					translator.setScoopProgram(true);
					scoopBlockSet.add(renderableBlock);
				}
				if (block.getGenusName().equals("scoop_pin_event")) {
					translator.setScoopProgram(true);
					scoopBlockSet.add(renderableBlock);
				}
				
			}
		}
		if (loopBlockSet.size() == 0) {
            if (editor != null) editor.error("No loop block found!");
			return "";
		}
		if (loopBlockSet.size() > 1) {
			for (RenderableBlock rb : loopBlockSet) {
				context.highlightBlock(rb);
			}
            if (editor != null) editor.error("Multiple loop blocks found!");
			return "";  
		}

		try {
			
			for (RenderableBlock renderableBlock : loopBlockSet) {
				translator.setRootBlockName("loop");
				Block loopBlock = renderableBlock.getBlock();
				code.append(translator.translate(loopBlock.getBlockID()));
			}
			
			for (RenderableBlock renderableBlock : scoopBlockSet) {
				translator.setRootBlockName("scoop");
				Block scoopBlock = renderableBlock.getBlock();
				code.append(translator.translate(scoopBlock.getBlockID()));
			}
			for (RenderableBlock renderableBlock : guinoBlockSet) {
				translator.setRootBlockName("guino");
				Block guinoBlock = renderableBlock.getBlock();
				code.append(translator.translate(guinoBlock.getBlockID()));
			}
			
			
			for (RenderableBlock renderableBlock : subroutineBlockSet) {
				translator.setRootBlockName("subroutine");
				Block subroutineBlock = renderableBlock.getBlock();
				code.append(translator.translate(subroutineBlock.getBlockID()));
			}
			
			translator.beforeGenerateHeader();
			code.insert(0, translator.genreateHeaderCommand());
		} catch (SocketNullException e1) {
			e1.printStackTrace();
			success = false;
			Long blockId = e1.getBlockId();
			Iterable<RenderableBlock> blocks = workspace.getRenderableBlocks();
			for (RenderableBlock renderableBlock2 : blocks) {
				Block block2 = renderableBlock2.getBlock();
				if (block2.getBlockID().equals(blockId)) {
					context.highlightBlock(renderableBlock2);
					break;
				}
			}
            if (editor != null) editor.error(e1);
		} catch (BlockException e2) {
			e2.printStackTrace();
			success = false;
			Long blockId = e2.getBlockId();
			Iterable<RenderableBlock> blocks = workspace.getRenderableBlocks();
			for (RenderableBlock renderableBlock2 : blocks) {
				Block block2 = renderableBlock2.getBlock();
				if (block2.getBlockID().equals(blockId)) {
					context.highlightBlock(renderableBlock2);
					break;
				}
			}
            if (editor != null) editor.error(e2);
		} catch (SubroutineNotDeclaredException e3) {
			e3.printStackTrace();
			success = false;
			Long blockId = e3.getBlockId();
			Iterable<RenderableBlock> blocks = workspace.getRenderableBlocks();
			for (RenderableBlock renderableBlock3 : blocks) {
				Block block2 = renderableBlock3.getBlock();
				if (block2.getBlockID().equals(blockId)) {
					context.highlightBlock(renderableBlock3);
					break;
				}
			}
            if (editor != null) editor.error(e3);
			
		}
		
		if (success) {
			AutoFormat formatter = new AutoFormat();
			String codeOut = code.toString();
			
			if (context.isNeedAutoFormat) {
				codeOut = formatter.format(codeOut);
			}
			
			context.didGenerate(codeOut);
            return codeOut;
		}
        return "";

	}

    

}
