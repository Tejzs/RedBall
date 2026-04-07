package redball.engine.editor;

import imgui.*;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import imgui.flag.ImGuiCol;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.joml.Vector3f;
import org.reflections.Reflections;
import redball.engine.core.Engine;
import redball.engine.logger.LogCapture;
import redball.engine.logger.LogLine;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.*;
import redball.engine.entity.components.Component;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.texture.Texture;
import redball.engine.save.SaveManager;
import redball.engine.scene.AssetManager;
import redball.engine.scene.SceneManager;
import redball.engine.utils.PakWriter;
import redball.engine.utils.ScriptManager;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.reflections.Reflections.log;

public class EditorLayer {
    private static EditorLayer INSTANCE;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private String selected = null;
    private final ImGuiIO io;
    private int selectedIndex = -1;
    private Long window;
    private File[] assets;
    private String currentFolder;
    private String[] breadCrumbs;
    private final ImString renameBuffer = new ImString(256);
    private File renamingFile = null;
    private String spriteName = "";
    private boolean showNewScenePopup = false;
    private boolean showNewScriptPopup = false;
    private ImString sceneName = new ImString(256);
    private ImString scriptName = new ImString(256);
    private ImBoolean showSceneManager = new ImBoolean(false);

    private static boolean compileSuccess = false;
    private static int fps = 0;

    private static String[] componentList = null;
    private static Set<Class<? extends Component>> subclasses;

    private ImString nameBuffer = null;
    private String prevSelected = null;

    private ImString searchBuffer = null;
    ImInt fixtureSelectedIndex = new ImInt(0);
    String[] bodyFixtures = Arrays.stream(BodyFixture.values()).map(Enum::name).toArray(String[]::new);

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
        io.getFonts().setFreeTypeRenderer(true);

        ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setFontBuilderFlags(ImGuiFreeTypeBuilderFlags.ForceAutoHint | ImGuiFreeTypeBuilderFlags.LightHinting);
        fontConfig.setOversampleH(1);
        fontConfig.setOversampleV(1);
        fontConfig.setPixelSnapH(true);
        io.getFonts().addFontFromFileTTF("resources/Inter_18pt-Regular.ttf", 15.0f, fontConfig);

        ImFontConfig iconConfig = new ImFontConfig();
        iconConfig.setMergeMode(true);
        iconConfig.setPixelSnapH(false);
        iconConfig.setOversampleH(2);
        iconConfig.setOversampleV(2);
        short[] iconRanges = {(short) 0xF000, (short) 0xF8FF, 0};
        io.getFonts().addFontFromFileTTF("resources/Font Awesome 7 Free-Solid-900.otf", 30.0f, iconConfig, iconRanges);

        io.getFonts().build();


        ImGuiStyle style = ImGui.getStyle();

        // === Backgrounds — push darker ===
        style.setColor(ImGuiCol.WindowBg, 0.110f, 0.110f, 0.110f, 1.00f); // #1C1C1C — main panels
        style.setColor(ImGuiCol.ChildBg, 0.090f, 0.090f, 0.090f, 1.00f); // #171717 — nested areas
        style.setColor(ImGuiCol.PopupBg, 0.122f, 0.122f, 0.122f, 1.00f); // #1F1F1F — popups/dropdowns

        // === Borders ===
        style.setColor(ImGuiCol.Border, 0.137f, 0.137f, 0.137f, 1.00f); // #232323
        style.setColor(ImGuiCol.BorderShadow, 0.000f, 0.000f, 0.000f, 0.00f); // transparent

        // === Frames (inputs, search bar) ===
        style.setColor(ImGuiCol.FrameBg, 0.098f, 0.098f, 0.098f, 1.00f); // #191919
        style.setColor(ImGuiCol.FrameBgHovered, 0.157f, 0.157f, 0.157f, 1.00f); // #282828
        style.setColor(ImGuiCol.FrameBgActive, 0.188f, 0.188f, 0.188f, 1.00f); // #303030

        // === Title bars — your orange accent ===
        style.setColor(ImGuiCol.TitleBg, 0.55f, 0.32f, 0.10f, 1.00f); // your original
        style.setColor(ImGuiCol.TitleBgActive, 0.75f, 0.45f, 0.15f, 1.00f); // your original
        style.setColor(ImGuiCol.TitleBgCollapsed, 0.157f, 0.157f, 0.157f, 1.00f); // #282828

        // === Menu bar ===
        style.setColor(ImGuiCol.MenuBarBg, 0.067f, 0.067f, 0.067f, 1.00f); // #111111

        // === Scrollbars ===
        style.setColor(ImGuiCol.ScrollbarBg, 0.078f, 0.078f, 0.078f, 1.00f); // #141414
        style.setColor(ImGuiCol.ScrollbarGrab, 0.373f, 0.373f, 0.373f, 1.00f); // #5F5F5F
        style.setColor(ImGuiCol.ScrollbarGrabHovered, 0.408f, 0.408f, 0.408f, 1.00f); // #686868
        style.setColor(ImGuiCol.ScrollbarGrabActive, 0.55f, 0.32f, 0.10f, 1.00f); // orange accent on drag

        // === Checkmark & sliders ===
        style.setColor(ImGuiCol.CheckMark, 0.95f, 0.58f, 0.20f, 1.00f); // orange accent
        style.setColor(ImGuiCol.SliderGrab, 0.75f, 0.45f, 0.15f, 1.00f); // orange accent
        style.setColor(ImGuiCol.SliderGrabActive, 0.95f, 0.58f, 0.20f, 1.00f); // orange accent bright

        // === Buttons — your original + hover states ===
        style.setColor(ImGuiCol.Button, 0.20f, 0.20f, 0.30f, 1.00f); // your original
        style.setColor(ImGuiCol.ButtonHovered, 0.55f, 0.32f, 0.10f, 1.00f); // orange on hover
        style.setColor(ImGuiCol.ButtonActive, 0.95f, 0.58f, 0.20f, 1.00f); // bright orange on click

        // === Headers (collapsing headers, hierarchy rows) — your orange accent ===
        style.setColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 1.00f); // your original
        style.setColor(ImGuiCol.HeaderHovered, 0.75f, 0.45f, 0.15f, 1.00f); // your original
        style.setColor(ImGuiCol.HeaderActive, 0.95f, 0.58f, 0.20f, 1.00f); // your original

        // === Separators ===
        style.setColor(ImGuiCol.Separator, 0.137f, 0.137f, 0.137f, 1.00f); // #232323
        style.setColor(ImGuiCol.SeparatorHovered, 0.75f, 0.45f, 0.15f, 1.00f); // orange
        style.setColor(ImGuiCol.SeparatorActive, 0.95f, 0.58f, 0.20f, 1.00f); // orange

        // === Resize grip ===
        style.setColor(ImGuiCol.ResizeGrip, 0.55f, 0.32f, 0.10f, 0.40f); // orange faded
        style.setColor(ImGuiCol.ResizeGripHovered, 0.75f, 0.45f, 0.15f, 0.70f); // orange
        style.setColor(ImGuiCol.ResizeGripActive, 0.95f, 0.58f, 0.20f, 1.00f); // orange bright

        // === Tabs ===
        style.setColor(ImGuiCol.Tab, 0.157f, 0.157f, 0.157f, 1.00f); // #282828
        style.setColor(ImGuiCol.TabHovered, 0.55f, 0.32f, 0.10f, 1.00f); // orange
        style.setColor(ImGuiCol.TabActive, 0.75f, 0.45f, 0.15f, 1.00f); // orange bright
        style.setColor(ImGuiCol.TabUnfocused, 0.122f, 0.122f, 0.122f, 1.00f); // #1F1F1F
        style.setColor(ImGuiCol.TabUnfocusedActive, 0.55f, 0.32f, 0.10f, 0.70f); // orange dimmed

        // === Docking ===
        style.setColor(ImGuiCol.DockingPreview, 0.75f, 0.45f, 0.15f, 0.70f); // orange
        style.setColor(ImGuiCol.DockingEmptyBg, 0.118f, 0.118f, 0.118f, 1.00f); // #1E1E1E

        // === Text ===
        style.setColor(ImGuiCol.Text, 0.824f, 0.824f, 0.824f, 1.00f); // #D2D2D2
        style.setColor(ImGuiCol.TextDisabled, 0.400f, 0.400f, 0.400f, 1.00f); // #666666
        style.setColor(ImGuiCol.TextSelectedBg, 0.55f, 0.32f, 0.10f, 0.55f); // orange selection

        // === Metrics — unchanged from your original ===
        style.setWindowRounding(2.0f);
        style.setFrameRounding(2.0f);
        style.setScrollbarRounding(2.0f);
        style.setGrabRounding(2.0f);
        style.setTabRounding(2.0f);
        style.setFramePadding(4.0f, 3.0f);
        style.setItemSpacing(4.0f, 4.0f);
    }

    public static void initComponentList() {
        System.out.println("Checking");
        Reflections reflections = new Reflections("redball.engine");
        subclasses = reflections.getSubTypesOf(Component.class);
        for (String className : ScriptManager.getClassMap().keySet()) {
            subclasses.add((Class<? extends Component>) ScriptManager.getClassMap().get(className));
        }

        componentList = new String[subclasses.size() - 1];
        int index = 0;
        for (Class<? extends Component> cls : subclasses) {
            if (cls.isAssignableFrom(Transform.class)) {
                continue;
            }
            System.out.println(cls.getSimpleName());
            componentList[index] = cls.getSimpleName();
            index++;
        }
    }

    public static EditorLayer getINSTANCE() {
        return INSTANCE;
    }

    public static boolean isCompileSuccess() {
        return compileSuccess;
    }

    public static void setCompileSuccess(boolean compileSuccess) {
        EditorLayer.compileSuccess = compileSuccess;
    }

    // Creates dockable space
    void createDockSpace() {
        int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus;

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

    public void renderDebug() throws Exception {
        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();

        renderStatusBar();

        createDockSpace();

        renderMenuBar();
        renderHierarchy();
        renderViewPort();

        ImGui.begin("Inspector");
        GameObject go = ECSWorld.findGameObjectByName(selected);
        if (go != null) {
            if (!selected.equals(prevSelected)) {
                nameBuffer = new ImString(go.getName(), 256);
                prevSelected = selected;
            }
            ImGui.text("Name");
            ImGui.sameLine();
            if (ImGui.inputText("##Name", nameBuffer)) {
                if (!nameBuffer.get().isEmpty()) {
                    int count = countDuplicates(nameBuffer.get());
                    if (count < 1) {
                        go.setName(nameBuffer.get());
                        selected = nameBuffer.get();
                        prevSelected = nameBuffer.get();
                    } else {
                        int suffix = count;
                        while (countDuplicates(nameBuffer.get() + " (" + suffix + ")") > 0) {
                            suffix++;
                        }
                        go.setName(nameBuffer.get() + " (" + suffix + ")");
                        prevSelected = go.getName();
                        selected = go.getName();
                    }
                }
            }
            tagComponent(go);
            transformComponent(go);
            rigidBodyComponent(go);
            spriteRendererComponent(go);

            Iterator<Component> componentIterator = go.getComponents().listIterator();
            while (componentIterator.hasNext()) {
                Component c = componentIterator.next();
                if (!(c instanceof Rigidbody) && !(c instanceof Transform) && !(c instanceof Tag) && !(c instanceof SpriteRenderer) && c != null) {
                    customComponents(c, componentIterator);
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
        renderSceneManager();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

    }

    private void renderHierarchy() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        ImGui.begin("Hierarchy");

        ImGui.pushStyleColor(ImGuiCol.FrameBg, 0.12f, 0.12f, 0.18f, 1.0f);
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        if (searchBuffer == null) searchBuffer = new ImString(256);
        ImGui.inputTextWithHint("##Search", "Search...", searchBuffer);
        ImGui.popStyleColor();
        ImGui.spacing();

        if (ImGui.isMouseClicked(1) && ImGui.isWindowHovered(ImGuiHoveredFlags.AllowWhenBlockedByPopup)) {
            ImGui.openPopup("HierarchyEdit");
        }

        if (ImGui.beginPopup("HierarchyEdit")) {
            if (ImGui.menuItem("Create GameObject")) {
                int count = countDuplicates("GameObject");
                String name;
                if (count < 1) {
                    name = "GameObject";
                } else {
                    int suffix = count;
                    while (countDuplicates("GameObject" + " (" + suffix + ")") > 0) {
                        suffix++;
                    }
                    name = "GameObject" + " (" + suffix + ")";
                }
                GameObject g = ECSWorld.createGameObject(name);
                g.addComponent(new Transform(new Vector3f(0.0f, 0.0f, 0.0f), 0.0f, new Vector3f(250.0f)));
            }
            ImGui.endMenu();
        }

        Iterator<GameObject> gameObjectIterator = ECSWorld.getGameObjects().listIterator();
        while (gameObjectIterator.hasNext()) {
            String search = searchBuffer.get().toLowerCase();
            GameObject go = gameObjectIterator.next();
            if (!search.isEmpty() && !go.getName().toLowerCase().contains(search)) continue;

            float itemHeight = 14;
            float x = ImGui.getCursorScreenPosX();
            float y = ImGui.getCursorScreenPosY();

            boolean isSelected = selected != null && selected.equals(go.getName());
            if (ImGui.selectable("##" + go.getName(), isSelected, ImGuiSelectableFlags.None, 0, itemHeight)) {
                selected = go.getName();
            }

            float textY = y + (itemHeight - ImGui.getTextLineHeight()) / 2;
            ImGui.getWindowDrawList().addText(x + 8, textY, ImGui.colorConvertFloat4ToU32(0.85f, 0.85f, 0.85f, 1.0f), go.getName());

            if (ImGui.isItemClicked(ImGuiMouseButton.Right)) {
                selected = go.getName();
                ImGui.openPopup("GameObjectContext##" + go.getName());
            }
            if (ImGui.beginPopup("GameObjectContext##" + go.getName())) {
                if (ImGui.menuItem("Delete")) {
                    gameObjectIterator.remove();
                    if (selected != null && selected.equals(go.getName())) {
                        selected = null;
                        prevSelected = null;
                    }
                    RenderManager.rebuild();
                }
                if (ImGui.menuItem("Create Prefab")) {
                    try (BufferedOutputStream sceneOut = new BufferedOutputStream(new FileOutputStream(AssetManager.getINSTANCE().prefabDirectory + go.getName() + ".prefab"))) {
                        IOUtils.write(SerializationUtils.serialize(go), sceneOut);
                    } catch (IOException e) {
                        System.out.println("ERROR:" + e);
                    }
                }
                ImGui.endPopup();
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

        float availX = ImGui.getContentRegionAvailX();
        float availY = ImGui.getContentRegionAvailY();
        ImGui.dummy(availX == 0 ? 1.0f : availX, availY == 0 ? 1.0f : availY);

        if (ImGui.beginDragDropTarget()) {
            Object stringPayload = ImGui.acceptDragDropPayload("String");
            if (stringPayload instanceof String dropped) {
                try (BufferedInputStream prefab = new BufferedInputStream(new FileInputStream(dropped))) {
                    // needs fix
                    ECSWorld.instantiate(SerializationUtils.deserialize(IOUtils.toByteArray(prefab)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            ImGui.endDragDropTarget();
        }

        ImGui.end();
    }

    void renderViewPort() {
        ImGui.begin("Viewport");

        if (ImGui.button("Save") && !Engine.isPlaying()) {
            SaveManager.save();
        }

        if (!Engine.isPlaying()) {
            if (ImGui.button("Play")) {
                Engine.onPlay();
            }
        } else {
            if (ImGui.button("Stop")) {
                Engine.onStop();
            }
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

    private void renderMenuBar() throws Exception {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("New")) {
                    System.out.println("New Clicked!!");
                }
                if (ImGui.menuItem("Save")) {
                    System.out.println("Save Clicked!!");
                }
                if (ImGui.menuItem("Scene Manager")) {
                    showSceneManager.set(true);
                }
                if (ImGui.menuItem("Build")) {
                    build();
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

    private void build() throws Exception {
        PakWriter.writePak(AssetManager.getINSTANCE().getWorkingDirectory());
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
                            if (c instanceof Rigidbody) ((Rigidbody) c).createBody();
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

                ImBoolean isDynamic = new ImBoolean(rb.getBodyType() == BodyType.DYNAMIC);
                if (ImGui.checkbox("isDynamic", isDynamic.get())) {
                    rb.setBodyType(isDynamic.get() ? BodyType.STATIC : BodyType.DYNAMIC);
                    rb.physicsSystemSetBodyType(isDynamic.get() ? BodyType.STATIC : BodyType.DYNAMIC);
                }

                fixtureSelectedIndex.set(rb.getBodyFixture().ordinal());
                if (ImGui.combo("Collision Shape", fixtureSelectedIndex, bodyFixtures)) {
                    rb.setFixture(BodyFixture.values()[fixtureSelectedIndex.get()]);
                    rb.physiosSystemSetBodyFixture(BodyFixture.values()[fixtureSelectedIndex.get()]);
                }

                int[] mass = {rb.getMass()};
                if (ImGui.dragInt("Mass", mass)) {
                    rb.setMass(mass[0]);
                }
                float[] bounciness = {rb.getBounce()};
                if (ImGui.dragFloat("Bounciness", bounciness)) {
                    rb.setBounce(bounciness[0]);
                }
                float[] friction = {rb.getFriction()};
                if (ImGui.dragFloat("Friction", friction)) {
                    rb.setFriction(friction[0]);
                }
            }
        }
    }

    private void customComponents(Component component, Iterator<Component> iterator) {
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
            for (Field field : component.getClass().getDeclaredFields()) {
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

                            Object stringPayload = ImGui.acceptDragDropPayload("String");
                            if (stringPayload instanceof String dropped) {
                                try (BufferedInputStream prefab = new BufferedInputStream(new FileInputStream(dropped))) {
                                    GameObject go = SerializationUtils.deserialize(IOUtils.toByteArray(prefab));
                                    field.set(component, go);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
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

    private void assetBrowser() throws IOException {
        ImGui.begin("AssetBrowser");
        float thumbnailSize = 64;
        float padding = 8;
        float cellSize = thumbnailSize + padding;

        float panelWidth = ImGui.getContentRegionAvailX();
        int columnCount = Math.max(1, (int) (panelWidth / cellSize));
        currentFolder = AssetManager.getINSTANCE().getFile().getPath();
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
                AssetManager.getINSTANCE().setFile(new File(currentFolder));
            }
        }

        assets = AssetManager.getINSTANCE().getFile().listFiles();

        ImGui.columns(columnCount, "assetGrid", false);
        // Allow only in asset browser tab
        if (ImGui.isMouseClicked(1) && ImGui.isWindowHovered(ImGuiHoveredFlags.AllowWhenBlockedByPopup)) {
            ImGui.openPopup("FileEditPopup");
        }

        if (ImGui.beginPopup("FileEditPopup")) {
            if (ImGui.beginMenu("Create")) {
                if (ImGui.menuItem("Folder")) {
                    System.out.println("Clicked folder");
                }
                if (currentFolder.contains("scripts")) {
                    if (ImGui.menuItem("Script")) {
                        System.out.println("Clicked script");
                        showNewScriptPopup = true;
                        scriptName.set("");
                    }
                }
                if (ImGui.menuItem("Scene")) {
                    showNewScenePopup = true;
                    sceneName.set("");
                }
                ImGui.endMenu();
            }
            ImGui.endPopup();
        }

        if (showNewScenePopup) {
            ImGui.openPopup("New Scene");
        }

        if (showNewScriptPopup) {
            ImGui.openPopup("New Script");
        }

        if (ImGui.beginPopupModal("New Scene")) {
            ImGui.inputTextWithHint("##sceneName", "Enter scene name...", sceneName);

            if (ImGui.button("Create")) {
                SaveManager.newScene(sceneName.get() + ".scene");
                showNewScenePopup = false;
                SceneManager.init();
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel")) {
                showNewScenePopup = false;
                ImGui.closeCurrentPopup();
            }
            ImGui.endPopup();
        }

        if (ImGui.beginPopupModal("New Script")) {
            ImGui.inputTextWithHint("##Script Name", "Enter script name...", scriptName);

            if (ImGui.button("Create")) {
                SaveManager.newScript(scriptName.get());
                initComponentList();
                showNewScriptPopup = false;
                SceneManager.init();
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel")) {
                showNewScriptPopup = false;
                ImGui.closeCurrentPopup();
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
            } else {
                ImGui.textWrapped(name);
            }

            if (clicked) {
                if (asset.isDirectory()) {
                    currentFolder += "/" + name;
                    AssetManager.getINSTANCE().setFile(new File(currentFolder));
                }
            }

            if (clicked) {
                if (fileType.equals(".scene") && !Engine.isPlaying()) {
                    SaveManager.loadScene(asset.getPath());
                    selected = null;
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
            } else {
                ImGui.text(line.getMessage());
            }
        }
        if (ImGui.button("Clear")) {
            LogCapture.clear();
        }
        ImGui.end();
    }

    private void renderSceneManager() {
        if (showSceneManager.get()) {
            ImGui.setNextWindowSize(280, 350, ImGuiCond.FirstUseEver);
            if (ImGui.begin("Scene Manager", showSceneManager)) {
                ImGui.beginChild("##scene_list", 0, 0, false);

                for (Entry<Integer, String> entry : SceneManager.getSceneList().entrySet()) {
                    int sceneIndex = entry.getKey();
                    String scenePath = entry.getValue();
                    ImGui.text(scenePath.substring(scenePath.lastIndexOf("/") + 1));
                    ImGui.sameLine();

                    float itemWidth = ImGui.calcTextSize(String.valueOf(sceneIndex)).x;
                    ImGui.setCursorPosX(ImGui.getContentRegionAvail().x - itemWidth + ImGui.getCursorPosX());
                    ImGui.text(String.valueOf(sceneIndex));
                }

                ImGui.endChild();
            }
            ImGui.end();
        }
    }

    void renderStatusBar() {
        float height = 22.0f;
        ImGui.setNextWindowPos(0, io.getDisplaySizeY() - height);
        ImGui.setNextWindowSize(io.getDisplaySizeX(), height);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 1f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f, 3f);

        ImGui.begin("##StatusBar", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoBringToFrontOnFocus);

        if (!Engine.isPlaying) {
            ImGui.textColored(0.3f, 0.7f, 1.0f, 1.0f, "EDIT MODE");
        } else {
            ImGui.textColored(0.8f, 0.8f, 0.3f, 1.0f, "PLAY MODE");
        }

        ImGui.sameLine();
        String sceneName = AssetManager.getINSTANCE().currentWorkingScene;
        sceneName = sceneName.substring(sceneName.lastIndexOf("/") + 1);
        ImGui.text(sceneName);

        ImGui.sameLine();
        if (compileSuccess) {
            ImGui.textColored(0.4f, 0.8f, 0.4f, 1.0f, "Ready");
        } else {
            ImGui.textColored(0.9f, 0.3f, 0.3f, 1.0f, "Failed");
        }

        ImGui.sameLine();
        String rightText = fps + " FPS | Errors: " + ScriptManager.getErrorCount();
        float windowWidth = ImGui.getWindowSizeX();
        float textWidth = ImGui.calcTextSize(rightText).x;

        ImGui.sameLine();
        ImGui.setCursorPosX(windowWidth - textWidth - 10);
        ImGui.text(rightText);

        ImGui.end();
        ImGui.popStyleVar(3);
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

    private int countDuplicates(String name) {
        int count = 0;
        for (GameObject go : ECSWorld.getGameObjects()) {
            int index = go.getName().lastIndexOf("(");
            String stripped = (index == -1 ? go.getName() : go.getName().substring(0, index - 1));
            if (go.getName().equals(name) || stripped.equals(name)) {
                count++;
            }
        }
        return count;
    }

    public static void setFps(int fps) {
        EditorLayer.fps = fps;
    }
}