package redball.engine.scene;

import redball.engine.save.SaveManager;

import java.io.*;

public class AssetManager {
    private static AssetManager INSTANCE;
    private String workingDirectory;
    public String scenesDirectory;
    public String scriptDirectory;
    public String compileDirectory;
    public String buildDirectory;
    private File file;
    public String currentWorkingScene = "";

    public AssetManager(String directory) {
        workingDirectory = directory;
        scenesDirectory = workingDirectory + File.separatorChar + "assets/scenes" + File.separatorChar;
        scriptDirectory = workingDirectory + File.separatorChar + "assets/scripts" + File.separatorChar;
        compileDirectory = workingDirectory + File.separatorChar + "out" + File.separatorChar;
        buildDirectory = workingDirectory + File.separatorChar + "build" + File.separatorChar;
        file = new File(workingDirectory);
    }

    public String getScenesDirectory() {
        return scenesDirectory;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getScriptDirectory() {
        return scriptDirectory;
    }

    public String getCompileDirectory() {
        return compileDirectory;
    }

    public String getBuildDirectory() {
        return buildDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        workingDirectory = workingDirectory;
    }

    public static void init(String workingDirectory) {
        INSTANCE = new AssetManager(workingDirectory);
    }

    public static AssetManager getINSTANCE() {
        return INSTANCE;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}