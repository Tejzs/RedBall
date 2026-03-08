package redball.engine.renderer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import redball.engine.editor.EditorLayer;
import redball.engine.core.Engine;
import redball.engine.core.PhysicsSystem;
import redball.engine.entity.ECSWorld;
import redball.engine.renderer.texture.Texture;
import redball.engine.scene.AssetManager;
import redball.engine.scene.SceneManager;
import redball.engine.utils.AbstractScene;
import redball.engine.utils.FolderObserver;
import redball.engine.utils.ScriptManager;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class WindowManager {
    private static long window = 0L;
    private int width = 1920;
    private int height = 1080;
    private int fpsCap = Integer.MAX_VALUE;
    private AbstractScene scene;

    public void init() {
        if (window != 0L) {
            return;
        }

        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalArgumentException("Can't create window");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
        // antialiasing: 2,4,8,16
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = GLFW.glfwCreateWindow(width, height, "Red Ball", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Can't create window");
        }
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GLFW.glfwShowWindow(window);

        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        setVSync(0);

        Texture.init();

        // GUI
        FrameBuffer.init(width, height);

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            glViewport(0, 0, w, h);
            width = w;
            height = h;
        });
    }

    public void loop(Shader shader, boolean build) throws Exception {
        double lastTime = glfwGetTime();
        double lastSecond = lastTime;
        double physicsStep = 1.0 / 60.0;
        double accumulator = 0;
        int fps = 0;

        shader.use();
        SceneManager.init();
        SceneManager.loadDefault();
        if (build)
        {
            ECSWorld.start();
        }

        while (!GLFW.glfwWindowShouldClose(window)) {
            ScriptManager.processReloads();
            double time = glfwGetTime();
            double deltaTime = time - lastTime;
            accumulator += deltaTime;
            lastTime = time;

            if (Engine.isPlaying() || build) {
                // RENDER
                while (accumulator >= physicsStep) {
                    PhysicsSystem.update((float) physicsStep);
                    accumulator -= physicsStep;
                }
                // use if corrupt
                // scene.update((float) deltaTime);
                ECSWorld.update(Objects.requireNonNull(ECSWorld.findGameObjectByTag("Camera")), (float) deltaTime);
            } else {
                accumulator = 0;
            }
            RenderManager.render(Objects.requireNonNull(ECSWorld.findGameObjectByTag("Camera")));
            if (!build) {
                EditorLayer.getINSTANCE().renderDebug();
            }

            // SWAP
            glfwPollEvents();
            glfwSwapBuffers(window);
            // FPS Counter
            if (time - lastSecond >= 1.0) {
                setTitle("Red Ball " + AssetManager.getINSTANCE().currentWorkingScene + " FPS: " + fps);
                fps = 0;
                lastSecond += 1.0;
            }
            fps++;
        }
        EditorLayer.getINSTANCE().dispose();
        glfwTerminate();
        FolderObserver.stop();
    }

    public void useScene(AbstractScene scene) {
        scene.start();
        this.scene = scene;
    }

    public void setVSync(int val) {
        glfwSwapInterval(val);
    }

    public void setTitle(String name) {
        glfwSetWindowTitle(window, name);
    }

    public long getWindow() {
        return window;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
