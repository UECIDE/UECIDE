package edu.mit.blocks.workspace;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.codeblockutil.CToolTip;
import edu.mit.blocks.renderable.RenderableBlock;

/**
 * A Page serves as both an abstract container of blocks
 * and also a graphical panel that renders its collection
 * of blocks.  Abstractly, a page has seven abstract fields:
 * a color, a name, a font, a drawer, width, a height,
 * and a set of blocks.  How it renders these abstract fields
 * depends on the state of the page, including: zoom level,
 * and minimumPixelWidth.
 *
 * A Page exists as a WorkspaceWidget, a SearchableContainer,
 * ISupportMemento, an RBParent, a Zoomable object, and a JPanel.
 * As a WorkspaceWidget, it can add, remove, blocks and manage
 * block manipulations within itself.  As a searchableContainer,
 * it can notify users that certain blocks have been queried.
 * As an ISupportMomento, it can undo the current values of
 * abstract fields.  As an RBParent, it can highlight blocks.
 *
 * Since a Page is both a Zoomable object and JPanel, Pages
 * separate its abstract model and view by allowing clients
 * to mutate its abstract fields directly.  But clients must
 * remember to reform the pages in order to synchronize the
 * data between the model and view.
 *
 * A page's abstract color is rendered the same no matter
 * what state the page is in.  A page's abstract name is
 * rendered thrice centered at every fourth of the page.
 * The name is rendered with a size depending on the zoom
 * level of that page (it tries to maintain a constant aspect
 * ratio).  The drawer name is not rendered.  The width and
 * height of the page is rendered differently depending on
 * the zoom level and minimumPixelWidth.  Using the zoom level,
 * it tries to maintain a constant aspect ratio but the
 * absolute sizes varies with a bigger/smaller zoom level.
 * the minimumPixelWidth limits the width from going below
 * a certain size, no matter what the system tries to set
 * the abstract width to be.  Finally the set of blocks are
 * rendered directly onto the page with the same transformation
 * as the ones imposed on the width and height of the page.
 *
 * As an implementation detail, a page tries to maintain a
 * separation between its abstract states and its view.
 * Clients of Pages should use reform*() methods to validate
 * information between the abstract states and view.  Clients
 * of Pages are warned against accessing Page.getJComponent(),
 * as the method provides clients a way to unintentionally mutate
 * an implementation specific detail of Pages.
 *
 * A Page implements ExplorerListener i.e. it listens for possible changes in
 * an explorer that affects the display of the page. When an explorer event
 * happens the page changes its display accordingly
 */
public class Page implements WorkspaceWidget, SearchableContainer, ISupportMemento {

    /** The workspace in use */
    private final Workspace workspace;

    /** Width while in collapsed mode */
    private static final int COLLAPSED_WIDTH = 20;
    /** The smallest value that this.minimumPixelWidth/zoom can be */
    private static final int DEFAULT_MINUMUM_WIDTH = 100;
    /** The default abstract width */
    private static final int DEFAULT_ABSTRACT_WIDTH = 700;
    /** The default abstract height */
    public static final int DEFAULT_ABSTRACT_HEIGHT = 3000;
    /** An empty string */
    private static final String emptyString = "";
    /** this.zoomLevel: zoom level state */
    static double zoom = 1.0;
    /** The JComponent of this page */
    private final PageJComponent pageJComponent = new PageJComponent();
    /** The abstract width of this page */
    private double abstractWidth;
    /** The abstract height of this page */
    private double abstractHeight;
    /** The name of the drawer that this page refers to */
    private String pageDrawer;
    /** The default page color.  OVERRIDED BY BLOCK CANVAS */
    private final Color defaultColor;
    /** MouseIn Flag: true if and only if the mouse is in this page */
    private boolean mouseIsInPage = false;
    /** The minimum width of the page in pixels */
    private int minimumPixelWidth = 0;
    /** Fullview */
    private boolean fullview;
    /** The GUI component for interfacing with the user
     * to help the user collapse or restore the page */
    private CollapseButton collapse;
    /** The user-time unique id of this page. Once set, cannot be changed. */
    private String pageId = null;
    /** Toggles to show/hide minimize page button. */
    private boolean hideMinimize = false;
    //////////////////////////////
    //Constructor/ Destructor	//
    //////////////////////////////

    /**
     * Constructs a new Page
     *
     * @param name - name of this page (this.name)
     * @param pageWidth - the abstract width of this page (this.width)
     * @param pageHeight - the abstract height of this page (this.height)
     * @param pageDrawer - the name of the page drawer that this page refers to
     *
     * @requires name != null && pageDrawer != null
     * @effects constructs a new Page such that:
     * 			1) The name of this page equals the argument "name".
     * 			2) The abstract width of this page equals "pageWidth".
     * 			   If "pageWidth is <= to zero, then set the
     * 			   width to the DEFAULT_ABSTRACT_WIDTH.
     * 			3) The abstract height of this page equals DEFAULT_ABSTRACT_HEIGHT.
     * 			4) The drawer name equals pageDrawer if and only if Workspace.everyPageHasDrawer==true.
     * 			5) The color of this page is null.
     * 			6) The font of this page is "Default", PLAIN, and 12.
     * 			7) The set of blocks is empty.
     */
    public Page(Workspace workspace, String name, int pageWidth, int pageHeight, String pageDrawer) {
        this(workspace, name, pageWidth, pageHeight, pageDrawer, true, null, true);
    }

    public Page(Workspace workspace, String name, int pageWidth, int pageHeight, String pageDrawer, boolean inFullview, Color defaultColor, boolean isCollapsible) {
        super();
        this.workspace = workspace;
        this.defaultColor = defaultColor;
        this.pageJComponent.setLayout(null);
        this.pageJComponent.setName(name);
        this.abstractWidth = pageWidth > 0 ? pageWidth : Page.DEFAULT_ABSTRACT_WIDTH;
        this.abstractHeight = Page.DEFAULT_ABSTRACT_HEIGHT;
        if (pageDrawer != null) {
            this.pageDrawer = pageDrawer;
        } else if (Workspace.everyPageHasDrawer) {
            this.pageDrawer = name;
        }
        this.pageJComponent.setOpaque(true);

        this.fullview = inFullview;
        this.collapse = new CollapseButton(inFullview, name);
        if (isCollapsible) {
            this.pageJComponent.add(collapse);
        }
        this.pageJComponent.setFullView(inFullview);
    }

    public void disableMinimize() {
        this.hideMinimize = true;
        this.collapse.repaint();
    }

    public void enableMinimize() {
        this.hideMinimize = false;
        this.collapse.repaint();
    }

    public void setHide(boolean hide) {
        this.hideMinimize = hide;
        this.collapse.repaint();
    }

    /**
     * Constructs a new Page
     *
     * @param workspace The workspace in use
     * @param name - name of this page (this.name)
     *
     * @requires name != null
     * @effects constructs a new Page such that:
     * 			1) The name of this page equals the argument "name".
     * 			2) The abstract width of this page equals DEFAULT_ABSTRACT_WIDTH.
     * 			3) The abstract height of this page equals DEFAULT_ABSTRACT_HEIGHT.
     * 			4) The drawer name equals "name"
     * 			5) The color of this page is null.
     * 			6) The font of this page is "Default", PLAIN, and 12.
     * 			7) The set of blocks is empty.
     */
    public Page(Workspace workspace, String name) {
        this(workspace, name, -1, -1, name);
    }

    /**
     * Constructs a new Page
     *
     * @param workspace The workspace in use
     * @requires none
     * @effects constructs a new Page such that:
     * 			1) The name of this page equals the argument "".
     * 			2) The abstract width of this page equals DEFAULT_ABSTRACT_WIDTH.
     * 			3) The abstract height of this page equals DEFAULT_ABSTRACT_HEIGHT.
     * 			4) The drawer name equals ""
     * 			5) The color of this page is null.
     * 			6) The font of this page is "Default", PLAIN, and 12.
     * 			7) The set of blocks is empty.
     */
    public static Page getBlankPage(Workspace workspace) {
        return new Page(workspace, emptyString);
    }

    /**
     * TODO: THIS METHOD NOT YET DOCUMENTED OR IMPLEMENTED
     * Removes all the RenderableBlock content of this.
     * Called when the Workspace is being reset.  Does not fire block
     * removed events.
     */
    public void reset() {
        this.pageJComponent.removeAll();
        Page.zoom = 1.0;
    }

    /**
     * Destructs this Page by setting its set of blocks to empty.
     * Does NOT fire block removed events.
     */
    public void clearPage() {
        for (RenderableBlock block : this.getBlocks()) {
            this.pageJComponent.remove(block);
        }
    }

    /**
     * Sets the page id. Consider the page id "final" but settable - once
     * set, it cannot be modified or unset.
     */
    public void setPageId(String id) {
        if (pageId == null) {
            pageId = id;
        } else {
            throw new RuntimeException("Tried to set pageId again: " + this);
        }
    }

    //////////////////////////////
    //Public Accessor			//
    //////////////////////////////
    /**
     * @return all the RenderableBlocks that reside within this page
     */
    @Override
    public Collection<RenderableBlock> getBlocks() {
        List<RenderableBlock> blocks = new ArrayList<RenderableBlock>();
        for (Component block : this.pageJComponent.getComponents()) {
            if (block instanceof RenderableBlock) {
                blocks.add((RenderableBlock) block);
            }
        }
        return blocks;
    }

    /**
     * @return a collection of top level blocks within this page (blocks with no
     * 			parents that and are the first block of each stack) or an empty
     * 			collection if no blocks are found on this page.
     */
    public Collection<RenderableBlock> getTopLevelBlocks() {
        List<RenderableBlock> topBlocks = new ArrayList<RenderableBlock>();
        for (RenderableBlock renderable : this.getBlocks()) {
            Block block = workspace.getEnv().getBlock(renderable.getBlockID());
            if (block.getPlug() == null || block.getPlugBlockID() == null || block.getPlugBlockID().equals(Block.NULL)) {
                if (block.getBeforeConnector() == null || block.getBeforeBlockID() == null || block.getBeforeBlockID().equals(Block.NULL)) {
                    topBlocks.add(renderable);
                    continue;
                }
            }
        }
        return topBlocks;
    }

    /**
     * Returns this page's id. Can be null, if id is not yet set.
     */
    public String getPageId() {
        return pageId;
    }

    /**
     * @return this page's name
     */
    public String getPageName() {
        return this.pageJComponent.getName();
    }

    /**
     * @return this page's color.  MAY RETURN NULL.
     */
    public Color getPageColor() {
        return this.pageJComponent.getBackground();
    }

    /**
     * @return this page's default color.  MAY RETURN NULL.
     */
    public Color getDefaultPageColor() {
        return this.defaultColor;
    }

    /**
     * @return this page's abstract width
     */
    public double getAbstractWidth() {
        return abstractWidth;
    }

    /**
     * @return this page's abstract height
     */
    public double getAbstractHeight() {
        return abstractHeight;
    }

    /**
     * @return this page drawer that this page refers to or null if non exists.
     * 			MAY RETURN NULL.
     */
    public String getPageDrawer() {
        return pageDrawer;
    }

    /**
     * @return icon of this.  MAY BE NULL
     */
    public Image getIcon() {
        return this.pageJComponent.getImage();
    }

    public boolean isInFullview() {
        return fullview;
    }

    //////////////////////////////
    //Rendering Mutators		//
    //////////////////////////////
    /**
     * @param newName - the new name of this page.
     *
     * @requires newName != null
     * @modifies this.name
     * @effects sets the name of this page to be newName.
     */
    public void setPageName(String newName) {
        if (pageDrawer.equals(this.pageJComponent.getName())) {
            pageDrawer = newName;
        }

        this.pageJComponent.setName(newName);
        this.collapse.setText(newName);

        //iterate through blocks and update the ones that are page label enabled
        for (RenderableBlock block : this.getBlocks()) {
            if (workspace.getEnv().getBlock(block.getBlockID()).isPageLabelSetByPage()) {
                workspace.getEnv().getBlock(block.getBlockID()).setPageLabel(this.getPageName());
                block.repaintBlock();
            }
        }

        PageChangeEventManager.notifyListeners();
    }

    /**
     * @param image - the new icon of this.  May be null
     *
     * @requires NONE
     * @modifies this.icon
     * @effects change this.icon to specified icon.  The new icon may be null
     */
    public void setIcon(Image image) {
        this.pageJComponent.setImage(image);
    }

    /**
     * @param newColor - the new color of this page
     *
     * @requires none
     * @modifies this.color
     * @effects Set the color of this page tobe newColor.
     * 			If newColor is null, sets the color to the deafult gray.
     */
    public void setPageColor(Color newColor) {
        this.pageJComponent.setBackground(newColor);
    }

    /**
     * @param deltaPixelWidth
     *
     * @requires Integer.MIN_VAL <= deltaPixelWidth <= Integer.MAX_VAL
     * @modifies this.width
     * @effects Adds deltaPixelWidth to the abstract width taking into
     * 			account the zoom level.  May need to convert form pixel to abstract model.
     */
    public void addPixelWidth(int deltaPixelWidth) {
        if (fullview) {
            this.setPixelWidth((int) (this.getAbstractWidth() * zoom + deltaPixelWidth));
        }
    }

    /**
     * @requires Integer.MIN_VAL <= pixelWidth <= Integer.MAX_VAL
     * @modifies this.width
     * @effects sets abstract width to pixelWidth taking into account the zoom level.
     * 			May need to convert form pixel to abstract model.

     */
    public void setPixelWidth(int pixelWidth) {
        if (pixelWidth < this.minimumPixelWidth) {
            this.abstractWidth = this.minimumPixelWidth / zoom;
        } else {
            this.abstractWidth = pixelWidth / zoom;
        }
    }

    //////////////////////////////
    //Reforming Mutators		//
    //////////////////////////////
    /**
     * @param pixelXCor - the new X location of page's JComponent in terms of pixels
     * @requires none
     * @return the current width of this page in terms of pixels
     * @modifies this.JComponent.size
     * @effects Reforms this page's JComponent in order to synchronize the
     * 			abstract width and height with the graphical view.
     * 			This process includes moving this page's JComponent to (pixelXCor,0)
     * 			and setting this page's JComponent size to (this.abstractwidth*zoom, this.abstractheight*zoom)
     */
    public int reformBounds(double pixelXCor) {
        if (fullview) {
            this.getJComponent().setBounds(
                    (int) (pixelXCor),
                    0,
                    (int) (this.abstractWidth * zoom),
                    (int) (this.abstractHeight * zoom));
            this.getJComponent().setFont(new Font("Ariel", Font.PLAIN, (int) (12 * zoom)));
            return (int) (this.abstractWidth * zoom);
        } else {
            this.getJComponent().setBounds(
                    (int) (pixelXCor),
                    0,
                    COLLAPSED_WIDTH + 2,
                    (int) (this.abstractHeight * zoom));
            this.getJComponent().setFont(new Font("Ariel", Font.PLAIN, (int) (12 * zoom)));
            return COLLAPSED_WIDTH + 2;
        }

    }

    /**
     * @param block - the new block being added whose position must be revalidated
     *
     * @requires block != null
     * @modifies block.location or this page's abstract width
     * @effects shifts this block into the page or increases the
     * 			width of this page to fit the new block.  It must then
     * 			notify listeners that the page's size may have changed
     */
    public void reformBlockPosition(RenderableBlock block) {
        //move blocks in
        Point p = SwingUtilities.convertPoint(block.getParent(), block.getLocation(), this.pageJComponent);
        if (p.x < block.getHighlightStrokeWidth() / 2 + 1) {
            block.setLocation(block.getHighlightStrokeWidth() / 2 + 1, p.y);
            block.moveConnectedBlocks();
            // the block has moved, so update p
            p = SwingUtilities.convertPoint(block.getParent(), block.getLocation(), this.pageJComponent);
        } else if (p.x + block.getWidth() + block.getHighlightStrokeWidth() / 2 + 1 > this.pageJComponent.getWidth()) {
            this.setPixelWidth(p.x + block.getWidth() + block.getHighlightStrokeWidth() / 2 + 1);
        }

        if (p.y < block.getHighlightStrokeWidth() / 2 + 1) {
            block.setLocation(p.x, block.getHighlightStrokeWidth() / 2 + 1);
            block.moveConnectedBlocks();
        } else if (p.y + block.getStackBounds().height + block.getHighlightStrokeWidth() / 2 + 1 > this.pageJComponent.getHeight()) {
            block.setLocation(p.x, this.pageJComponent.getHeight() - block.getStackBounds().height - block.getHighlightStrokeWidth() / 2 + 1);
            block.moveConnectedBlocks();
        }

        if (block.hasComment()) {
            //p = SwingUtilities.convertPoint(block.getComment().getParent(), block.getComment().getLocation(), this.pageJComponent);
            p = block.getComment().getLocation();
            if (p.x + block.getComment().getWidth() + 1 > this.pageJComponent.getWidth()) {
                this.setPixelWidth(p.x + block.getComment().getWidth() + 1);
            }
        }

        //repaint all pages
        PageChangeEventManager.notifyListeners();
    }

    /**
     * @modifies this.miniPixelWidth
     * @effects sets the minimumPixelWidth such that the following condition holds:
     * 			DEFAULT_MINIMUMWIDTH < new minimumPixelWidth &&
     * 			for each block, b, in this page's set of blocks {
     * 				b.x+b.width < new minimumPixelWidth}
     */
    public void reformMinimumPixelWidth() {
        minimumPixelWidth = 0; // reset min to 0

        // loop through blocks, growing min to fit each block
        for (RenderableBlock b : this.getBlocks()) {
            if (b.getX() + b.getWidth() + b.getHighlightStrokeWidth() / 2 > minimumPixelWidth) {
                // increase min width to fit this block
                minimumPixelWidth = b.getX() + b.getWidth() + b.getHighlightStrokeWidth() / 2 + 1;
            }

            if (b.hasComment()) {
                if (b.getComment().getX() + b.getComment().getWidth() > minimumPixelWidth) {
                    // increase min width to fit this block
                    minimumPixelWidth = b.getComment().getX() + b.getComment().getWidth() + 1;
                }
            }
        }
        if (this.minimumPixelWidth < Page.DEFAULT_MINUMUM_WIDTH * zoom) {
            this.minimumPixelWidth = (int) (Page.DEFAULT_MINUMUM_WIDTH * zoom);
        }
    }

    /**
     * @requires the current set of blocks of this page != null (though it may be empty)
     * @modifies all the block in this page's set of blocks
     * @effects Automatically arranges all the blocks within this page naively.
     */
    public void reformBlockOrdering() {
        BlockStackSorterUtil.sortBlockStacks(this, this.getTopLevelBlocks());
    }

    //////////////////////////////
    //Zoomable Interface		//
    //////////////////////////////
    /**
     * @param newZoom - the new zoom level
     *
     * @requires zoom != 0
     * @modifies zoom level
     * @effects Sets all the Zoomable Pages in contained in this BlockCanvas and
     * sets the zoom level to newZoom.
     */
    public static void setZoomLevel(double newZoom) {
        Page.zoom = newZoom;
    }

    /** @overrides Zoomable.getZoomLevel() */
    public static double getZoomLevel() {
        return Page.zoom;
    }

    //////////////////////////////
    //WORKSPACEWIDGET METHODS 	//
    //////////////////////////////
    /** @overrides WorkspaceWidget.blockDropped() */
    @Override
    public void blockDropped(RenderableBlock block) {
        //add to view at the correct location
        Component oldParent = block.getParent();
        block.setLocation(SwingUtilities.convertPoint(oldParent,
                block.getLocation(), this.pageJComponent));
        addBlock(block);
        this.pageJComponent.setComponentZOrder(block, 0);
        this.pageJComponent.revalidate();
    }

    /** @overrides WorkspaceWidget.blockDragged() */
    @Override
    public void blockDragged(RenderableBlock block) {
        if (mouseIsInPage == false) {
            mouseIsInPage = true;
            this.pageJComponent.repaint();
        }
    }

    /** @overrides WorkspaceWidget.blockEntered() */
    @Override
    public void blockEntered(RenderableBlock block) {
        if (mouseIsInPage == false) {
            mouseIsInPage = true;
            this.pageJComponent.repaint();
        }
    }

    /** @overrides WorkspaceWidget.blockExited() */
    @Override
    public void blockExited(RenderableBlock block) {
        mouseIsInPage = false;
        this.pageJComponent.repaint();
    }

    /** @overrides WorkspaceWidget.addBlock() */
    @Override
    public void addBlock(RenderableBlock block) {
        //update parent widget if dropped block
        WorkspaceWidget oldParent = block.getParentWidget();
        if (oldParent != this) {
            if (oldParent != null) {
                oldParent.removeBlock(block);
                if (block.hasComment()) {
                    block.getComment().getParent().remove(block.getComment());
                }
            }
            block.setParentWidget(this);
            if (block.hasComment()) {
                block.getComment().setParent(block.getParentWidget().getJComponent());
            }
        }

        this.getRBParent().addToBlockLayer(block);
        block.setHighlightParent(this.getRBParent());

        //if block has page labels enabled, in other words, if it can, then set page label to this
        if (workspace.getEnv().getBlock(block.getBlockID()).isPageLabelSetByPage()) {
            workspace.getEnv().getBlock(block.getBlockID()).setPageLabel(this.getPageName());
        }

        //notify block to link default args if it has any
        block.linkDefArgs();

        //fire to workspace that block was added to canvas if oldParent != this
        if (oldParent != this) {
            workspace.notifyListeners(new WorkspaceEvent(workspace, oldParent, block.getBlockID(), WorkspaceEvent.BLOCK_MOVED));
            workspace.notifyListeners(new WorkspaceEvent(workspace, this, block.getBlockID(), WorkspaceEvent.BLOCK_ADDED, true));
        }

        // if the block is off the edge, shift everything or grow as needed to fully show it
        this.reformBlockPosition(block);

        this.pageJComponent.setComponentZOrder(block, 0);
    }

    /**
     * @param blocks the Collection of RenderableBlocks to add
     *
     * @requires blocks != null
     * @modifies this page's set of blocks
     * @effects Add the collection of blocks internally and graphically,
     * 			delaying graphicalupdates until all of the blocks have been added.
     * @overrides WorkspaceWidget.blockEntered()
     */
    @Override
    public void addBlocks(Collection<RenderableBlock> blocks) {
        for (RenderableBlock block : blocks) {
            this.addBlock(block);
        }
        //since new components added, need to validate
        this.pageJComponent.revalidate();
    }

    /** @overrides WorkspaceWidget.removeBlock() */
    @Override
    public void removeBlock(RenderableBlock block) {
        this.pageJComponent.remove(block);
    }

    /** @overrides WorkspaceWidget.getJComponent() */
    @Override
    public JComponent getJComponent() {
        return this.pageJComponent;
    }

    /**
     * @return the RBParent representation of this Page
     */
    public RBParent getRBParent() {
        return (RBParent) this.pageJComponent;
    }

    /** @overrides WorkspaceWidget.contains() */
    @Override
    public boolean contains(int x, int y) {
        return this.pageJComponent.contains(x, y);
    }

    /** @overrides WorkspaceWidget.contains() */
    public boolean contains(Point p) {
        return this.contains(p.x, p.y);
    }

    /** Returns string representation of this */
    @Override
    public String toString() {
        return "Page name: " + getPageName() + " page color " + getPageColor() + " page width " + getAbstractWidth() + " page drawer " + pageDrawer;
    }

    //////////////////////////////////
    // SearchableContainer Methods	//
    //////////////////////////////////
    /** @overrides SearchableContainer.getSearchableElements */
    @Override
    public Iterable<RenderableBlock> getSearchableElements() {
        return getBlocks();
    }

    /** @overrides SearchableContainer.updateContainerSearchResults */
    @Override
    public void updateContainsSearchResults(boolean containsSearchResults) {
        // Do nothing, at least for now
    }

    //////////////////////////
    //SAVING AND LOADING	//
    //////////////////////////
    public ArrayList<RenderableBlock> loadPageFrom(Node pageNode, boolean importingPage) {
        //note: this code is duplicated in BlockCanvas.loadSaveString().
        NodeList pageChildren = pageNode.getChildNodes();
        Node pageChild;
        ArrayList<RenderableBlock> loadedBlocks = new ArrayList<RenderableBlock>();
        HashMap<Long, Long> idMapping = importingPage ? new HashMap<Long, Long>() : null;
        if (importingPage) {
            reset();
        }
        for (int i = 0; i < pageChildren.getLength(); i++) {
            pageChild = pageChildren.item(i);
            if (pageChild.getNodeName().equals("PageBlocks")) {
                NodeList blocks = pageChild.getChildNodes();
                Node blockNode;
                for (int j = 0; j < blocks.getLength(); j++) {
                    blockNode = blocks.item(j);
                    RenderableBlock rb = RenderableBlock.loadBlockNode(workspace, blockNode, this, idMapping);
                    // save the loaded blocks to add later
                    loadedBlocks.add(rb);
                }
                break;  //should only have one set of page blocks
            }
        }
        return loadedBlocks;
    }

    public void addLoadedBlocks(Collection<RenderableBlock> loadedBlocks, boolean importingPage) {
        for (RenderableBlock rb : loadedBlocks) {
            if (rb != null) {
                //add graphically
                getRBParent().addToBlockLayer(rb);
                rb.setHighlightParent(this.getRBParent());
                //System.out.println("loading rb to canvas: "+rb+" at: "+rb.getBounds());
                //add internallly
                workspace.notifyListeners(new WorkspaceEvent(workspace, this, rb.getBlockID(), WorkspaceEvent.BLOCK_ADDED));
                if (importingPage) {
                	workspace.getEnv().getBlock(rb.getBlockID()).setFocus(false);
                    rb.resetHighlight();
                    rb.clearBufferedImage();
                }
            }
        }


        //now we need to redraw all the blocks now that all renderable blocks
        //within this page have been loaded, to update the socket dimensions of
        //blocks, etc.
        for (RenderableBlock rb : this.getTopLevelBlocks()) {
            rb.redrawFromTop();
            if (rb.isCollapsed()) {
                //This insures that blocks connected to a collapsed top level block
                //are located properly and have the proper visibility set.
                //This doesn't work until all blocks are loaded and dimensions are set.
                rb.updateCollapse();
            }
        }
        this.pageJComponent.revalidate();
        this.pageJComponent.repaint();
    }

    public Node getSaveNode(Document document) {
    	Element pageElement = document.createElement("Page");

    	pageElement.setAttribute("page-name", getPageName());
    	pageElement.setAttribute("page-color", getPageColor().getRed() + " " + getPageColor().getGreen() + " " + getPageColor().getBlue());
    	pageElement.setAttribute("page-width", String.valueOf((int)getAbstractWidth()));
        if (fullview) {
            pageElement.setAttribute("page-infullview", "yes");
        } else {
            pageElement.setAttribute("page-infullview", "no");
        }
        if (pageDrawer != null) {
            pageElement.setAttribute("page-drawer", pageDrawer);
        }
        if (pageId != null) {
            pageElement.setAttribute("page-id", pageId);
        }

        //retrieve save strings of blocks within this Page
        Collection<RenderableBlock> blocks = this.getBlocks();
        if (blocks.size() > 0) {
            Element pageBlocksElement = document.createElement("PageBlocks");
            for (RenderableBlock rb : blocks) {
                pageBlocksElement.appendChild(rb.getSaveNode(document));
            }
            pageElement.appendChild(pageBlocksElement);
        }
    	return pageElement;
    }

    ////////////////////////////////////
    //State Saving Stuff for Undo/Redo//
    ////////////////////////////////////
    /**
     * a data structure that holds the name, width, color, set of blocks,
     * and set of renderable blocks in this page.
     */
    private class PageState {

        public String name;
        public String id;
        public int width;
        public Color color;
        public boolean fullview;
        public Map<Long, Object> blocks = new HashMap<Long, Object>();
        public Map<Long, Object> renderableBlocks = new HashMap<Long, Object>();
    }

    /** @overrides ISupportMomento.getState */
    @Override
    public Object getState() {
        PageState state = new PageState();
        //Populate basic page information
        state.name = getPageName();
        state.id = getPageId();
        state.color = getPageColor();
        state.width = this.pageJComponent.getWidth();
        //Fill in block information
        for (RenderableBlock rb : this.getBlocks()) {
            state.renderableBlocks.put(rb.getBlockID(), rb.getState());
        }
        return state;
    }

    /** @overrides ISupportMomento.loadState() */
    @Override
    public void loadState(Object memento) {
        assert (memento instanceof PageState) : "ISupportMemento contract violated in Page";
        if (memento instanceof PageState) {
            PageState state = (PageState) memento;
            //load basic page information
            this.setPageName(state.name);
            this.setPageId(state.id);
            this.setPageColor(state.color);
            this.setPixelWidth(state.width);
            //Load block information
            Map<Long, Object> renderableBlockStates = state.renderableBlocks;
            List<Long> unloadedRenderableBlockStates = new LinkedList<Long>();
            List<Long> loadedBlocks = new LinkedList<Long>();
            for (Long id : renderableBlockStates.keySet()) {
                unloadedRenderableBlockStates.add(id);
            }
            //First, load all the blocks that are in the state to be loaded
            //against all the blocks that already exist.
            for (RenderableBlock existingBlock : getBlocks()) {
                Long existingBlockID = existingBlock.getBlockID();
                if (renderableBlockStates.containsKey(existingBlockID)) {
                    existingBlock.loadState(renderableBlockStates.get(existingBlockID));
                    unloadedRenderableBlockStates.remove(existingBlockID);
                    loadedBlocks.add(existingBlockID);
                }
            }
            ArrayList<RenderableBlock> blocksToRemove = new ArrayList<RenderableBlock>();
            //Now, find all the blocks that don't exist in the save state and flag them to be removed.
            for (RenderableBlock existingBlock : this.getBlocks()) {
                Long existingBlockID = existingBlock.getBlockID();
                if (!loadedBlocks.contains(existingBlockID)) {
                    blocksToRemove.add(existingBlock);
                }
            }
            //This loop is necessary to avoid a concurrent modification error that occurs
            //if the loop above removes the block while iterating over an unmodifiable
            //iterator.
            for (RenderableBlock toBeRemovedBlock : blocksToRemove) {
                this.removeBlock(toBeRemovedBlock);
            }
            //Finally, add all the remaining blocks that weren't there before
            ArrayList<RenderableBlock> blocksToAdd = new ArrayList<RenderableBlock>();
            for (Long newBlockID : unloadedRenderableBlockStates) {
                RenderableBlock newBlock = new RenderableBlock(workspace, this, newBlockID);
                newBlock.loadState(renderableBlockStates.get(newBlockID));
                blocksToAdd.add(newBlock);
            }
            this.addBlocks(blocksToAdd);
            this.pageJComponent.repaint();
        }
    }

    private class CollapseButton extends JPanel implements MouseListener {

        private static final long serialVersionUID = 328149080273L;
        //To get the shadow effect the text must be displayed multiple times at
        //multiple locations.  x represents the center, white label.
        // o is color values (0,0,0,0.5f) and b is black.
        //			  o o
        //			o x b o
        //			o b o
        //			  o
        //offsetArrays representing the translation movement needed to get from
        // the center location to a specific offset location given in {{x,y},{x,y}....}
        //..........................................grey points.............................................black points
        private final int[][] shadowPositionArray = {{0, -1}, {1, -1}, {-1, 0}, {2, 0}, {-1, 1}, {1, 1}, {0, 2}, {1, 0}, {0, 1}};
        private final float[] shadowColorArray = {0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0, 0};
        private double offsetSize = 1;
        private String[] charSet;
        private int FONT_SIZE = 12;
        private boolean pressed = false;
        private boolean focus = false;
        private String button_text = "";

        public CollapseButton(boolean inFullview, String text) {
            super();
            //this.setBounds(0,0,COLLAPSED_WIDTH,COLLAPSED_WIDTH);
            this.addMouseListener(this);
            this.setOpaque(false);
            this.charSet = new String[0];
            this.setFont(new Font("Ariel", Font.BOLD, FONT_SIZE));
            this.setText(text);
            loadBounds(inFullview);
        }

        @Override
        public JToolTip createToolTip() {
            return new CToolTip(new Color(0xFFFFDD));
        }

        public void setText(String text) {
            if (text != null) {
                text = text.toUpperCase();
                List<String> characters = new ArrayList<String>();
                for (int i = 0; i < text.length(); i++) {
                    characters.add(text.substring(i, i + 1));
                }
                charSet = characters.toArray(charSet);
                this.button_text = text;
            }
        }

        private void paintFull(Graphics g) {
            paintFull(g, Color.white);
        }

        private void paintCollapsed(Graphics g) {
            paintCollapsed(g, Color.white);
        }

        private void paintFull(Graphics g, Color col) {
            int w = this.getWidth();
            g.setColor(col);
            g.fillRect(5, 5, w - 10, w - 17);
            g.setColor(col);
            g.drawRoundRect(3, 3, w - 6, w - 6, 3, 3);
        }

        private void paintCollapsed(Graphics g, Color col) {
            int w = this.getWidth();
            g.setColor(col);
            g.fillRect(5, 5, w - 10, w - 15);
            Graphics2D g2 = (Graphics2D) g;
            for (int j = 0; j < charSet.length; j++) {
                String c = charSet[j];
                int x = 5;
                int y = (j + 2) * (FONT_SIZE + 3);
                g.setColor(Color.black);
                for (int i = 0; i < shadowPositionArray.length; i++) {
                    int dx = shadowPositionArray[i][0];
                    int dy = shadowPositionArray[i][1];
                    g2.setColor(new Color(0.5f, 0.5f, 0.5f, shadowColorArray[i]));
                    g2.drawString(c, x + (int) ((dx) * offsetSize), y + (int) ((dy) * offsetSize));
                }
                g2.setColor(col);
                g2.drawString(c, x, y);
            }
            g2.drawRoundRect(3, 3, w - 6, w - 6 + charSet.length * (FONT_SIZE + 3), 3, 3);
        }

        @Override
        public void paintComponent(Graphics g) {
            int w = this.getWidth();

            if ((!Page.this.hideMinimize)) {
                if (fullview) {
                    this.setToolTipText("Collapse " + this.button_text);
                    paintFull(g);
                    if (pressed) {
                        g.setColor(Color.blue.darker());
                        g.fillRoundRect(3, 3, w - 6, w - 6, 3, 3);
                        paintFull(g);
                    } else {
                        if (focus) {
                            g.setColor(new Color(51, 153, 255)); //light blue
                            g.fillRoundRect(3, 3, w - 6, w - 6, 3, 3);
                            paintFull(g);
                        }
                    }
                } else {
                    this.setToolTipText("Restore " + this.button_text);
                    paintCollapsed(g);
                    if (pressed) {
                        g.setColor(Color.blue.darker());
                        g.fillRoundRect(3, 3, w - 6, w - 6 + charSet.length
                                * (FONT_SIZE + 3), 3, 3);
                        paintCollapsed(g);
                    } else {
                        if (focus) {
                            g.setColor(new Color(51, 153, 255)); //light blue
                            g.fillRoundRect(3, 3, w - 6, w - 6 + charSet.length
                                    * (FONT_SIZE + 3), 3, 3);
                            paintCollapsed(g);
                        }
                    }
                }
            } else {
                if (fullview) {
                    paintFull(g, Color.gray);
                } else {
                    paintCollapsed(g, Color.gray);
                }
            }

        }

        private void loadBounds(boolean fullview) {
            if (!fullview) {
                this.setBounds(0, 0, COLLAPSED_WIDTH, charSet.length
                        * (FONT_SIZE + 3) + COLLAPSED_WIDTH);
            } else {
                this.setBounds(0, 0, COLLAPSED_WIDTH, COLLAPSED_WIDTH);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {

            if ((!Page.this.hideMinimize)) {
                if (fullview) {
                    this.setBounds(0, 0, COLLAPSED_WIDTH, charSet.length
                            * (FONT_SIZE + 3) + COLLAPSED_WIDTH);
                } else {
                    this.setBounds(0, 0, COLLAPSED_WIDTH, COLLAPSED_WIDTH);
                }
                fullview = !fullview;
                pageJComponent.setFullView(fullview);
                PageChangeEventManager.notifyListeners();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if ((!Page.this.hideMinimize)) {
                pressed = true;
                this.repaint();
            }
        }

        public void mouseDragged(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if ((!Page.this.hideMinimize)) {
                pressed = false;
                this.repaint();
            }
        }

        public void mouseMoved(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if ((!Page.this.hideMinimize)) {
                focus = true;
                this.repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if ((!Page.this.hideMinimize)) {
                focus = false;
                this.repaint();
            }
        }
    }
}

/**
 * This class serves as the zoomable JComponent and RBParent of the page
 * that wraps it.
 */
class PageJComponent extends JLayeredPane implements RBParent {

    private static final long serialVersionUID = 83982193213L;
    private static final Integer BLOCK_LAYER = 1;
    private static final Integer HIGHLIGHT_LAYER = 0;
    private static final int IMAGE_WIDTH = 60;
    private Image image = null;
    private boolean fullview = true;

    public void setFullView(boolean isFullView) {
        this.fullview = isFullView;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    /**
     * renders this JComponent
     */
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        //paint page
        super.paintComponent(g);
        //set label color
        if (this.getBackground().getBlue() + this.getBackground().getGreen() + this.getBackground().getRed() > 400) {
            g.setColor(Color.DARK_GRAY);
        } else {
            g.setColor(Color.LIGHT_GRAY);
        }

        //paint label at correct position
        if (fullview) {
            int xpos = (int) (this.getWidth() * 0.5 - g.getFontMetrics().getStringBounds(this.getName(), g).getCenterX());
            g.drawString(this.getName(), xpos, getHeight() / 2);
            g.drawString(this.getName(), xpos, getHeight() / 4);
            g.drawString(this.getName(), xpos, getHeight() * 3 / 4);


            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.33F));
            int imageX = (int) (this.getWidth() / 2 - IMAGE_WIDTH / 2 * Page.zoom);
            int imageWidth = (int) (IMAGE_WIDTH * Page.zoom);
            g.drawImage(this.getImage(), imageX, getHeight() / 2 + 5, imageWidth, imageWidth, null);
            g.drawImage(this.getImage(), imageX, getHeight() / 4 + 5, imageWidth, imageWidth, null);
            g.drawImage(this.getImage(), imageX, getHeight() * 3 / 4 + 5, imageWidth, imageWidth, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
        }

    }

    //////////////////////////////////
    //RBParent implemented methods	//
    //////////////////////////////////
    /** @overrides RBParent.addToBlockLayer() */
    @Override
    public void addToBlockLayer(Component c) {
        this.add(c, BLOCK_LAYER);

    }

    /** @overrides RBParent.addToHighlightLayer() */
    @Override
    public void addToHighlightLayer(Component c) {
        this.add(c, HIGHLIGHT_LAYER);
    }
}

/**
 * A BlockStatckSortUtil is a utilities class that serves to order
 * blocks from closest to furthest blocks (relative to the x=0 axis).
 */
class BlockStackSorterUtil {

    /** The minimum bounds between blocks */
    private static final int BUFFER_BETWEEN_BLOCKS = 20;
    /** A helper rectangle that maintains the bounds between blocks */
    private static final Rectangle positioningBounds = new Rectangle(BUFFER_BETWEEN_BLOCKS, BUFFER_BETWEEN_BLOCKS, 0, 0);
    /** An ordered set of blocks.  Blocks are ordered from closest to furthest (relative to x=0 axis) */
    private static final TreeSet<RenderableBlock> blocksToArrange = new TreeSet<RenderableBlock>(
            //TODO ria for now they are ordered in y-coor order
            //this naive ordering will also fail if two blocks have the same coordinates
            new Comparator<RenderableBlock>() {

        @Override
        public int compare(RenderableBlock rb1, RenderableBlock rb2) {
            if (rb1 == rb2) {
                return 0;
            } else {
                //translate points to a common reference: the parent of rb1
                Point pt1 = rb1.getLocation();
                Point pt2 = SwingUtilities.convertPoint(rb2.getParentWidget().getJComponent(),
                        rb2.getLocation(), rb1.getParentWidget().getJComponent());
                if (pt1.getY() < pt2.getY()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    });

    /**
     * This method serves to help clients sort blocks within a page
     * in some manner.
     *
     * @param page
     * @param topLevelBlocks
     *
     * @requires page != null && topLevelBlocks != null
     * @modifies the location of all topLevelBlocks
     * @effects sort the topLevelBlocks and move them to an order location on the page
     */
    protected static void sortBlockStacks(Page page, Collection<RenderableBlock> topLevelBlocks) {
        blocksToArrange.clear();
        positioningBounds.setBounds(BUFFER_BETWEEN_BLOCKS, BUFFER_BETWEEN_BLOCKS, 0, BUFFER_BETWEEN_BLOCKS);
        //created an ordered list of blocks based on x-coordinate position
        blocksToArrange.addAll(topLevelBlocks);

        //Naively places blocks from top to bottom, left to right.
        for (RenderableBlock block : blocksToArrange) {
            Rectangle bounds = block.getStackBounds();
            if (positioningBounds.height + bounds.height > page.getJComponent().getHeight()) {
                //need to go to next column
                positioningBounds.x = positioningBounds.x + positioningBounds.width + BUFFER_BETWEEN_BLOCKS;
                positioningBounds.width = 0;
                positioningBounds.height = BUFFER_BETWEEN_BLOCKS;
            }
            block.setLocation(positioningBounds.x, positioningBounds.height);

            //sets the x and y position for when workspace is unzoomed
            block.setUnzoomedX(block.calculateUnzoomedX(positioningBounds.x));
            block.setUnzoomedY(block.calculateUnzoomedY(positioningBounds.height));
            block.moveConnectedBlocks();

            //update positioning bounds
            positioningBounds.width = Math.max(positioningBounds.width, bounds.width);
            positioningBounds.height = positioningBounds.height + bounds.height + BUFFER_BETWEEN_BLOCKS;

            if (positioningBounds.x + positioningBounds.width > page.getJComponent().getWidth()) {
                //resize page to the difference
                page.addPixelWidth(positioningBounds.x + positioningBounds.width - page.getJComponent().getWidth());
            }
        }
    }
}
