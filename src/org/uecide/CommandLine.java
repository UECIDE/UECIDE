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
    HashMap<String, ArrayList<Object>> parameterValues = new HashMap<String, ArrayList<Object>>();
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
        parameterValues = new HashMap<String, ArrayList<Object>>();
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



                // A boolean could be there multiple times, but it makes no sense to record such a fact.
                if (aclass == Boolean.class) {
                    Boolean b = true;
                    addParameterEntry(arg, b);
                    continue;
                }

                // Everything else takes a parameter. 
                if (value.equals("")) {
                    help();
                    System.exit(0);
                }

                if (aclass == Integer.class) {
                    Integer i = 0;
                    try {
                        i = Integer.parseInt(value);
                    } catch (Exception ignored) {
                        Debug.exception(ignored);
                    }
                    addParameterEntry(arg, i);
                    continue;
                }

                if (aclass == Float.class) {
                    Float f = 0F;
                    try {
                        f = Float.parseFloat(value);
                    } catch (Exception ignored) {
                        Debug.exception(ignored);
                    }
                    addParameterEntry(arg, f);
                    continue;
                }

                if (aclass == Double.class) {
                    Double d = 0D;
                    try {
                        d = Double.parseDouble(value);
                    } catch (Exception ignored) {
                        Debug.exception(ignored);
                    }
                    addParameterEntry(arg, d);
                    continue;
                }

                if (aclass == String.class) {
                    addParameterEntry(arg, value);
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
            System.out.println(UECIDE.i18n.string(parameterComments.get(s)));
        }
    }

    public boolean isSet(String key) {
        ArrayList<Object> value = parameterValues.get(key);
        if (value == null) {
            return false;
        }
        return true;
    }

    public int count(String key) {
        ArrayList<Object> value = parameterValues.get(key);
        if (value == null) return 0;
        return value.size();
    }

    public String[] getString(String key) { return parameterValues.get(key).toArray(new String[0]); }
    public Integer[] getInteger(String key) { return parameterValues.get(key).toArray(new Integer[0]); }
    public Float[] getFloat(String key) { return parameterValues.get(key).toArray(new Float[0]); }
    public Double[] getDouble(String key) { return parameterValues.get(key).toArray(new Double[0]); }

    public void set(String key, String value) { addParameterEntry(key, value); }
    public void set(String key, Integer value) { addParameterEntry(key, value); }
    public void set(String key, Boolean value) { addParameterEntry(key, value); }
    public void set(String key, Float value) { addParameterEntry(key, value); }
    public void set(String key, Double value) { addParameterEntry(key, value); }

    public void set(String key, String[] value) { 
        ArrayList<Object> newArray = new ArrayList<Object>();
        Collections.addAll(newArray, value);
        parameterValues.put(key, newArray);
    }

    public void set(String key, Integer[] value) { 
        ArrayList<Object> newArray = new ArrayList<Object>();
        Collections.addAll(newArray, value);
        parameterValues.put(key, newArray);
    }

    public void set(String key, Boolean[] value) { 
        ArrayList<Object> newArray = new ArrayList<Object>();
        Collections.addAll(newArray, value);
        parameterValues.put(key, newArray);
    }

    public void set(String key, Float[] value) { 
        ArrayList<Object> newArray = new ArrayList<Object>();
        Collections.addAll(newArray, value);
        parameterValues.put(key, newArray);
    }

    public void set(String key, Double[] value) { 
        ArrayList<Object> newArray = new ArrayList<Object>();
        Collections.addAll(newArray, value);
        parameterValues.put(key, newArray);
    }


    public void addParameterEntry(String key, Object value) {
        ArrayList<Object> array = parameterValues.get(key);
        if (array == null) {
            array = new ArrayList<Object>();
        }
        array.add(value);
        parameterValues.put(key, array);
    }
}
