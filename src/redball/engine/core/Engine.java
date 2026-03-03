package redball.engine.core;

import redball.engine.core.Logger.LogCapture;
import redball.engine.input.KeyboardInput;
import redball.engine.renderer.Shader;
import redball.engine.renderer.WindowManager;
import redball.engine.utils.AssetPool;

import java.lang.reflect.InvocationTargetException;

public class Engine {
    private static boolean started = false;
    private static WindowManager windowManager = null;
    private static Shader shader = null;

    public static void start() throws InvocationTargetException, InstantiationException, IllegalAccessException {
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

        windowManager.switchScene(1);

        windowManager.loop(shader);
    }


    public static WindowManager getWindowManager() {
        return windowManager;
    }

    public static Shader getShader() {
        return shader;
    }
}