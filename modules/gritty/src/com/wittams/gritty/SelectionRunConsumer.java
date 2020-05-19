/**
 * 
 */
package com.wittams.gritty;

import java.awt.Point;

public class SelectionRunConsumer implements StyledRunConsumer {
	private final StringBuffer selection;
	private final Point begin;
	private final Point end;

	boolean first = true;

	public SelectionRunConsumer(final StringBuffer selection, final Point begin, final Point end) {
		this.selection = selection;
		this.end = end;
		this.begin = begin;
	}

	public void consumeRun(final int x, final int y, final Style style, final char[] buf, final int start, final int len) {
		int startPos = start;
		int extent = len;
		
		if(y == end.y){
			extent = Math.min(end.x  - x, extent);
			
		}
		if(y == begin.y ){
			final int xAdj = Math.max(0, begin.x - x);
			startPos += xAdj;
			extent -= xAdj;
			if( extent < 0) return; 
		}
		if(extent < 0) return; // The run is off the left edge of the selection on the first line, 
							   //  or off the right edge on the last line.
		if(len > 0){
			if(!first && x == 0) selection.append('\n');
			first = false;
			if( startPos < 0 ){
			}else if (startPos + extent >= buf.length){
			}else{
				selection.append(buf,startPos, extent);
			}
		}
	}
}
