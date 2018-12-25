package edu.mit.blocks.workspace.typeblocking;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.typeblocking.TypeBlockManager.Direction;

/**
 * The KeyInputMap manages the processing of particular keys
 * and delegates the task of performing an action to the
 * TypeBlockManager.
 *
 * In a sense, the KeyInputMap assists in mapping a character to
 * an Action.
 */
public class KeyInputMap {

    private static boolean DEFAULT_ENABLED = true;
    private static final Map<Character, String[]> defaultInputMap = new HashMap<Character, String[]>();
    private static final Map<Character, String[]> customInputMap = new HashMap<Character, String[]>();

    private KeyInputMap() {
    }

    /**
     * Enables this if and only if enable == true.
     * By enabling the default key mapping, generic key to
     * genus mappings are used.
     * @param enable
     */
    protected static void enableDefaultKeyMapping(boolean enable) {
        KeyInputMap.DEFAULT_ENABLED = enable;
        char[] key = {KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2,
            KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
            KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8,
            KeyEvent.VK_9, '+', //process negative signs uniquely
            '>', '<', '*',
            '/', '=', '"',
            'x', 'X', '|',
            '!', '&', '^'};

        String[][] value = {{"number", "0"}, {"number", "1"}, {"number", "2"},
            {"number", "3"}, {"number", "4"}, {"number", "5"},
            {"number", "6"}, {"number", "7"}, {"number", "8"},
            {"number", "9"}, {"sum", null}, //process negative signs uniquely
            {"greaterthan", null}, {"lessthan", null}, {"product", null},
            {"quotient", null}, {"equals", null}, {"string", null},
            {"product", null}, {"product", null}, {"or", null},
            {"not", null}, {"and", null}, {"power", null}};

        for (int i = 0; i < key.length; i++) {
            defaultInputMap.put(key[i], value[i]);
        }
    }

    /**
     * Processes user-generated keyEvent. This method
     * is intended to react in one of three ways:
     * 		1.  It should traverse the block canvas; that is,
     * 			it should move the cursor the the next appropriate
     * 			block.
     * 		2.  It should display a graphical interface (also called
     * 			the "View" interface of TypeBlockManager) to
     * 			assist the user in selecting the a pre-defined
     * 			pattern.
     * 		3.  It should parse the key event and drop down the
     * 			appropriate block as well as make the needed
     * 			connections.
     *
     * Both Key_Pressed and Kep_Types events
     * must pass through here
     * @requires key!=null
     * @param workspace The workspace in use
     * @param key
     */
    public static void processKeyChar(Workspace workspace, KeyEvent key) {

//=====================================================================
//======Process COUPLED Virtual Key Modifers + ALPHANUMERIC character
//=====================================================================
        //handle virtual modifiers: CONTROL DOWN or COMMAND (FOR MAC) DOWN
        if ((key.getModifiersEx() & Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()) != 0) {
            switch (key.getKeyCode()) {
                case KeyEvent.VK_C:
                    //This is already accounted for the SLMenu
                    //TypeBlockManager.automateCopyBlock();
                    return;
                case KeyEvent.VK_V:
                    //This is already accounted for the SLMenu
                    //TypeBlockManager.automatePasteBlock();
                    return;
                case KeyEvent.VK_A:
                    TypeBlockManager.automateCopyAll(workspace);
                    return;
                default:  //ALL OTHER COMMANDS RETURN
                    return;

            }
        }

        //handle virtual modifiers: ALT DOWN
        if (key.isAltDown()) {
            //do nothing
        }

        //handle virtual modifers: SHIFT DOWN
        if (key.isShiftDown()) {
            if (key.getKeyCode() == KeyEvent.VK_TAB) {
                TypeBlockManager.automateFocusTraversal(workspace, Direction.LEFT);
                return;
            }
        }


//=====================================================================
//======Process ISOLATED Virtual Key Input (key modifiers)
//=====================================================================
        switch (key.getKeyCode()) {
            case KeyEvent.VK_DELETE:
                workspace.getTypeBlockManager().automateBlockDeletion(workspace);
                return;
            case KeyEvent.VK_BACK_SPACE:
                workspace.getTypeBlockManager().automateBlockDeletion(workspace);
                return;
            case KeyEvent.VK_DOWN:
                TypeBlockManager.automateFocusTraversal(workspace, Direction.DOWN);
                return;
            case KeyEvent.VK_UP:
                TypeBlockManager.automateFocusTraversal(workspace, Direction.UP);
                return;
            case KeyEvent.VK_LEFT:
                TypeBlockManager.automateFocusTraversal(workspace, Direction.LEFT);
                return;
            case KeyEvent.VK_RIGHT:
                TypeBlockManager.automateFocusTraversal(workspace, Direction.RIGHT);
                return;
            case KeyEvent.VK_ESCAPE:
                TypeBlockManager.automateFocusTraversal(workspace, Direction.ESCAPE);
                return;
            case KeyEvent.VK_ENTER:
                TypeBlockManager.automateFocusTraversal(workspace, Direction.ENTER);
                return;
            case KeyEvent.VK_SHIFT:
                return;
            case KeyEvent.VK_CONTROL:
                return;
            case KeyEvent.VK_TAB:
                TypeBlockManager.automateFocusTraversal(workspace, Direction.RIGHT);
                return;
            default:
                break;
        }

//=====================================================================
//======Process Default Key Input Mappings
//=====================================================================
        //process default key input mappings if and ONLY if
        //default_enabled is set to true;
        if (KeyInputMap.DEFAULT_ENABLED) {
            //for the special negative sign
            if (key.getKeyChar() == '-') {
                workspace.getTypeBlockManager().automateNegationInsertion(workspace);
                return;
            }
            if (key.getKeyChar() == 'x' || key.getKeyChar() == 'X') {
                workspace.getTypeBlockManager().automateMultiplication(workspace, key.getKeyChar());
                return;
            }
            if (key.getKeyChar() == '+') {
                workspace.getTypeBlockManager().automateAddition(workspace, key.getKeyChar());
                return;
            }
            //For all other special default input mappings
            for (Character keyChar : KeyInputMap.defaultInputMap.keySet()) {
                if (keyChar.equals(key.getKeyChar())) {
                    workspace.getTypeBlockManager().automateBlockInsertion(
                            workspace,
                            KeyInputMap.defaultInputMap.get(keyChar)[0],
                            KeyInputMap.defaultInputMap.get(keyChar)[1]);
                    return;
                }
            }
        }

//=====================================================================
//======Process Custom Key Input Mappings
//=====================================================================
        for (Character keyChar : KeyInputMap.customInputMap.keySet()) {
            if (keyChar.equals(key.getKeyChar())) {
                workspace.getTypeBlockManager().automateBlockInsertion(
                        workspace,
                        KeyInputMap.customInputMap.get(keyChar)[0],
                        KeyInputMap.customInputMap.get(keyChar)[1]);
                return;
            }
        }

//=====================================================================
//======Process AlphaNumeric Key Inputs
//=====================================================================
        if (Character.isLetterOrDigit(key.getKeyChar())) {
            workspace.getTypeBlockManager().automateAutoComplete(workspace, key.getKeyChar());
        } // takes care of the +, -, = ... when not set to automated block placements
        else {
            workspace.getTypeBlockManager().automateAutoComplete(workspace, key.getKeyChar());
        }
    }
}
