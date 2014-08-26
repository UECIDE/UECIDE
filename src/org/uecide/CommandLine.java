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

    public void process(String[] args) {
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

                if (arg.equals("help")) {
                    help();
                    System.exit(0);
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
            System.out.print("    --");
            String pname = s;
            if (parameterTypes.get(s) != Boolean.class) {
                pname += "=";
                pname += parameterNames.get(s);
            }
            while (pname.length() < maxlen) {
                pname += " ";
            }
            System.out.print(pname);
            System.out.print("  ");
            System.out.println(parameterComments.get(s));
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
}
