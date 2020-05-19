/**
 * 
 */
package com.wittams.gritty;

import java.io.IOException;
import java.util.ArrayList;

import com.wittams.gritty.CharacterUtils.CharacterType;

public class ControlSequence {
	private int argc;

	private int[] argv;

	private Mode[] modeTable;

	private byte finalChar;

	private int startInBuf;

	private int lengthInBuf;

	private int bufferVersion;

	private static Mode[] normalModes = {

	};

	private static Mode[] questionMarkModes = { Mode.Null,
		    Mode.CursorKey, Mode.ANSI,
			Mode.WideColumn, Mode.SmoothScroll, Mode.ReverseScreen,
			Mode.RelativeOrigin, Mode.WrapAround, Mode.AutoRepeat,
			Mode.Interlace };
	
	private ArrayList<Byte> unhandledChars;

	ControlSequence(final TtyChannel channel) throws IOException {
		argv = new int[10];
		argc = 0;
		modeTable = normalModes;
		readControlSequence(channel);
	}

	private void readControlSequence(final TtyChannel channel) throws IOException {
		argc = 0;
		// Read integer arguments
		int digit = 0;
		int seenDigit = 0;
		int pos = -1;

		bufferVersion = channel.serial;
		startInBuf = channel.offset;

		while (true) {
			final byte b = channel.getChar();
			pos++;
			if (b == '?' && pos == 0){
				modeTable = questionMarkModes;
		    }else if (b == ';') {
				if (digit > 0) {
					argc++;
					argv[argc] = 0;
					digit = 0;
				}
			} else if ('0' <= b && b <= '9') {
				argv[argc] = argv[argc] * 10 + b - '0';
				digit++;
				seenDigit = 1;
				continue;
			} else if (':' <= b && b <= '?'){
				addUnhandled(b);
			}else if (0x40 <= b && b <= 0x7E) {
				finalChar = b;
				break;
			} else
				addUnhandled(b);
		}
		if (bufferVersion == channel.serial)
			lengthInBuf = channel.offset - startInBuf;
		else
			lengthInBuf = -1;
		argc += seenDigit;
	}

	private void addUnhandled(final byte b) {
		if(unhandledChars == null)
			unhandledChars = new ArrayList<Byte>();
		unhandledChars.add(b);
	}

	public boolean pushBackReordered(final TtyChannel channel) throws IOException {
		if(unhandledChars == null) return false;
		final byte[] bytes = new byte[1024]; // can't be more than the whole buffer... 
		int i = 0;
		for(final byte b : unhandledChars)
			bytes[i++] = b;
		bytes[i++] = (byte)CharacterUtils.ESC;
		bytes[i++] = (byte)'[';
		
		if(modeTable == questionMarkModes)
			bytes[i++] = (byte)'?';
		for(int argi = 0; argi < argc; argi++){
			if(argi != 0) bytes[i++] = (byte)';';
			for(final byte b: Integer.toString(argv[argi]).getBytes())
				bytes[i++] = b;
		}
		bytes[i++] = finalChar;
		channel.pushBackBuffer(bytes, i);
		return true;
	}
	
	int getCount() {
		return argc;
	}

	final int getArg(final int index, final int def) {
		if (index >= argc)
			return def;
		return argv[index];
	}

	public final void appendToBuffer( final StringBuffer sb ) {
		sb.append("ESC[");
		if (modeTable == questionMarkModes)
			sb.append("?");

		String sep = "";
		for (int i = 0; i < argc; i++) {
			sb.append(sep);
			sb.append(argv[i]);
			sep = ";";
		}
		sb.append((char) finalChar);
		
		if(unhandledChars != null){
			sb.append(" Unhandled:");
			CharacterType last = CharacterType.NONE;
			for(final byte b : unhandledChars)
				last = CharacterUtils.appendChar(sb, last,(char) b);
		}
	}

	public final void appendActualBytesRead(final StringBuffer sb,
			final TtyChannel buffer) {
		if (lengthInBuf == -1)
			sb.append("TermIOBuffer filled in reading");
		else if (bufferVersion != buffer.serial)
			sb.append("TermIOBuffer filled after reading");
		else
			buffer.appendBuf(sb, startInBuf, lengthInBuf);
	}

	public byte getFinalChar() {
		return finalChar;
	}

	public Mode[] getModeTable() {
		return modeTable;
	}

}