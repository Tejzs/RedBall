package redball.engine.core;

import redball.engine.core.Logger.LogCapture;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.Rigidbody;
import redball.engine.input.KeyboardInput;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.Shader;
import redball.engine.renderer.WindowManager;
import redball.engine.save.SaveObject;
import redball.engine.scene.AssetManager;
import redball.engine.utils.AssetPool;

import java.lang.reflect.InvocationTargetException;

public class Engine {
    private static boolean started = false;
    private static WindowManager windowManager = null;
    private static Shader shader = null;
    public static boolean isPlaying = false;
    private static byte[] savedScene;

    public static void onPlay() {
        // save current scene to memory
        savedScene = new SaveObject().toByteArray();

        // start physics, call start() on all components
        ECSWorld.start();
        isPlaying = true;
    }

    public static void onStop() {
        isPlaying = false;

        PhysicsSystem.clear();
        ECSWorld.removeAll();
        RenderManager.clear();
        ECSWorld.setGameObjects(SaveObject.parseFrom(savedScene).getGameObjects());
        for (GameObject go : ECSWorld.getGameObjects()) {
            Rigidbody rb = go.getComponent(Rigidbody.class);
            if (rb != null) {
                rb.createBody();
            }
        }
        RenderManager.prepare(ECSWorld.findGameObjectByTag("Camera"));
    }

    public static void start() throws InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
        if (started) {
            return;
        }

        started = true;
        LogCapture.start();
        AssetManager.init("src/redball/example");
        windowManager = new WindowManager();
        windowManager.init();

        EditorLayer.init(windowManager.getWindow());

        KeyboardInput.init(windowManager.getWindow(), EditorLayer.getINSTANCE().getImGuiGlfw());

        shader = new Shader(AssetPool.getVertexShaderSource(), AssetPool.getFragmentShaderSource());

//        windowManager.switchScene(1);

        windowManager.loop(shader);
    }


    public static WindowManager getWindowManager() {
        return windowManager;
    }

    public static Shader getShader() {
        return shader;
    }

    public static boolean isPlaying() {
        return isPlaying;
    }
}