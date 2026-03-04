package redball.engine.save;

import org.apache.commons.io.IOUtils;
import redball.engine.core.AssetManager;
import redball.engine.entity.ECSWorld;
import redball.engine.renderer.texture.TextureManager;

import java.io.*;

public class SaveManager {
    public static void save() {
        try (BufferedOutputStream sceneOut =
                     new BufferedOutputStream(new FileOutputStream((AssetManager.getScenesDirectory() + "Scene.scene")))) {
            IOUtils.write(new SaveObject().toByteArray(), sceneOut);
        } catch (IOException e) {
            System.out.println("ERROR:" + e);
        }
    }

    public static void loadScene() {
        try (BufferedInputStream sceneIn =
                     new BufferedInputStream(new FileInputStream(AssetManager.getScenesDirectory() + "Scene.scene"))) {
            SaveObject saveObject = SaveObject.parseFrom(IOUtils.toByteArray(sceneIn));
            ECSWorld.setGameObjects(saveObject.getGameObjects());
            TextureManager.reBindAllTextures(saveObject.getTextures());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
