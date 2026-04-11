package redball.engine.save;

import org.apache.commons.io.IOUtils;
import org.joml.Vector3f;
import redball.engine.core.Engine;
import redball.engine.entity.components.*;
import redball.engine.scene.AssetManager;
import redball.engine.core.PhysicsSystem;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.renderer.RenderManager;
import redball.engine.renderer.texture.TextureManager;
import redball.engine.utils.PakWriter;
import redball.engine.utils.ScriptManager;

import java.io.*;
import java.util.ArrayList;
import java.util.IllegalFormatCodePointException;

public class SaveManager {
    public static void save() {
        try (BufferedOutputStream sceneOut = new BufferedOutputStream(new FileOutputStream((AssetManager.getINSTANCE().currentWorkingScene)))) {
            IOUtils.write(new SaveObject().toByteArray(), sceneOut);
        } catch (IOException e) {
            System.out.println("ERROR:" + e);
        }
    }

    public static void loadScene(String scene) throws IOException {
        SaveObject saveObject = null;
        BufferedInputStream sceneIn = null;
        if (Engine.isBuild) {
            saveObject = SaveObject.parseFrom(PakWriter.getAsset(scene));
        } else {
            sceneIn = new BufferedInputStream(new FileInputStream(scene));
            saveObject = SaveObject.parseFrom(IOUtils.toByteArray(sceneIn));
        }


        if (PhysicsSystem.getWorld() != null) {
            PhysicsSystem.clear();
        }
        ECSWorld.removeAll();
        RenderManager.clear();
        TextureManager.clear();

        PhysicsSystem.init();
        ECSWorld.setGameObjects(saveObject.getGameObjects());

        // reload all textures from file paths
        for (GameObject go : ECSWorld.getGameObjects()) {
            SpriteRenderer sr = go.getComponent(SpriteRenderer.class);
            Rigidbody rb = go.getComponent(Rigidbody.class);
            if (rb != null) {
                rb.createBody();
            }

            if (sr != null && sr.getFilePath() != null) {
                if (Engine.isBuild) {
                    sr.setTexture(TextureManager.getTexture(sr.getFilePath(), PakWriter.getAsset(sr.getFilePath())));
                } else {
                    sr.setTexture(TextureManager.getTexture(sr.getFilePath()));
                }
            }
        }

        RenderManager.prepare(ECSWorld.findGameObjectByTag("Camera"));
        AssetManager.getINSTANCE().currentWorkingScene = scene;
    }

    public static void newScene(String sceneName) {
        // create default camera
        GameObject camera = new GameObject("Camera");
        camera.addComponent(new Transform(new Vector3f(0, 0, 0), 0f, new Vector3f(1, 1, 1)));
        camera.addComponent(new CameraComponent(Engine.getWindowManager().getWidth(), Engine.getWindowManager().getHeight()));
        camera.addComponent(new Tag("Camera"));
        ArrayList<GameObject> gameObjects = new ArrayList<>();
        gameObjects.add(camera);
        try (BufferedOutputStream sceneOut = new BufferedOutputStream(new FileOutputStream((AssetManager.getINSTANCE().getScenesDirectory() + sceneName)))) {
            IOUtils.write(new SaveObject(gameObjects).toByteArray(), sceneOut);
        } catch (IOException e) {
            System.out.println("ERROR:" + e);
        }
    }

    public static void newScript(String scriptName) throws IOException {

        String template = "package car.assets.scripts;\n" +
                "\n" +
                "import redball.engine.entity.components.Component;\n" +
                "\n" +
                "import java.io.Serial;\n" +
                "\n" +
                "public class " + scriptName + " extends Component {\n" +
                "    @Serial\n" +
                "    private static final long serialVersionUID = 1L;\n" +
                "\n" +
                "    @Override\n" +
                "    public void start() {\n" +
                "        super.start();\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void update(float dt) {\n" +
                "\n" +
                "    }\n" +
                "}\n";

        FileOutputStream fileOutputStream = new FileOutputStream(AssetManager.getINSTANCE().getScriptDirectory() + File.separator + scriptName + ".java");
        fileOutputStream.write(template.getBytes());
        try {
            ScriptManager.compileAll(AssetManager.getINSTANCE().getScriptDirectory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
