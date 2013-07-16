package org.fife.ui.rsyntaxtextarea.modes;

import java.io.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;

public class ArduinoTokenMaker extends CPlusPlusTokenMaker {
    private TokenMap extraTokens;

    public ArduinoTokenMaker() {
        extraTokens = new TokenMap();
    }

    @Override
    public void addToken(char[] array, int start, int end, int tokenType, int startOffset, boolean hyperlink) {
        // This assumes all of your extra tokens would normally be scanned as IDENTIFIER.
        if (tokenType == TokenTypes.IDENTIFIER) {
            int newType = extraTokens.get(array, start, end);
            if (newType>-1) {
                tokenType = newType;
            }
        }
        super.addToken(array, start, end, tokenType, startOffset, hyperlink);
    }

    public void addKeyword(String keyword, int type) {
        extraTokens.put(keyword, type);
    }

    public void clear() {
        extraTokens = new TokenMap();
    }
}
