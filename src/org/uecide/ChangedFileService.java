package org.uecide;

public class ChangedFileService extends Service {
    public ChangedFileService() {
        setName("Changed Files");
        setInterval(1000);
    }

    public void setup() {
    }

    public void cleanup() {
    }

    public void loop() {
        for (Editor ed : Editor.editorList) {
            int tabs = ed.getTabCount();
            for (int i = 0; i < tabs; i++) {
                TabLabel tl = ed.getTabLabel(i);
                if (tl == null) {
                    continue;
                }
                if (tl.needsReload()) {
                    if(tl.isModified()) {
                        tl.askReload();
                    } else {
                        tl.reloadFile();
                    }
                }
            }
        }
    }
}
