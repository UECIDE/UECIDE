package edu.mit.blocks.workspace;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.mit.blocks.codeblocks.Block;
import edu.mit.blocks.renderable.FactoryRenderableBlock;
import edu.mit.blocks.renderable.RenderableBlock;

/**
 * Utilities class that provides the loading and saving of
 * pages and drawers
 *
 * @author An Ho
 */
public class PageDrawerLoadingUtils {

    private static Pattern attrExtractor = Pattern.compile("\"(.*)\"");

    private static String getNodeValue(Node node, String nodeKey) {
        Node opt_item = node.getAttributes().getNamedItem(nodeKey);
        if (opt_item != null) {
            Matcher nameMatcher = attrExtractor.matcher(opt_item.toString());
            if (nameMatcher.find()) {
                return nameMatcher.group(1);

            }
        }
        return null;
    }

    private static Color getColorValue(Node node, String nodeKey) {
        String color = getNodeValue(node, nodeKey);
        if (color != null) {
            StringTokenizer col = new StringTokenizer(color);
            if (col.countTokens() == 3) {
                return new Color(Integer.parseInt(col.nextToken()), Integer.parseInt(col.nextToken()), Integer.parseInt(col.nextToken()));
            }
        }
        return null;
    }

    public static boolean getBooleanValue(final Node node, final String nodeKey) {
        final String bool = getNodeValue(node, nodeKey);
        return bool == null || !"no".equals(bool);
    }

    private static int getIntValue(Node node, String nodeKey) {
        String num = getNodeValue(node, nodeKey);
        if (num != null) {
            return Integer.parseInt(num);
        }
        return 0;
    }

    public static void loadPagesAndDrawers(Workspace workspace, Element root, FactoryManager manager) {
        List<Page> pageList = new ArrayList<Page>();
        //pagesToAdd is needed so that we can add pages all at once
        //to the page bar once all the the pages been loaded
        //Before adding all the pages, this method makes a check
        //if there is only one page with an empty name - if so, it will just
        //add the page to the workspace/block canvas but not add it to this
        //LinkedHashMap<Page, PageBlockDrawer> pagesToAdd = new LinkedHashMap<Page, PageBlockDrawer>();
        LinkedHashMap<String, ArrayList<RenderableBlock>> blocksForDrawers = new LinkedHashMap<String, ArrayList<RenderableBlock>>();
        LinkedHashMap<Page, ArrayList<RenderableBlock>> blocksForPages = new LinkedHashMap<Page, ArrayList<RenderableBlock>>();


        NodeList pagesRoot = root.getElementsByTagName("Pages");
        if (pagesRoot != null) {
            //isBlankPage denotes if the page being loaded is a default blank page
            //in other words, the project did not specify any pages for their environment.
            //EvoBeaker does this
            boolean isBlankPage = false;
            Node pagesNode = pagesRoot.item(0);
            if (pagesNode == null) {
                return; // short-circuit exit if there's nothing to load
            }
            Node opt_item = pagesNode.getAttributes().getNamedItem("drawer-with-page");
            if (opt_item != null) {
                Matcher nameMatcher = attrExtractor.matcher(opt_item.toString());
                if (nameMatcher.find()) {
                    Workspace.everyPageHasDrawer = nameMatcher.group(1).equals("yes") ? true : false;
                }
            }
            opt_item = pagesNode.getAttributes().getNamedItem("is-blank-page");
            if (opt_item != null) {
                Matcher nameMatcher = attrExtractor.matcher(opt_item.toString());
                if (nameMatcher.find()) {
                    isBlankPage = nameMatcher.group(1).equals("yes") ? true : false;
                }
            }

            // whether pages should show a control to collapse them or not
            boolean collapsiblePages = getBooleanValue(pagesNode, "collapsible-pages");

            Page page;
            NodeList pages = pagesNode.getChildNodes();
            Node pageNode;
            String pageName;
            String pageDrawer;
            Color pageColor;
            boolean pageInFullView;
            int pageWidth;
            String pageId;
            for (int i = 0; i < pages.getLength(); i++) { //find them
                pageNode = pages.item(i);
                if (pageNode.getNodeName().equals("Page")) { // a page entry
                    pageName = getNodeValue(pageNode, "page-name");
                    pageColor = getColorValue(pageNode, "page-color");
                    pageWidth = getIntValue(pageNode, "page-width");
                    pageDrawer = getNodeValue(pageNode, "page-drawer");
                    pageInFullView = getBooleanValue(pageNode, "page-infullview");
                    pageId = getNodeValue(pageNode, "page-id");
                    page = new Page(workspace, pageName, pageWidth, 0, pageDrawer, pageInFullView, pageColor, collapsiblePages);
                    page.setPageId(pageId);

                    NodeList pageNodes = pageNode.getChildNodes();
                    String drawer = null;
                    if (Workspace.everyPageHasDrawer) {
                        //create drawer instance
                        manager.addDynamicDrawer(page.getPageDrawer());
                        ArrayList<RenderableBlock> drawerBlocks = new ArrayList<RenderableBlock>();

                        for (int k = 0; k < pageNodes.getLength(); k++) {
                            Node drawerNode = pageNodes.item(k);
                            if (drawerNode.getNodeName().equals("PageDrawer")) {
                                NodeList genusMembers = drawerNode.getChildNodes();
                                String genusName;
                                for (int j = 0; j < genusMembers.getLength(); j++) {
                                    Node genusMember = genusMembers.item(j);
                                    if (genusMember.getNodeName().equals("BlockGenusMember")) {
                                        genusName = genusMember.getTextContent();
                                        assert workspace.getEnv().getGenusWithName(genusName) != null : "Unknown BlockGenus: " + genusName;
                                        Block block = new Block(workspace, genusName);
                                        drawerBlocks.add(new FactoryRenderableBlock(workspace, manager, block.getBlockID()));
                                    }
                                }
                                blocksForDrawers.put(drawer, drawerBlocks);
                                break;  //there can only be one drawer for this page
                            }
                        }
                    }

                    if (isBlankPage) {
                        //place a blank page as the first page
                        workspace.putPage(page, 0);
                        //if the system uses blank pages, then we expect only one page
                        break;  //we anticipate only one page
                    } else {
                        //we add to the end of the set of pages
                        int position = pageList.size();
                        //add to workspace
                        if (position == 0) {
                            //replace the blank default page
                            workspace.putPage(page, 0);
                        } else {
                            workspace.addPage(page, position);
                        }
                        pageList.add(position, page);
                    }

                    blocksForPages.put(page, page.loadPageFrom(pageNode, false));
                }
            }
            //add blocks in drawers
            for (String d : blocksForDrawers.keySet()) {
                manager.addDynamicBlocks(blocksForDrawers.get(d), d);
            }
            //blocks in pages
            for (Page p : blocksForPages.keySet()) {
                p.addLoadedBlocks(blocksForPages.get(p), false);
            }
        }
    }

    public static void loadBlockDrawerSets(Workspace workspace, Element root, FactoryManager manager) {
        Pattern attrExtractor = Pattern.compile("\"(.*)\"");
        Matcher nameMatcher;
        NodeList drawerSetNodes = root.getElementsByTagName("BlockDrawerSet");
        Node drawerSetNode;
        for (int i = 0; i < drawerSetNodes.getLength(); i++) {
            drawerSetNode = drawerSetNodes.item(i);
            if (drawerSetNode.getNodeName().equals("BlockDrawerSet")) {
                NodeList drawerNodes = drawerSetNode.getChildNodes();
                Node drawerNode;
                //retreive drawer information of this bar
                for (int j = 0; j < drawerNodes.getLength(); j++) {
                    drawerNode = drawerNodes.item(j);
                    if (drawerNode.getNodeName().equals("BlockDrawer")) {
                        String drawerName = null;
                        Color buttonColor = Color.blue;
                        StringTokenizer col;
                        nameMatcher = attrExtractor.matcher(drawerNode.getAttributes().getNamedItem("name").toString());
                        if (nameMatcher.find()) {//will be true
                            drawerName = nameMatcher.group(1);
                        }

                        //get drawer's color:
                        Node colorNode = drawerNode.getAttributes().getNamedItem("button-color");
                        if (colorNode != null) {
                            nameMatcher = attrExtractor.matcher(colorNode.toString());
                            if (nameMatcher.find()) { //will be true
                                col = new StringTokenizer(nameMatcher.group(1));
                                if (col.countTokens() == 3) {
                                    buttonColor = new Color(Integer.parseInt(col.nextToken()), Integer.parseInt(col.nextToken()), Integer.parseInt(col.nextToken()));
                                } else {
                                    buttonColor = Color.BLACK;
                                }
                            }
                        }

                        manager.addStaticDrawer(drawerName, buttonColor);

                        //get block genuses in drawer and create blocks
                        NodeList drawerBlocks = drawerNode.getChildNodes();
                        Node blockNode;
                        ArrayList<RenderableBlock> drawerRBs = new ArrayList<RenderableBlock>();
                        for (int k = 0; k < drawerBlocks.getLength(); k++) {
                            blockNode = drawerBlocks.item(k);
                            if (blockNode.getNodeName().equals("BlockGenusMember")) {
                                String genusName = blockNode.getTextContent();
                                assert workspace.getEnv().getGenusWithName(genusName) != null : "Unknown BlockGenus: " + genusName;
                                Block newBlock;
                                //don't link factory blocks to their stubs because they will
                                //forever remain inside the drawer and never be active
                                newBlock = new Block(workspace, genusName, false);
                                drawerRBs.add(new FactoryRenderableBlock(workspace, manager, newBlock.getBlockID()));
                            }
                        }
                        manager.addStaticBlocks(drawerRBs, drawerName);
                    }
                }
            }
        }
    }
}
