package redball.engine.renderer.texture;

import java.util.*;

public class TextureManager {
    private static Map<String, Texture> textureMap = new HashMap<>();

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

    public static Collection<String> listBoundTextures() {
        return textureMap.keySet();
    }

    public static void reBindAllTextures(Collection<String> paths) {
        for (String path : paths) {
            getTexture(path);
        }
    }
}
