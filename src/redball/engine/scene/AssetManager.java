package redball.engine.scene;

import redball.engine.save.SaveManager;

import java.io.*;

public class AssetManager {
    private static AssetManager INSTANCE;
    private String workingDirectory;
    public String scenesDirectory;
    private File file;
    public String currentWorkingScene = "";

    public AssetManager(String directory) {
        workingDirectory = directory;
        scenesDirectory = workingDirectory + File.separatorChar + "assets/scenes" + File.separatorChar;
        file = new File(workingDirectory);
    }

    public String getScenesDirectory() {
        return scenesDirectory;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
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