/* -*-mode:java; c-basic-offset:2; -*- */
/* JCTerm
 * Copyright (C) 2002 ymnk, JCraft,Inc.
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

package com.wittams.gritty;

import static com.wittams.gritty.CharacterUtils.*;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InterruptedIOException;

public class Emulator {
	private final TerminalWriter tw;
	final protected TtyChannel channel;

	public Emulator(final TerminalWriter tw, final TtyChannel channel) {
		this.channel = channel;
		this.tw = tw;
	}

	public void sendBytes(final byte[] bytes) throws IOException {
		channel.sendBytes(bytes);
	}

	public void start(){
		go();
	}

	public byte[] getCode(final int key){
		return CharacterUtils.getCode(key);
	}

	void go() {
		try {
			while ( ! Thread.currentThread().isInterrupted() ){
				singleIteration();	
			}
		} catch (final InterruptedIOException e){
		} catch (final Exception e) {
		}
	}
	
	public void postResize(final Dimension dimension, final RequestOrigin origin) {
		Dimension pixelSize;
		synchronized (tw) {
			pixelSize = tw.resize(dimension, origin);
		}
		channel.postResize(dimension, pixelSize);
	}

	void singleIteration() throws IOException {
		byte b = channel.getChar();
		
		switch (b) {
		case 0:
			break;
		case ESC: // ESC
			b = channel.getChar();
			handleESC(b);
			break;
		case BEL:
			tw.beep(); 
			break;
		case BS:
			tw.backspace();
			break;
		case TAB: // ht(^I) TAB
			tw.horizontalTab();
			break;
		case CR:
			tw.carriageReturn();
			break;
		case FF:
		case VT:
		case LF:
			// '\n'
			tw.newLine();
			break;
		default:
			if( b <= CharacterUtils.US ){
			} else if ( b > CharacterUtils.DEL ) { 
				//TODO: double byte character.. this is crap
				final byte[] bytesOfChar = new byte[2];
				bytesOfChar[0] = b;
				bytesOfChar[1] = channel.getChar();
				tw.writeDoubleByte(bytesOfChar);
			} else{
				channel.pushChar(b);
				final int availableChars = channel.advanceThroughASCII(tw.distanceToLineEnd());
				tw.writeASCII(channel.buf, channel.offset - availableChars, availableChars);
			}
			break;
		}
	}

	private void handleESC(byte initByte) throws IOException {
		byte b = initByte;
		if (b == '['){
			doControlSequence();
		} else {
			final byte[] intermediate = new byte[10];
			int intCount = 0;
			while (b >= 0x20 && b <= 0x2F) {
				intCount++;
				intermediate[intCount - 1] = b;
				b = channel.getChar();
			}
			if (b >= 0x30 && b <= 0x7E)
				synchronized(tw){
					switch (b) {
					case 'M':
						// Reverse index ESC M
						tw.reverseIndex();
						break;
					case 'D':
						// Index ESC D
						tw.index();
						break;
					case 'E':
						tw.nextLine();
						break;
					case '7':
						saveCursor();
						break;
					case '8':
						if(intCount > 0 && intermediate[0] == '#' )
							tw.fillScreen('E');
						else
							restoreCursor();
						break;
					default:
					}
				}
			else {
				// Push backwards
				for (int i = intCount - 1; i >= 0 ; i--) {
					final byte ib = intermediate[i];
					channel.pushChar(ib);
				}
				channel.pushChar(b);
			}
		}
	}

	StoredCursor storedCursor = null;

	private void saveCursor() {

		if (storedCursor == null)
			storedCursor = new StoredCursor();
		tw.storeCursor(storedCursor);
	}

	private void restoreCursor() {
		tw.restoreCursor(storedCursor);
	}

	private String escapeSequenceToString(final byte[] intermediate,
			final int intCount, final byte b) {
		
		StringBuffer sb = new StringBuffer("ESC ");
		
		for (int i = 0; i < intCount; i++) {
			final byte ib = intermediate[i];
			sb.append(' ');
			sb.append((char) ib);
		}
		sb.append(' ');
		sb.append((char) b);
		return sb.toString();
	}

	private void doControlSequence() throws IOException {
		final ControlSequence cs = new ControlSequence(channel);
		
		if(cs.pushBackReordered(channel)) return;
		
		synchronized (tw) {

		switch (cs.getFinalChar()) {
			case 'm':
				tw.setCharacterAttributes(cs);
				break;
			case 'r':
				tw.setScrollingRegion(cs);
				break;
			case 'A':
				tw.cursorUp(cs);
				break;
			case 'B':
				tw.cursorDown(cs);
				break;
			case 'C':
				tw.cursorForward(cs);
				break;
			case 'D':
				tw.cursorBackward(cs);
				break;
			case 'f':
			case 'H':
				tw.cursorPosition(cs);
				break;
			case 'K':
				tw.eraseInLine(cs);
				break;
			case 'J':
				tw.eraseInDisplay(cs);
				break;
			case 'h':
				setModes(cs, true);
				break;
			case 'l':
				setModes(cs, false);
				break;
			case 'c':
				// What are you
				// ESC [ c or ESC [ 0 c
				// Response is ESC [ ? 6 c
				channel.sendBytes(deviceAttributesResponse);
				break;
			default:
				break;
			}	
		}
	}

	private void setModes(final ControlSequence args, final boolean on) throws IOException {
		final int argCount = args.getCount();
		final Mode[] modeTable = args.getModeTable();
		for (int i = 0; i < argCount; i++) {
			final int num = args.getArg(i, -1);
			Mode mode = null;
			if (num >= 0 && num < modeTable.length){
				mode = modeTable[num];
			}
			
			if (mode == null){
			}else if (on) {
				tw.setMode(mode);
					
			} else {
				tw.unsetMode(mode);
			}
		}
	}
}
