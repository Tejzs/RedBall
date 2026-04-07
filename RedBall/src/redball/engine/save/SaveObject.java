package redball.engine.save;

import org.apache.commons.lang3.SerializationUtils;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.renderer.texture.TextureManager;
import redball.engine.utils.ScriptManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SaveObject implements Serializable {
    private final ArrayList<GameObject> gameObjects;
    private List<String> textures;

    public SaveObject() {
        this.gameObjects = (ArrayList<GameObject>) ECSWorld.getGameObjects();
        this.textures = new ArrayList<>(TextureManager.listBoundTextures());
    }

    public SaveObject(ArrayList<GameObject> gameObjects) {
        this.gameObjects = gameObjects;
    }


    public static SaveObject parseFrom(byte[] bytes) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                ClassLoader loader = ScriptManager.getScriptClassLoader(desc.getName());
                try {
                    return Class.forName(desc.getName(), false, loader);
                } catch (ClassNotFoundException e) {
                    return super.resolveClass(desc);
                }
            }
        }) {
            return (SaveObject) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize scene: " + e.getMessage(), e);
        }
    }


    public byte[] toByteArray() {
        return SerializationUtils.serialize(this);
    }

    public List<GameObject> getGameObjects() {
        return gameObjects;
    }

    public Collection<String> getTextures() {
        return textures;
    }

    public void setTextures(List<String> textures) {
        this.textures = textures;
    }
}
