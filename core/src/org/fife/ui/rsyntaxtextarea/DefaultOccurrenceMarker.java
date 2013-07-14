/*
 * 03/09/2013
 *
 * DefaultOccurrenceMarker - Marks occurrences of the current token for most
 * languages.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import javax.swing.text.BadLocationException;


/**
 * The default implementation of {@link OccurrenceMarker}.  It goes through
 * the document and marks all instances of the specified token.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class DefaultOccurrenceMarker implements OccurrenceMarker {


	/**
	 * {@inheritDoc}
	 */
	public void markOccurrences(RSyntaxDocument doc, Token t,
			RSyntaxTextAreaHighlighter h, MarkOccurrencesHighlightPainter p) {

		char[] lexeme = t.getLexeme().toCharArray();
		int type = t.type;
		int lineCount = doc.getDefaultRootElement().getElementCount();

		for (int i=0; i<lineCount; i++) {
			Token temp = doc.getTokenListForLine(i);
			while (temp!=null && temp.isPaintable()) {
				if (temp.is(type, lexeme)) {
					try {
						int end = temp.offset + temp.textCount;
						h.addMarkedOccurrenceHighlight(temp.offset, end, p);
					} catch (BadLocationException ble) {
						ble.printStackTrace(); // Never happens
					}
				}
				temp = temp.getNextToken();
			}
		}

	}


}