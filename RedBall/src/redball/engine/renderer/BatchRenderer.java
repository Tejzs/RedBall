package redball.engine.renderer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import org.dyn4j.geometry.MassType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.BodyType;
import redball.engine.entity.components.Rigidbody;
import redball.engine.entity.components.SpriteRenderer;
import redball.engine.entity.components.Transform;
import redball.engine.renderer.texture.Texture;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static redball.engine.core.PhysicsSystem.PPM;

public class BatchRenderer {
    public static final int MAX_ENTITIES = 1000;
    private static final int POS_SIZE = 3;
    private static final int COLOR_SIZE = 4;
    private static final int TEXTURE_COORDS_SIZE = 2;
    private static final int TEXTURE_ID_SIZE = 1;
    private static final int OVERALL_SIZE = POS_SIZE + COLOR_SIZE + TEXTURE_COORDS_SIZE + TEXTURE_ID_SIZE;
    private static final int OVERALL_STRIDE = OVERALL_SIZE * Float.BYTES;

    public int entityCount = 0;
    private int verticesAdded = 0;
    private List<GameObject> entities;
    private float[] verticesData = new float[OVERALL_SIZE * MAX_ENTITIES * 4];
    private int[] vertexIndex = new int[MAX_ENTITIES * 6];
    private int hightest = 0;
    private int vao;
    int vbo;

    private int indexCount = 0;

    BatchRenderer(List<GameObject> go) {
        entities = new ArrayList<>();
        for (GameObject g : go) {
            if (g.getComponent(SpriteRenderer.class) != null) {
                entities.add(g);
            }
        }
        entityCount = entities.size();
    }

    public int updateAllVertices() {
        int offset = 0;
        int h = hightest;

        for (GameObject entity : entities) {
            int quadOffset = offset * 4 * OVERALL_SIZE;

            SpriteRenderer sr  = entity.getComponent(SpriteRenderer.class);
            if (sr == null) continue;
            Texture texture = sr.getTexture();

            int textureSlot = 0;
            if (texture != null) {
                textureSlot = texture.getUsedTexSlot();
            }
            updateComponentVertices(entity, quadOffset + 0 * OVERALL_SIZE, -0.5f, 0.5f, 0, 1, textureSlot);
            updateComponentVertices(entity, quadOffset + 1 * OVERALL_SIZE, -0.5f, -0.5f, 0, 0, textureSlot);
            updateComponentVertices(entity, quadOffset + 2 * OVERALL_SIZE, 0.5f, -0.5f, 1, 0, textureSlot);
            updateComponentVertices(entity, quadOffset + 3 * OVERALL_SIZE, 0.5f, 0.5f, 1, 1, textureSlot);
            for (int i : new int[]{0, 1, 2, 2, 3, 0}) {
                int eboVal = hightest + i;
                vertexIndex[verticesAdded++] = eboVal;
                h = Math.max(h, eboVal);
            }
            offset++;
            hightest = h + 1;
        }

        int vao = glGenVertexArrays();
        vbo = glGenBuffers();
        int EBO = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verticesData, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, vertexIndex, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, OVERALL_STRIDE, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 4, GL_FLOAT, false, OVERALL_STRIDE, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 2, GL_FLOAT, false, OVERALL_STRIDE, 7 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, 1, GL_FLOAT, false, OVERALL_STRIDE, 9 * Float.BYTES);
        glEnableVertexAttribArray(3);

        indexCount = verticesAdded;
        verticesAdded = 0;
        return vao;
    }

    private void rebuildVertices() {
        int offset = 0;

        for (GameObject entity : entities) {
            int quadOffset = offset * 4 * OVERALL_SIZE;
            Transform t = entity.getComponent(Transform.class);
            Rigidbody rb = entity.getComponent(Rigidbody.class);

            boolean needsRebuild = t.isDirty() || (rb != null && !rb.getBody().isAtRest());

            if (needsRebuild) {
                t.markAsClean();
                SpriteRenderer sr = entity.getComponent(SpriteRenderer.class);
                if (sr == null) { offset++; continue; }
                Texture texture = sr.getTexture();
                int textureSlot = texture != null ? texture.getUsedTexSlot() : 0;

                updateComponentVertices(entity, quadOffset + 0 * OVERALL_SIZE, -0.5f,  0.5f, 0, 1, textureSlot);
                updateComponentVertices(entity, quadOffset + 1 * OVERALL_SIZE, -0.5f, -0.5f, 0, 0, textureSlot);
                updateComponentVertices(entity, quadOffset + 2 * OVERALL_SIZE,  0.5f, -0.5f, 1, 0, textureSlot);
                updateComponentVertices(entity, quadOffset + 3 * OVERALL_SIZE,  0.5f,  0.5f, 1, 1, textureSlot);
            }

            offset++;
        }
    }

    public void updateVertices() {
        boolean anyDirty = false;

        for (GameObject entity : entities) {
            Rigidbody rb = entity.getComponent(Rigidbody.class);
            Transform t = entity.getComponent(Transform.class);

            // check if body is awake and simulating, not just its type
            if (rb != null && rb.getBody() != null) {
                if (!rb.getBody().isAtRest()) {
                    anyDirty = true;
                    break;
                }
            } else if (t.isDirty()) {
                    anyDirty = true;
                    break;
            }
        }

        if (!anyDirty) return;

        rebuildVertices();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, verticesData);
    }

    private void updateComponentVertices(GameObject go, int off, float x, float y, int tx, int ty, int tId) {
        Rigidbody rb = go.getComponent(Rigidbody.class);
        Transform transform = go.getComponent(Transform.class);
        Matrix4f matrix = new Matrix4f().translate(transform.position).rotateZ(transform.rotation).scale(transform.scale);
        Vector4f result = matrix.transform(new Vector4f(x, y, 0, 1));
        if (rb != null && rb.getBody() != null) {
            org.dyn4j.geometry.Transform physTransform = rb.getBody().getTransform();

            Vector3f pos = new Vector3f((float) physTransform.getTranslationX() * PPM, (float) physTransform.getTranslationY() * PPM, transform.position.z);
            float rotation = (float) physTransform.getRotationAngle();

            matrix = new Matrix4f().translate(pos).rotateZ(rotation).scale(transform.scale);
            result = matrix.transform(new Vector4f(x, y, 0, 1));
        }
        verticesData[off] = result.x;
        verticesData[off + 1] = result.y;
        verticesData[off + 2] = result.z;
        verticesData[off + 3] = 1;
        verticesData[off + 4] = 1;
        verticesData[off + 5] = 1;
        verticesData[off + 6] = 1;
        verticesData[off + 7] = tx;
        verticesData[off + 8] = ty;
        verticesData[off + 9] = tId;
    }

    public void bindTextures() {
        for (GameObject entity : entities) {
            SpriteRenderer sr = entity.getComponent(SpriteRenderer.class);
            if (sr != null) {
                if (sr.getTexture() != null) {
                    sr.getTexture().bindTexture();
                }
            }
        }
    }

    public void prepare() {
        vao = updateAllVertices();
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
    }
}
