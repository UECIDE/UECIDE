package com.wittams.gritty;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BackBuffer {
	private static final char EMPTY_CHAR = ' '; // (char) 0x0;

	private  char[] buf;
	private  Style[] styleBuf;
	private BitSet damage;
	
	StyleState styleState;
	
	private int width;
	private int height;
	
	private final Lock lock = new ReentrantLock();
	
	public BackBuffer(final int width, final int height, StyleState styleState) {
		this.styleState = styleState;
		allocateBuffers(width, height);
	}

	private void allocateBuffers(final int width, final int height) {
        if (width * height > 0) {
            this.width = width;
            this.height = height;
		
            buf = new char[width * height];
            Arrays.fill(buf, EMPTY_CHAR);
		
            styleBuf = new Style[width * height];
            Arrays.fill(styleBuf, Style.EMPTY);
		
            damage = new BitSet(width * height);
        }
	}

	public Dimension doResize(final Dimension pendingResize, final RequestOrigin origin ) {
		final char [] oldBuf = buf;
		final Style [] oldStyleBuf = styleBuf;
		final int oldHeight = height;
		final int oldWidth = width;
		allocateBuffers(pendingResize.width, pendingResize.height);
		
		clear();
		
		// copying lines... 
		final int copyWidth = Math.min(oldWidth, width);
		final int copyHeight = Math.min(oldHeight, height);
		
		final int oldStart = oldHeight - copyHeight;
		final int start = height - copyHeight;
		
		for(int i = 0; i < copyHeight; i++){
			System.arraycopy(oldBuf, (oldStart + i) * oldWidth, buf, (start + i) * width , copyWidth);
			System.arraycopy(oldStyleBuf, (oldStart + i) * oldWidth, styleBuf, (start + i) * width , copyWidth);
		}
		
        damage.set(0, width * height -1 , true);
		
		return pendingResize;
	}

	public void clear() {
		Arrays.fill(buf, EMPTY_CHAR);
		damage.set(0, width * height , true);
	}

	public void clearArea(final int leftX, final int topY, final int rightX, final int bottomY) {
		if(topY > bottomY ){
			return;
		}
		for (int y = topY; y < bottomY; y++){
			if (y > height - 1 || y < 0){
			}else if (leftX > rightX){
			}else {
				Arrays.fill(buf, 
						   y * width + leftX, 
						   y * width + rightX,
							EMPTY_CHAR);
				Arrays.fill(styleBuf,
							y * width + leftX, 
						    y * width + rightX,
						    Style.EMPTY
						);
				damage.set( y * width + leftX, 
						    y * width + rightX, 
						    true);
			}
		}
	}

	public void drawBytes(final byte[] bytes, final int s, final int len, final int x, final int y) {
		final int adjY = y - 1;
		if (adjY >= height || adjY < 0) {
			return;
		}

		for (int i = 0; i < len; i++) {
			final int location = adjY * width + x + i;
			buf[location] = (char) bytes[s + i]; // Arraycopy does not convert
			styleBuf[location] = styleState.getCurrent();
		}
		damage.set(adjY * width + x, adjY * width + x + len );
	}

	public void drawString(final String str, final int x, final int y) {
		final int adjY = y - 1;
		if (adjY >= height || adjY < 0) {
			return;
		}
		str.getChars(0, str.length(), buf, adjY * width + x);
		for(int i = 0; i < str.length(); i++ ){
			final int location = adjY * width + x + i;
			styleBuf[location] = styleState.getCurrent();
		}
		damage.set(adjY * width + x, adjY * width + x + str.length() );
	}

	public void scrollArea(final int y, final int h, final int dy) {
		final int lastLine = y + h;
		if (dy > 0)
			// Moving lines down
			for (int line = lastLine - dy; line >= y; line--) {
				if( line < 0 ){
					continue;
				}
				if( line + dy + 1 > height ){
					continue;
				}
				System.arraycopy(buf, line * width, buf, (line + dy) * width, width);
				System.arraycopy(styleBuf, line * width, styleBuf, (line + dy) * width, width);
				Util.bitsetCopy(damage, line * width, damage, (line + dy) * width, width);
				//damage.set( (line + dy) * width, (line + dy + 1) * width );
			}
		else
			// Moving lines up
			for (int line = y + dy + 1; line < lastLine; line++) {
				if( line > height - 1 ){
					continue;
				}
				if( line + dy < 0 ){
					continue;
				}
				
				System.arraycopy(buf, line * width, buf, (line + dy) * width, width);
				System.arraycopy(styleBuf, line * width, styleBuf, (line + dy) * width, width);
				Util.bitsetCopy(damage, line * width, damage, (line + dy) * width, width);
				//damage.set( (line + dy) * width, (line + dy + 1) * width );
			}
	}

	public String getStyleLines(){
		lock.lock();
		try{
			final StringBuilder sb = new StringBuilder();
			for (int row = 0; row < height; row++) {
				for(int col = 0; col < width; col++){
					final Style style = styleBuf[row * width + col];
					int styleNum = style == null?  styleNum = 0 : style.getNumber();
					sb.append(String.format("%03d ",styleNum ));
				}
				sb.append("\n");
			}
			return sb.toString();
		}finally{
			lock.unlock();
		}
	}

	public String getLines() {
		lock.lock();
		try{
			final StringBuffer sb = new StringBuffer();
			for (int row = 0; row < height; row++) {
				sb.append(buf, row * width, width);
				sb.append('\n');
			}
			return sb.toString();
		}finally{
			lock.unlock();
		}
	}
	
	public String getDamageLines(){
		lock.lock();
		try{
			final StringBuffer sb = new StringBuffer();
			for (int row = 0; row < height; row++){
				for(int col = 0; col < width; col++){
					boolean isDamaged = damage.get( row * width + col);
					sb.append( isDamaged ? 'X' : '-');
				}
				sb.append("\n");
			}
			return sb.toString();
		}finally{
			lock.unlock();
		}
	}
	
	public void resetDamage(){
		lock.lock();
		try{
			damage.clear();
		}finally{
			lock.unlock();
		}
	}

	public void pumpRuns(final int x, final int y, final int w, final int h, final StyledRunConsumer consumer){
		
		final int startRow = y;
		final int endRow = y + h;
		final int startCol = x;
		final int endCol = x + w;
		
		lock.lock();
		try{
			for(int row = startRow ; row < endRow ; row++ ){	
				Style lastStyle = null;
				int beginRun = startCol;
				for(int col = startCol; col < endCol; col++ ){
					final int location = row * width + col;
					if(location < 0 || location > styleBuf.length ){
						continue;
					}
					final Style cellStyle = styleBuf[ location ];
					if(lastStyle == null){
						//begin line
						lastStyle = cellStyle;
					}else if(!cellStyle.equals(lastStyle)){ 
						//start of new run
						consumer.consumeRun(beginRun, row, lastStyle, buf, row * width + beginRun, col - beginRun);
						beginRun = col;
						lastStyle = cellStyle;
					}
				}
				//end row
				if(lastStyle == null) {
				} else {
					consumer.consumeRun(beginRun, row, lastStyle, buf, row * width + beginRun, endCol - beginRun);
                }
			}
		}finally{
			lock.unlock();
		}
	}
	
	public void pumpRunsFromDamage(final StyledRunConsumer consumer) {
		final int startRow = 0;
		final int endRow = height;
		final int startCol = 0;
		final int endCol = width;
		lock.lock();
		try{
			for(int row = startRow ; row < endRow ; row++ ){
				
				Style lastStyle = null;
				int beginRun = startCol;
				for(int col = startCol; col < endCol; col++ ){
					final int location = row * width + col;
					if(location < 0 || location > styleBuf.length ){
						continue;
					}
					final Style cellStyle = styleBuf[ location ];
					final boolean isDamaged = damage.get(location);
					if(!isDamaged){
						if(lastStyle != null) 
							consumer.consumeRun(beginRun, row, lastStyle, buf, row * width + beginRun, col - beginRun);
						beginRun = col;
						lastStyle = null;
					}else if(lastStyle == null){
						//begin damaged run
						lastStyle = cellStyle;
					}else if(!cellStyle.equals(lastStyle)){ 
						//start of new run
						consumer.consumeRun(beginRun, row, lastStyle, buf, row * width + beginRun, col - beginRun);
						beginRun = col;
						lastStyle = cellStyle;
					}
				}
				//end row
				if(lastStyle != null)
					consumer.consumeRun(beginRun, row, lastStyle, buf, row * width + beginRun, endCol - beginRun);
			}
		}finally{
			lock.unlock();
		}
	}

	public boolean hasDamage() {
		return damage.nextSetBit(0) != -1;
	}

	public void lock() {
		lock.lock();
		
	}

	public void unlock() {
		lock.unlock();
	}

	public boolean tryLock() {
		return lock.tryLock();
	}

}
