package redball.engine.scene;

import redball.engine.save.SaveManager;

import java.io.File;
import java.util.HashMap;

public class SceneManager {
    private static HashMap<Integer, String> sceneList = new HashMap<>();

    public static void init() {
        int index = 0;
        File[] scenes = new File(AssetManager.getINSTANCE().getScenesDirectory()).listFiles();

        if (scenes != null) {
            for (File scene : scenes) {
                sceneList.put(index, scene.getPath());
                index++;
            }
        }
    }

    public static void loadDefault() {
        SaveManager.loadScene(sceneList.get(0));
        AssetManager.getINSTANCE().currentWorkingScene = sceneList.get(0);
    }

    public static void switchScenes(int index) {
        // need to check for null
        SaveManager.loadScene(sceneList.get(index));
    }

    public static HashMap<Integer, String> getSceneList() {
        return sceneList;
    }
}
