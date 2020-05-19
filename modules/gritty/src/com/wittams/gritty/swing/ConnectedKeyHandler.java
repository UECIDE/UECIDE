package com.wittams.gritty.swing;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import com.wittams.gritty.Emulator;

public class ConnectedKeyHandler implements KeyListener {
	private final Emulator emulator;
	
	public ConnectedKeyHandler(Emulator emu){
		emulator = emu;
	}
	
	public void keyPressed(final KeyEvent e) {
		try {
			final int keycode = e.getKeyCode();
			final byte[] code = emulator.getCode(keycode);
			if (code != null)
				emulator.sendBytes(code);
			else {
				final char keychar = e.getKeyChar();
				final byte[] obuffer = new byte[1];
				if ((keychar & 0xff00) == 0) {
					obuffer[0] = (byte) e.getKeyChar();
					emulator.sendBytes(obuffer);
				}
			}
		} catch (final IOException ex) {
		}
	}

	public void keyTyped(final KeyEvent e) {
		final char keychar = e.getKeyChar();
		if ((keychar & 0xff00) != 0) {
			final char[] foo = new char[1];
			foo[0] = keychar;
			try {
				final byte[] bytes = new String(foo).getBytes("EUC-JP");
				emulator.sendBytes(bytes);
			} catch (final IOException ex) {
			}
		}
	}
	
	//Ignore releases
	public void keyReleased(KeyEvent e){}
}
