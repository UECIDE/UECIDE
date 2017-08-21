/**
 * 
 */
package com.wittams.gritty;

import java.awt.Color;
import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.EnumSet;

public class TerminalWriter {
	private final int tab = 8;
	
	private int scrollRegionTop;
	private int scrollRegionBottom;
	private int cursorX = 0;
	private int cursorY = 1;
	
	private int termWidth = 80;
	private int termHeight = 24;
	
	private final TerminalDisplay display;
	private final BackBuffer backBuffer;
	private final StyleState styleState;
	
	private final EnumSet<Mode> modes = EnumSet.of(Mode.ANSI);

	public TerminalWriter(final TerminalDisplay term, final BackBuffer buf, final StyleState styleState) {
		this.display = term;
		this.backBuffer = buf;
		this.styleState = styleState;

		termWidth = term.getColumnCount();
		termHeight = term.getRowCount();

		scrollRegionTop = 1;
		scrollRegionBottom = termHeight;
	}
	
	public void setMode(Mode mode){
		modes.add(mode);
		switch(mode){
		case WideColumn:
			resize(new Dimension(132, 24), RequestOrigin.Remote );
			clearScreen();
			restoreCursor(null);
			break;
		}
	}
	
	public void unsetMode(Mode mode){
		modes.remove(mode);
		switch(mode){
		case WideColumn:
			resize(new Dimension(80, 24), RequestOrigin.Remote);
			clearScreen();
			restoreCursor(null);
			break;
		}
	}

	private void wrapLines() {
		if (cursorX >= termWidth) {
			cursorX = 0;
			cursorY += 1;
		}
	}

	private void finishText() {
		display.setCursor(cursorX, cursorY);
		scrollY();
	}

	public void writeASCII(final byte[] chosenBuffer, final int start,
			final int length) {
		backBuffer.lock();
		try {
			wrapLines();
			if (length != 0) {
				backBuffer.clearArea(cursorX, cursorY - 1, cursorX + length, cursorY);
				backBuffer.drawBytes(chosenBuffer, start, length, cursorX, cursorY);
			}
			cursorX += length;
			finishText();
		} finally {
			backBuffer.unlock();
		}
	}

	public void writeDoubleByte(final byte[] bytesOfChar) throws UnsupportedEncodingException {
		writeString(new String(bytesOfChar, 0, 2, "EUC-JP"));
	}
	
	public void writeString(String string) {
		backBuffer.lock();
		try {
			wrapLines();
			backBuffer.clearArea(cursorX, cursorY - 1, cursorX + string.length(), cursorY);
			backBuffer.drawString(string, cursorX, cursorY);
			cursorX += string.length();
			finishText();
		} finally {
			backBuffer.unlock();
		}
	}
	
	public void writeUnwrappedString(String string){
		int length = string.length();
		int off = 0;
		while(off < length ){
			int amountInLine = Math.min(distanceToLineEnd(), length - off);
			writeString( string.substring(off, off + amountInLine) );
			wrapLines();
			scrollY();
			off += amountInLine;
		}
	}
	
	
	public void scrollY() {
		backBuffer.lock();
		try {
			if (cursorY > scrollRegionBottom) {
				final int dy = scrollRegionBottom - cursorY;
				cursorY = scrollRegionBottom;
				scrollArea(scrollRegionTop, scrollRegionBottom
						- scrollRegionTop, dy);
				backBuffer.clearArea(0, cursorY - 1, termWidth, cursorY);
				display.setCursor(cursorX, cursorY);
			}
		} finally {
			backBuffer.unlock();
		}
	}

	public void newLine() {
		cursorY += 1;
		display.setCursor(cursorX, cursorY);
		scrollY(); 
	}

	public void backspace() {
		cursorX -= 1;
		if (cursorX < 0) {
			cursorY -= 1;
			cursorX = termWidth - 1;
		}
		display.setCursor(cursorX, cursorY);
	}

	public void carriageReturn() {
		cursorX = 0;
		display.setCursor(cursorX, cursorY);
	}

	public void horizontalTab() {
		cursorX = (cursorX / tab + 1) * tab;
		if (cursorX >= termWidth) {
			cursorX = 0;
			cursorY += 1;
		}
		display.setCursor(cursorX, cursorY);
	}

	public void eraseInDisplay(final ControlSequence args) {
		// ESC [ Ps J
		backBuffer.lock();
		try {
			final int arg = args.getArg(0, 0);
			int beginY;
			int endY;

			switch (arg) {
			case 0:
				// Initial line
				if (cursorX < termWidth) {
					backBuffer.clearArea(cursorX, cursorY - 1, termWidth, cursorY);
				}
				// Rest
				beginY = cursorY;
				endY = termHeight;

				break;
			case 1:
				// initial line
				backBuffer.clearArea(0, cursorY - 1, cursorX + 1, cursorY);

				beginY = 0;
				endY = cursorY - 1;
				break;
			case 2:
				beginY = 0;
				endY = termHeight;
				break;
			default:
				beginY = 1;
				endY = 1;
				break;
			}
			// Rest of lines
			if (beginY != endY)
				clearLines(beginY, endY);
		} finally {
			backBuffer.unlock();
		}
	}

	public void clearLines(final int beginY, final int endY) {
		backBuffer.lock();
		try {
			backBuffer.clearArea(0, beginY, termWidth, endY);
		} finally {
			backBuffer.unlock();
		}
	}

	public void clearScreen() {
		clearLines(0, termHeight);
	}

	public void eraseInLine(final ControlSequence args) {
		// ESC [ Ps K
		final int arg = args.getArg(0, 0);
		eraseInLine(arg);
	}
	
	public void eraseInLine(int arg){
		backBuffer.lock();
		try {
			switch (arg) {
			case 0:
				if (cursorX < termWidth) {
					backBuffer.clearArea(cursorX, cursorY - 1, termWidth, cursorY);
				}
				break;
			case 1:
				final int extent = Math.min(cursorX + 1, termWidth);
				backBuffer.clearArea(0, cursorY - 1, extent, cursorY);
				break;
			case 2:
				backBuffer.clearArea(0, cursorY - 1, termWidth, cursorY);
				break;
			default:
				break;
			}
		} finally {
			backBuffer.unlock();
		}
	}

	public void cursorUp(final ControlSequence args) {
		backBuffer.lock();
		try {
			int arg = args.getArg(0, 0);
			arg = arg == 0 ? 1 : arg;

			cursorY -= arg;
			cursorY = Math.max(cursorY, 1);
			display.setCursor(cursorX, cursorY);
		} finally {
			backBuffer.unlock();
		}
	}

	public void cursorDown(final ControlSequence args) {
		backBuffer.lock();
		try {
			int arg = args.getArg(0, 0);
			arg = arg == 0 ? 1 : arg;
			cursorY += arg;
			cursorY = Math.min(cursorY, termHeight);
			display.setCursor(cursorX, cursorY);
		} finally {
			backBuffer.unlock();
		}
	}

	public void index() {
		backBuffer.lock();
		try {
			if (cursorY == termHeight) {
				scrollArea(scrollRegionTop, scrollRegionBottom
						- scrollRegionTop, -1);
				backBuffer.clearArea(0, scrollRegionBottom - 1, termWidth,
						scrollRegionBottom);
			} else {
				cursorY += 1;
				display.setCursor(cursorX, cursorY);
			}
		} finally {
			backBuffer.unlock();
		}
	}

	// Dodgy ?
	private void scrollArea(int y, int h, int dy){
		display.scrollArea(y,h,dy);
		backBuffer.scrollArea(y, h, dy);
	}

	public void nextLine() {
		backBuffer.lock();
		try {
			cursorX = 0;
			if (cursorY == termHeight) {
				scrollArea(scrollRegionTop, scrollRegionBottom
						- scrollRegionTop, -1);
				backBuffer.clearArea(0, scrollRegionBottom - 1, termWidth,
						scrollRegionBottom);
			}else{
				cursorY += 1;
			}
			display.setCursor(cursorX, cursorY);
		} finally {
			backBuffer.unlock();
		}
	}

	public void reverseIndex() {
		backBuffer.lock();
		try {
			if (cursorY == 1) {
				scrollArea(scrollRegionTop - 1, scrollRegionBottom
						- scrollRegionTop, 1);
				backBuffer.clearArea(cursorX, cursorY - 1, termWidth, cursorY);
			} else {
				cursorY -= 1;
				display.setCursor(cursorX, cursorY);
			}
		} finally {
			backBuffer.unlock();
		}
	}

	public void cursorForward(final ControlSequence args) {
		int arg = args.getArg(0, 1);
		arg = arg == 0 ? 1 : arg;
		cursorX += arg;
		cursorX = Math.min(cursorX, termWidth - 1);
		display.setCursor(cursorX, cursorY);
	}

	public void cursorBackward(final ControlSequence args) {
		int arg = args.getArg(0, 1);
		arg = arg == 0 ? 1 : arg;
		cursorX -= arg;
		cursorX = Math.max(cursorX, 0);
		display.setCursor(cursorX, cursorY);
	}

	public void cursorPosition(final ControlSequence args) {
		final int argy = args.getArg(0, 1);
		final int argx = args.getArg(1, 1);
		cursorX = argx - 1;
		cursorY = argy;
		display.setCursor(cursorX, cursorY);
	}

	public void setScrollingRegion(final ControlSequence args) {
		final int y1 = args.getArg(0, 1);
		final int y2 = args.getArg(1, termHeight);

		scrollRegionTop = y1;
		scrollRegionBottom = y2;
	}

	/*
	 * Character Attributes
	 * 
	 * ESC [ Ps;Ps;Ps;...;Ps m
	 * 
	 * Ps refers to a selective parameter. Multiple parameters are separated by
	 * the semicolon character (0738). The parameters are executed in order and
	 * have the following meanings: 0 or None All Attributes Off 1 Bold on 4
	 * Underscore on 5 Blink on 7 Reverse video on
	 * 
	 * Any other parameter values are ignored.
	 */

	static Color[] colors = { Color.BLACK, Color.RED, Color.GREEN,
			Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.WHITE };

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jcraft.jcterm.ITerminalWriter#setCharacterAttributes(int[], int)
	 */
	public void setCharacterAttributes(final ControlSequence args) {
		final int argCount = args.getCount();
		if (argCount == 0)
			styleState.reset();

		for (int i = 0; i < argCount; i++) {
			final int arg = args.getArg(i, -1);
			if (arg == -1) {
				continue;
			}

			switch (arg) {
			case 0:
				styleState.reset();
				break;
			case 1:// Bright
				styleState.setOption(Style.Option.BOLD, true);
				break;
			case 2:// Dim
				styleState.setOption(Style.Option.DIM, true);
				break;
			case 4:// Underscore on
				styleState.setOption(Style.Option.UNDERSCORE, true);
				break;
			case 5:// Blink on
				styleState.setOption(Style.Option.BLINK, true);
				break;
			case 7:// Reverse video on
				styleState.setOption(Style.Option.REVERSE, true);
				break;
			case 8: // Hidden
				styleState.setOption(Style.Option.HIDDEN, true);
				break;
			default:
				if (arg >= 30 && arg <= 37){
					styleState.setCurrentForeground(colors[arg - 30]);
				}else if (arg >= 40 && arg <= 47){
					styleState.setCurrentBackground(colors[arg - 40]);
				}else{
				}
			}
		}
	}

	public void beep() {
		display.beep();
	}

	public int distanceToLineEnd() {
		return termWidth - cursorX;
	}

	public void storeCursor(final StoredCursor storedCursor) {
		storedCursor.x = cursorX;
		storedCursor.y = cursorY;
	}

	public void restoreCursor(final StoredCursor storedCursor) {
		cursorX = 0;
		cursorY = 1;
		if (storedCursor != null) {
			// TODO: something with origin modes
			cursorX = storedCursor.x;
			cursorY = storedCursor.y;
		}
		display.setCursor(cursorX, cursorY);
	}

	public Dimension resize(final Dimension pendingResize, final RequestOrigin origin) {
		final int oldHeight = termHeight;
		final Dimension pixelSize = display.doResize(pendingResize, origin);

		termWidth = display.getColumnCount();
		termHeight = display.getRowCount();

		scrollRegionBottom += termHeight - oldHeight;
		cursorY += termHeight - oldHeight;
		cursorY = Math.max(1, cursorY);
		return pixelSize;
	}

	public void fillScreen(final char c) {
		backBuffer.lock();
		try {
			final char[] chars = new char[termWidth];
			Arrays.fill(chars, c);
			final String str = new String(chars);

			for (int row = 1; row <= termHeight; row++){
				backBuffer.drawString(str, 0, row);
			}
		} finally {
			backBuffer.unlock();
		}
	}


}
