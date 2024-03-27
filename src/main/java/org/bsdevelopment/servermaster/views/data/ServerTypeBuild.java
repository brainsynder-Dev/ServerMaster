package org.bsdevelopment.servermaster.views.data;

public record ServerTypeBuild(String build, String jar) {
    @Override
    public String toString() {
        return build;
    }
}
