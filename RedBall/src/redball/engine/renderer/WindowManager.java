package redball.engine.renderer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import redball.engine.editor.EditorLayer;
import redball.engine.core.Engine;
import redball.engine.core.PhysicsSystem;
import redball.engine.entity.ECSWorld;
import redball.engine.logger.LogCapture;
import redball.engine.renderer.texture.Texture;
import redball.engine.input.MouseInput;
import redball.engine.scene.AssetManager;
import redball.engine.scene.SceneManager;
import redball.engine.utils.FolderObserver;
import redball.engine.utils.ScriptManager;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class WindowManager {
    private static long window = 0L;
    private int width = 1920;
    private int height = 1080;
    private int fpsCap = Integer.MAX_VALUE;

    public void init() {
        if (window != 0L) {
            return;
        }

        GLFWErrorCallback.createPrint(System.err).set();

        glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
        // antialiasing: 2,4,8,16
        glfwWindowHint(GLFW_SAMPLES, 4);

        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
        
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
        setVSync(1);

        Texture.init();

        // GUI
        FrameBuffer.init(width, height);

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            glViewport(0, 0, w, h);
            if (Engine.isBuild) {
                width = w;
                height = h;
            }
            FrameBuffer.getINSTANCE().resize(w, h);
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

        if (build) {
            ECSWorld.start();
        } else {
            glfwSetWindowTitle(window, "RedBall Engine " + AssetManager.getINSTANCE().currentWorkingScene);
        }
        Engine.getShader().initTextureSamplers();

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
                ECSWorld.update(ECSWorld.getCamera(), (float) deltaTime);
            } else {
                accumulator = 0;
            }

            RenderManager.render(ECSWorld.getCamera());
            if (!build) {
                EditorLayer.getINSTANCE().renderDebug();
            }

            // SWAP
            MouseInput.endFrame();
            glfwPollEvents();
            glfwSwapBuffers(window);

            // FPS Counter
            fps++;
            if (time - lastSecond >= 1.0) {
                EditorLayer.setFps(fps);
                fps = 0;
                lastSecond = time;
            }
        }
        if (!Engine.isBuild) {
            EditorLayer.getINSTANCE().dispose();
            LogCapture.stop();
        }
        glfwTerminate();
        FolderObserver.stop();
    }

    public void setVSync(int val) {
        glfwSwapInterval(val);
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
