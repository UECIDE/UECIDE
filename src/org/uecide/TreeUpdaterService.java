package org.uecide;

public class TreeUpdaterService extends Service {
    public TreeUpdaterService() {
        setName("Tree Updater");
        setInterval(1000);
    }

    public void setup() {
    }

    public void cleanup() {
    }

    public void loop() {
        for (Editor e : Editor.editorList) {
            e.updateSourceTree();
        }
    }
}
