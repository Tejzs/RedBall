package redball.engine.renderer.texture;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.stb.STBImage;
import redball.engine.core.Engine;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.glActiveTexture;

public class Texture implements Serializable {
    private static int usedTexSlots = 1;
    private static int maxSlots = GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS);
    private static int[] texSlots = new int[maxSlots];

    private int texSlot;
    private int usedTexSlot;
    private int texId;
    private int width;
    private int height;
    private String filePath;

    public static void init() {
        maxSlots = Math.min(maxSlots, 16);

        texSlots = new int[maxSlots];

        for (int i = 0; i < maxSlots; i++) {
            texSlots[i] = GL13.GL_TEXTURE0 + i;
        }
    }

    public Texture(String filePath) {
        this.filePath = filePath;
        texId = GL11.glGenTextures();
        usedTexSlot = usedTexSlots;
        texSlot = texSlots[usedTexSlots - 1];
        usedTexSlots++;
        GL13.glActiveTexture(texSlot);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        STBImage.stbi_set_flip_vertically_on_load(true);
        ByteBuffer textureImg;

        if (Engine.isBuild) {
            try (FileInputStream fis = new FileInputStream(filePath)) {
                byte[] data = fis.readAllBytes();

                ByteBuffer imageBuffer = ByteBuffer.allocateDirect(data.length);
                imageBuffer.put(data);
                imageBuffer.flip();

                textureImg = STBImage.stbi_load_from_memory(imageBuffer, width, height, channels, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            textureImg = STBImage.stbi_load(filePath, width, height, channels, 0);
        }

        if (textureImg != null) {
            this.width = width.get(0);
            this.height = height.get(0);
            int format = channels.get(0) == 4 ? GL11.GL_RGBA : GL11.GL_RGB;
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format, this.width, this.height, 0, format, GL11.GL_UNSIGNED_BYTE, textureImg);
            STBImage.stbi_image_free(textureImg);
        } else {
            System.err.println("Failed to loadScene texture: " + filePath);
            System.err.println(STBImage.stbi_failure_reason());
        }
    }

    public void bindTexture() {
        glActiveTexture(texSlot);
        glBindTexture(GL_TEXTURE_2D, texId);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTexID() {
        return texId;
    }

    public int getTexSlot() {
        return texSlot;
    }

    public int getUsedTexSlot() {
        return usedTexSlot;
    }

    public String getFilePath() {
        return filePath;
    }

    public static void resetSlotCounter() {
        usedTexSlots = 1;
    }
}
