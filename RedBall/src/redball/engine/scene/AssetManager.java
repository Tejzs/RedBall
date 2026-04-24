package redball.engine.scene;

import redball.engine.save.SaveManager;

import java.io.*;

public class AssetManager {
    private static AssetManager INSTANCE;
    private String workingDirectory;
    private String scenesDirectory;
    private String scriptDirectory;
    private String compileDirectory;
    private String buildDirectory;
    private String prefabDirectory;
    private File file;
    public String currentWorkingScene = "";

    public AssetManager(String directory) {
        workingDirectory = directory;
        scenesDirectory = workingDirectory + File.separatorChar + "assets/scenes" + File.separatorChar;
        scriptDirectory = workingDirectory + File.separatorChar + "assets/scripts" + File.separatorChar;
        compileDirectory = workingDirectory + File.separatorChar + "out" + File.separatorChar;
        buildDirectory = workingDirectory + File.separatorChar + "build" + File.separatorChar;
        prefabDirectory = workingDirectory + File.separatorChar + "assets/prefabs" + File.separatorChar;
        file = new File(workingDirectory + "/assets/");
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

    public String getPrefabDirectory() {
        return prefabDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
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