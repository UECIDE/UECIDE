package org.uecide.gui.swing;

import org.uecide.Context;
import org.uecide.Library;
import javax.swing.JMenu;
import java.util.TreeSet;

public class LibraryCategoryMenu extends JMenu {
    Context ctx;
    String category;

    public LibraryCategoryMenu(Context c, String cat) {
        super("Placeholder");
        ctx = c;
        category = cat;

        TreeSet<Library> libraries = Library.getLibraries(cat, ctx.getCore().getName());

        for (Library library : libraries) {
            LibraryMenu item = new LibraryMenu(ctx, library);
            add(item);
        }

    }

    public String getText() {
        if (category == null) return "Placeholder"; // This is needed for the constructor to function.
        return Library.getCategoryName(category);
    } 

}
