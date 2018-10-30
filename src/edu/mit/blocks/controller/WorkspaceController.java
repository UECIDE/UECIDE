package edu.mit.blocks.controller;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;

import edu.mit.blocks.codeblocks.ProcedureOutputManager;	//*****

import edu.mit.blocks.codeblocks.BlockConnectorShape;
import edu.mit.blocks.codeblocks.BlockGenus;
import edu.mit.blocks.codeblocks.BlockLinkChecker;
import edu.mit.blocks.codeblocks.CommandRule;
import edu.mit.blocks.codeblocks.Constants;
import edu.mit.blocks.codeblocks.SocketRule;
import edu.mit.blocks.codeblocks.ParamRule;
import edu.mit.blocks.codeblocks.PolyRule;
import edu.mit.blocks.codeblocks.StackRule;
import edu.mit.blocks.workspace.SearchBar;
import edu.mit.blocks.workspace.SearchableContainer;
import edu.mit.blocks.workspace.Workspace;

/**
 * Example entry point to OpenBlock application creation.
 *
 * @author Ricarose Roque
 */
public class WorkspaceController {

    private Element langDefRoot;
    private boolean isWorkspacePanelInitialized = false;
    protected JPanel workspacePanel;
    protected final Workspace workspace;
    protected SearchBar searchBar;

    public Workspace getWorkspace() {
        return this.workspace;
    }

    //flag to indicate if a new lang definition file has been set
    private boolean langDefDirty = true;
    // handle the case of loading the DTD from jar file. 
    private InputStream langDefDtd;
    //flag to indicate if a workspace has been loaded/initialized 
    private boolean workspaceLoaded = false;
    // last directory that was selected with open or save action
    private File lastDirectory;
    // file currently loaded in workspace
    private File selectedFile;
    // Reference kept to be able to update frame title with current loaded file
    private JFrame frame;
    
    // I18N resource bundle
    private ResourceBundle langResourceBundle;
	// List of styles
    private List<String[]> styleList;
    
    private static ProcedureOutputManager pom;	//*****
    

    /**
     * Constructs a WorkspaceController instance that manages the
     * interaction with the codeblocks.Workspace
     *
     */
    public WorkspaceController() {
        this.workspace = new Workspace();
        pom = new ProcedureOutputManager(workspace);	//*****
    }
    
    public void setLangDefDtd(InputStream is) {
    	langDefDtd = is;
    }
    
    public void setLangResourceBundle(ResourceBundle bundle) {
    	langResourceBundle = bundle;
    }
    
    public void setStyleList(List<String[]> list) {
    	styleList = list;
    }

    /**
     * Sets the file path for the language definition file, if the
     * language definition file is located in
     */
    public void setLangDefFilePath(final String filePath) {
        InputStream in = null;
        try {
            in = new FileInputStream(filePath);
            setLangDefStream(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Sets language definition file from the given input stream
     * @param in input stream to read
     */
    public void setLangDefStream(InputStream in) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder;
        final Document doc;
        try {
            builder = factory.newDocumentBuilder();
            if (langDefDtd != null) {
            	builder.setEntityResolver(new EntityResolver () {
            		public InputSource resolveEntity( String publicId, String systemId) throws SAXException, IOException {
            			return new InputSource(langDefDtd);
            		}
            	});
            }
            doc = builder.parse(in);
            // TODO modify the L10N text and style here
            ardublockLocalize(doc);
            ardublockStyling(doc);
            
            langDefRoot = doc.getDocumentElement();
            langDefDirty = true;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Styling the color of the BlockGenus and other elements with color
     */
	private void ardublockStyling(Document doc) {
		if (styleList != null) {
			XPathFactory factory = XPathFactory.newInstance();
			for (String[] style : styleList) {
				XPath xpath = factory.newXPath();
				try {
					// XPathExpression expr = xpath.compile("//BlockGenus[@name[starts-with(.,\"Tinker\")]]/@color");
					XPathExpression expr = xpath.compile(style[0]);
					NodeList bgs = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
					for (int i = 0; i < bgs.getLength(); i++) {
						Node bg = bgs.item(i);
						bg.setNodeValue(style[1]);
						// bg.setAttribute("color", "128 0 0");
					}
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
			}
		}
	}
    
    /** 
     * l10n process for ArduBlock
     */
    private void ardublockLocalize(Document doc) {
        if (langResourceBundle != null) {
        	NodeList nodes = doc.getElementsByTagName("BlockGenus");
        	for (int i = 0 ; i < nodes.getLength(); i++) {
        		Element elm = (Element)nodes.item(i);
        		String name = elm.getAttribute("name");
				
        		// System.out.println("Translating BlockGenu:" + name);
				
        		String altName = langResourceBundle.getString("bg." + name);
        		if (altName != null) {
        			elm.setAttribute("initlabel", altName);
        		}
				NodeList descriptions = elm.getElementsByTagName("description");
				Element description = (Element)descriptions.item(0);
				if (description != null) {
					NodeList texts = description.getElementsByTagName("text");
					Element text = (Element)texts.item(0);
					if (text != null) {
						String pname = "bg." + name + ".description";
						try {
							altName = langResourceBundle.getString(pname);
							if (altName != null) {
								text.setTextContent(altName);
							}
						} catch (java.util.MissingResourceException mre) {
							System.err.println("ardublock.xml: missing " + pname);
						}
					}
				}
				NodeList arg_descs = elm.getElementsByTagName("arg-description");
				for (int j = 0 ; j < arg_descs.getLength(); j++) {
					Element arg_desc = (Element)arg_descs.item(j);
					String arg_name = arg_desc.getAttribute("name");
					// System.out.println("bg." + name + ".arg_desc." + arg_name);
				}
			}
        	nodes = doc.getElementsByTagName("BlockDrawer");
        	for (int i = 0 ; i < nodes.getLength(); i++) {
        		Element elm = (Element)nodes.item(i);
        		String name = elm.getAttribute("name");
        		String altName = langResourceBundle.getString(name);
        		if (altName != null) {
        			elm.setAttribute("name", altName);
        		}
        	}
        	nodes = doc.getElementsByTagName("BlockConnector");
        	for (int i = 0 ; i < nodes.getLength(); i++) {
        		Element elm = (Element)nodes.item(i);
        		String name = elm.getAttribute("label");
        		if (name.startsWith("bc.")) {
					String altName = langResourceBundle.getString(name);
					if (altName != null) {
						elm.setAttribute("label", altName);
					}
				}
        	}
        }
    }

    /**
     * Loads all the block genuses, properties, and link rules of
     * a language specified in the pre-defined language def file.
     * @param root Loads the language specified in the Element root
     */
    public void loadBlockLanguage(final Element root) {
        /* MUST load shapes before genuses in order to initialize
         connectors within each block correctly */
        BlockConnectorShape.loadBlockConnectorShapes(root);

        //load genuses
        BlockGenus.loadBlockGenera(workspace, root);

        //load rules
        BlockLinkChecker.addRule(workspace, new CommandRule(workspace));
        BlockLinkChecker.addRule(workspace, new SocketRule());
        BlockLinkChecker.addRule(workspace, new PolyRule(workspace));
        BlockLinkChecker.addRule(workspace, new StackRule(workspace));
        BlockLinkChecker.addRule(workspace, new ParamRule());

        //set the dirty flag for the language definition file
        //to false now that the lang file has been loaded
        langDefDirty = false;
    }

    /**
     * Resets the current language within the active
     * Workspace.
     *
     */
    public void resetLanguage() {
        BlockConnectorShape.resetConnectorShapeMappings();
        getWorkspace().getEnv().resetAllGenuses();
        BlockLinkChecker.reset();
    }

    /**
     * Returns the save string for the entire workspace.  This includes the block workspace, any
     * custom factories, canvas view state and position, pages
     * @return the save string for the entire workspace.
     */
    public String getSaveString() {
        try {
            Node node = getSaveNode();

            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        }
        catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a DOM node for the entire workspace.  This includes the block workspace, any
     * custom factories, canvas view state and position, pages
     * @return the DOM node for the entire workspace.
     */
    public Node getSaveNode() {
        return getSaveNode(true);
    }

    /**
     * Returns a DOM node for the entire workspace. This includes the block
     * workspace, any custom factories, canvas view state and position, pages
     *
     * @param validate If {@code true}, perform a validation of the output
     * against the code blocks schema
     * @return the DOM node for the entire workspace.
     */
    public Node getSaveNode(final boolean validate) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            Element documentElement = document.createElementNS(Constants.XML_CODEBLOCKS_NS, "cb:CODEBLOCKS");
            // schema reference
            documentElement.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:schemaLocation", Constants.XML_CODEBLOCKS_NS+" "+Constants.XML_CODEBLOCKS_SCHEMA_URI);

            Node workspaceNode = workspace.getSaveNode(document);
            if (workspaceNode != null) {
                documentElement.appendChild(workspaceNode);
            }

            document.appendChild(documentElement);
            //if (validate) {
            //    validate(document);
            //}

            return document;
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validates the code blocks document against the schema
     * @param document The document to check
     * @throws RuntimeException If the validation failed
     */
    private void validate(Document document) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL schemaUrl = this.getClass().getResource("/edu/mit/blocks/codeblocks/codeblocks.xsd");
            Schema schema = schemaFactory.newSchema(schemaUrl);
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(document));
        }
        catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        catch (SAXException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads a fresh workspace based on the default specifications in the language
     * definition file.  The block canvas will have no live blocks.
     */
    public void loadFreshWorkspace() {
        if (workspaceLoaded) {
            resetWorkspace();
        }
        if (langDefDirty) {
            loadBlockLanguage(langDefRoot);
        }
        workspace.loadWorkspaceFrom(null, langDefRoot);
        workspaceLoaded = true;
        
    }

    /**
     * Loads the programming project from the specified file path.
     * This method assumes that a Language Definition File has already
     * been specified for this programming project.
     * @param path String file path of the programming project to load
     */
    public void loadProjectFromPath(final String path) throws IOException
    {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder;
        final Document doc;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new File(path));

            // XXX here, we could be strict and only allow valid documents...
            // validate(doc);
            final Element projectRoot = doc.getDocumentElement();
            //load the canvas (or pages and page blocks if any) blocks from the save file
            //also load drawers, or any custom drawers from file.  if no custom drawers
            //are present in root, then the default set of drawers is loaded from
            //langDefRoot
            workspace.loadWorkspaceFrom(projectRoot, langDefRoot);
            workspaceLoaded = true;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads the programming project from the specified element. This method
     * assumes that a Language Definition File has already been specified for
     * this programming project.
     *
     * @param element element of the programming project to load
     */
    public void loadProjectFromElement(Element elementToLoad) {
        workspace.loadWorkspaceFrom(elementToLoad, langDefRoot);
        workspaceLoaded = true;
    }

    /**
     * Loads the programming project specified in the projectContents String,
     * which is associated with the language definition file contained in the
     * specified langDefContents.  All the blocks contained in projectContents
     * must have an associted block genus defined in langDefContents.
     *
     * If the langDefContents have any workspace settings such as pages or
     * drawers and projectContents has workspace settings as well, the
     * workspace settings within the projectContents will override the
     * workspace settings in langDefContents.
     *
     * NOTE: The language definition contained in langDefContents does
     * not replace the default language definition file set by: setLangDefFilePath() or
     * setLangDefFile().
     *
     * @param projectContents
     * @param langDefContents String XML that defines the language of
     * projectContents
     */
    public void loadProject(String projectContents, String langDefContents) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder;
        final Document projectDoc;
        final Document langDoc;
        try {
            builder = factory.newDocumentBuilder();
            projectDoc = builder.parse(new InputSource(new StringReader(projectContents)));
            final Element projectRoot = projectDoc.getDocumentElement();
            langDoc = builder.parse(new InputSource(new StringReader(projectContents)));
            final Element langRoot = langDoc.getDocumentElement();
            if (workspaceLoaded) {
                resetWorkspace();
            }
            if (langDefContents == null) {
                loadBlockLanguage(langDefRoot);
            } else {
                loadBlockLanguage(langRoot);
            }
            workspace.loadWorkspaceFrom(projectRoot, langRoot);
            workspaceLoaded = true;

        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the entire workspace.  This includes all blocks, pages, drawers, and trashed blocks.
     * Also resets the undo/redo stack.  The language (i.e. genuses and shapes) is not reset.
     */
    public void resetWorkspace() {
        //clear all pages and their drawers
        //clear all drawers and their content
        //clear all block and renderable block instances
        workspace.reset();
        //clear procedure output information
        ProcedureOutputManager.reset();	//*****

    }

    /**
     * This method creates and lays out the entire workspace panel with its
     * different components.  Workspace and language data not loaded in
     * this function.
     * Should be call only once at application startup.
     */
    private void initWorkspacePanel() {
        workspacePanel = new JPanel();
        workspacePanel.setLayout(new BorderLayout());
        workspacePanel.add(workspace, BorderLayout.CENTER);
        isWorkspacePanelInitialized = true;
    }

    /**
     * Returns the JComponent of the entire workspace.
     * @return the JComponent of the entire workspace.
     */
    public JComponent getWorkspacePanel() {
        if (!isWorkspacePanelInitialized) {
            initWorkspacePanel();
        }
        return workspacePanel;
    }

    /**
     * Action bound to "Open" action.
     */
    private class OpenAction extends AbstractAction {

        private static final long serialVersionUID = -2119679269613495704L;

        OpenAction() {
            super("Open");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser(lastDirectory);
            if (fileChooser.showOpenDialog((Component)e.getSource()) == JFileChooser.APPROVE_OPTION) {
                setSelectedFile(fileChooser.getSelectedFile());
                lastDirectory = selectedFile.getParentFile();
                String selectedPath = selectedFile.getPath();
                loadFreshWorkspace();
                try
                {
                	loadProjectFromPath(selectedPath);
                }
                catch (IOException ee)
                {
                	throw new RuntimeException(ee);
                }
            }
        }
    }

    /**
     * Action bound to "Save" button.
     */
    private class SaveAction extends AbstractAction {
        private static final long serialVersionUID = -5540588250535739852L;
        SaveAction() {
            super("Save");
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (selectedFile == null) {
                JFileChooser fileChooser = new JFileChooser(lastDirectory);
                if (fileChooser.showSaveDialog((Component) evt.getSource()) == JFileChooser.APPROVE_OPTION) {
                    setSelectedFile(fileChooser.getSelectedFile());
                    lastDirectory = selectedFile.getParentFile();
                }
            }
            try {
                saveToFile(selectedFile);
            }
            catch (IOException e) {
                JOptionPane.showMessageDialog((Component) evt.getSource(),
                        e.getMessage());
            }
        }
    }

    /**
     * Action bound to "Save As..." button.
     */
    private class SaveAsAction extends AbstractAction {
         private static final long serialVersionUID = 3981294764824307472L;
        private final SaveAction saveAction;

        SaveAsAction(SaveAction saveAction) {
            super("Save As...");
            this.saveAction = saveAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            selectedFile = null;
            // delegate to save action
            saveAction.actionPerformed(e);
        }
    }

    /**
     * Saves the content of the workspace to the given file
     * @param file Destination file
     * @throws IOException If save failed
     */
    private void saveToFile(File file) throws IOException {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(getSaveString());
        }
        finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }

    public void setSelectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
        frame.setTitle("WorkspaceDemo - "+selectedFile.getPath());
    }

    /**
     * Return the lower button panel.
     */
    private JComponent getButtonPanel() {
        JPanel buttonPanel = new JPanel();
        // Open
        OpenAction openAction = new OpenAction();
        buttonPanel.add(new JButton(openAction));
        // Save
        SaveAction saveAction = new SaveAction();
        buttonPanel.add(new JButton(saveAction));
        // Save as
        SaveAsAction saveAsAction = new SaveAsAction(saveAction);
        buttonPanel.add(new JButton(saveAsAction));
        return buttonPanel;
    }

    /**
     * Returns a SearchBar instance capable of searching for blocks
     * within the BlockCanvas and block drawers
     */
    public JComponent getSearchBar() {
        final SearchBar sb = new SearchBar(
                "Search blocks", "Search for blocks in the drawers and workspace", workspace);
        for (SearchableContainer con : getAllSearchableContainers()) {
            sb.addSearchableContainer(con);
        }
        return sb.getComponent();
    }

    /**
     * Returns an unmodifiable Iterable of SearchableContainers
     * @return an unmodifiable Iterable of SearchableContainers
     */
    public Iterable<SearchableContainer> getAllSearchableContainers() {
        return workspace.getAllSearchableContainers();
    }

    /**
     * Create the GUI and show it.  For thread safety, this method should be
     * invoked from the event-dispatching thread.
     */
    private void createAndShowGUI() {
        frame = new JFrame("WorkspaceDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBounds(100, 100, 800, 600);
        final SearchBar sb = new SearchBar("Search blocks",
                "Search for blocks in the drawers and workspace", workspace);
        for (final SearchableContainer con : getAllSearchableContainers()) {
            sb.addSearchableContainer(con);
        }
        final JPanel topPane = new JPanel();
        sb.getComponent().setPreferredSize(new Dimension(130, 23));
        topPane.add(sb.getComponent());
        frame.add(topPane, BorderLayout.PAGE_START);
        frame.add(getWorkspacePanel(), BorderLayout.CENTER);
        frame.add(getButtonPanel(), BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    public static void main(final String[] args) {
        if (args.length < 1) {
            System.err.println("usage: WorkspaceController lang_def.xml");
            System.exit(1);
        }
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                final WorkspaceController wc = new WorkspaceController();
                wc.setLangDefFilePath(args[0]);
                wc.loadFreshWorkspace();
                wc.createAndShowGUI();
            }
        });
    }

}
