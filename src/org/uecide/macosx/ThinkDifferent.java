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

package org.uecide.macosx;

import org.uecide.*;

import com.apple.eawt.*;


/**
 * Deal with issues related to thinking different. This handles the basic
 * Mac OS X menu commands (and apple events) for open, about, prefs, etc.
 *
 * Based on OSXAdapter.java from Apple DTS.
 *
 * As of 0140, this code need not be built on platforms other than OS X,
 * because of the new platform structure which isolates through reflection.
 */
public class ThinkDifferent implements ApplicationListener {

    // pseudo-singleton model; no point in making multiple instances
    // of the EAWT application or our adapter
    private static ThinkDifferent adapter;
    // http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/Application.html
    private static Application application;

    // reference to the app where the existing quit, about, prefs code is
    private Base base;


    static protected void init(Base base) {
        if(application == null) {
            //application = new com.apple.eawt.Application();
            application = com.apple.eawt.Application.getApplication();
        }

        if(adapter == null) {
            adapter = new ThinkDifferent(base);
        }

        application.addApplicationListener(adapter);
        application.setEnabledAboutMenu(true);
        application.setEnabledPreferencesMenu(true);
    }


    public ThinkDifferent(Base base) {
        this.base = base;
    }


    // implemented handler methods.  These are basically hooks into existing
    // functionality from the main app, as if it came over from another platform.
    public void handleAbout(ApplicationEvent ae) {
        if(base != null) {
            ae.setHandled(true);
            Splash splash = new Splash();
            splash.enableCloseOnClick();
        } else {
            System.err.println("handleAbout: Base instance detached from listener");
        }
    }


    public void handlePreferences(ApplicationEvent ae) {
        if(base != null) {
            base.handlePrefs();
            ae.setHandled(true);
        } else {
            System.err.println("handlePreferences: Base instance detached from listener");
        }
    }


    public void handleOpenApplication(ApplicationEvent ae) {
    }


    public void handleOpenFile(ApplicationEvent ae) {
//    System.out.println("got open file event " + ae.getFilename());
        String filename = ae.getFilename();
        Base.createNewEditor(filename);
        ae.setHandled(true);
    }


    public void handlePrintFile(ApplicationEvent ae) {
        // TODO implement os x print handler here (open app, call handlePrint, quit)
    }


    public void handleQuit(ApplicationEvent ae) {
        if(base != null) {
            /*
            / You MUST setHandled(false) if you want to delay or cancel the quit.
            / This is important for cross-platform development -- have a universal quit
            / routine that chooses whether or not to quit, so the functionality is identical
            / on all platforms.  This example simply cancels the AppleEvent-based quit and
            / defers to that universal method.
            */
            boolean result = Editor.closeAllEditors();
            ae.setHandled(result);
        } else {
            System.err.println("handleQuit: Base instance detached from listener");
        }
    }


    public void handleReOpenApplication(ApplicationEvent arg0) {
    }
}
