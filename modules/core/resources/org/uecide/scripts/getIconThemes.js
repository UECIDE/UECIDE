function run() {
    try {
        load("nashorn:mozilla_compat.js");
    } catch (e) {
       // ignore the exception - perhaps we are running on Rhino!
    }

    importPackage(org.uecide);

    return Base.getIconSets();
}
