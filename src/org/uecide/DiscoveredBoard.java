package org.uecide;

public class DiscoveredBoard {
    public Board board;
    public String name;
    public Object location;
    public String version;
    public int type;
    public String programmer;

    public PropertyFile properties = new PropertyFile();

    static public final int SERIAL = 1;
    static public final int NETWORK = 2;
    static public final int USB = 3;

    public String toString() {
        String loc = location.toString();

        if(type == NETWORK) {
            if(loc.startsWith("/")) {
                loc = loc.substring(1);
            }
        }

        return String.format("%s v%s (%s) on %s",
                             board.getDescription(),
                             version,
                             name,
                             loc
                            );
    }

}
