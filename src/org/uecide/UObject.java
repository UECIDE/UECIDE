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

package org.uecide;

import java.io.*;
import java.util.*;
import javax.swing.*;

/*
 * The UObject is the heart of the UECIDE system.  It defines all the boards,
 * cores and compilers.
 */

public class UObject implements Comparable {
    private File _folder;
    private boolean _valid;
    private File _configFile;
    private PropertyFile _properties;
    private int _type;
    private String _name;
    private String _version;
    private String _revision;
    private String _family;
    private String _description;

    public static final int None = 0;
    public static final int Board = 1;
    public static final int Core = 2;
    public static final int Compiler = 3;

    public UObject(File dir) {
        _valid = false;
        _folder = dir;

        if(_folder.exists() && _folder.isDirectory()) {
            try {
                if(this instanceof Board) {
                    _configFile = new File(_folder, "board.txt");
                    _type = Board;
                }

                if(this instanceof Core) {
                    _configFile = new File(_folder, "core.txt");
                    _type = Core;
                }

                if(this instanceof Compiler) {
                    _configFile = new File(_folder, "compiler.txt");
                    _type = Compiler;
                }

                if(!_configFile.exists()) {
                    _type = None;
                    _valid = false;
                    return;
                }

                _properties = new PropertyFile(_configFile);
                _name = get("name");

                if(_name == null) {
                    _name = _folder.getName();
                }

                _version = get("version");
                _revision = get("revision");

                if(_version == null) {
                    _version = "0";
                }

                if(_revision == null) {
                    _revision = "0";
                }

                _family = get("family");
                _description = get("description");

                if(_description == null) {
                    _description = _name;
                }

                _valid = true;
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public File getLibrariesFolder() {
        String f = get("libraries");

        if(f == null) {
            f = "libraries";
        }

        File o = new File(_folder, f);
        return o;
    }

    public File getExamplesFolder() {
        String f = get("examples");

        if(f == null) {
            f = "examples";
        }

        File o = new File(_folder, f);
        return o;
    }

    public String getName() {
        return _name;
    }

    public File getFolder() {
        return _folder;
    }

    public boolean isValid() {
        return _valid;
    }

    public String get(String k) {
        if(_properties == null) {
            return null;
        }

        return (String) _properties.get(k);
    }

    public String get(String k, String d) {
        String dat = get(k);

        if(dat == null) {
            return d;
        }

        return dat;
    }

    public String getRevision() {
        return _revision;
    }

    public String getVersion() {
        return _version;
    }

    public String getFullVersion() {
        return _version + "-" + _revision;
    }

    public String getFamily() {
        return _family;
    }

    public boolean inFamily(String fam) {
        String fly = getFamily();

        if(fly == null) {
            return false;
        }

        String fams[] = fly.split("::");

        for(String thisfam : fams) {
            if(thisfam.equals(fam)) {
                return true;
            }
        }

        return false;
    }

    public PropertyFile getProperties() {
        return _properties;
    }

    public String getDescription() {
        return _description;
    }

    public int compareTo(Object o) {
        if(o == null) {
            return 0;
        }

        UObject ob = (UObject)o;
        return _description.compareTo(ob.getDescription());
    }

    public int getType() {
        return _type;
    }

    public boolean worksWith(UObject c) {
        String fam = get("family");

        if(fam == null) {
            return false;
        }

        String[] myFamilies = fam.split("::");

        if(c == null) {
            return false;
        }

        String[] otherFamilies = c.get("family").split("::");

        for(String mf : myFamilies) {
            for(String of : otherFamilies) {
                if(mf.equals(of)) {
                    return true;
                }
            }
        }

        return false;
    }

    public ImageIcon getIcon(int size) {
        String path = get("icon." + size);

        if(path  == null) {
            return null;
        }

        File f = new File(getFolder(), path);

        if(!f.exists()) {
            return null;
        }

        return new ImageIcon(f.getAbsolutePath());
    }


    public File getManual() {
        String path = get("manual");
        if (path != null) {
            return new File(getFolder(), path);
        } else {
            return new File(getFolder(), "manual");
        }
    }

    public String toString() {
        return _description;
    }

    public File getKeywords() {
        return new File(getFolder(), get("keywords", "keywords.txt"));
    }
}
