module securitymodule {
    requires imagemodule;
    requires miglayout;
    requires java.desktop;
    requires com.google.common;
    requires com.google.gson;
    requires java.prefs;
    exports com.udacity.catpoint.security.application;
    exports com.udacity.catpoint.security.data;
    exports com.udacity.catpoint.security.service;
    opens com.udacity.catpoint.security.data to com.google.gson;

}