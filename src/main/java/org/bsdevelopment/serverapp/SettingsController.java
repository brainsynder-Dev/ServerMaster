package org.bsdevelopment.serverapp;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private TextField serverPort;
    @FXML private ChoiceBox<String> themeSelector;
    @FXML private CheckBox acceptEULA;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Properties properties = ServerMasterApplication.getProperties();

        themeSelector.getItems().addAll(Arrays.asList(
                "No Theme",
                "CupertinoDark",
                "CupertinoLight",
                "Dracula",
                "NordDark",
                "NordLight",
                "PrimerDark",
                "PrimerLight"
        ));

        themeSelector.setValue(properties.getProperty("app-theme"));
        acceptEULA.setSelected(Boolean.parseBoolean(properties.getProperty("eula", "false")));
        serverPort.setText(properties.getProperty("server-port", "25565"));

        themeSelector.valueProperty().addListener(observable -> {
            properties.setProperty("app-theme", themeSelector.getValue());
            ServerMasterApplication.updateTheme(themeSelector.getValue());

            try {
                properties.store(new FileOutputStream(ServerMasterApplication.getPropertiesFile()), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @FXML
    public void saveSettings () {
        Properties properties = ServerMasterApplication.getProperties();
        properties.setProperty("server-port", serverPort.getText());
        properties.setProperty("app-theme", themeSelector.getValue());
        properties.setProperty("eula", String.valueOf(acceptEULA.isSelected()));

        try {
            properties.store(new FileOutputStream(ServerMasterApplication.getPropertiesFile()), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
