package org.bsdevelopment.utils;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

public enum Theme {
    LIGHT(FlatLightLaf::setup),
    DARK(FlatDarkLaf::setup),

    ATOM_DARK(com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneDarkIJTheme::setup),
    ATOM_LIGHT(com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme::setup),
    DARK_PURPLE(com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme::setup),
    DRACULA(com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme::setup),
    GITHUB(com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme::setup),
    GITHUB_DARK(com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkIJTheme::setup),
    MAC_LIGHT(FlatMacLightLaf::setup),
    MAC_DARK(FlatMacDarkLaf::setup),
    NORD(com.formdev.flatlaf.intellijthemes.FlatNordIJTheme::setup),
    OCEANIC(com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme::setup),
    PALENIGHT(com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightIJTheme::setup),
    SOLARIZED_DARK(com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme::setup),
    SOLARIZED_LIGHT(com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme::setup),

    UNKNOWN(() -> {
    });

    private final Runnable apply;

    Theme(final Runnable apply) {
        this.apply = apply;
    }

    public void apply() {
        apply.run();
    }
}