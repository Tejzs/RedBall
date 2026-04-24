package redball.engine.editor;

import imgui.*;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Mode;
import imgui.extension.imguizmo.flag.Operation;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import imgui.flag.ImGuiCol;
import org.apache.commons.io.IOUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.reflections.Reflections;
import redball.engine.core.Engine;
import redball.engine.core.PhysicsSystem;
import redball.engine.input.MouseInput;
import redball.engine.logger.LogCapture;
import redball.engine.logger.LogLine;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.*;
import redball.engine.entity.components.Component;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.texture.Texture;
import redball.engine.save.SaveManager;
import redball.engine.save.SaveObject;
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

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

public class EditorLayer {
    private static EditorLayer INSTANCE;

    private ImBoolean showHierarchyPanel = new ImBoolean(true);
    private ImBoolean showInspectorPanel = new ImBoolean(true);
    private ImBoolean showAssetsPanel = new ImBoolean(true);
    private ImBoolean showConsolePanel = new ImBoolean(true);

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private String selected = null;
    GameObject selectedGameObject = null;
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
    private ImBoolean showProjectSettings = new ImBoolean(false);
    private ImString projectNameBuffer = null;

    private static boolean compileSuccess = false;
    private static int fps = 0;
    boolean saveClicked = false;
    boolean buildClicked = false;
    boolean buildRes = false;

    public static float zoom = 1;
    private int currOpr = Operation.TRANSLATE;
    private boolean localSpace = false;
    private float[] objectMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];

    private static String[] componentList = null;
    private static Set<Class<? extends Component>> subclasses;

    double clickTime = 0.0;
    double waitSeconds = 2.0;

    private ImString nameBuffer = null;
    private String prevSelected = null;

    private Map<String, SaveObject> prefabPool = new HashMap<>();

    private ImString searchBuffer = null;
    ImInt fixtureSelectedIndex = new ImInt(0);
    String[] bodyFixtures = Arrays.stream(BodyFixture.values()).map(Enum::name).toArray(String[]::new);

    private GameObject camera;
    private boolean changed = false;

    public static void init(Long window) {
        INSTANCE = new EditorLayer(window);
    }

    public EditorLayer(Long window) {
        this.window = window;
        ImGui.createContext();
        imGuiGlfw.init(window, true);
        imGuiGl3.init("#version 150");

        io = ImGui.getIO();
        io.setIniFilename("editor.ini");
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
        short[] iconRanges = {(short) 0xe000, (short) 0xF8FF, 0};
        io.getFonts().addFontFromFileTTF("resources/Font Awesome 7 Free-Solid-900.otf", 30.0f, iconConfig, iconRanges);

        io.getFonts().build();

        ImGuiStyle style = ImGui.getStyle();
        style.setWindowMenuButtonPosition(ImGuiDir.None);

        // === Backgrounds — push darker ===
        style.setColor(ImGuiCol.WindowBg, 0.110f, 0.110f, 0.110f, 1.00f); // #1C1C1C — main panels
        style.setColor(ImGuiCol.ChildBg, 0.090f, 0.090f, 0.090f, 1.00f); // #171717 — nested areas
        style.setColor(ImGuiCol.PopupBg, 0.122f, 0.122f, 0.122f, 1.00f); // #1F1F1F — popups/dropdowns

        // === Borders ===
        style.setColor(ImGuiCol.Border, 0.137f, 0.137f, 0.137f, 1.00f); // #232323
        style.setColor(ImGuiCol.BorderShadow, 0.000f, 0.000f, 0.000f, 0.05f);

        // === Frames (inputs, search bar) ===
        style.setColor(ImGuiCol.FrameBg, 0.16f, 0.16f, 0.16f, 1.00f);
        style.setColor(ImGuiCol.FrameBgHovered, 0.19f, 0.19f, 0.19f, 1.00f);
        style.setColor(ImGuiCol.FrameBgActive, 0.16f, 0.16f, 0.16f, 1.00f);

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
        style.setColor(ImGuiCol.Button, 0.15f, 0.15f, 0.15f, 1.00f);
        style.setColor(ImGuiCol.ButtonHovered, 0.55f, 0.32f, 0.10f, 1.00f);
        style.setColor(ImGuiCol.ButtonActive, 0.95f, 0.58f, 0.20f, 1.00f);

        // === Headers (collapsing headers, hierarchy rows) — your orange accent ===
        style.setColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 0.6f);        // softer selected
        style.setColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.3f); // subtle hover
        style.setColor(ImGuiCol.HeaderActive, 0.75f, 0.45f, 0.15f, 0.8f);  // brighter on click

        // === Separators ===
        style.setColor(ImGuiCol.Separator, 0.25f, 0.25f, 0.25f, 1.00f);
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
        style.setFrameRounding(3.0f);
        style.setFrameBorderSize(1.0f);
        style.setWindowRounding(2.0f);
        style.setScrollbarRounding(2.0f);
        style.setGrabRounding(2.0f);
        style.setTabRounding(2.0f);
        style.setFramePadding(4.0f, 3.0f);
        style.setItemSpacing(4.0f, 4.0f);
    }

    public static void initComponentList() {
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

        float menuBarHeight = 5;
        ImGui.setNextWindowPos(0, menuBarHeight);
        ImGui.setNextWindowSize(ImGui.getIO().getDisplaySizeX(), ImGui.getIO().getDisplaySizeY());

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
        ImGuizmo.beginFrame();

        camera = ECSWorld.getCamera();

        // ShortCuts
        if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyReleased(ImGuiKey.P)) {
            if (!Engine.isPlaying()) {
                Engine.onPlay();
                saveClicked = false;
            } else {
                Engine.onStop();
                camera = ECSWorld.getCamera();
                saveClicked = false;
            }
        }

        if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyReleased(ImGuiKey.S)) {
            if (!Engine.isPlaying()) {
                SaveManager.save();
            }
            clickTime = glfwGetTime();
            saveClicked = !saveClicked;
        }

        if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyReleased(ImGuiKey.B)) {
            build();
        }

        if (ImGui.getIO().getKeyCtrl() && ImGui.isKeyReleased(ImGuiKey.N) && !ImGui.getIO().getKeyShift()) {
            showNewScenePopup = true;
            sceneName.set("");
        }

        if (ImGui.getIO().getKeyCtrl() && ImGui.getIO().getKeyShift() && ImGui.isKeyReleased(ImGuiKey.N)) {
            createGameObject();
        }

        renderStatusBar();
        createDockSpace();
        renderMenuBar();
        renderHierarchy();
        renderViewPort();
        renderInspector();
        assetBrowser();
        renderConsole();
        renderSceneManager();
        renderProjectSettings();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

    }

    private void renderInspector() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (showInspectorPanel.get()) {
            ImGui.begin("Inspector");
            selectedGameObject = ECSWorld.findGameObjectByName(selected);
            projectNameBuffer = new ImString(Engine.getProjectName(), 256);

            if (selectedGameObject != null) {
                changed = false;

                if (!selected.equals(prevSelected)) {
                    nameBuffer = new ImString(selectedGameObject.getName(), 256);
                    prevSelected = selected;
                }

                ImGui.spacing();
                ImGui.alignTextToFramePadding();
                ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
                ImGui.text("Name");
                ImGui.popStyleColor();
                ImGui.sameLine();
                ImGui.setNextItemWidth(-1);
                if (ImGui.inputText("##Name", nameBuffer)) {
                    if (!nameBuffer.get().isEmpty()) {
                        int count = ECSWorld.countDuplicates(nameBuffer.get());
                        if (count < 1) {
                            selectedGameObject.setName(nameBuffer.get());
                            selected = nameBuffer.get();
                            prevSelected = nameBuffer.get();
                        } else {
                            int suffix = count;
                            while (ECSWorld.countDuplicates(nameBuffer.get() + " (" + suffix + ")") > 0) suffix++;
                            selectedGameObject.setName(nameBuffer.get() + " (" + suffix + ")");
                            prevSelected = selectedGameObject.getName();
                            selected = selectedGameObject.getName();
                        }
                    }
                }

                ImGui.spacing();
                ImGui.separator();
                ImGui.spacing();

                tagComponent(selectedGameObject);
                transformComponent(selectedGameObject);

                if (selectedGameObject.getComponent(CameraComponent.class) != null) {
                    cameraComponent(selectedGameObject);
                }

                rigidBodyComponent(selectedGameObject);
                spriteRendererComponent(selectedGameObject);

                Iterator<Component> componentIterator = selectedGameObject.getComponents().listIterator();
                while (componentIterator.hasNext()) {
                    Component c = componentIterator.next();
                    if (!(c instanceof Rigidbody) && !(c instanceof Transform) && !(c instanceof Tag) && !(c instanceof SpriteRenderer) && c != null && !(c instanceof CameraComponent)) {
                        customComponents(c, componentIterator);
                    }
                }

                ImGui.spacing();
                ImGui.separator();
                ImGui.spacing();

                float btnWidth = 140f;
                ImGui.setCursorPosX((ImGui.getContentRegionAvailX() - btnWidth) / 2);
                ImGui.pushStyleColor(ImGuiCol.Button, 0.18f, 0.18f, 0.18f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.55f, 0.32f, 0.10f, 0.6f);
                ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.75f, 0.45f, 0.15f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.Border, 0.30f, 0.30f, 0.30f, 1.0f);
                ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 1.0f);
                ImGui.setNextItemWidth(btnWidth);
                addComponent(selectedGameObject);
                ImGui.popStyleVar();
                ImGui.popStyleColor(4);

                if (ImGui.beginDragDropTarget()) {
                    Object payload = ImGui.acceptDragDropPayload("String");
                    if (payload instanceof String dropped) {
                        selectedGameObject.addComponent(getComponent(dropped));
                    }
                    ImGui.endDragDropTarget();
                }
            }

            ImGui.end();
        }
    }

    private void renderHierarchy() throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (showHierarchyPanel.get()) {
            ImGui.begin("Hierarchy");

            ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.Border, 0.60f, 0.45f, 0.20f, 0.2f);
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
            if (searchBuffer == null) searchBuffer = new ImString(256);
            ImGui.inputTextWithHint("##Search", "Search...", searchBuffer);
            ImGui.popStyleColor();
            ImGui.popStyleVar();
            ImGui.spacing();

            if (ImGui.isMouseClicked(1) && ImGui.isWindowHovered(ImGuiHoveredFlags.AllowWhenBlockedByPopup)) {
                ImGui.openPopup("HierarchyEdit");
            }

            ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.13f, 0.13f, 0.13f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.5f);
            if (ImGui.beginPopup("HierarchyEdit")) {
                if (ImGui.menuItem("Create GameObject")) {
                    createGameObject();
                }
                ImGui.endPopup();
            }
            ImGui.popStyleColor(2);

            Iterator<GameObject> gameObjectIterator = ECSWorld.getGameObjects().listIterator();
            while (gameObjectIterator.hasNext()) {
                String search = searchBuffer.get().toLowerCase();
                GameObject go = gameObjectIterator.next();
                if (!search.isEmpty() && !go.getName().toLowerCase().contains(search)) continue;

                float itemHeight = 18;
                float x = ImGui.getCursorScreenPosX();
                float y = ImGui.getCursorScreenPosY();

                boolean isSelected = selected != null && selected.equals(go.getName());

                if (isSelected) {
                    ImGui.pushStyleColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 0.5f);
                    ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.65f, 0.38f, 0.12f, 0.6f);
                } else {
                    ImGui.pushStyleColor(ImGuiCol.Header, 0.20f, 0.20f, 0.20f, 0.3f);
                    ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.25f, 0.25f, 0.25f, 0.4f);
                }

                if (ImGui.selectable("##" + go.getName(), isSelected, ImGuiSelectableFlags.None, 0, itemHeight)) {
                    selected = go.getName();
                }
                ImGui.popStyleColor(2);

                float textY = y + (itemHeight - ImGui.getTextLineHeight()) / 2;
                int iconColor = isSelected ? ImGui.colorConvertFloat4ToU32(0.95f, 0.58f, 0.20f, 1.0f) : ImGui.colorConvertFloat4ToU32(0.65f, 0.65f, 0.65f, 1.0f);
                int nameColor = isSelected ? ImGui.colorConvertFloat4ToU32(1.00f, 1.00f, 1.00f, 1.0f) : ImGui.colorConvertFloat4ToU32(0.82f, 0.82f, 0.82f, 1.0f);

                ImGui.setWindowFontScale(0.5f);
                String icon = getIcon("gameobject");
                float iconWidth = ImGui.calcTextSizeX(icon) + 4;
                ImGui.getWindowDrawList().addText(x + 5, textY + ImGui.calcTextSizeY(icon) - 1, iconColor, icon);
                ImGui.setWindowFontScale(1.0f);
                ImGui.getWindowDrawList().addText(x + iconWidth + 5, textY, nameColor, go.getName());

                if (ImGui.isItemClicked(ImGuiMouseButton.Right)) {
                    selected = go.getName();
                    ImGui.openPopup("GameObjectContext##" + go.getName());
                }

                ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.13f, 0.13f, 0.13f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.5f);
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
                        try (BufferedOutputStream sceneOut = new BufferedOutputStream(new FileOutputStream(AssetManager.getINSTANCE().getPrefabDirectory() + go.getName() + ".prefab"))) {
                            ArrayList<GameObject> list = new ArrayList<>();
                            list.add(go);
                            IOUtils.write(new SaveObject(list).toByteArray(), sceneOut);
                        } catch (IOException e) {
                            System.out.println("ERROR:" + e);
                        }
                    }
                    ImGui.endPopup();
                }
                ImGui.popStyleColor(2);

                if (ImGui.beginDragDropSource()) {
                    ImGui.setDragDropPayload("GAME_OBJECT", go);
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.95f, 0.58f, 0.20f, 1.0f);
                    ImGui.text(go.getName());
                    ImGui.popStyleColor();
                    ImGui.endDragDropSource();
                }
                if (ImGui.beginDragDropTarget()) {
                    Object payload = ImGui.acceptDragDropPayload("String");
                    if (payload instanceof String dropped) {
                        go.addComponent(getComponent(dropped));
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
                    if (dropped.endsWith(".prefab") && !Engine.isPlaying()) {
                        if (prefabPool.containsKey(dropped)) {
                            ECSWorld.addPrefab(prefabPool.get(dropped).getGameObjects().getFirst());
                        } else {
                            try (BufferedInputStream prefab = new BufferedInputStream(new FileInputStream(dropped))) {
                                SaveObject saveObject = SaveObject.parseFrom(IOUtils.toByteArray(prefab));
                                prefabPool.put(dropped, saveObject);
                                ECSWorld.addPrefab(saveObject.getGameObjects().getFirst());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
                ImGui.endDragDropTarget();
            }

            ImGui.end();
        }
    }

    void renderViewPort() {
        ImGui.begin("Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        float buttonWidth = 60f;
        float spacing = 5f;
        int numButtons = 2;
        float totalWidth = numButtons * buttonWidth + (numButtons - 1) * spacing;

        ImGui.pushStyleColor(ImGuiCol.Border, 0.25f, 0.25f, 0.25f, 1.00f);

        ImGui.pushStyleColor(ImGuiCol.Text, currOpr == Operation.TRANSLATE ? 0.95f : 0.55f, currOpr == Operation.TRANSLATE ? 0.58f : 0.55f, currOpr == Operation.TRANSLATE ? 0.20f : 0.55f, 1.0f);
        if (ImGui.radioButton("Translate", currOpr == Operation.TRANSLATE)) currOpr = Operation.TRANSLATE;
        ImGui.popStyleColor();

        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, currOpr == Operation.ROTATE ? 0.95f : 0.55f, currOpr == Operation.ROTATE ? 0.58f : 0.55f, currOpr == Operation.ROTATE ? 0.20f : 0.55f, 1.0f);
        if (ImGui.radioButton("Rotate", currOpr == Operation.ROTATE)) currOpr = Operation.ROTATE;
        ImGui.popStyleColor();

        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, currOpr == Operation.SCALE ? 0.95f : 0.55f, currOpr == Operation.SCALE ? 0.58f : 0.55f, currOpr == Operation.SCALE ? 0.20f : 0.55f, 1.0f);
        if (ImGui.radioButton("Scale", currOpr == Operation.SCALE)) currOpr = Operation.SCALE;
        ImGui.popStyleColor();

        ImGui.sameLine();
        if (ImGui.checkbox("Space", localSpace)) localSpace = !localSpace;

        ImGui.sameLine();
        ImGui.setCursorPosX((ImGui.getWindowSize().x - totalWidth) / 2);

        ImGui.pushStyleColor(ImGuiCol.Button, 0.15f, 0.30f, 0.55f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.20f, 0.40f, 0.70f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.10f, 0.22f, 0.42f, 1.0f);
        if (ImGui.button("Save")) {
            if (!Engine.isPlaying()) SaveManager.save();
            clickTime = glfwGetTime();
            saveClicked = !saveClicked;
        }
        ImGui.popStyleColor(3);

        ImGui.sameLine();
        if (!Engine.isPlaying()) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0.20f, 0.50f, 0.25f, 1.0f);     // green play
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.28f, 0.65f, 0.33f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.15f, 0.40f, 0.20f, 1.0f);
            if (ImGui.button("Play")) {
                Engine.onPlay();
                ECSWorld.getCamera().getComponent(CameraComponent.class).camera.adjustProjection(Engine.getWindowManager().getWidth(), Engine.getWindowManager().getHeight());
                saveClicked = false;
            }
            ImGui.popStyleColor(3);
        } else {
            ImGui.pushStyleColor(ImGuiCol.Button, 0.60f, 0.18f, 0.18f, 1.0f);     // red stop
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.75f, 0.25f, 0.25f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.45f, 0.13f, 0.13f, 1.0f);
            if (ImGui.button("Stop")) {
                Engine.onStop();
                saveClicked = false;
            }
            ImGui.popStyleColor(3);
        }

        ImGui.popStyleColor();

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

        float availX = ImGui.getContentRegionAvailX();
        float offsetX = (availX - renderWidth) * 0.5f;
        if (offsetX > 0) ImGui.setCursorPosX(ImGui.getCursorPosX() + offsetX);

        CameraComponent cameraComponent = camera.getComponent(CameraComponent.class);
        ImVec2 cursorPos = ImGui.getCursorScreenPos();

        ImGui.image(RenderManager.getFrameBuffer().getTextureId(), new ImVec2(renderWidth, renderHeight), new ImVec2(0, 1), new ImVec2(1, 0));
        ImDrawList drawList = ImGui.getWindowDrawList();

        if (!Engine.isPlaying()) {
            ImVec2 screenPos = ImGui.getItemRectMin();
            float gridSize = 50f * getZoom();
            int cols = (int) (renderWidth / gridSize);
            int rows = (int) (renderHeight / gridSize);

            int gridColor = ImGui.colorConvertFloat4ToU32(0.3f, 0.3f, 0.3f, 0.4f);

            for (int i = 0; i <= cols; i++) {
                float x = screenPos.x + i * gridSize;
                drawList.addLine(x, screenPos.y, x, screenPos.y + renderHeight, gridColor, 2f);
            }
            for (int i = 0; i <= rows; i++) {
                float y = screenPos.y + i * gridSize;
                drawList.addLine(screenPos.x, y, screenPos.x + renderWidth, y, gridColor, 2f);
            }
        }

        if (ImGui.isItemHovered() && ImGui.isMouseDragging(ImGuiMouseButton.Middle) && !Engine.isPlaying()) {
            ImVec2 delta = new ImVec2();
            ImGui.getIO().getMouseDelta(delta);
            cameraComponent.getCamera().setEditorPosition(new Vector2f(cameraComponent.camera.editorPosition.x - delta.x, cameraComponent.camera.editorPosition.y + delta.y));
            ImGui.resetMouseDragDelta(ImGuiMouseButton.Middle);
        }

        float worldWidth = 2f / cameraComponent.camera.getProjectionMat().m00();
        float worldHeight = 2f / Math.abs(cameraComponent.camera.getProjectionMat().m11());

        float mouseX = ((MouseInput.getX() - cursorPos.x) - renderWidth / 2) * (worldWidth / renderWidth) + cameraComponent.camera.editorPosition.x;
        float mouseY = ((MouseInput.getY() - cursorPos.y) - renderHeight / 2) * (worldHeight / renderHeight) - cameraComponent.camera.editorPosition.y;

        if (!ImGuizmo.isUsing() && !ImGuizmo.isOver() && ImGui.isWindowHovered() && MouseInput.getX() - cursorPos.x > 0 && MouseInput.getX() - cursorPos.x < renderWidth && MouseInput.getY() - cursorPos.y > 0 && MouseInput.getY() - cursorPos.y < renderHeight) {
            if (ImGui.getIO().getMouseClicked(0)) {
                for (int i = ECSWorld.getGameObjects().size() - 1; i >= 0; i--) {
                    GameObject go = ECSWorld.getGameObjects().get(i);
                    if (go.getBounds().contains(mouseX, -mouseY, (float) Math.toDegrees(go.getComponent(Transform.class).rotation))) {
                        selected = go.getName();
                        selectedGameObject = go;
                        changed = true;
                        break;
                    } else {
                        selected = null;
                        selectedGameObject = null;
                    }
                }
            }
        }

        if (ImGui.isItemHovered() && !Engine.isPlaying()) {
            float scroll = ImGui.getIO().getMouseWheel();
            if (scroll != 0) setZoom(getZoom() * (float) Math.pow(0.9f, -scroll));
        }

        if (selectedGameObject != null) {
            ImGuizmo.setOrthographic(true);
            ImGuizmo.setDrawList();
            ImGuizmo.setRect(cursorPos.x, cursorPos.y, renderWidth, renderHeight);

            cameraComponent.getViewMatrix().get(viewMatrix);
            cameraComponent.getProjectionMatrix().get(projectionMatrix);

            Transform t = selectedGameObject.getComponent(Transform.class);
            t.getMatrix().get(objectMatrix);

            ImGuizmo.manipulate(viewMatrix, projectionMatrix, currOpr, localSpace ? Mode.LOCAL : Mode.WORLD, objectMatrix);

            if (ImGuizmo.isUsing()) {
                float[] translation = new float[3];
                float[] rotation = new float[3];
                float[] scaleArr = new float[3];

                ImGuizmo.decomposeMatrixToComponents(objectMatrix, translation, rotation, scaleArr);

                t.setXPosition(translation[0]);
                t.setYPosition(translation[1]);
                t.setRotation((float) Math.toRadians(rotation[2]));
                t.setXScale(scaleArr[0]);
                t.setYScale(scaleArr[1]);
            }
        }

        ImGui.end();
    }

    private void renderMenuBar() throws Exception {
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 10f, 6f);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 6f, 5f);

        if (ImGui.beginMainMenuBar()) {
            ImGui.pushStyleColor(ImGuiCol.Text, 0.91f, 0.38f, 0.17f, 1f);
            ImGui.text(" RB ");
            ImGui.popStyleColor();
            ImGui.separator();

            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("  New Scene", "Ctrl+N")) {
                    showNewScenePopup = true;
                    sceneName.set("");
                }
                ImGui.separator();
                if (ImGui.menuItem("  Save", "Ctrl+S")) {
                    if (!Engine.isPlaying()) {
                        SaveManager.save();
                    }
                    clickTime = glfwGetTime();
                    saveClicked = !saveClicked;
                }
                if (ImGui.menuItem("  Build", "Ctrl+B")) {
                    build();
                }
                ImGui.separator();
                if (ImGui.menuItem("  Exit", "Alt+F4")) {
                    glfwSetWindowShouldClose(window, true);
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("  Scene Manager")) {
                    showSceneManager.set(true);
                }
                if (ImGui.menuItem("  Project Settings")) {
                    showProjectSettings.set(true);
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("View")) {
                if (ImGui.menuItem("  Hierarchy", "", showHierarchyPanel.get())) {
                    showHierarchyPanel.set(!showHierarchyPanel.get());
                }

                if (ImGui.menuItem("  Inspector", "", showInspectorPanel.get())) {
                    showInspectorPanel.set(!showInspectorPanel.get());
                }

                if (ImGui.menuItem("  Assets", "", showAssetsPanel.get())) {
                    showAssetsPanel.set(!showAssetsPanel.get());
                }

                if (ImGui.menuItem("  Console", "", showConsolePanel.get())) {
                    showConsolePanel.set(!showConsolePanel.get());
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu("GameObject")) {

                if (ImGui.menuItem("  Empty Object", "Ctrl+Shift+N")) {
                    createGameObject();
                }
                if (ImGui.menuItem("  Add Tag")) {
                    if (selectedGameObject != null) {
                        selectedGameObject.addComponent(new Tag(""));
                    }
                }
                ImGui.separator();

                if (ImGui.beginMenu("  Components")) {
                    if (ImGui.menuItem("  Sprite")) {
                        if (selectedGameObject != null) {
                            selectedGameObject.addComponent(new SpriteRenderer(null));
                        }
                    }
                    if (ImGui.menuItem("  Rigidbody 2D")) {
                        if (selectedGameObject != null) {
                            selectedGameObject.addComponent(new Rigidbody());
                        }
                    }

                    ImGui.endMenu();
                }
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
        ImGui.popStyleVar(2);
    }

    private void build() {
        if (ScriptManager.getErrorCount() == 0 && !Engine.isPlaying()) {
            try {
                clickTime = glfwGetTime();
                buildClicked = true;
                PakWriter.writePak(AssetManager.getINSTANCE().getWorkingDirectory());
                buildRes = true;
            } catch (Exception e) {
                buildRes = false;
            }
        } else {
            clickTime = glfwGetTime();
            buildClicked = true;
            buildRes = false;
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
                Component instance = (Component) constructor.newInstance(params);
                return instance;
            }
        }
        return null;
    }

    private void addComponent(GameObject go) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        ImString searchBuffer = new ImString(256);

        float btnWidth = 140f;
        ImGui.setCursorPosX((ImGui.getContentRegionMaxX() - btnWidth) / 2);
        if (ImGui.button("Add Component", btnWidth, 0)) {
            ImGui.openPopup("##addComponent");
        }

        ImGui.setNextWindowSize(220, 280);
        ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.12f, 0.12f, 0.12f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Border, 0.25f, 0.25f, 0.25f, 1.00f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f, 8f);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 4f, 6f); // more vertical spacing between items

        if (ImGui.beginPopup("##addComponent")) {

            ImGui.pushStyleColor(ImGuiCol.FrameBg, 0.18f, 0.18f, 0.18f, 1.0f);
            ImGui.setNextItemWidth(-1);
            ImGui.inputTextWithHint("##search", "Search", searchBuffer);
            ImGui.popStyleColor();

            ImGui.spacing();
            ImGui.pushStyleColor(ImGuiCol.Separator, 0.25f, 0.25f, 0.25f, 1.0f);
            ImGui.separator();
            ImGui.popStyleColor();
            ImGui.spacing();

            for (int n = 0; n < componentList.length; n++) {
                String query = searchBuffer.get().toLowerCase();
                if (!query.isEmpty() && !componentList[n].toLowerCase().contains(query)) continue;

                boolean is_selected = (selectedIndex == n);

                // Icon
                int iconSize = 8;
                float cx = ImGui.getCursorScreenPosX();
                float cy = ImGui.getCursorScreenPosY();
                String icon = getIcon("gameobject");
                ImGui.getWindowDrawList().addText(ImGui.getFont(), iconSize, cx, cy + ImGui.calcTextSizeY(icon) / 2, ImGui.colorConvertFloat4ToU32(0.75f, 0.45f, 0.15f, 1.0f), icon);
                ImGui.dummy(iconSize + 4f, ImGui.getTextLineHeight());
                ImGui.sameLine(0, 8f);

                // Item
                ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.4f);
                ImGui.pushStyleColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 0.6f);
                if (ImGui.selectable(componentList[n], is_selected, ImGuiSelectableFlags.AllowDoubleClick)) {
                    if (ImGui.isMouseDoubleClicked(0)) {
                        selectedIndex = n;
                        Component c = go.addComponent(getComponent(n));
                        if (c instanceof SpriteRenderer) RenderManager.rebuild();
                        if (c instanceof Rigidbody) ((Rigidbody) c).createBody();
                    }
                }
                ImGui.popStyleColor(2);

                if (is_selected) ImGui.setItemDefaultFocus();
            }

            ImGui.endPopup();
        }

        ImGui.popStyleVar(2);
        ImGui.popStyleColor(2);
        selectedIndex = -1;
    }

    private void transformComponent(GameObject go) {
        ImGui.pushStyleColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.65f, 0.38f, 0.12f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0.75f, 0.45f, 0.15f, 1.0f);

        if (ImGui.collapsingHeader("Transform", ImGuiTreeNodeFlags.DefaultOpen)) {
            ImGui.popStyleColor(3);
            ImGui.spacing();

            float labelWidth = 60f;
            float fieldWidth = ImGui.getContentRegionAvailX() - labelWidth;
            Transform transform = go.getComponent(Transform.class);

            // Position
            float[] pos = {transform.getXPosition(), transform.getYPosition()};
            ImGui.alignTextToFramePadding();
            ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
            ImGui.text("Position");
            ImGui.popStyleColor();
            ImGui.sameLine(labelWidth);
            ImGui.setNextItemWidth(fieldWidth);
            if (ImGui.dragFloat2("##Position", pos) && !changed) {
                transform.setXPosition(pos[0]);
                transform.setYPosition(pos[1]);
            }

            // Rotation
            float[] rot = {(float) (Math.toDegrees(transform.getRotation()) / PhysicsSystem.PPM)};
            ImGui.alignTextToFramePadding();
            ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
            ImGui.text("Rotation");
            ImGui.popStyleColor();
            ImGui.sameLine(labelWidth);
            ImGui.setNextItemWidth(fieldWidth);
            if (ImGui.dragFloat("##Rotation", rot) && !changed) {
                transform.setRotation((float) Math.toRadians(rot[0] % 360));
            }

            // Scale
            float[] scale = {transform.getScaleX(), transform.getScaleY()};
            ImGui.alignTextToFramePadding();
            ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
            ImGui.text("Scale");
            ImGui.popStyleColor();
            ImGui.sameLine(labelWidth);
            ImGui.setNextItemWidth(fieldWidth);
            if (ImGui.dragFloat2("##Scale", scale) && !changed) {
                transform.setXScale(scale[0]);
                transform.setYScale(scale[1]);
            }

            ImGui.spacing();
        } else {
            ImGui.popStyleColor(3);
        }
    }

    private void tagComponent(GameObject go) {
        Tag tag = go.getComponent(Tag.class);
        if (tag != null) {
            float labelWidth = 60f;
            ImGui.alignTextToFramePadding();
            ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
            ImGui.text("Tag");
            ImGui.popStyleColor();
            ImGui.sameLine(labelWidth);
            ImGui.setNextItemWidth(-1);
            ImString val = new ImString(tag.getTag(), 256);
            if (ImGui.inputText("##Tag", val)) {
                if (!tag.getTag().equals("Camera")) {
                    tag.setTag(val.get());
                }
            }
        }
    }

    private void cameraComponent(GameObject go) {
        ImGui.pushStyleColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.65f, 0.38f, 0.12f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0.75f, 0.45f, 0.15f, 1.0f);

        if (ImGui.collapsingHeader("Camera Component", ImGuiTreeNodeFlags.DefaultOpen)) {
            ImGui.popStyleColor(3);
            ImGui.spacing();

            float labelWidth = 60f;
            CameraComponent cameraComponent = go.getComponent(CameraComponent.class);

            ImGui.alignTextToFramePadding();
            ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
            ImGui.text("IsMain");
            ImGui.popStyleColor();
            ImGui.sameLine(labelWidth);
            if (ImGui.checkbox("##IsMain", cameraComponent.isMain())) {
            }

            ImGui.alignTextToFramePadding();
            ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
            ImGui.text("BG Color");
            ImGui.popStyleColor();
            ImGui.sameLine(labelWidth);
            ImGui.setNextItemWidth(-1);
            ImGui.colorEdit3("##BackGround Color", cameraComponent.getCameraColor());

            ImGui.spacing();
        } else {
            ImGui.popStyleColor(3);
        }
    }

    private void spriteRendererComponent(GameObject go) {
        SpriteRenderer spriteRenderer = go.getComponent(SpriteRenderer.class);
        if (spriteRenderer != null) {
            ImGui.pushStyleColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.65f, 0.38f, 0.12f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0.75f, 0.45f, 0.15f, 1.0f);

            if (ImGui.collapsingHeader("Sprite Renderer", ImGuiTreeNodeFlags.DefaultOpen)) {
                ImGui.popStyleColor(3);
                ImGui.spacing();

                boolean rightClicked = ImGui.isItemHovered() && ImGui.isMouseClicked(1);
                if (rightClicked) ImGui.openPopup("RemoveSpriteRendererPopup");

                ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.13f, 0.13f, 0.13f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.5f);
                if (ImGui.beginPopup("RemoveSpriteRendererPopup")) {
                    if (ImGui.menuItem("Remove Component")) {
                        go.removeComponent(SpriteRenderer.class);
                        RenderManager.rebuild();
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endMenu();
                }
                ImGui.popStyleColor(2);

                float labelWidth = 60f;
                ImGui.alignTextToFramePadding();
                ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
                ImGui.text("Sprite");
                ImGui.popStyleColor();
                ImGui.sameLine(labelWidth);
                ImGui.setNextItemWidth(-1);
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

                ImGui.spacing();
            } else {
                ImGui.popStyleColor(3);
            }
            spriteName = new File(spriteRenderer.getFilePath()).getName();
        }
    }

    private void rigidBodyComponent(GameObject go) {
        Rigidbody rb = go.getComponent(Rigidbody.class);
        if (rb != null) {
            ImGui.pushStyleColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.65f, 0.38f, 0.12f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0.75f, 0.45f, 0.15f, 1.0f);

            if (ImGui.collapsingHeader("RigidBody", ImGuiTreeNodeFlags.DefaultOpen)) {
                ImGui.popStyleColor(3);
                ImGui.spacing();

                if (ImGui.beginDragDropSource()) {
                    ImGui.setDragDropPayload("GAME_OBJECT", go);
                    ImGui.text(go.getName());
                    ImGui.endDragDropSource();
                }

                boolean rightClicked = ImGui.isItemHovered() && ImGui.isMouseClicked(1);
                if (rightClicked) ImGui.openPopup("RemoveRigidBodyPopup");

                ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.13f, 0.13f, 0.13f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.5f);
                if (ImGui.beginPopup("RemoveRigidBodyPopup")) {
                    if (ImGui.menuItem("Remove Component")) {
                        go.removeComponent(rb.getClass());
                        RenderManager.rebuild();
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endMenu();
                }
                ImGui.popStyleColor(2);

                float labelWidth = 100f;
                float fieldWidth = ImGui.getContentRegionAvailX() - labelWidth;

                // isDynamic
                ImBoolean isDynamic = new ImBoolean(rb.getBodyType() == BodyType.DYNAMIC);
                ImGui.alignTextToFramePadding();
                ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
                ImGui.text("isDynamic");
                ImGui.popStyleColor();
                ImGui.sameLine(labelWidth);
                if (ImGui.checkbox("##isDynamic", isDynamic.get())) {
                    rb.setBodyType(isDynamic.get() ? BodyType.STATIC : BodyType.DYNAMIC);
                    rb.physicsSystemSetBodyType(isDynamic.get() ? BodyType.STATIC : BodyType.DYNAMIC);
                }

                // Collision Shape
                fixtureSelectedIndex.set(rb.getBodyFixture().ordinal());
                ImGui.alignTextToFramePadding();
                ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
                ImGui.text("Shape");
                ImGui.popStyleColor();
                ImGui.sameLine(labelWidth);
                ImGui.setNextItemWidth(fieldWidth);
                if (ImGui.combo("##Collision Shape", fixtureSelectedIndex, bodyFixtures)) {
                    rb.setFixture(BodyFixture.values()[fixtureSelectedIndex.get()]);
                    rb.physicsSystemSetBodyFixture(BodyFixture.values()[fixtureSelectedIndex.get()]);
                }

                // Mass
                int[] mass = {rb.getMass()};
                ImGui.alignTextToFramePadding();
                ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
                ImGui.text("Mass");
                ImGui.popStyleColor();
                ImGui.sameLine(labelWidth);
                ImGui.setNextItemWidth(fieldWidth);
                if (ImGui.dragInt("##Mass", mass)) rb.setMass(mass[0]);

                // Bounciness
                float[] bounciness = {rb.getBounce()};
                ImGui.alignTextToFramePadding();
                ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
                ImGui.text("Bounciness");
                ImGui.popStyleColor();
                ImGui.sameLine(labelWidth);
                ImGui.setNextItemWidth(fieldWidth);
                if (ImGui.dragFloat("##Bounciness", bounciness)) rb.setBounce(bounciness[0]);

                // Friction
                float[] friction = {rb.getFriction()};
                ImGui.alignTextToFramePadding();
                ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
                ImGui.text("Friction");
                ImGui.popStyleColor();
                ImGui.sameLine(labelWidth);
                ImGui.setNextItemWidth(fieldWidth);
                if (ImGui.dragFloat("##Friction", friction)) rb.setFriction(friction[0]);

                ImGui.spacing();
            } else {
                ImGui.popStyleColor(3);
            }
        }
    }

    private void customComponents(Component component, Iterator<Component> iterator) {
        Class<?> clazz = component.getClass();

        ImGui.pushStyleColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.65f, 0.38f, 0.12f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0.75f, 0.45f, 0.15f, 1.0f);

        if (ImGui.collapsingHeader(clazz.getSimpleName(), ImGuiTreeNodeFlags.DefaultOpen)) {
            ImGui.popStyleColor(3);
            ImGui.spacing();

            boolean rightClicked = ImGui.isItemHovered() && ImGui.isMouseClicked(1);
            if (rightClicked) ImGui.openPopup("Remove" + clazz.getSimpleName() + "Popup");

            ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.13f, 0.13f, 0.13f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.5f);
            if (ImGui.beginPopup("Remove" + clazz.getSimpleName() + "Popup")) {
                if (ImGui.menuItem("Remove Component")) {
                    iterator.remove();
                    ImGui.closeCurrentPopup();
                }
                ImGui.endMenu();
            }
            ImGui.popStyleColor(2);

            float labelWidth = 100f;
            float fieldWidth = ImGui.getContentRegionAvailX() - labelWidth;

            for (Field field : component.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(field.getModifiers())) continue;
                try {
                    String name = field.getName();
                    Object value = field.get(component);

                    ImGui.alignTextToFramePadding();
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f);
                    ImGui.text(name);
                    ImGui.popStyleColor();
                    ImGui.sameLine(labelWidth);
                    ImGui.setNextItemWidth(fieldWidth);

                    if (field.getType() == float.class) {
                        float[] val = {(float) value};
                        if (ImGui.dragFloat("##" + name, val)) field.set(component, val[0]);
                    } else if (field.getType() == int.class) {
                        int[] val = {(int) value};
                        if (ImGui.dragInt("##" + name, val)) field.set(component, val[0]);
                    } else if (field.getType() == boolean.class) {
                        boolean val = (boolean) value;
                        if (ImGui.checkbox("##" + name, val)) field.set(component, val);
                    } else if (field.getType() == String.class) {
                        ImString val = new ImString(value.toString());
                        if (ImGui.inputText("##" + name, val)) field.set(component, val.get());
                    } else if (Component.class.isAssignableFrom(field.getType())) {
                        Component ref = (Component) value;
                        String label = (ref != null) ? ref.gameObject.getName() + " (" + field.getType().getSimpleName() + ")" : "None (" + field.getType().getSimpleName() + ")";
                        ImGui.inputText("##" + name, new ImString(label), ImGuiInputTextFlags.ReadOnly);
                        if (ImGui.beginDragDropTarget()) {
                            Object payload = ImGui.acceptDragDropPayload("GAME_OBJECT");
                            if (payload instanceof GameObject dropped) {
                                Component comp = dropped.getComponent((Class<? extends Component>) field.getType());
                                if (comp != null) field.set(component, comp);
                            }
                            ImGui.endDragDropTarget();
                        }
                    } else if (field.getType() == GameObject.class) {
                        GameObject ref = (GameObject) value;
                        String label = (ref != null) ? ref.getName() + " (" + field.getType().getSimpleName() + ")" : "None (" + field.getType().getSimpleName() + ")";
                        ImGui.inputText("##" + name, new ImString(label), ImGuiInputTextFlags.ReadOnly);
                        if (ImGui.beginDragDropTarget()) {
                            Object payload = ImGui.acceptDragDropPayload("GAME_OBJECT");
                            if (payload instanceof GameObject dropped) field.set(component, dropped);
                            Object stringPayload = ImGui.acceptDragDropPayload("String");
                            if (stringPayload instanceof String dropped) {
                                try (BufferedInputStream prefab = new BufferedInputStream(new FileInputStream(dropped))) {
                                    SaveObject saveObject = SaveObject.parseFrom(IOUtils.toByteArray(prefab));
                                    field.set(component, saveObject.getGameObjects().getFirst());
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

            ImGui.spacing();
        } else {
            ImGui.popStyleColor(3);
        }
    }

    private void assetBrowser() throws IOException {
        if (showAssetsPanel.get()) {

            ImGui.begin("AssetBrowser");
            float thumbnailSize = 64;
            float padding = 8;
            float cellSize = thumbnailSize + padding;

            float panelWidth = ImGui.getContentRegionAvailX();
            int columnCount = Math.max(1, (int) (panelWidth / cellSize));
            currentFolder = AssetManager.getINSTANCE().getFile().getPath();
            breadCrumbs = currentFolder.split("/");

            ImGui.pushStyleColor(ImGuiCol.Border, 0.25f, 0.25f, 0.25f, 1.00f);
            for (int i = 0; i < breadCrumbs.length; i++) {
                if (i != 0) {
                    ImGui.sameLine();
                    ImGui.setWindowFontScale(0.3f);
                    float posX = ImGui.getCursorPosX();
                    float posY = ImGui.getCursorPosY();
                    ImGui.setCursorPos(posX, posY + 7);
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.4f, 0.4f, 0.4f, 1.0f);
                    ImGui.text(getIcon("chevron"));
                    ImGui.popStyleColor();
                    ImGui.setWindowFontScale(1.0f);
                    ImGui.sameLine();
                }

                boolean isLast = i == breadCrumbs.length - 1;
                if (isLast) ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.40f, 0.10f, 1.0f);
                if (ImGui.button(breadCrumbs[i])) {
                    String[] sub = Arrays.copyOfRange(breadCrumbs, 0, i + 1);
                    currentFolder = String.join("/", sub);
                    AssetManager.getINSTANCE().setFile(new File(currentFolder));
                }
                if (isLast) ImGui.popStyleColor();
            }
            ImGui.popStyleColor();

            assets = AssetManager.getINSTANCE().getFile().listFiles();
            if (assets != null) Arrays.sort(assets);

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            ImGui.columns(columnCount, "assetGrid", false);

            if (ImGui.isMouseClicked(1) && ImGui.isWindowHovered(ImGuiHoveredFlags.AllowWhenBlockedByPopup)) {
                ImGui.openPopup("FileEditPopup");
            }

            ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.13f, 0.13f, 0.13f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.5f);
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
            ImGui.popStyleColor(2);

            if (showNewScenePopup) ImGui.openPopup("New Scene");
            if (showNewScriptPopup) ImGui.openPopup("New Script");

            ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.13f, 0.13f, 0.13f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.Border, 0.25f, 0.25f, 0.25f, 1.0f);
            if (ImGui.beginPopupModal("New Scene")) {
                ImGui.spacing();
                ImGui.setNextItemWidth(-1);
                ImGui.inputTextWithHint("##sceneName", "Enter scene name...", sceneName);
                ImGui.spacing();
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
                ImGui.spacing();
                ImGui.endPopup();
            }

            if (ImGui.beginPopupModal("New Script")) {
                ImGui.spacing();
                ImGui.setNextItemWidth(-1);
                ImGui.inputTextWithHint("##Script Name", "Enter script name...", scriptName);
                ImGui.spacing();
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
                ImGui.spacing();
                ImGui.endPopup();
            }
            ImGui.popStyleColor(2);

            for (File asset : assets) {
                String name = asset.getName();
                String fileType = asset.isDirectory() ? "FOLDER" : asset.getName().substring(asset.getName().lastIndexOf("."));

                ImGui.pushID(asset.getName());

                float posX = ImGui.getCursorPosX();
                float posY = ImGui.getCursorPosY();

                if (asset.isDirectory()) {
                    ImGui.pushStyleColor(ImGuiCol.Header, 0.55f, 0.32f, 0.10f, 0.15f);
                    ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.30f);
                    ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0.75f, 0.45f, 0.15f, 0.50f);
                } else {
                    ImGui.pushStyleColor(ImGuiCol.Header, 0.20f, 0.20f, 0.20f, 0.15f);
                    ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.30f, 0.30f, 0.30f, 0.30f);
                    ImGui.pushStyleColor(ImGuiCol.HeaderActive, 0.40f, 0.40f, 0.40f, 0.50f);
                }

                ImGui.selectable("##" + name, false, 0, thumbnailSize, thumbnailSize + 10);
                ImGui.popStyleColor(3);

                boolean clicked = ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0);
                boolean rightClicked = ImGui.isItemHovered() && ImGui.isMouseClicked(1);

                if (ImGui.beginDragDropSource()) {
                    if (fileType.equals(".java")) {
                        ImGui.setDragDropPayload("String", name.substring(0, asset.getName().lastIndexOf(".")));
                    } else {
                        ImGui.setDragDropPayload("String", asset.getPath());
                    }
                    ImGui.setWindowFontScale(0.3f);
                    ImGui.text(asset.isDirectory() ? getIcon("folder") : getIcon("file"));
                    ImGui.setWindowFontScale(1.0f);
                    ImGui.text(asset.getName());
                    ImGui.endDragDropSource();
                }

                ImGui.setCursorPos(posX + (thumbnailSize / 2) - thumbnailSize / 4, posY + thumbnailSize / 2);
                ImGui.setWindowFontScale(1.0f);

                if (asset.isDirectory()) {
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.45f, 0.15f, 1.0f); // orange folders
                    ImGui.text(getIcon("folder"));
                    ImGui.popStyleColor();
                } else if (asset.getName().endsWith(".prefab")) {
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.40f, 0.70f, 0.90f, 1.0f); // blue prefabs
                    ImGui.text(getIcon("gameobject"));
                    ImGui.popStyleColor();
                } else if (asset.getName().endsWith(".scene")) {
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.50f, 0.85f, 0.55f, 1.0f); // green scenes
                    ImGui.text(getIcon("scene"));
                    ImGui.popStyleColor();
                } else if (asset.getName().endsWith(".png") || asset.getName().endsWith(".jpeg") || asset.getName().endsWith(".jpg")) {
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.85f, 0.70f, 0.30f, 1.0f); // yellow images
                    ImGui.text(getIcon("fileImg"));
                    ImGui.popStyleColor();
                } else {
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.60f, 0.60f, 0.60f, 1.0f); // grey other
                    ImGui.text(getIcon("file"));
                    ImGui.popStyleColor();
                }

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
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.75f, 0.75f, 0.75f, 1.0f);
                    ImGui.textWrapped(name);
                    ImGui.popStyleColor();
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

                if (rightClicked) ImGui.openPopup("FileSelectPopup");

                ImGui.pushStyleColor(ImGuiCol.PopupBg, 0.13f, 0.13f, 0.13f, 1.0f);
                ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0.55f, 0.32f, 0.10f, 0.5f);
                if (ImGui.beginPopup("FileSelectPopup")) {
                    if (ImGui.menuItem("Delete")) {
                        asset.delete();
                        ImGui.closeCurrentPopup();
                    }
                    if (ImGui.menuItem("Rename")) {
                        renamingFile = asset;
                        renameBuffer.set(name);
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endMenu();
                }
                ImGui.popStyleColor(2);

                ImGui.popID();
                ImGui.nextColumn();
            }

            ImGui.columns(1);
            ImGui.end();
        }
    }

    private void renderConsole() {
        if (showConsolePanel.get()) {
            ImGui.begin("Console");
            ArrayDeque<LogLine> lines = LogCapture.getLogs();

            if (ImGui.button("Clear")) {
                if (ScriptManager.getErrorCount() == 0) {
                    LogCapture.clear();
                }
            }

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            ImGui.beginChild("##consoleLogs", 0, -ImGui.getFrameHeightWithSpacing(), false);

            float width = ImGui.getContentRegionAvailX();
            var dl = ImGui.getWindowDrawList();
            float rowH = 40.0f;
            float accentW = 3.0f;
            float padX = 8.0f;

            final int COL_BG_EVEN = ImGui.colorConvertFloat4ToU32(0.11f, 0.11f, 0.11f, 1.0f);
            final int COL_BG_ODD = ImGui.colorConvertFloat4ToU32(0.13f, 0.13f, 0.13f, 1.0f);
            final int COL_ACCENT_ERR = ImGui.colorConvertFloat4ToU32(0.85f, 0.25f, 0.25f, 1.0f);
            final int COL_ACCENT_INF = ImGui.colorConvertFloat4ToU32(0.55f, 0.32f, 0.10f, 1.0f);
            final int COL_ICON_ERR = ImGui.colorConvertFloat4ToU32(0.85f, 0.25f, 0.25f, 1.0f);
            final int COL_ICON_INF = ImGui.colorConvertFloat4ToU32(0.75f, 0.45f, 0.15f, 1.0f);
            final int COL_TEXT = ImGui.colorConvertFloat4ToU32(0.82f, 0.82f, 0.82f, 1.0f);
            final int COL_TEXT_ERR = ImGui.colorConvertFloat4ToU32(0.90f, 0.45f, 0.45f, 1.0f);

            int i = 0;
            for (LogLine line : lines) {
                boolean isError = line.isError();

                int colBg = (i % 2 == 0) ? COL_BG_EVEN : COL_BG_ODD;
                int colAccent = isError ? COL_ACCENT_ERR : COL_ACCENT_INF;
                int colIcon = isError ? COL_ICON_ERR : COL_ICON_INF;
                int colText = isError ? COL_TEXT_ERR : COL_TEXT;
                String icon = isError ? getIcon("error") : getIcon("info");

                ImVec2 cursor = ImGui.getCursorScreenPos();

                dl.addRectFilled(cursor.x, cursor.y, cursor.x + width, cursor.y + rowH, colBg);
                dl.addRectFilled(cursor.x, cursor.y, cursor.x + accentW, cursor.y + rowH, colAccent);

                int fontSize = 10;
                float iconX = cursor.x + accentW + padX;
                float iconY = cursor.y + (rowH / 2f) - ((fontSize - padX) / 2f);
                dl.addText(ImGui.getFont(), fontSize, iconX, iconY, colIcon, icon);

                float textX = iconX + fontSize + 25;
                float textY = cursor.y + (rowH / 2f) - ((fontSize + 5) / 2f);
                dl.addText(ImGui.getFont(), fontSize + 5, textX, textY, colText, line.getMessage());

                ImGui.dummy(width, rowH);
                i++;
            }

            ImGui.endChild();
            ImGui.end();
        }
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

    private void renderProjectSettings() {
        if (showProjectSettings.get()) {
            ImGui.setNextWindowSize(280, 350, ImGuiCond.FirstUseEver);
            if (ImGui.begin("Project Settings", showProjectSettings)) {
                ImGui.text("Project Name");
                ImGui.sameLine();
                if (ImGui.inputText("##Project Name", projectNameBuffer)) {
                    Engine.setProjectName(projectNameBuffer.get());
                }
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

        ImVec4 editColor = new ImVec4(0.3f, 0.7f, 1.0f, 1.0f);
        ImVec4 playColor = new ImVec4(1.0f, 0.722f, 0.424f, 1.0f);
        ImVec4 errorColor = new ImVec4(1.0f, 0.361f, 0.361f, 1.0f);
        ImVec4 secondaryColor = new ImVec4(0.604f, 0.604f, 0.604f, 1.0f);
        ImVec4 primaryColor = new ImVec4(0.7f, 0.7f, 0.7f, 1.0f);
        ImVec4 successColor = new ImVec4(0.4f, 0.85f, 0.5f, 1.0f);


        ImGui.begin("##StatusBar", ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoDocking | ImGuiWindowFlags.NoBringToFrontOnFocus);

        String sceneName = AssetManager.getINSTANCE().currentWorkingScene;
        sceneName = sceneName.substring(sceneName.lastIndexOf("/") + 1);
        double currentTime;

        if (!Engine.isPlaying()) {
            if (saveClicked) {
                ImGui.textColored(editColor, "Edit Mode");
                ImGui.sameLine();
                if (buildClicked) {
                    if (buildRes) {
                        ImGui.textColored(secondaryColor, " | Saved " + sceneName + " | ");
                        ImGui.sameLine();
                        ImGui.textColored(successColor, "Build Successful");
                    } else {
                        ImGui.textColored(secondaryColor, " | Saved " + sceneName + " | ");
                        ImGui.sameLine();
                        ImGui.textColored(errorColor, "Build Failed");
                    }
                    currentTime = glfwGetTime();
                    if (currentTime - clickTime >= waitSeconds) {
                        buildClicked = false;
                        buildRes = false;
                    }
                } else {
                    ImGui.textColored(secondaryColor, " | Saved " + sceneName);
                }
                currentTime = glfwGetTime();
                if (currentTime - clickTime >= waitSeconds) {
                    saveClicked = false;
                }
            } else {
                ImGui.textColored(editColor, "Edit Mode");
                ImGui.sameLine();
                if (buildClicked) {
                    if (buildRes) {
                        ImGui.textColored(secondaryColor, " | " + sceneName + " | ");
                        ImGui.sameLine();
                        ImGui.textColored(successColor, "Build Successful");
                    } else {
                        ImGui.textColored(secondaryColor, " | " + sceneName + " | ");
                        ImGui.sameLine();
                        ImGui.textColored(errorColor, "Build Failed");
                    }
                    currentTime = glfwGetTime();
                    if (currentTime - clickTime >= waitSeconds) {
                        buildClicked = false;
                        buildRes = false;
                    }
                } else {
                    ImGui.textColored(secondaryColor, " | " + sceneName);
                }
            }
        } else {
            ImGui.textColored(playColor, "Play Mode");
            ImGui.sameLine();
            ImGui.textColored(secondaryColor, " | " + sceneName + " | ");
            ImGui.sameLine();
            ImGui.textColored(0.5f, 0.5f, 0.5f, 1.0f, "Saving Disabled");
        }

        float windowWidth = ImGui.getWindowSizeX();

        String fpsString = fps + " FPS | ";
        String errIcons = getIcon("error");
        String errString = " Errors: " + ScriptManager.getErrorCount();


        ImGui.sameLine();
        ImGui.setCursorPosX(windowWidth - ImGui.calcTextSize(fpsString).x - ImGui.calcTextSize(errIcons).x - ImGui.calcTextSize(errString).x + 3);
        ImGui.textColored(secondaryColor, fpsString);

        ImGui.sameLine();
        ImGui.setCursorPosX(windowWidth - ImGui.calcTextSize(errIcons).x - ImGui.calcTextSize(errString).x + 5);
        ImGui.setCursorPosY((height) / 2);
        ImGui.setWindowFontScale(0.45f);
        if (ScriptManager.getErrorCount() > 0) {
            ImGui.textColored(errorColor, errIcons);
        } else {
            ImGui.textColored(primaryColor, errIcons);
        }
        ImGui.setWindowFontScale(1.0f);

        ImGui.sameLine();
        ImGui.setCursorPosX(windowWidth - ImGui.calcTextSize(errString).x - 10);
        if (ScriptManager.getErrorCount() > 0) {
            ImGui.textColored(errorColor, errString);
        } else {
            ImGui.textColored(primaryColor, errString);
        }

        ImGui.end();
        ImGui.popStyleVar(3);
    }

    public String getIcon(String icon) {
        return switch (icon) {
            case "folder" -> "\uF07B";
            case "file" -> "\uF1C9";
            case "chevron" -> "\uF054";
            case "error" -> "\uf057";
            case "info" -> "\uf05a";
            case "gameobject" -> "\uf1b2";
            case "scene" -> "\ue209";
            case "fileImg" -> "\uf1c5";
            default -> "?";
        };
    }

    public void dispose() {
        imGuiGl3.destroyDeviceObjects();
        ImGui.destroyContext();
    }

    public ImGuiImplGlfw getImGuiGlfw() {
        return imGuiGlfw;
    }

    public static void setFps(int fps) {
        EditorLayer.fps = fps;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
        ECSWorld.getCamera().getComponent(CameraComponent.class).camera.adjustProjection(Engine.getWindowManager().getWidth(), Engine.getWindowManager().getHeight());
    }

    public void resetZoom() {
        setZoom(1);
    }

    private void createGameObject() {
        int count = ECSWorld.countDuplicates("GameObject");
        String name;
        if (count < 1) {
            name = "GameObject";
        } else {
            int suffix = count;
            while (ECSWorld.countDuplicates("GameObject" + " (" + suffix + ")") > 0) {
                suffix++;
            }
            name = "GameObject" + " (" + suffix + ")";
        }
        GameObject g = ECSWorld.createGameObject(name);
        g.addComponent(new Transform(new Vector3f(0.0f, 0.0f, 0.0f), 0.0f, new Vector3f(250.0f)));
    }
}