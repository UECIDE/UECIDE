/*
 * Copyright (c) 2014, Majenko Technologies
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of Majenko Technologies nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.uecide.debug;


/**
 * An exception with a line number attached that occurs
 * during either compile time or run time.
 */
public class RunnerException extends Exception { /*RuntimeException*/
    protected String message;
    protected int codeIndex;
    protected int codeLine;
    protected int codeColumn;
    protected boolean showStackTrace;


    public RunnerException(String message) {
        this(message, true);
    }

    public RunnerException(String message, boolean showStackTrace) {
        this(message, -1, -1, -1, showStackTrace);
    }

    public RunnerException(String message, int file, int line) {
        this(message, file, line, -1, true);
    }


    public RunnerException(String message, int file, int line, int column) {
        this(message, file, line, column, true);
    }


    public RunnerException(String message, int file, int line, int column,
                           boolean showStackTrace) {
        this.message = message;
        this.codeIndex = file;
        this.codeLine = line;
        this.codeColumn = column;
        this.showStackTrace = showStackTrace;
    }


    /**
     * Override getMessage() in Throwable, so that I can set
     * the message text outside the constructor.
     */
    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }


    public int getCodeIndex() {
        return codeIndex;
    }


    public void setCodeIndex(int index) {
        codeIndex = index;
    }


    public boolean hasCodeIndex() {
        return codeIndex != -1;
    }


    public int getCodeLine() {
        return codeLine;
    }


    public void setCodeLine(int line) {
        this.codeLine = line;
    }


    public boolean hasCodeLine() {
        return codeLine != -1;
    }


    public void setCodeColumn(int column) {
        this.codeColumn = column;
    }


    public int getCodeColumn() {
        return codeColumn;
    }


    public void showStackTrace() {
        showStackTrace = true;
    }


    public void hideStackTrace() {
        showStackTrace = false;
    }


    /**
     * Nix the java.lang crap out of an exception message
     * because it scares the children.
     * <P>
     * This function must be static to be used with super()
     * in each of the constructors above.
     */
    /*
    static public final String massage(String msg) {
      if (msg.indexOf("java.lang.") == 0) {
        //int dot = msg.lastIndexOf('.');
        msg = msg.substring("java.lang.".length());
      }
      return msg;
      //return (dot == -1) ? msg : msg.substring(dot+1);
    }
    */


    public void printStackTrace() {
        if(showStackTrace) {
            super.printStackTrace();
        }
    }
}
