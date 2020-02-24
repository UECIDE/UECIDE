/*
 * Copyright (c) 2015, Majenko Technologies
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

import java.io.File;
import javax.swing.ImageIcon;

/*
 * The UObject is the heart of the UECIDE system.  It defines all the boards,
 * cores and compilers.
 */

public class UObject implements Comparable {
    protected File _folder;
    protected boolean _valid;
    protected File _configFile;
    protected PropertyFile _properties;
    protected int _type;
    protected String _name;
    protected String _version;
    protected String _revision;
    protected String _family;
    protected String _description;
    protected UObject _related;

    public static final int None = 0;
    public static final int Board = 1;
    public static final int Core = 2;
    public static final int Compiler = 3;
    public static final int Programmer = 4;
    public static final int Tool = 5;

    public UObject() {
        _properties = new PropertyFile();
    }

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

                if(this instanceof Programmer) {
                    _configFile = new File(_folder, "programmer.txt");
                    _type = Programmer;
                }

                if(this instanceof Tool) {
                    _configFile = new File(_folder, "tool.txt");
                    _type = Tool;
                }

                if(!_configFile.exists()) {
                    _type = None;
                    _valid = false;
                    return;
                }

                _properties = new PropertyFile(_configFile);
                updateSources();
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

    public void set(String k, String v) {
        if (_properties != null) {
            _properties.set(k, v);
        }
    }

    public void unset(String k) {
        if (_properties != null) {
            _properties.unset(k);
        }
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

        // No family means all families.
        if(fam == null) {
            return true;
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
        File f = null;
        if (path != null) {
            f = new File(getFolder(), path);
        } else {
            f = new File(getFolder(), "manual");
        }
        if (f == null) {
            return null;
        }
        if (!f.exists()) {
            return null;
        }
        return f;
    }

    public String toString() {
        return _description;
    }

    public File getKeywords() {
        return new File(getFolder(), get("keywords", "keywords.txt"));
    }

    public String getEmbedded(String name) {
        return _properties.getEmbedded(name);
    }

    public void updateSources() {
        for (String prop : _properties.keySet()) {
            _properties.setSource(prop, _name);
        }
    }

    public UObject getRelatedObject() {
        return _related;
    }

    public void setRelatedObject(UObject o) {
        _related = o;
    }

    public void onSelected(Context ctx) {
    }
}
