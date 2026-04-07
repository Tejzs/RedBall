package redball.engine.renderer.texture;

import redball.engine.renderer.RenderManager;

import java.util.*;

public class TextureManager {
    public static final TreeMap<String, Texture> textureMap = new TreeMap<>();

    private TextureManager() {}

    public static Texture getTexture(TextureMap texture) {
        return getTexture(texture.getFilePath());
    }

    public static Texture getTexture(String path) {
        if (textureMap.containsKey(path)) {
            return textureMap.get(path);
        } else {
            Texture tex = new Texture(path);
            textureMap.put(path, tex);
            return tex;
        }
    }

    public static Texture getTexture(String name, byte[] data) {
        if (textureMap.containsKey(name)) {
            return textureMap.get(name);
        } else {
            Texture tex = new Texture(name, data);
            textureMap.put(name, tex);
            return tex;
        }
    }

    public static Collection<String> listBoundTextures() {
        return textureMap.keySet();
    }

    public static void bindTextures() {
        for (Texture texture : textureMap.values()) {
            texture.bindTexture();
        }
    }

    public static void clear() {
        textureMap.clear();
        Texture.resetSlotCounter();
    }
}
