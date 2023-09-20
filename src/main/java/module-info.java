module org.bsdevelopment.serverapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;
    requires java.logging;
    requires org.apache.commons.io;
    requires atlantafx.base;

    opens org.bsdevelopment.serverapp to javafx.fxml;
    exports org.bsdevelopment.serverapp;
}