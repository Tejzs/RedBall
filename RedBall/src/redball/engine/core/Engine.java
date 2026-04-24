package redball.engine.core;

import redball.engine.entity.components.CameraComponent;
import redball.engine.logger.LogCapture;
import redball.engine.editor.EditorLayer;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.Rigidbody;
import redball.engine.input.KeyboardInput;
import redball.engine.input.MouseInput;
import redball.engine.logger.LogLine;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.Shader;
import redball.engine.renderer.WindowManager;
import redball.engine.save.SaveObject;
import redball.engine.scene.AssetManager;
import redball.engine.utils.AssetPool;
import redball.engine.utils.PakWriter;
import redball.engine.utils.ScriptManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.GLFW.*;

public class Engine {
    private static boolean started = false;
    private static WindowManager windowManager = null;
    private static Shader shader = null;
    private static boolean isPlaying = false;
    private static byte[] savedScene;
    public static boolean isBuild;
    private static String projectName;

    public static void onPlay() {
        // save current scene to memory
        savedScene = new SaveObject().toByteArray();

        // start physics, call start() on all components
        ECSWorld.start();
        isPlaying = true;
        ECSWorld.getCamera().getComponent(CameraComponent.class).camera.adjustProjection(Engine.getWindowManager().getWidth(), Engine.getWindowManager().getHeight());
    }

    public static void onStop() {
        isPlaying = false;

        LogCapture.getLogs().removeIf(log -> !log.isError());
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
        RenderManager.prepare(ECSWorld.getCamera());
        ECSWorld.getCamera().getComponent(CameraComponent.class).camera.adjustProjection(Engine.getWindowManager().getWidth(), Engine.getWindowManager().getHeight());
    }

    public static void start(String path, boolean build) throws Exception {
        if (started) {
            return;
        }

        started = true;
        isBuild = build;
        if (!isBuild) {
            AssetManager.init(path);
        }

        windowManager = new WindowManager();
        windowManager.init();

        if (build) {
            PakWriter.buildIndex();
        } else {
            Executors.newSingleThreadExecutor().execute(new ScriptManager());
            loadConfig();
            LogCapture.start();
            EditorLayer.init(windowManager.getWindow());
        }

        KeyboardInput.init(windowManager.getWindow(), EditorLayer.getINSTANCE());
        MouseInput.init(windowManager.getWindow(), EditorLayer.getINSTANCE());

        shader = new Shader(AssetPool.getVertexShaderSource(), AssetPool.getFragmentShaderSource());
        if (isBuild) {
            ScriptManager.compileAll("");
        } else {
            ScriptManager.compileAll(AssetManager.getINSTANCE().getScriptDirectory());
        }

        if (!build) {
            EditorLayer.initComponentList();
        }

        windowManager.loop(shader, build);
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

    public static void quit() {
        if (isBuild) {
            glfwSetWindowShouldClose(Engine.getWindowManager().getWindow(), true);
        }
    }

    public static void setProjectName(String projectName) {
        Engine.projectName = projectName;
        if (isBuild) {
            glfwSetWindowTitle(getWindowManager().getWindow(), Engine.getProjectName());
        } else {
            Properties props = new Properties();
            props.setProperty("projectName", projectName);
            try (FileOutputStream out = new FileOutputStream(AssetManager.getINSTANCE().getCompileDirectory() + "config.properties")) {
                props.store(out, "config");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(AssetManager.getINSTANCE().getCompileDirectory() + "config.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Engine.projectName = props.getProperty("projectName");
    }

    public static String getProjectName() {
        return projectName;
    }
}