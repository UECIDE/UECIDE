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

import java.util.*;

public class CommandLine {
    HashMap<String, Class<?>> parameterTypes = new HashMap<String, Class<?>>();
    HashMap<String, String> parameterComments = new HashMap<String, String>();
    HashMap<String, Object> parameterValues = new HashMap<String, Object>();
    HashMap<String, String> parameterNames = new HashMap<String, String>();
    ArrayList<String> extraValues = new ArrayList<String>();

    public CommandLine() {
    }

    public void addParameter(String key, String name, Class<?> type, String comment) {
        parameterNames.put(key, name);
        parameterTypes.put(key, type);
        parameterComments.put(key, comment);
    }

    public String[] process(String[] args) {
        parameterValues = new HashMap<String, Object>();
        extraValues = new ArrayList<String>();

        for (String arg : args) {
            if (arg.startsWith("--")) {
                arg = arg.substring(2);
                String value = "";
                int equals = arg.indexOf("=");
                if (equals > -1) {
                    value = arg.substring(equals + 1);
                    arg = arg.substring(0, equals);
                }

                Class<?> aclass = parameterTypes.get(arg);
                if (aclass == null) {
                    help();
                    System.exit(0);
                }
                if (aclass == Boolean.class) {
                    Boolean b = true;
                    parameterValues.put(arg, b);
                    continue;
                }
                if (value.equals("")) {
                    help();
                    System.exit(0);
                }
                if (aclass == Integer.class) {
                    Integer i = 0;
                    try {
                        i = Integer.parseInt(value);
                    } catch (Exception ignored) {
                    }
                    parameterValues.put(arg, i);
                    continue;
                }
                if (aclass == Float.class) {
                    Float f = 0F;
                    try {
                        f = Float.parseFloat(value);
                    } catch (Exception ignored) {
                    }
                    parameterValues.put(arg, f);
                    continue;
                }
                if (aclass == Double.class) {
                    Double d = 0D;
                    try {
                        d = Double.parseDouble(value);
                    } catch (Exception ignored) {
                    }
                    parameterValues.put(arg, d);
                    continue;
                }
                if (aclass == String.class) {
                    parameterValues.put(arg, value);
                    continue;
                }
            } else {
                extraValues.add(arg);
            }
        }
        
        return extraValues.toArray(new String[0]);
    }

    public void help() {
        System.out.println("Available command line arguments:");
        int maxlen = 0;

        String[] arglist = parameterTypes.keySet().toArray(new String[0]);
        Arrays.sort(arglist);

        for (String s : arglist) {
            int thislen = s.length();
            if (parameterTypes.get(s) != Boolean.class) {
                thislen++;
                thislen += parameterNames.get(s).length();
            }
            if (thislen > maxlen) {
                maxlen = thislen;
            }
        }
        for (String s : arglist) {
            StringBuilder sb = new StringBuilder();
            System.out.print("    --");
            sb.append(s);
            if (parameterTypes.get(s) != Boolean.class) {
                sb.append("=");
                sb.append(parameterNames.get(s));
            }
            while (sb.length() < maxlen) {
                sb.append(" ");
            }
            System.out.print(sb.toString());
            System.out.print("  ");
            System.out.println(Base.i18n.string(parameterComments.get(s)));
        }
    }

    public boolean isSet(String key) {
        Object value = parameterValues.get(key);
        if (value == null) {
            return false;
        }
        return true;
    }

    public String getString(String key) {
        Class<?> type = parameterTypes.get(key);
        if (type == null) {
            return null;
        }
        if (type != String.class) {
            return null;
        }
        String value = (String)parameterValues.get(key);
        return value;
    }

    public int getInteger(String key) {
        Class<?> type = parameterTypes.get(key);
        if (type == null) {
            return 0;
        }
        if (type != Integer.class) {
            return 0;
        }
        Integer value = (Integer)parameterValues.get(key);
        if (value == null) {
            return 0;
        }
        return (int)value;
    }

    public float getFloat(String key) {
        Class<?> type = parameterTypes.get(key);
        if (type == null) {
            return 0f;
        }
        if (type != Float.class) {
            return 0f;
        }
        Float value = (Float)parameterValues.get(key);
        if (value == null) {
            return 0f;
        }
        return (float)value;
    }

    public double getDouble(String key) {
        Class<?> type = parameterTypes.get(key);
        if (type == null) {
            return 0d;
        }
        if (type != Double.class) {
            return 0d;
        }
        Double value = (Double)parameterValues.get(key);
        if (value == null) {
            return 0d;
        }
        return (double)value;
    }

    public void set(String key, String value) {
        parameterValues.put(key, value);
    }
}
