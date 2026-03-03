package redball.engine.core;

import imgui.*;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import imgui.flag.ImGuiCol;
import org.dyn4j.geometry.Rectangle;
import org.reflections.Reflections;
import redball.engine.core.Logger.LogCapture;
import redball.engine.core.Logger.LogLine;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.*;
import redball.engine.entity.components.Component;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.texture.Texture;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.reflections.Reflections.log;

public class EditorLayer {
    private static EditorLayer INSTANCE;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private String selected = null;
    private final ImGuiIO io;
    private int selectedIndex = -1;
    private String[] componentList = null;
    private Set<Class<? extends Component>> subclasses;
    private Long window;
    private File[] assets;
    private String currentFolder;
    private String[] breadCrumbs;
    private final ImString renameBuffer = new ImString(256);
    private File renamingFile = null;
    private String spriteName = "";

    public static void init(Long window) {
        INSTANCE = new EditorLayer(window);
    }

    public EditorLayer(Long window) {
        this.window = window;
        ImGui.createContext();
        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 150");
        io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.DpiEnableScaleFonts);
        io.getFonts().setFreeTypeRenderer(true);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.getFonts().addFontDefault();

        ImFontConfig config = new ImFontConfig();
        config.setMergeMode(true);
        config.setPixelSnapH(true);

        short[] iconRanges = {(short) 0xF000, (short) 0xF8FF, 0};

        io.getFonts().addFontFromFileTTF("resources/Font Awesome 7 Free-Solid-900.otf", 32.0f, config, iconRanges);
        io.getFonts().build();

        ImGuiStyle style = ImGui.getStyle();
        style.setColor(ImGuiCol.WindowBg, 0.08f, 0.08f, 0.12f, 1.00f);
        style.setColor(ImGuiCol.ChildBg, 0.10f, 0.10f, 0.15f, 1.00f);
        style.setColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 1.00f);
        style.setColor(ImGuiCol.HeaderHovered, 0.75f, 0.45f, 0.15f, 1.00f);
        style.setColor(ImGuiCol.HeaderActive, 0.95f, 0.58f, 0.20f, 1.00f);
        style.setColor(ImGuiCol.TitleBg, 1.37f, 0.74f, 0.39f, 1.00f);
        style.setColor(ImGuiCol.TitleBgActive, 1.17f, 0.63f, 0.33f, 1.00f);
        style.setColor(ImGuiCol.FrameBg, 0.14f, 0.14f, 0.20f, 1.00f);
        style.setColor(ImGuiCol.Button, 0.20f, 0.20f, 0.30f, 1.00f);

        // Get all components
        // needs fix
        Reflections reflections = new Reflections("redball");
        subclasses = reflections.getSubTypesOf(Component.class);
        componentList = new String[subclasses.size()-1];
        int index = 0;
        for (Class<? extends Component> cls : subclasses) {
            if (cls.isAssignableFrom(Transform.class)) {
                continue;
            }
            componentList[index] = cls.getSimpleName();
            index++;
        }
    }

    public static EditorLayer getINSTANCE() {
        return INSTANCE;
    }

    // Creates dockable space
    void createDockSpace() {
        int windowFlags = ImGuiWindowFlags.MenuBar
                | ImGuiWindowFlags.NoDocking
                | ImGuiWindowFlags.NoTitleBar
                | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoBringToFrontOnFocus
                | ImGuiWindowFlags.NoNavFocus;

        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getPosX(), viewport.getPosY());
        ImGui.setNextWindowSize(viewport.getSizeX(), viewport.getSizeY());
        ImGui.setNextWindowViewport(viewport.getID());

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);

        ImGui.begin("DockSpace", new ImBoolean(true), windowFlags);
        ImGui.popStyleVar(3);

        // Create the actual dockspace
        int dockspaceId = ImGui.getID("MyDockSpace");
        ImGui.dockSpace(dockspaceId, 0.0f, 0.0f, ImGuiDockNodeFlags.None);

        ImGui.end();
    }

    public void renderDebug() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();
        createDockSpace();

        renderMenuBar();

        ImGui.begin("Hierarchy");
        for (GameObject go : ECSWorld.getGameObjects()) {
            if (ImGui.button(go.getName())) {
                selected = go.getName();
            }
            if (ImGui.beginDragDropSource()) {
                ImGui.setDragDropPayload("GAME_OBJECT", go);
                ImGui.text(go.getName());
                ImGui.endDragDropSource();
            }
            if (ImGui.beginDragDropTarget()) {
                Object payload = ImGui.acceptDragDropPayload("String");
                if (payload instanceof String dropped) {
                    Component component = go.addComponent(getComponent(dropped));
                    if (component != null) {
                        try {
                            component.start();
                        } catch (Exception e) {
                            log.error("e: ", e);
                        }
                    }
                }
                ImGui.endDragDropTarget();
            }
        }
        ImGui.end();

        renderViewPort();

        ImGui.begin("Inspector");
        if (selected != null) {
            GameObject go = ECSWorld.findGameObjectByName(selected);
            ImGui.text("Name");
            ImGui.sameLine();
            ImGui.inputText("##Name", new ImString(go.getName()));
            tagComponent(go);
            transformComponent(go);
            rigidBodyComponent(go);
            spriteRendererComponent(go);

            ListIterator<Component> componentIterator = go.getComponents().listIterator();
            while (componentIterator.hasNext()) {
                Component c = componentIterator.next();
                if (!(c instanceof Rigidbody) && !(c instanceof Transform) && !(c instanceof Tag) && !(c instanceof SpriteRenderer) && c != null) {
                    customComponents(go.getComponent(c.getClass()), componentIterator);
                }
            }

            addComponent(go);
            if (ImGui.beginDragDropTarget()) {
                Object payload = ImGui.acceptDragDropPayload("String");
                if (payload instanceof String dropped) {
                    Component component = go.addComponent(getComponent(dropped));
                    if (component != null) {
                        try {
                            component.start();
                        } catch (Exception e) {
                            System.err.println("ERROR: " + e);
                        }
                    }
                }
                ImGui.endDragDropTarget();
            }
        }
        ImGui.end();

        assetBrowser();
        renderConsole();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    void renderViewPort() throws IllegalAccessException {
        ImGui.begin("Viewport");
        if (ImGui.button("Save")) {
            Serialization.save();
        }
        ImVec2 size = ImGui.getContentRegionAvail();

        float frameBufferWidth = RenderManager.getFrameBuffer().getWidth();
        float frameBufferHeight = RenderManager.getFrameBuffer().getHeight();

        float aspect = frameBufferWidth / frameBufferHeight;
        float windowAspect = size.x / size.y;

        float renderWidth = size.x;
        float renderHeight = size.y;

        if (windowAspect > aspect) {
            renderWidth = size.y * aspect;
        } else {
            renderHeight = size.x / aspect;
        }

        ImVec2 cur = ImGui.getCursorPos();
        ImGui.setCursorPos((size.x + cur.x - renderWidth) / 2, (25f + size.y + cur.y - renderHeight) / 2);

        ImGui.image(RenderManager.getFrameBuffer().getTextureId(), new ImVec2(renderWidth, renderHeight), new ImVec2(0, 1), new ImVec2(1, 0));

        ImGui.end();
    }

    private void renderMenuBar() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("New")) {
                    System.out.println("New Clicked!!");
                }
                if (ImGui.menuItem("Save")) {
                    System.out.println("Save Clicked!!");
                }
                ImGui.separator();
                if (ImGui.menuItem("Exit")) {
                    glfwSetWindowShouldClose(window, true);
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("Add Component")) {
                    System.out.println("Add clicked!!");
                }
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }
    }

    private Component getComponent(int n) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        for (Class<? extends Component> cls : subclasses) {
            if (cls.getSimpleName().equals(componentList[n])) {
                Constructor<?> constructor = cls.getConstructors()[0];
                Object[] params = new Object[constructor.getParameterCount()];
                // params are already null by default
                Component instance = (Component) constructor.newInstance(params);
                return instance;
            }
        }
        return null;
    }

    private Component getComponent(String name) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        for (Class<? extends Component> cls : subclasses) {
            if (cls.getSimpleName().equals(name)) {
                Constructor<?> constructor = cls.getConstructors()[0];
                Object[] params = new Object[constructor.getParameterCount()];
                // params are already null by default
                Component instance = (Component) constructor.newInstance(params);
                return instance;
            }
        }
        return null;
    }

    private void addComponent(GameObject go) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        ImString searchBuffer = new ImString(256);

        if (ImGui.button("Add Component")) {
            ImGui.openPopup("##addComponent");
        }

        if (ImGui.beginPopup("##addComponent")) {
            ImGui.inputTextWithHint("##search", "Search...", searchBuffer);

            if (ImGui.beginListBox("##ListBox")) {
                for (int n = 0; n < componentList.length; n++) {

                    String query = searchBuffer.get().toLowerCase();
                    if (!query.isEmpty() && !componentList[n].toLowerCase().contains(query)) {
                        continue;
                    }

                    boolean is_selected = (selectedIndex == n);
                    if (ImGui.selectable(componentList[n], is_selected, ImGuiSelectableFlags.AllowDoubleClick)) {
                        if (ImGui.isMouseDoubleClicked(0)) {
                            selectedIndex = n;
                            Component c = go.addComponent(getComponent(n));
                            if (c instanceof SpriteRenderer) RenderManager.rebuild();
                            try {
                                c.start();
                            } catch (Exception e) {
                                System.err.println("ERROR: " + e);
                            }
                        }
                    }
                    if (is_selected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endListBox();
            }
            ImGui.endPopup();
        }
        selectedIndex = -1;
    }


    private void transformComponent(GameObject go) {
        if (ImGui.collapsingHeader("Transform", ImGuiTreeNodeFlags.DefaultOpen)) {
            Transform transform = go.getComponent(Transform.class);
            float[] pos = {transform.getXPosition(), transform.getYPosition()};
            if (ImGui.dragFloat2("Position", pos)) {
                transform.setXPosition(pos[0]);
                transform.setYPosition(pos[1]);
            }
            float[] rot = {transform.getRotation()};
            if (ImGui.dragFloat("Rotation", rot)) {
                transform.setRotation(rot[0] % 360);
            }
            float[] scale = {transform.getScaleX(), transform.getScaleY()};
            if (ImGui.dragFloat2("Scale", scale)) {
                transform.setXScale(scale[0]);
                transform.setYScale(scale[1]);
            }
        }
    }

    private void tagComponent(GameObject go) {
        Tag tag = go.getComponent(Tag.class);
        if (tag != null) {
            ImGui.text("Tag");
            ImGui.sameLine();
            ImString val = new ImString(tag.getTag(), 256);
            if (ImGui.inputText("##Tag", val)) {
                tag.setTag(val.get());
            }
        }
    }

    private void spriteRendererComponent(GameObject go) {
        SpriteRenderer spriteRenderer = go.getComponent(SpriteRenderer.class);
        if (spriteRenderer != null) {
            if (ImGui.collapsingHeader("Sprite Renderer", ImGuiTreeNodeFlags.DefaultOpen)) {
                boolean rightClicked = ImGui.isItemHovered() && ImGui.isMouseClicked(1);
                if (rightClicked) {
                    ImGui.openPopup("RemoveSpriteRendererPopup");
                }

                if (ImGui.beginPopup("RemoveSpriteRendererPopup")) {
                    if (ImGui.menuItem("Remove Component")) {
                        go.removeComponent(SpriteRenderer.class);
                        RenderManager.rebuild();
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endMenu();
                }
                ImGui.text("Sprite");
                ImGui.sameLine();
                String label = (spriteRenderer != null) ? spriteName : "None ( Sprite Renderer )";
                ImGui.inputText("##Label", new ImString(label));
                if (ImGui.beginDragDropTarget()) {
                    String payload = ImGui.acceptDragDropPayload("String");
                    if (payload instanceof String dropped) {
                        spriteRenderer.setTexture(new Texture(dropped));
                        RenderManager.rebuild();
                    }
                    ImGui.endDragDropTarget();
                }
            }
            spriteName = new File(spriteRenderer.getFilePath()).getName();
        }
    }

    private void rigidBodyComponent(GameObject go) {
        Rigidbody rb = go.getComponent(Rigidbody.class);
        if (rb != null) {
            if (ImGui.collapsingHeader("RigidBody", ImGuiTreeNodeFlags.DefaultOpen)) {
                if (ImGui.beginDragDropSource()) {
                    ImGui.setDragDropPayload("GAME_OBJECT", go);
                    ImGui.text(go.getName());
                    ImGui.endDragDropSource();
                }
                boolean rightClicked = ImGui.isItemHovered() && ImGui.isMouseClicked(1);
                if (rightClicked) {
                    ImGui.openPopup("RemoveRigidBodyPopup");
                }

                if (ImGui.beginPopup("RemoveRigidBodyPopup")) {
                    if (ImGui.menuItem("Remove Component")) {
                        go.removeComponent(rb.getClass());
                        RenderManager.rebuild();
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endMenu();
                }

                ImGui.checkbox("isDynamic", rb.getBodyType() == BodyType.DYNAMIC ? true : false);
                ImGui.inputText("Shape", new ImString(rb.getBody().getFixture(0).getShape() instanceof Rectangle ? "Rectangle" : "Circle"));
                ImGui.inputText("Mass", new ImString(String.valueOf(rb.getMass())));
                ImGui.inputText("Bounciness", new ImString(String.valueOf(rb.getBounce())));
                ImGui.inputText("Friction", new ImString(String.valueOf(rb.getFriction())));
            }
        }
    }

    private void customComponents(Component component, ListIterator<Component> iterator) {
        Class<?> clazz = component.getClass();
        if (ImGui.collapsingHeader(clazz.getSimpleName(), ImGuiTreeNodeFlags.DefaultOpen)) {
            boolean rightClicked = ImGui.isItemHovered() && ImGui.isMouseClicked(1);
            if (rightClicked) {
                ImGui.openPopup("Remove" + clazz.getSimpleName() + "Popup");
            }

            if (ImGui.beginPopup("Remove" + clazz.getSimpleName() + "Popup")) {
                if (ImGui.menuItem("Remove Component")) {
                    iterator.remove();
                    ImGui.closeCurrentPopup();
                }
                ImGui.endMenu();
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isPublic(field.getModifiers())) continue;
                try {
                    String name = field.getName();
                    Object value = field.get(component);
                    if (field.getType() == float.class) {
                        float[] val = {(float) value};
                        if (ImGui.dragFloat(name, val)) {
                            field.set(component, val[0]);
                        }
                    } else if (field.getType() == int.class) {
                        int[] val = {(int) value};
                        if (ImGui.dragInt(name, val)) {
                            field.set(component, val[0]);
                        }
                    } else if (field.getType() == boolean.class) {
                        boolean val = (boolean) value;
                        if (ImGui.checkbox(name, val)) {
                            field.set(component, val);
                        }
                    } else if (field.getType() == String.class) {
                        ImString val = new ImString(value.toString());
                        if (ImGui.inputText(name, new ImString(value.toString()))) {
                            field.set(component, val);
                        }
                    } else if (Component.class.isAssignableFrom(field.getType())) {
                        Component ref = (Component) value;
                        String label = (ref != null) ? ref.gameObject.getName() + " (" + field.getType().getSimpleName() + ")" : "None (" + field.getType().getSimpleName() + ")";
                        ImGui.inputText(name, new ImString(label), ImGuiInputTextFlags.ReadOnly);

                        if (ImGui.beginDragDropTarget()) {
                            Object payload = ImGui.acceptDragDropPayload("GAME_OBJECT");
                            if (payload instanceof GameObject dropped) {
                                Component comp = dropped.getComponent((Class<? extends Component>) field.getType());
                                if (comp != null) {
                                    field.set(component, comp);
                                }
                            }
                            ImGui.endDragDropTarget();
                        }
                    } else if (field.getType() == GameObject.class) {
                        GameObject ref = (GameObject) value;
                        String label = (ref != null) ? ref.getName() + " (" + field.getType().getSimpleName() + ")" : "None (" + field.getType().getSimpleName() + ")";
                        ImGui.inputText(name, new ImString(label), ImGuiInputTextFlags.ReadOnly);

                        if (ImGui.beginDragDropTarget()) {
                            Object payload = ImGui.acceptDragDropPayload("GAME_OBJECT");
                            if (payload instanceof GameObject dropped) {
                                field.set(component, dropped);
                            }
                            ImGui.endDragDropTarget();
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void assetBrowser() {
        ImGui.begin("AssetBrowser");
        float thumbnailSize = 64;
        float padding = 8;
        float cellSize = thumbnailSize + padding;

        float panelWidth = ImGui.getContentRegionAvailX();
        int columnCount = Math.max(1, (int) (panelWidth / cellSize));
        currentFolder = AssetManager.getFile().getPath();
        breadCrumbs = currentFolder.split("/");


        for (int i = 0; i < breadCrumbs.length; i++) {
            if (i != 0) {
                ImGui.sameLine();
                ImGui.setWindowFontScale(0.3f);
                float posX = ImGui.getCursorPosX();
                float posY = ImGui.getCursorPosY();
                ImGui.setCursorPos(posX, posY + 7);
                ImGui.text(getIcon("chevron"));
                ImGui.setWindowFontScale(1.0f);
                ImGui.sameLine();
            }

            if (ImGui.button(breadCrumbs[i])) {
                String[] sub = Arrays.copyOfRange(breadCrumbs, 0, i + 1);
                currentFolder = String.join("/", sub);
                AssetManager.setFile(new File(currentFolder));
            }
        }

        assets = AssetManager.getFile().listFiles();

        ImGui.columns(columnCount, "assetGrid", false);
        // Allow only in asset browser tab
        if (ImGui.isMouseClicked(1) && ImGui.isWindowHovered(ImGuiHoveredFlags.AllowWhenBlockedByPopup)) {
            ImGui.openPopup("FileEditPopup");
        }

        if (ImGui.beginPopup("FileEditPopup")) {
            if (ImGui.beginMenu("Create"))  {
                if (ImGui.menuItem("Folder")) {
                    System.out.println("Clicked folder");
                }
                if (ImGui.menuItem("Script")) {
                    System.out.println("Clicked script");
                }
                ImGui.endMenu();
            }
            ImGui.endPopup();
        }
        for (File asset : assets) {
            String name = asset.getName();
            String fileType = asset.isDirectory() ? "FOLDER" : asset.getName().substring(asset.getName().lastIndexOf("."));

            ImGui.pushID(asset.getName());

            float posX = ImGui.getCursorPosX();
            float posY = ImGui.getCursorPosY();

            ImGui.selectable("##" + name, false, 0, thumbnailSize, thumbnailSize + 10);
            boolean clicked = ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0);
            boolean rightClicked = ImGui.isItemHovered() && ImGui.isMouseClicked(1);
            if (ImGui.beginDragDropSource()) {
                if (fileType.equals(".java")) {
                    ImGui.setDragDropPayload("String", name.substring(0, asset.getName().lastIndexOf(".")));
                } else {
                    ImGui.setDragDropPayload("String", asset.getPath());
                }
                ImGui.setWindowFontScale(0.3f);
                ImGui.text(asset.isDirectory() ? "\uF07B" : "\uF15B");
                ImGui.setWindowFontScale(1.0f);
                ImGui.text(asset.getName());
                ImGui.endDragDropSource();
            }

            ImGui.setCursorPos(posX + (thumbnailSize / 2) - thumbnailSize / 4, posY + thumbnailSize / 2);
            ImGui.setWindowFontScale(1.0f);

            ImGui.text(asset.isDirectory() ? getIcon("folder") : getIcon("file"));
            float textWidth = Math.min(ImGui.calcTextSize(name).x, thumbnailSize);
            ImGui.setCursorPos(posX + (thumbnailSize / 2) - (textWidth / 2), posY + thumbnailSize - 10);

            if (renamingFile != null && renamingFile.equals(asset)) {
                ImGui.setKeyboardFocusHere();
                if (ImGui.inputText("##rename", renameBuffer, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    asset.renameTo(new File(asset.getParent() + "/" + renameBuffer.get()));
                    renamingFile = null;
                }
                if (ImGui.isKeyPressed(ImGuiKey.Escape)) {
                    renamingFile = null;
                }
            }
            else {
                ImGui.textWrapped(name);
            }

            if (clicked) {
                if (asset.isDirectory()) {
                    currentFolder += "/" + name;
                    AssetManager.setFile(new File(currentFolder));
                }
            }

            if (rightClicked) {
                ImGui.openPopup("FileSelectPopup");
            }

            if (ImGui.beginPopup("FileSelectPopup")) {
                if (ImGui.menuItem("Rename")) {
                    renamingFile = asset;
                    renameBuffer.set(name);
                    ImGui.closeCurrentPopup();
                }
                ImGui.endMenu();
            }

            ImGui.popID();
            ImGui.nextColumn();
        }

        ImGui.columns(1);
        ImGui.end();
    }

    private void renderConsole() {
        ImGui.begin("Console");
        ArrayDeque<LogLine> lines = LogCapture.getLogs();
        for (LogLine line : lines) {
            if (line.isError()) {
                ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.0f, 0.0f, 1.0f);
                ImGui.text(line.getMessage());
                ImGui.popStyleColor();
            }
            else {
                ImGui.text(line.getMessage());
            }
        }
        if (ImGui.button("Clear")) {
            LogCapture.clear();
        }
        ImGui.end();
    }

    public String getIcon(String icon) {
        return switch (icon) {
            case "folder" -> "\uF07B";
            case "file" -> "\uF1C9";
            case "chevron" -> "\uF054";
            default -> "?";
        };
    }

    public void dispose() {
        imGuiGl3.destroyDeviceObjects();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
    }

    public ImGuiImplGlfw getImGuiGlfw() {
        return imGuiGlfw;
    }
}
