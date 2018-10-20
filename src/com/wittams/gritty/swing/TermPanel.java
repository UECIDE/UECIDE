/* -*-mode:java; c-basic-offset:2; -*- */
/* JCTerm
 * Copyright (C) 2002-2004 ymnk, JCraft,Inc.
 *  
 * Written by: 2002 ymnk<ymnk@jcaft.com>
 *   
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.wittams.gritty.swing;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.awt.Color;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.wittams.gritty.BackBuffer;
import com.wittams.gritty.Emulator;
import com.wittams.gritty.RequestOrigin;
import com.wittams.gritty.ResizePanelDelegate;
import com.wittams.gritty.ScrollBuffer;
import com.wittams.gritty.SelectionRunConsumer;
import com.wittams.gritty.Style;
import com.wittams.gritty.StyleState;
import com.wittams.gritty.StyledRunConsumer;
import com.wittams.gritty.TerminalDisplay;
import com.wittams.gritty.Util;

public class TermPanel extends JComponent implements TerminalDisplay, ClipboardOwner, StyledRunConsumer {
	private static final long serialVersionUID = -1048763516632093014L;
	private static final double FPS = 20;

	private BufferedImage img;

	private Graphics2D gfx;

	private final Component termComponent = this;

	private Font normalFont;

	private Font boldFont;

	private int descent = 0;

	private int lineSpace = 0;

	Dimension charSize = new Dimension();

	Dimension termSize = new Dimension(80, 24);

	protected Point cursor = new Point();

	private boolean antialiasing = true;

	private Emulator emulator = null;

	protected Point selectionStart;

	protected Point selectionEnd;

	protected boolean selectionInProgress;

	private Clipboard clipBoard;

	private ResizePanelDelegate resizePanelDelegate;

	final private BackBuffer backBuffer;
	final private ScrollBuffer scrollBuffer;
	final private StyleState styleState;
	
	private final BoundedRangeModel brm = new DefaultBoundedRangeModel(0,80,0,80);

	protected int clientScrollOrigin;
	protected volatile int newClientScrollOrigin;
	protected volatile boolean shouldDrawCursor;
	private KeyListener keyHandler;

    public static final int CURSOR_BOX = 0;
    public static final int CURSOR_BLOCK = 1;

    private int cursorStyle = CURSOR_BLOCK;

    private boolean isEnabled = true;

	public TermPanel(BackBuffer backBuffer, ScrollBuffer scrollBuffer, StyleState styleState) {
		this.scrollBuffer = scrollBuffer;
		this.backBuffer = backBuffer;
		this.styleState = styleState;
		brm.setRangeProperties(0, termSize.height, - scrollBuffer.getLineCount() , termSize.height, false );
		
		normalFont = Font.decode("Monospaced-14");
		boldFont = normalFont.deriveFont(Font.BOLD);
		
		establishFontMetrics();

		setUpImages();
		setUpClipboard();
		setAntiAliasing(antialiasing);

		setPreferredSize(new Dimension(getPixelWidth(), getPixelHeight()));
		
		setFocusable(true);
		enableInputMethods(true);

		setFocusTraversalKeysEnabled(false);

		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(final MouseEvent e) {
				final Point charCoords = panelToCharCoords(e.getPoint());
				
				if (!selectionInProgress) {
					selectionStart = new Point(charCoords);
					selectionInProgress = true;
				}
				repaint();
				selectionEnd = charCoords;
				selectionEnd.x = Math.min(selectionEnd.x + 1, termSize.width );
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(final MouseEvent e) {
				selectionInProgress = false;
				if ( selectionStart != null && selectionEnd != null)
					copySelection(selectionStart, selectionEnd);
				repaint();
			}

			@Override
			public void mouseClicked(final MouseEvent e) {
				requestFocusInWindow();
				selectionStart = null;
				selectionEnd = null;
				if(e.getButton() == MouseEvent.BUTTON3 )
					pasteSelection();
				repaint();
			}
		});

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				sizeTerminalFromComponent();
			}
		});

        addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                cursorStyle = CURSOR_BLOCK;
                cursorChanged = true;
            }
            public void focusLost(FocusEvent e) {
                cursorStyle = CURSOR_BOX;
                cursorChanged = true;
            }
        });

		brm.addChangeListener(new ChangeListener() {
			public void stateChanged(final ChangeEvent e) {
				newClientScrollOrigin = brm.getValue();
			}
		});
		
		Timer redrawTimer = new Timer( (int) (1000/FPS) , new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				redrawFromDamage();	
			}
		});
		setDoubleBuffered(true);
		redrawTimer.start();
		repaint();
		
	}
	
	private Point panelToCharCoords(final Point p) {
		return new Point(p.x / charSize.width , p.y / charSize.height + clientScrollOrigin);
	}

	void setUpClipboard() {
		clipBoard = Toolkit.getDefaultToolkit().getSystemSelection();
		if (clipBoard == null)
			clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
	}
	
	private void copySelection(final Point selectionStart, final Point selectionEnd) {
		if (selectionStart == null || selectionEnd == null)
			return;
		
		Point top;
		Point bottom;

		if (selectionStart.y == selectionEnd.y) {
			/* same line */
			top = selectionStart.x < selectionEnd.x ? selectionStart
					: selectionEnd;
			bottom = selectionStart.x >= selectionEnd.x ? selectionStart
					: selectionEnd;
		} else {
			top = selectionStart.y < selectionEnd.y ? selectionStart
					: selectionEnd;
			bottom = selectionStart.y > selectionEnd.y ? selectionStart
					: selectionEnd;
		}
		
		final StringBuffer selection = new StringBuffer();
		if( top.y < 0 ){
			final Point scrollEnd = bottom.y >= 0 ? new Point(termSize.width, -1) : bottom;
			scrollBuffer.pumpRuns(top.y, scrollEnd.y - top.y, 
					new SelectionRunConsumer(selection, top, scrollEnd));
			
		}
		
		if( bottom.y >= 0 ){
			final Point backBegin = top.y < 0 ? new Point(0, 0) : top;
			backBuffer.pumpRuns( 0, backBegin.y, termSize.width, bottom.y -  backBegin.y + 1, 
					new SelectionRunConsumer(selection, backBegin, bottom));
		}
		
		if(selection.length() == 0) return;
		
		try {
			clipBoard.setContents(new StringSelection(selection.toString()), this);
		} catch (final IllegalStateException e) {
		}
	}
	
	void pasteSelection(){
		try {
			final String selection = (String) clipBoard.getData( DataFlavor.stringFlavor );
			emulator.sendBytes(selection.getBytes());
		} catch (final UnsupportedFlavorException e) {
			
		} catch (final IOException e) {
			
		}
	}
	/* Do not care
	 */
	public void lostOwnership(final Clipboard clipboard, final Transferable contents) {}

	private void setUpImages() {
		final BufferedImage oldImage = img;
        if (getPixelWidth() < 0) return;
        if (getPixelHeight() < 0) return;
		img = new BufferedImage(getPixelWidth(), getPixelHeight(),
				BufferedImage.TYPE_INT_RGB);
		
		gfx = img.createGraphics();
        gfx.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
		gfx.fillRect(0, 0, getPixelWidth(), getPixelHeight());

		if (oldImage != null){
			gfx.drawImage(oldImage, 0, img.getHeight() - oldImage.getHeight(),
					oldImage.getWidth(), oldImage.getHeight(), termComponent);
		}
	}

	private void sizeTerminalFromComponent() {
		if (emulator != null) {
			int newWidth = getWidth() / charSize.width ;
			int newHeight = getHeight() / charSize.height;

            if (newWidth == 0) newWidth = 1;
            if (newHeight == 0) newHeight = 1;

			final Dimension newSize = new Dimension(newWidth, newHeight);

			emulator.postResize(newSize, RequestOrigin.User);
		}
	}

	public void setEmulator(final Emulator emulator) {
		this.emulator = emulator;
		this.sizeTerminalFromComponent();
	}
	
	public void setKeyHandler(final KeyListener keyHandler){
		this.keyHandler = keyHandler;
	}

	public Dimension doResize(final Dimension newSize, final RequestOrigin origin) {
		if(!newSize.equals(termSize)){
            setAntiAliasing(antialiasing);
			backBuffer.lock();
			try{
				backBuffer.doResize(newSize, origin);
				termSize = (Dimension) newSize.clone();
				// resize images..
				setUpImages();
				
				final Dimension pixelDimension = new Dimension(getPixelWidth(), getPixelHeight());
				
				setPreferredSize( pixelDimension );
				if(resizePanelDelegate != null) resizePanelDelegate.resizedPanel( pixelDimension, origin);
				brm.setRangeProperties(0, termSize.height, - scrollBuffer.getLineCount() , termSize.height, false );
			
			}finally{
				backBuffer.unlock();
			}
		}
		return new Dimension(getPixelWidth(), getPixelHeight());
	}
	
	public void setResizePanelDelegate(final ResizePanelDelegate resizeDelegate) {
		resizePanelDelegate = resizeDelegate;
	}
	
	private void establishFontMetrics() {
		final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		final Graphics2D graphics = img.createGraphics();
		graphics.setFont(normalFont);
		
		final FontMetrics fo = graphics.getFontMetrics();
		descent = fo.getDescent();
		charSize.width = fo.charWidth('@');
		charSize.height = fo.getHeight() + lineSpace * 2;
		descent += lineSpace;
		
		img.flush();
		graphics.dispose();
	}

	@Override
	public void paintComponent(final Graphics g) {
		Graphics2D gfx = (Graphics2D) g;
        gfx.setRenderingHints(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));
		super.paintComponent(g);
		if (img != null){
		
			gfx.drawImage(img, 0, 0, termComponent);
			if (shouldDrawCursor) drawCursor(gfx);

            if (!isEnabled) {
                Color gray = new Color(128, 128, 128);
                for (int y = 0; y < getPixelHeight(); y++) {
                    for (int x = 0; x < getPixelWidth(); x++) {
                        if ((((y & 1) == 0) && ((x & 1) == 0)) ||
                            (((y & 1) == 1) && ((x & 1) == 1))) {
                            img.setRGB(x, y, gray.getRGB());
                        }
                    }
                }
            }


			drawSelection(gfx);
		}
	}

	@Override
	public void processKeyEvent(final KeyEvent e) {
		final int id = e.getID();
		if (id == KeyEvent.KEY_PRESSED){
			keyHandler.keyPressed(e);
	    }else if (id == KeyEvent.KEY_RELEASED) {
			/* keyReleased(e); */
		} else if (id == KeyEvent.KEY_TYPED){
			keyHandler.keyTyped(e);
		}
		e.consume();
	}

	

	public int getPixelWidth() {
		return charSize.width * termSize.width;
	}

	public int getPixelHeight() {
		return charSize.height * termSize.height;
	}

	public int getColumnCount() {
		return termSize.width;
	}

	public int getRowCount() {
		return termSize.height;
	}
	
	public void drawCursor(Graphics2D g) {
		final int y = (cursor.y - 1 - clientScrollOrigin);
		if(y >= 0 && y < termSize.height ){
			Style current = styleState.getCurrent();
			g.setColor(current.getForeground());
			g.setXORMode(current.getBackground());
            if (cursorStyle == CURSOR_BOX) {
                g.drawRect(cursor.x * charSize.width, y * charSize.height, 
                           charSize.width, charSize.height);
            } else {
                g.fillRect(cursor.x * charSize.width, y * charSize.height, 
                           charSize.width, charSize.height);
            }
		}
	}

	public void drawSelection(Graphics2D g) {
		/* which is the top one */
		Point top;
		Point bottom;
		Style current = styleState.getCurrent();
		g.setColor(current.getForeground());
		g.setXORMode(current.getBackground());
		if (selectionStart == null || selectionEnd == null)
			return;

		if (selectionStart.y == selectionEnd.y) {
			/* same line */
			if (selectionStart.x == selectionEnd.x)
				return;
			top = selectionStart.x < selectionEnd.x ? selectionStart
					: selectionEnd;
			bottom = selectionStart.x >= selectionEnd.x ? selectionStart
					: selectionEnd;

			g.fillRect(top.x * charSize.width, (top.y - clientScrollOrigin) * charSize.height,
					(bottom.x - top.x) * charSize.width, charSize.height);

		} else {
			top = selectionStart.y < selectionEnd.y ? selectionStart
					: selectionEnd;
			bottom = selectionStart.y > selectionEnd.y ? selectionStart
					: selectionEnd;
			/* to end of first line */
			g.fillRect(top.x * charSize.width, (top.y - clientScrollOrigin) * charSize.height,
					(termSize.width - top.x) * charSize.width, charSize.height);

			if (bottom.y - top.y > 1) {
				/* intermediate lines */
				g.fillRect(0, (top.y + 1 - clientScrollOrigin) * charSize.height,
						termSize.width * charSize.width, (bottom.y - top.y - 1)
								* charSize.height);
			}

			/* from beginning of last line */

			g.fillRect(0, (bottom.y  - clientScrollOrigin) * charSize.height, bottom.x
					* charSize.width, charSize.height);
		}
	}

	public void consumeRun(final int x, final int y, final Style style, final char[] buf, final int start, final int len) {
		gfx.setColor(style.getBackgroundForRun());
		gfx.fillRect(x * charSize.width, (y - clientScrollOrigin) * charSize.height, len * charSize.width, charSize.height);
		
		gfx.setFont( style.hasOption(Style.Option.BOLD) ? boldFont : normalFont );
		gfx.setColor(style.getForegroundForRun());
		
		int baseLine =  (y + 1 - clientScrollOrigin) * charSize.height - descent;
		gfx.drawChars(buf, start, len, x * charSize.width, baseLine );
		if(style.hasOption(Style.Option.UNDERSCORE)){
			gfx.drawLine(x * charSize.width , baseLine + 1, (x + len) * charSize.width , baseLine + 1);
		}
	}
	
	private void clientScrollOriginChanged(int oldOrigin) {
		int dy = clientScrollOrigin - oldOrigin  ;
		
		int dyPix = dy * charSize.height;
		
		gfx.copyArea(0, Math.max(0 , dyPix)  , 
				     getPixelWidth() , getPixelHeight() - Math.abs(dyPix), 
				     0, -dyPix );
		
		if(dy < 0){
			// Scrolling up; Copied down
			// New area at the top to be filled in - can only be from scroll buffer
			//
			
			scrollBuffer.pumpRuns(clientScrollOrigin, -dy, this);
		}else{
			//Scrolling down; Copied up
			// New area at the bottom to be filled - can be from both
			
			int oldEnd = oldOrigin + termSize.height;
			
			// Either its the whole amount above the back buffer + some more 
			// Or its the whole amount we moved
			// Or we are already out of the scroll buffer
			int portionInScroll = oldEnd < 0 ? Math.min(-oldEnd, dy) : 0; 
			
			int portionInBackBuffer = dy - portionInScroll ;
			
			if(portionInScroll > 0 ){
				scrollBuffer.pumpRuns( oldEnd, portionInScroll, this);
			}
			
			if(portionInBackBuffer > 0){
				backBuffer.pumpRuns(0, oldEnd + portionInScroll , termSize.width, portionInBackBuffer, this);
			}
			
		}
		
	}
	
	int noDamage = 0;
	int framesSkipped = 0;
	private boolean cursorChanged;
	
	public void redrawFromDamage(){
		
		final int newOrigin = newClientScrollOrigin;
		if(!backBuffer.tryLock()){
			if(framesSkipped >= 5){
				backBuffer.lock();
			}else{
				framesSkipped++;
				return;
			}
		}
		try{
			framesSkipped =0;
			
			boolean serverScroll = pendingScrolls.enact(gfx, getPixelWidth(), charSize.height );
			
			boolean clientScroll = clientScrollOrigin != newOrigin;
			if( clientScroll ){
		    	final int oldOrigin = clientScrollOrigin;
		    	clientScrollOrigin = newOrigin;
		    	clientScrollOriginChanged(oldOrigin);
		    }
			
			boolean hasDamage = backBuffer.hasDamage();
			if(hasDamage){
				noDamage = 0;
				
				backBuffer.pumpRunsFromDamage(this);
				backBuffer.resetDamage();
			}else{ 
				noDamage++;
			}
			
			if(serverScroll || clientScroll || hasDamage || cursorChanged){
				repaint();
				cursorChanged = false;
			}
		}finally{
			backBuffer.unlock();
		}
	}
	
	public void scrollArea(final int y, final int h, int dy) {
		if( dy < 0 ){ 
			//Moving lines off the top of the screen
			//TODO: Something to do with application keypad mode
			//TODO: Something to do with the scroll margins
			backBuffer.pumpRuns(0, y - 1, termSize.width, -dy, scrollBuffer);
			
			brm.setRangeProperties(0, termSize.height, - scrollBuffer.getLineCount() , termSize.height, false );
		}
		selectionStart = null;
		selectionEnd = null;
		pendingScrolls.add(y,h,dy);
	}
	
	static class PendingScrolls{
		int[] ys = new int[10];
		int[] hs = new int[10]; 
		int[] dys = new int[10];
		int scrollCount = -1;
		
		void ensureArrays(int index){
			int curLen = ys.length;
			if(index >= curLen){
				ys = Util.copyOf(ys, curLen * 2);
				hs = Util.copyOf(hs, curLen * 2);
				dys = Util.copyOf(dys, curLen * 2);
			}
		}
		
		void add(int y, int h, int dy){
			if(dy == 0) return;
			if( scrollCount >= 0 &&
			    y == ys[scrollCount] && 
			    h == hs[scrollCount] ){
				dys[scrollCount] += dy;
			}else{
				scrollCount++;
				ensureArrays(scrollCount);
				ys[scrollCount] = y;
				hs[scrollCount] = h;
				dys[scrollCount] = dy;
			}
		}
		
		boolean enact(Graphics2D gfx, int width, int charHeight){
			if(scrollCount < 0) return false;
			for(int i = 0; i <= scrollCount; i++ ){
				gfx.copyArea(0, ys[i] * charHeight, width, hs[i] * charHeight, 0, dys[i] * charHeight);
			}
			scrollCount = -1;
			return true;
		}
	}
	
	final PendingScrolls pendingScrolls = new PendingScrolls();
	
	public void setCursor(final int x, final int y) {
		cursor.x = x;
		cursor.y = y;
		cursorChanged = true;
	}

	public void beep() {
		Toolkit.getDefaultToolkit().beep();
	}

	public void setLineSpace(final int foo) {
		lineSpace = foo;
	}

	public void setAntiAliasing(final boolean foo) {
		if (gfx == null)
			return;
		antialiasing = foo;
		final java.lang.Object mode = foo ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
				: RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
		final RenderingHints hints = new RenderingHints(
				RenderingHints.KEY_TEXT_ANTIALIASING, mode);
		gfx.setRenderingHints(hints);
	}

	public BoundedRangeModel getBoundedRangeModel() {
		return brm ;
	}

	public BackBuffer getBackBuffer() {
		return backBuffer;
	}

	public ScrollBuffer getScrollBuffer() {
		return scrollBuffer;
	}

	public void lock() {
		backBuffer.lock();
	}
	
	public void unlock() {
		backBuffer.unlock();
	}

    public void setFont(Font f) {
        normalFont = f;
		boldFont = normalFont.deriveFont(Font.BOLD);
        
		establishFontMetrics();
    }

    public void setCursorEnabled(boolean ce) {
        shouldDrawCursor = ce;
        cursorChanged = true;
//        redrawFromDamage();
    }

    public void enableDisplay() {
        isEnabled = true;
        repaint();
    }

    public void disableDisplay() {
        isEnabled = false;
        repaint();
    }
	
}
