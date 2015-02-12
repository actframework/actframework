package org.osgl.oms.app;

import java.io.File;

/**
 * The current running directory is the app base
 */
class DevModeAppScanner extends AppScanner {

    @Override
    protected File[] appBases() {
        File file = currentDir();
        return new File[]{file};
    }

    private static File currentDir() {
        return new File(".");
    }

    @Override
    public AppScanner register(ProjectLayoutProbe probe) {
        return this;
    }

    public static void main(String[] args) {
        System.out.println(currentDir());
    }
}
