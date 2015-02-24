package org.uecide;

public class TreeUpdaterService extends Service {
    public TreeUpdaterService() {
        setName("Tree Updater");
        setInterval(5000);
    }

    public void setup() {
    }

    public void cleanup() {
    }

    public void loop() {
        synchronized (Editor.editorList) {
            for (Editor e : Editor.editorList) {
                if (!e.compilerRunning()) {
                    e.loadedSketch.findAllFunctions();
                    e.loadedSketch.updateKeywords();
                    e.loadedSketch.updateLibraryList();
                    e.updateKeywords();
                    e.updateLibrariesTree();
                    e.updateSourceTree();
                }
            }
        }
    }
}
