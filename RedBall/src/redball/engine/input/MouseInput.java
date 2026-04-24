package redball.engine.input;

import redball.engine.editor.EditorLayer;

import static org.lwjgl.glfw.GLFW.*;

public class MouseInput {
    private static double lastX, lastY, x, y;
    private static double scrollX, scrollY;
    private static final boolean[] MOUSE_BUTTONS = new boolean[GLFW_MOUSE_BUTTON_LAST];
    private static boolean isDragging;

    private MouseInput() {}

    public static void init(long window, EditorLayer instance) {
        glfwSetCursorPosCallback(window, (w, xpos, ypos) -> {
            if (instance != null) {
                instance.getImGuiGlfw().cursorPosCallback(w, xpos, ypos);
            }
            x = xpos;
            y = ypos;
            
            boolean anyDown = false;
            for (boolean buttonDown : MOUSE_BUTTONS) {
                if (buttonDown) {
                    anyDown = true;
                    break;
                }
            }
            isDragging = anyDown;
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (instance != null) {
                instance.getImGuiGlfw().mouseButtonCallback(w, button, action, mods);
            }

            if (button < MOUSE_BUTTONS.length) {
                if (action == GLFW_PRESS) {
                    MOUSE_BUTTONS[button] = true;
                } else if (action == GLFW_RELEASE) {
                    MOUSE_BUTTONS[button] = false;
                    isDragging = false;
                }
            }
        });

        glfwSetScrollCallback(window, (w, xoffset, yoffset) -> {
            if (instance != null) {
                instance.getImGuiGlfw().scrollCallback(w, xoffset, yoffset);
            }
            scrollX = xoffset;
            scrollY = yoffset;
        });
    }

    public static void endFrame() {
        scrollX = 0;
        scrollY = 0;
        lastX = x;
        lastY = y;
    }

    public static float getX() { return (float) x; }
    public static float getY() { return (float) y; }
    public static float getDx() { return (float) (x - lastX); }
    public static float getDy() { return (float) (y - lastY); }
    public static float getScrollX() { return (float) scrollX; }
    public static float getScrollY() { return (float) scrollY; }
    public static boolean isDragging() { return isDragging; }
    public static boolean isMouseButtonDown(int button) {
        if (button < MOUSE_BUTTONS.length) {
            return MOUSE_BUTTONS[button];
        }
        return false;
    }
}
