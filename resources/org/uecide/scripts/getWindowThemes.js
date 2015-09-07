function run() {
    try {
        load("nashorn:mozilla_compat.js");
    } catch (e) {
       // ignore the exception - perhaps we are running on Rhino!
    }


    importPackage(javax.swing);
    importPackage(java.util);

    importPackage(org.uecide);

    return Base.getLookAndFeelList();
}
