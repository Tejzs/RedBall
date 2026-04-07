package redball.engine.input;

import imgui.glfw.ImGuiImplGlfw;
import redball.engine.editor.EditorLayer;

import static org.lwjgl.glfw.GLFW.*;

public class KeyboardInput {
    private static final boolean[] KEYS = new boolean[GLFW_KEY_LAST];

    private KeyboardInput() {}

    public static void init(long window, EditorLayer instance) {
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (instance != null) {
                instance.getImGuiGlfw().keyCallback(w, key, scancode, action, mods);
            }

            if (key >= 0 && key < GLFW_KEY_LAST) {
                KEYS[key] = (action != GLFW_RELEASE);
            }
        });
    }

    public static boolean isKeyDown(int keyCode) {
        return KEYS[keyCode];
    }
}