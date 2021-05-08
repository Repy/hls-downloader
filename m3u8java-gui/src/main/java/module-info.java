module info.repy.m3u8java.gui {
    requires commons.cli;
    requires javafx.controls;
    requires javafx.fxml;
    requires info.repy.m3u8java.core;
    requires java.logging;
    opens info.repy.m3u8java.gui;
    opens info.repy.m3u8java.cui;
    exports info.repy.m3u8java.gui;
    exports info.repy.m3u8java.cui;
}