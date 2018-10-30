package edu.mit.blocks.codeblocks;

import java.awt.Color;
import java.awt.Image;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ResourceBundle;
import java.util.MissingResourceException;

import javax.swing.ImageIcon;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.blocks.renderable.BlockImageIcon;
import edu.mit.blocks.renderable.BlockImageIcon.ImageLocation;
import edu.mit.blocks.workspace.Workspace;
import edu.mit.blocks.workspace.WorkspaceEnvironment;

/**
 * A genus describes the properties that define a block.  For example, fd is a block genus
 * that describes all forward block instances in Starlogo.  The BlockGenus class stores all the immutable
 * properties and socket information of a genus.
 */
public class BlockGenus {

    private static final String EMPTY_STRING = "";

    private final WorkspaceEnvironment env;
   
    private String genusName;
    private Color color;
    private String kind;
    private String initLabel;
    private String labelPrefix = EMPTY_STRING;
    private String labelSuffix = EMPTY_STRING;
    private String blockDescription;

    private boolean isStarter;
    private boolean isTerminator;
    private boolean isLabelEditable;
    private boolean labelMustBeUnique;
    private boolean isLabelValue;
    private boolean isPageLabelEnabled;
    private boolean hasDefArgs;
    private boolean areSocketsExpandable;
    //is this genus an infix operater - checks if it has two bottom sockets
    private boolean isInfix;

    //connector information
    private BlockConnector plug = null;
    private List<BlockConnector> sockets = new ArrayList<BlockConnector>();
    private BlockConnector before = null;
    private BlockConnector after = null;

    //list of family genuses names (i.e. fd, bk are in the same family)
    private List<String> familyList = new ArrayList<String>();

    //list of type of stub genuses this genus has
    private List<String> stubList = new ArrayList<String>();

    //mapping of ImageLocations to BlockImageIcons.  only one BlockImageIcon instance per ImageLocation
    private Map<ImageLocation, BlockImageIcon> blockImageMap = new HashMap<ImageLocation, BlockImageIcon>();

    //hashmap of language specific properties.
    private Map<String, String> properties = new HashMap<String, String>();

    //Set of argument index to desciptions.
    //Argument index to argument description relationship
    //may not be inferred as one-to-one
    private List<String> argumentDescriptions = new ArrayList<String>();

    /**
     * The expand-groups. A list is used instead of a map, because we don't
     * expect a lot of groups in one block.
     */
    private List<List<BlockConnector>> expandGroups = new ArrayList<List<BlockConnector>>();

    /**
     * Only BlockGenus can create BlockGenus objects, specifically only the function that loads
     * BlockGenuses information from the loadString can create BlockGenuses objects
     */
    private BlockGenus(WorkspaceEnvironment workspaceEnvironment) {
        env = workspaceEnvironment;
    }

    /**
     * Constructs a BlockGenus copy with the specified genusName
     * @param genusName
     */
    private BlockGenus(WorkspaceEnvironment workspaceEnvironment, String genusName, String newGenusName) {

        env = workspaceEnvironment;

        assert !genusName.equals(newGenusName) : "BlockGenuses must have unique names: " + genusName;

        BlockGenus genusToCopy = env.getGenusWithName(genusName);

        this.genusName = newGenusName;
        this.areSocketsExpandable = genusToCopy.areSocketsExpandable;
        this.color = new Color(genusToCopy.color.getRed(), genusToCopy.color.getGreen(), genusToCopy.color.getBlue());
        this.familyList = new ArrayList<String>(genusToCopy.familyList);
        this.hasDefArgs = genusToCopy.hasDefArgs;
        this.initLabel = new String(genusToCopy.initLabel);
        this.isLabelEditable = genusToCopy.isLabelEditable;
        this.isLabelValue = genusToCopy.isLabelValue;
        this.isStarter = genusToCopy.isStarter;
        this.isTerminator = genusToCopy.isTerminator;
        this.isInfix = genusToCopy.isInfix;
        this.kind = genusToCopy.kind;
        this.labelPrefix = genusToCopy.labelPrefix;
        this.labelSuffix = genusToCopy.labelSuffix;
        if (genusToCopy.plug != null) {
            this.plug = new BlockConnector(genusToCopy.plug);
        }
        if (genusToCopy.before != null) {
            this.before = new BlockConnector(genusToCopy.before);
        }
        if (genusToCopy.after != null) {
            this.after = new BlockConnector(genusToCopy.after);
        }
        this.properties = new HashMap<String, String>(genusToCopy.properties);
        this.sockets = new ArrayList<BlockConnector>(genusToCopy.sockets);
        this.stubList = new ArrayList<String>(genusToCopy.stubList);
        this.expandGroups = genusToCopy.expandGroups;   // doesn't change
    }

    /**
     * Returns the siblings of this genus.  If this genus has no siblings, returns an empty list.
     * Each element in the list is the block genus name of a sibling.
     * Note: For a genus to have siblings, its label must be uneditable.  An editable label
     * interferes with the drop down menu widget that blocks with siblings have.
     * @return the siblings of this genus.
     */
    public List<String> getSiblingsList() {
        return Collections.unmodifiableList(familyList);
    }

    /**
     * Returns true if this genus has siblings; false otherwise.
     * Note: For a genus to have siblings, its label must be uneditable.  An editable label
     * interferes with the drop down menu widget that blocks with siblings have.
     * @return true if this genus has siblings; false otherwise.
     */
    public boolean hasSiblings() {
        return (familyList.size() > 0);
    }

    /**
     * Returns a list of the stub kinds (or stub genus names) of this; if this genus does not have any stubs,
     * returns an empty list
     * @return a list of the stub kinds (or stub genus names) of this; if this genus does not have any stubs,
     * returns an empty list
     */
    public Iterable<String> getStubList() {
        return Collections.unmodifiableList(stubList);
    }

    /**
     * Returns true is this genus has stubs (references such as getters, setters, etc.); false otherwise
     * @return true is this genus has stubs (references such as getters, setters, etc.); false otherwise
     */
    public boolean hasStubs() {
        return (stubList.size() > 0);
    }

    /**
     * Returns true iff any one of the connectors for this genus has default arguments; false otherwise
     * @return true iff any one of the connectors for this genus has default arguments; false otherwise
     */
    public boolean hasDefaultArgs() {
        return hasDefArgs;
    }

    /**
     * Returns true if this block is a command block (i.e. forward, say, etc.); false otherwise
     * @return true if this block is a command block (i.e. forward, say, etc.); false otherwise
     */
    public boolean isCommandBlock() {
        return kind.equals("command");
    }

    /**
     * Returns true if this block is a data block a.k.a. a primitive (i.e. number, string, boolean);
     * false otherwise
     * @return Returns true if this block is a data block a.k.a. a primitive (i.e. number, string, boolean);
     * false otherwise
     */
    public boolean isDataBlock() {
        return kind.equals("data");
    }

    /**
     * Returns true iff this block is a function block, which takes in an input and produces an
     * output. (i.e. math blocks, arctan, add to list); false otherwise.
     * @return true iff this block is a function block, which takes in an input and produces an
     * output. (i.e. math blocks, arctan, add to list); false otherwise.
     */
    public boolean isFunctionBlock() {
        return kind.equals("function");
    }

    /**
     * Returns true if this block is a variable declaration block; false otherwise
     * @return true if this block is a variable declaration block; false otherwise
     */
    public boolean isVariableDeclBlock() {
        return kind.equals("variable");
    }

    /**
     * Returns true if this block is a procedure declaration block; false otherwise
     * @return true if this block is a procedure declaration block; false otherwise
     */
    public boolean isProcedureDeclBlock() {
        return kind.equals("procedure");
    }

    /**
     * Returns true if this block is a procedure parameter block; false otherwise
     */
    public boolean isProcedureParamBlock() {
        return kind.equals("param");
    }

    /**
     * Returns true if this genus is a declaration block.  Declaration blocks define variables and procedures.
     */
    public boolean isDeclaration() {
        return this.isVariableDeclBlock() || this.isProcedureDeclBlock();
    }

    /**
     * Returns true if this block is a list or a list operator (determined by whether it has at
     * least one list connector of any type); false otherwise.
     * @return is determined by whether it has at least one list connector of any type.
     */
    public boolean isListRelated() {
        boolean hasListConn = false;
        if (plug != null) {
            hasListConn = plug.getKind().contains("list");
        }
        for (BlockConnector socket : sockets) {
            hasListConn |= socket.getKind().contains("list");
        }
        return hasListConn;
    }

    /**
     * Returns true if this genus has a "before" connector; false otherwise.
     * @return true is this genus has a "before" connector; false otherwise.
     */
    public boolean hasBeforeConnector() {
        return !isStarter;
    }

    /**
     * Returns true if this genus has a "after" connector; false otherwise.
     * @return true if this genus has a "after" connector; false otherwise.
     */
    public boolean hasAfterConnector() {
        return !isTerminator;
    }

    /**
     * Returns true if the value of this genus is contained within the label of this; false
     * otherwise
     * @return true if the value of this genus is contained within the label of this; false
     * otherwise
     */
    public boolean isLabelValue() {
        return isLabelValue;
    }

    /**
     * Returns true if the label of this is editable; false otherwise
     * @return true if the label of this is editable; false otherwise
     */
    public boolean isLabelEditable() {
        return isLabelEditable;
    }

    /**
     * Returns true iff this genus can have page label.
     * @return true iff this genus can have page label
     */
    public boolean isPageLabelSetByPage() {
        return isPageLabelEnabled;
    }

    /**
     * Returns true if the label of this must be unique; false otherwise
     * @return true if the label of this must be unique; false otherwise
     */
    public boolean labelMustBeUnique() {
        return labelMustBeUnique;
    }

    /**
     * Returns true iff this genus's sockets are expandable
     */
    public boolean areSocketsExpandable() {
        return areSocketsExpandable;
    }

    /**
     * Returns true iff this genus is an infix operator.  This genus must be supporting two bottom sockets.
     * @return true iff this genus is an infix operator.  This genus must be supporting two bottom sockets.
     */
    public boolean isInfix() {
        return isInfix;
    }

    /**
     * Returns the name of this genus
     * @return the name of this genus
     */
    public String getGenusName() {
        return genusName;
    }

    /**
     * Returns the initial label of this
     * @return the initial label of this
     */
    public String getInitialLabel() {
        return initLabel;
    }

    /**
     * Returns the String block label prefix of this
     * @return the String block label prefix of this
     */
    public String getLabelPrefix() {
        return labelPrefix;
    }

    /**
     * Returns the String block label prefix of this
     * @return the String block label prefix of this
     */
    public String getLabelSuffix() {
        return labelSuffix;
    }

    /**
     * Returns the String block text description of this.
     * Also known as the block tool tip, or block description.
     * If no descriptions exists, return null.
     * @return the String block text description of this or NULL.
     */
    public String getBlockDescription() {
        return blockDescription;
    }

    /**
     * Returns the set of argument descriptions of this.
     * Argument index to argument description relationship
     * may not be inferred as one-to-one.  That is, an existing
     * socket may not have an existing description (in the case
     * if incomplete descriptions).
     * An existing description may not have an existing
     * socket (in the case of expandable socket sizes).
     * If no descriptions exists, return null.
     * @return the String argument descriptions of this or NULL.
     */
    public Iterable<String> getInitialArgumentDescriptions() {
        return Collections.unmodifiableList(argumentDescriptions);
    }

    /**
     * Returns the Color of this; May return Color.Black if color was unspecified.
     * @return the Color of this; May return Color.Black if color was unspecified.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the initial BlockImageIcon mapping of this.  Returned Map is unmodifiable.
     * @return the initial and unmodifiable BlockImageIcon mapping of this
     */
    public Map<ImageLocation, BlockImageIcon> getInitBlockImageMap() {
        return Collections.unmodifiableMap(blockImageMap);
    }

    /**
     * Returns the value of the specified language dependent property
     * @param property the property to look up
     * @return the value of the specified language dependent property; null if property does not exist
     */
    public String getProperty(String property) {
        return properties.get(property);
    }

    /**
     * Returns the initial set of sockets of this
     * @return the initial set of sockets of this
     */
    public Iterable<BlockConnector> getInitSockets() {
        return Collections.unmodifiableList(sockets);
    }

    /**
     * Returns the initial plug connector of this
     * @return the initial plug connector of this
     */
    public BlockConnector getInitPlug() {
        return plug;
    }

    /**
     * Returns the initial before connector of this
     * @return the initial before connector of this
     */
    public BlockConnector getInitBefore() {
        return before;
    }

    /**
     * Returns the initial after connector of this
     * @return the initial after connector of this
     */
    public BlockConnector getInitAfter() {
        return after;
    }

    /**
     * Returns the expand groups of this. Not modifiable.
     */
    public List<List<BlockConnector>> getExpandGroups() {
        return Collections.unmodifiableList(expandGroups);
    }

    /**
     * Return the expand-group for the given group. Can be null if group
     * doesn't exist.
     */
    private static List<BlockConnector> getExpandGroup(List<List<BlockConnector>> groups, String group) {
        for (List<BlockConnector> list : groups) {
            // Always at least one element in the group.
            if (list.get(0).getExpandGroup().equals(group)) {
                return list;
            }
        }
        return null;
    }

    /**
     * Add a connector type to an expand group.
     */
    private static void addToExpandGroup(List<List<BlockConnector>> groups, BlockConnector socket) {
        List<BlockConnector> eGroup = getExpandGroup(groups, socket.getExpandGroup());
        if (eGroup == null) {
            eGroup = new ArrayList<BlockConnector>();
            groups.add(eGroup);
        }
        eGroup.add(new BlockConnector(socket));
    }

    /**
     * Modified to translate the ToolTip
	 *  ldgneto@gmail.com 
	 * */
    private static void loadGenusDescription(NodeList descriptions, BlockGenus genus, String attribName) {
        Node description;        
		ResourceBundle bundle = ResourceBundle.getBundle("com/ardublock/block/ardublock");	
		for (int k = 0; k < descriptions.getLength(); k++) {
			description = descriptions.item(k);	
			if (description.getNodeName().equals("text") && !attribName.equals(null)) {
				try{//Trying to bundle the attribute
					genus.blockDescription = bundle.getString("tip." + attribName);
				}catch (MissingResourceException e) {
					genus.blockDescription = description.getTextContent();
				}
			} else if (description.getNodeName().equals("arg-description")) {
				String argumentDescription = description.getTextContent();
				if (argumentDescription != null) {
					genus.argumentDescriptions.add(argumentDescription);
				}
			}
		}
    }

    /**
     * Loads the BlockConnector information of the specified genus
     * @param workspace The workspace in use
     * @param connectors NodeList of connector information to load from
     * @param genus BlockGenus to load block connector information onto
     */
    private static void loadBlockConnectorInformation(Workspace workspace, NodeList connectors, BlockGenus genus) {
        Pattern attrExtractor = Pattern.compile("\"(.*)\"");
        Matcher nameMatcher;
        Node opt_item;
        Node connector;
        for (int k = 0; k < connectors.getLength(); k++) {
            connector = connectors.item(k);
            if (connector.getNodeName().equals("BlockConnector")) {
                String label = "";
                String connectorType = "none";
                int connectorKind = 0; //where 0 is socket, 1 is plug
                String positionType = "single";
                boolean isExpandable = false;
                boolean isLabelEditable = false;
                String expandGroup = "";
                String defargname = null;
                String defarglabel = null;

                if (connector.getAttributes().getLength() > 0) {
                    nameMatcher = attrExtractor.matcher(connector.getAttributes().getNamedItem("connector-kind").toString());
                    if (nameMatcher.find()) {
                        connectorKind = nameMatcher.group(1).equals("socket") ? 0 : 1;
                    }
                    nameMatcher = attrExtractor.matcher(connector.getAttributes().getNamedItem("connector-type").toString());
                    if (nameMatcher.find()) {
                        connectorType = nameMatcher.group(1);
                    }
                    nameMatcher = attrExtractor.matcher(connector.getAttributes().getNamedItem("position-type").toString());
                    if (nameMatcher.find()) {
                        positionType = nameMatcher.group(1);
                    }
                    nameMatcher = attrExtractor.matcher(connector.getAttributes().getNamedItem("is-expandable").toString());
                    if (nameMatcher.find()) {
                        isExpandable = nameMatcher.group(1).equals("yes");
                    }
                    nameMatcher = attrExtractor.matcher(connector.getAttributes().getNamedItem("label-editable").toString());
                    if (nameMatcher.find()) {
                        isLabelEditable = nameMatcher.group(1).equals("yes");
                    }
                    //load optional items
                    opt_item = connector.getAttributes().getNamedItem("label");
                    if (opt_item != null) {
                        nameMatcher = attrExtractor.matcher(opt_item.toString());
                        if (nameMatcher.find()) {
                            label = nameMatcher.group(1);
                        }
                    }
                    opt_item = connector.getAttributes().getNamedItem("expand-group");
                    if (opt_item != null) {
                        nameMatcher = attrExtractor.matcher(opt_item.toString());
                        if (nameMatcher.find()) {
                            expandGroup = nameMatcher.group(1);
                        }
                    }
                }

                if (connector.hasChildNodes()) {
                    //load default arguments
                    NodeList defargs = connector.getChildNodes();  //should really only be one
                    Node defarg;
                    for (int l = 0; l < defargs.getLength(); l++) {
                        defarg = defargs.item(l);
                        if (defarg.getNodeName().equals("DefaultArg")) {
                            if (defarg.getAttributes().getLength() > 0) {
                                nameMatcher = attrExtractor.matcher(defarg.getAttributes().getNamedItem("genus-name").toString());
                                if (nameMatcher.find()) {
                                    defargname = nameMatcher.group(1);
                                }
                                assert workspace.getEnv().getGenusWithName(defargname) != null : "Unknown BlockGenus: " + defargname;
                                //warning: if this block genus does not have an editable label, the label being loaded does not
                                //have an affect
                                opt_item = defarg.getAttributes().getNamedItem("label");
                                if (opt_item != null) {
                                    nameMatcher = attrExtractor.matcher(opt_item.toString());
                                    if (nameMatcher.find()) {
                                        defarglabel = nameMatcher.group(1);
                                    }
                                }
                                genus.hasDefArgs = true;
                            }
                        }
                    }
                }

                final BlockConnector socket;
                //set the position type for this new connector, by default its set to single
                if (positionType.equals("mirror")) {
                    socket = new BlockConnector(workspace, connectorType, BlockConnector.PositionType.MIRROR, label, isLabelEditable, isExpandable, expandGroup, Block.NULL);
                } else if (positionType.equals("bottom")) {
                    socket = new BlockConnector(workspace, connectorType, BlockConnector.PositionType.BOTTOM, label, isLabelEditable, isExpandable, expandGroup, Block.NULL);
                } else {
                    socket = new BlockConnector(workspace, connectorType, BlockConnector.PositionType.SINGLE, label, isLabelEditable, isExpandable, expandGroup, Block.NULL);
                }

                //add def args if any
                if (defargname != null) {
                    socket.setDefaultArgument(defargname, defarglabel);
                }

                //set the connector kind
                if (connectorKind == 0) {
                    genus.sockets.add(socket);
                } else {
                    genus.plug = socket;
                    assert (!socket.isExpandable()) : genus.genusName + " can not have an expandable plug.  Every block has at most one plug.";
                }
                if (socket.isExpandable()) {
                    genus.areSocketsExpandable = true;
                }
                if (expandGroup.length() > 0) {
                    addToExpandGroup(genus.expandGroups, socket);
                }
            }
        }
    }

    /**
     * Loads the images to be drawn on the visible block instances of this
     * @param images NodeList of image information to load from
     * @param genus BlockGenus instance to load images onto
     */
    private static void loadBlockImages(NodeList images, BlockGenus genus) {
        Pattern attrExtractor = Pattern.compile("\"(.*)\"");
        Matcher nameMatcher;
        Node imageNode;
        String location = null;
        boolean isEditable = false;
        boolean textWrap = false;
        for (int i = 0; i < images.getLength(); i++) {
            imageNode = images.item(i);
            if (imageNode.getNodeName().equals("Image")) {
                if (imageNode.getAttributes().getLength() > 0) {
                    //load image properties
                    nameMatcher = attrExtractor.matcher(imageNode.getAttributes().getNamedItem("block-location").toString());
                    if (nameMatcher.find()) {
                        location = nameMatcher.group(1);
                    }
                    nameMatcher = attrExtractor.matcher(imageNode.getAttributes().getNamedItem("image-editable").toString());
                    if (nameMatcher.find()) {
                        isEditable = nameMatcher.group(1).equals("yes") ? true : false;
                    }
                    nameMatcher = attrExtractor.matcher(imageNode.getAttributes().getNamedItem("wrap-text").toString());
                    if (nameMatcher.find()) {
                        textWrap = nameMatcher.group(1).equals("yes") ? true : false;
                    }
                    int width = -1;
                    int height = -1;
                    Node opt_item = imageNode.getAttributes().getNamedItem("width");
                    if (opt_item != null) {
                        nameMatcher = attrExtractor.matcher(opt_item.toString());
                        if (nameMatcher.find()) {
                            width = Integer.parseInt(nameMatcher.group(1));
                        }
                    }
                    opt_item = imageNode.getAttributes().getNamedItem("height");
                    if (opt_item != null) {
                        nameMatcher = attrExtractor.matcher(opt_item.toString());
                        if (nameMatcher.find()) {
                            height = Integer.parseInt(nameMatcher.group(1));
                        }
                    }
                    //load actual image
                    NodeList imageChildren = imageNode.getChildNodes();
                    Node imageLocationNode;
                    for (int j = 0; j < imageChildren.getLength(); j++) {
                        imageLocationNode = imageChildren.item(j);
                        if (imageLocationNode.getNodeName().equals("FileLocation")) {
                            String fileLocation = imageLocationNode.getTextContent();
                            try {
                            	URL fileURL = BlockGenus.class.getClassLoader().getResource(fileLocation);
                            	if (fileURL == null ) {
                            		fileURL = new URL("file", "", /*workingDirectory +*/ fileLocation);
                            	}
                                if (fileURL != null && location != null) {
                                    //translate location String to ImageLocation representation
                                    ImageLocation imgLoc = ImageLocation.getImageLocation(location);
                                    assert imgLoc != null : "Invalid location string loaded: " + imgLoc;

                                    //store in blockImageMap
                                    ImageIcon icon = new ImageIcon(fileURL);
                                    if (width > 0 && height > 0) {
                                        icon.setImage(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
                                    }
                                    genus.blockImageMap.put(imgLoc, new BlockImageIcon(icon, imgLoc, isEditable, textWrap));
                                }
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Loads the language definition properties of the specified genus
     * @param properties NodeList of properties to load from file
     * @param genus BlockGenus to load the properties onto
     */
    private static void loadLangDefProperties(NodeList properties, BlockGenus genus) {
        Pattern attrExtractor = Pattern.compile("\"(.*)\"");
        Matcher nameMatcher;
        Node prop;
        String key = null, value = null;
        for (int l = 0; l < properties.getLength(); l++) {
            prop = properties.item(l);
            if (prop.getNodeName().equals("LangSpecProperty")) {
                if (prop.getAttributes().getLength() > 0) {
                    nameMatcher = attrExtractor.matcher(prop.getAttributes().getNamedItem("key").toString());
                    if (nameMatcher.find()) {
                        key = nameMatcher.group(1);
                    }
                    Node opt_item = prop.getAttributes().getNamedItem("value");
                    if (opt_item != null) {
                        nameMatcher = attrExtractor.matcher(opt_item.toString());
                        if (nameMatcher.find()) {
                            value = nameMatcher.group(1);
                        }
                    } else {
                        value = prop.getTextContent();
                    }
                    if (key != null && value != null) {
                        genus.properties.put(key, value);
                    }
                }
            }
        }
    }

    /**
     * Loads the stub information of the specified genus
     * @param stubs NodeList of stub information to load
     * @param genus BlockGenus to load stub information onto
     */
    private static void loadStubs(NodeList stubs, BlockGenus genus) {
        Pattern attrExtractor = Pattern.compile("\"(.*)\"");
        Matcher nameMatcher;
        Node stub;
        String stubGenus = "";
        for (int m = 0; m < stubs.getLength(); m++) {
            stub = stubs.item(m);
            if (stub.getNodeName().equals("Stub")) {
                if (stub.getAttributes().getLength() > 0) {
                    nameMatcher = attrExtractor.matcher(stub.getAttributes().getNamedItem("stub-genus").toString());
                    if (nameMatcher.find()) {
                        stubGenus = nameMatcher.group(1);
                    }
                    if (stub.hasChildNodes()) {
                        //this stub for this genus deviates from generic stub
                        //generate genus by copying one of generic ones

                        BlockGenus newStubGenus = new BlockGenus(genus.env, stubGenus, stubGenus + genus.genusName);
                        //load unique stub genus properties
                        NodeList stubChildren = stub.getChildNodes();
                        for (int n = 0; n < stubChildren.getLength(); n++) {
                            Node stubChild = stubChildren.item(n);
                            if (stubChild.getNodeName().equals("LangSpecProperties")) {
                                loadLangDefProperties(stubChild.getChildNodes(), newStubGenus);
                            }
                        }
                        genus.env.addBlockGenus(newStubGenus);
                        genus.stubList.add(newStubGenus.genusName);
                    } else {
                        //not a unique stub, add generic stub
                        genus.stubList.add(stubGenus);
                    }
                }
            }
        }
    }

    /**
     * Loads the all the initial BlockGenuses and BlockGenus families of this language
     * @param workspace The workspace in use
     * @param root the Element carrying the specifications of the BlockGenuses
     */
    public static void loadBlockGenera(Workspace workspace, Element root) {
    	WorkspaceEnvironment env = workspace.getEnv();

        Pattern attrExtractor = Pattern.compile("\"(.*)\"");
        Matcher nameMatcher;
        NodeList genusNodes = root.getElementsByTagName("BlockGenus"); //look for genus
        Node genusNode;
        StringTokenizer col;
        String attribName = "";//Added to get attribute name
        for (int i = 0; i < genusNodes.getLength(); i++) { //find them
            genusNode = genusNodes.item(i);
            if (genusNode.getNodeName().equals("BlockGenus")) {
                /// LOAD BLOCK GENUS PROPERTIES ///
                BlockGenus newGenus = new BlockGenus(env);
                //first, parse out the attributes
                nameMatcher = attrExtractor.matcher(genusNode.getAttributes().getNamedItem("name").toString());
                //Attribute name to loadGenusDescription method
                attribName = genusNode.getAttributes().getNamedItem("name").getNodeValue();
                if (nameMatcher.find()) {
                    newGenus.genusName = nameMatcher.group(1);
                }
                //assert that no other genus has this name
                assert env.getGenusWithName(newGenus.genusName) == null : "Block genus names must be unique.  A block genus already exists with this name: " + newGenus.genusName;
                nameMatcher = attrExtractor.matcher(genusNode.getAttributes().getNamedItem("color").toString());
                if (nameMatcher.find()) { //will be true
                    col = new StringTokenizer(nameMatcher.group(1));
                    if (col.countTokens() == 3) {
                        newGenus.color = new Color(Integer.parseInt(col.nextToken()), Integer.parseInt(col.nextToken()), Integer.parseInt(col.nextToken()));
                    } else {
                        newGenus.color = Color.BLACK;
                    }
                }
                nameMatcher = attrExtractor.matcher(genusNode.getAttributes().getNamedItem("kind").toString());
                if (nameMatcher.find()) {
                    newGenus.kind = nameMatcher.group(1);
                }
                nameMatcher = attrExtractor.matcher(genusNode.getAttributes().getNamedItem("initlabel").toString());
                if (nameMatcher.find()) {
                    //implied that it is global, but it may be redefined
                    newGenus.initLabel = nameMatcher.group(1);
                }
                nameMatcher = attrExtractor.matcher(genusNode.getAttributes().getNamedItem("editable-label").toString());
                if (nameMatcher.find()) {
                    newGenus.isLabelEditable = nameMatcher.group(1).equals("yes") ? true : false;
                }
                nameMatcher = attrExtractor.matcher(genusNode.getAttributes().getNamedItem("label-unique").toString());
                if (nameMatcher.find()) {
                    newGenus.labelMustBeUnique = nameMatcher.group(1).equals("yes") ? true : false;
                }
                //load optional items
                Node opt_item = genusNode.getAttributes().getNamedItem("is-starter");
                if (opt_item != null) {
                    nameMatcher = attrExtractor.matcher(opt_item.toString());
                    if (nameMatcher.find()) {
                        newGenus.isStarter = nameMatcher.group(1).equals("yes") ? true : false;
                    }
                }
                opt_item = genusNode.getAttributes().getNamedItem("is-terminator");
                if (opt_item != null) {
                    nameMatcher = attrExtractor.matcher(opt_item.toString());
                    if (nameMatcher.find()) {
                        newGenus.isTerminator = nameMatcher.group(1).equals("yes") ? true : false;
                    }
                }
                opt_item = genusNode.getAttributes().getNamedItem("is-label-value");
                if (opt_item != null) {
                    nameMatcher = attrExtractor.matcher(opt_item.toString());
                    if (nameMatcher.find()) {
                        newGenus.isLabelValue = nameMatcher.group(1).equals("yes") ? true : false;
                    }
                }
                opt_item = genusNode.getAttributes().getNamedItem("label-prefix");
                if (opt_item != null) {
                    nameMatcher = attrExtractor.matcher(opt_item.toString());
                    if (nameMatcher.find()) {
                        newGenus.labelPrefix = nameMatcher.group(1);
                    }
                }
                opt_item = genusNode.getAttributes().getNamedItem("label-suffix");
                if (opt_item != null) {
                    nameMatcher = attrExtractor.matcher(opt_item.toString());
                    if (nameMatcher.find()) {
                        newGenus.labelSuffix = nameMatcher.group(1);
                    }
                }
                opt_item = genusNode.getAttributes().getNamedItem("page-label-enabled");
                if (opt_item != null) {
                    nameMatcher = attrExtractor.matcher(opt_item.toString());
                    if (nameMatcher.find()) {
                        newGenus.isPageLabelEnabled = nameMatcher.group(1).equals("yes") ? true : false;
                    }
                }
                //if genus is a data genus (kind=data) or a variable block (and soon a declaration block)
                //it is both a starter and terminator
                //in other words, it should not have before and after connectors
                if (newGenus.isDataBlock() || newGenus.isVariableDeclBlock() || newGenus.isFunctionBlock()) {
                    newGenus.isStarter = true;
                    newGenus.isTerminator = true;
                }

                //next, parse out the elements
                NodeList genusChildren = genusNode.getChildNodes();
                Node genusChild;
                for (int j = 0; j < genusChildren.getLength(); j++) {
                    genusChild = genusChildren.item(j);
                    if (genusChild.getNodeName().equals("description")) {
                        /// LOAD BLOCK GENUS DESCRIPTION ///
                        loadGenusDescription(genusChild.getChildNodes(), newGenus, attribName);
                    } else if (genusChild.getNodeName().equals("BlockConnectors")) {
                        /// LOAD BLOCK CONNECTOR INFORMATION ///
                        loadBlockConnectorInformation(workspace, genusChild.getChildNodes(), newGenus);
                        //if genus has two connectors both of bottom position type than this block is an infix operator
                        if (newGenus.sockets != null && newGenus.sockets.size() == 2
                                && newGenus.sockets.get(0).getPositionType() == BlockConnector.PositionType.BOTTOM
                                && newGenus.sockets.get(1).getPositionType() == BlockConnector.PositionType.BOTTOM) {
                            newGenus.isInfix = true;
                        }
                    } else if (genusChild.getNodeName().equals("Images")) {
                        /// LOAD BLOCK IMAGES ///
                        loadBlockImages(genusChild.getChildNodes(), newGenus);
                    } else if (genusChild.getNodeName().equals("LangSpecProperties")) {
                        /// LOAD LANGUAGE SPECIFIC PROPERTIES ///
                        loadLangDefProperties(genusChild.getChildNodes(), newGenus);

                    } else if (genusChild.getNodeName().equals("Stubs")) {
                        /// LOAD STUBS INFO AND GENERATE GENUSES FOR EACH STUB ///
                        loadStubs(genusChild.getChildNodes(), newGenus);
                    }
                }

                // John's code to add command sockets... probably in the wrong place
                if (!newGenus.isStarter) {
                    newGenus.before = new BlockConnector(workspace, BlockConnectorShape.getCommandShapeName(), BlockConnector.PositionType.TOP, "", false, false, Block.NULL);
                }
                if (!newGenus.isTerminator) {
                    newGenus.after = new BlockConnector(workspace, BlockConnectorShape.getCommandShapeName(), BlockConnector.PositionType.BOTTOM, "", false, false, Block.NULL);
                }

                //System.out.println("Added "+newGenus.toString());
                env.addBlockGenus(newGenus);
            }

        }

        /////////////////////////////////////
        /// LOAD BLOCK FAMILY INFORMATION ///
        /////////////////////////////////////
        NodeList families = root.getElementsByTagName("BlockFamily");
        Node family;
        Node member;
        String name;
        ArrayList<String> famList = new ArrayList<String>();
        for (int i = 0; i < families.getLength(); i++) {
            family = families.item(i);
            for (int j = 0; j < family.getChildNodes().getLength(); j++) {
                member = family.getChildNodes().item(j);
                if (member.getNodeName().equals("FamilyMember")) { //a family member entry
                    name = member.getTextContent();
                    assert env.getGenusWithName(name) != null : "Unknown BlockGenus: " + name;
                    assert !env.getGenusWithName(name).isLabelEditable : "Genus " + name + " is in a family, but its name is editable";
                    famList.add(name);
                }

            }
            if (famList.size() > 0) {
                for (String memName : famList) {
                    ArrayList<String> newFamList = new ArrayList<String>(famList);
                    newFamList.remove(memName); //filter out current memName, so that only
                    //sibling names are included
                    env.getGenusWithName(memName).familyList = newFamList;
                }
            }
            famList.clear();
        }
    }

    /**
     * Returns String representation of this
     */
    public String toString() {
        StringBuffer out = new StringBuffer("BlockGenus ");

        out.append(this.genusName);
        out.append(" kind: ");
        out.append(this.kind);
        out.append(" with label ");
        out.append(this.initLabel);
        out.append(" with color: ");
        out.append(this.color);
        out.append("\n");
        out.append("isStarter=");
        out.append(this.isStarter);
        out.append(" isTerminator=");
        out.append(this.isTerminator);
        out.append("\n");

        if (before != null) {
            out.append("before:");
            out.append(before.toString());
            out.append("\n");
        }
        if (after != null) {
            out.append("after:");
            out.append(after.toString());
            out.append("\n");
        }

        if (plug != null) {
            out.append("plug:");
            out.append(plug.toString());
            out.append("\n");
        }

        for (int i = 0; i < sockets.size(); i++) {
            out.append(sockets.get(i).toString());
            out.append("\n");
        }
        return out.toString();
    }

}
