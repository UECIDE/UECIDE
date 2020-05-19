/**
 * 
 */
package com.wittams.gritty;

import java.awt.Dimension;
import java.io.IOException;

public class TtyChannel {
	private Tty tty;
	
	byte[] buf = new byte[1024];

	int offset = 0;

	int length = 0;

	int serial;

	public TtyChannel(final Tty tty) {
		this.tty = tty;
		serial = 0;
	}

	public byte getChar() throws java.io.IOException {
		if (length == 0)
			fillBuf();
		length--;

		return buf[offset++];
	}

	public void appendBuf(final StringBuffer sb, final int begin, final int length) {
		CharacterUtils.appendBuf(sb, buf, begin, length);
	}

	private void fillBuf() throws java.io.IOException {
		length = offset = 0;
		length = tty.read(buf, offset, buf.length - offset);
		serial++;

		if (length <= 0) {
			length = 0;
			throw new IOException("fillBuf");
		}

	}

	public void pushChar(final byte b) throws java.io.IOException {
		if(offset == 0){
			// Pushed back too many... shift it up to the end.
			offset = buf.length - length;
			System.arraycopy(buf, 0, buf, offset, length);
		}
		
		length++;
		buf[--offset] = b;
	}

	int advanceThroughASCII(int toLineEnd) throws java.io.IOException {
		if (length == 0)
			fillBuf();

		int len = toLineEnd > length ? length : toLineEnd;
		
		final int origLen = len;
		byte tmp;
		while (len > 0) {
			tmp = buf[offset++];
			if (0x20 <= tmp && tmp <= 0x7f) {
				length--;
				len--;
				continue;
			}
			offset--;
			break;
		}		
		return origLen - len;
	}

	public void sendBytes(final byte[] bytes) throws IOException {
		tty.write(bytes);
	}

	public void postResize(final Dimension termSize, final Dimension pixelSize) {
		tty.resize(termSize, pixelSize);
	}

	public void pushBackBuffer(final byte[] bytes, final int len) throws IOException {
		for(int i = len - 1; i >= 0; i--)
			pushChar(bytes[i]);
	}
}
