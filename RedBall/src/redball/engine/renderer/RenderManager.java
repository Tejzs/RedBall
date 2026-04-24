package redball.engine.renderer;

import org.apache.commons.lang3.SerializationUtils;
import redball.engine.core.Engine;
import redball.engine.entity.ECSWorld;
import redball.engine.entity.GameObject;
import redball.engine.entity.components.CameraComponent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL30.*;
import static redball.engine.renderer.BatchRenderer.MAX_ENTITIES;


public class RenderManager {
    private static List<BatchRenderer> batches = new ArrayList<>();
    private static FrameBuffer frameBuffer;

    public static void prepare(GameObject camera) {
        for (BatchRenderer batch : batches) {
            batch.dispose();
        }
        batches.clear();
        frameBuffer = FrameBuffer.getINSTANCE();
        List<GameObject> gos = ECSWorld.getGameObjects();

        for (int i = 0; i < gos.size(); i += MAX_ENTITIES) {
            List<GameObject> chunk = gos.subList(i, Math.min(i + MAX_ENTITIES, gos.size()));
            BatchRenderer batch = new BatchRenderer(chunk);
            batch.prepare();
            batches.add(batch);
        }
    }

    public static void rebuild() {
        for (BatchRenderer batch : batches) {
            batch.dispose();
        }
        batches.clear();

        List<GameObject> gos = ECSWorld.getGameObjects();

        for (int i = 0; i < gos.size(); i += MAX_ENTITIES) {
            List<GameObject> chunk = gos.subList(i, Math.min(i + MAX_ENTITIES, gos.size()));
            BatchRenderer batch = new BatchRenderer(chunk);
            batch.prepare();
            batches.add(batch);
        }
    }

    public static void render(GameObject camera) {
        CameraComponent cameraComponent = camera.getComponent(CameraComponent.class);
        Engine.getShader().setMat4f("projection", cameraComponent.getProjectionMatrix());
        Engine.getShader().setMat4f("view", cameraComponent.getViewMatrix());

        frameBuffer.bind();

        // CLEAR
        glClearColor(cameraComponent.getCameraColor()[0], cameraComponent.getCameraColor()[1], cameraComponent.getCameraColor()[2], 1);
        glClear(GL_COLOR_BUFFER_BIT);

        for (BatchRenderer batchRenderer : batches) {
            batchRenderer.bindTextures();
            batchRenderer.updateVertices();
            batchRenderer.render();
        }
        frameBuffer.unbind();

        // Blit FBO to window
        glBindFramebuffer(GL_READ_FRAMEBUFFER, frameBuffer.getFboId());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(
                0, 0, frameBuffer.getWidth(), frameBuffer.getHeight(),
                0, 0, Engine.getWindowManager().getWidth(), Engine.getWindowManager().getHeight(),
                GL_COLOR_BUFFER_BIT, GL_NEAREST
        );
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public static FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }

    public static void clear() {
        batches.clear();
    }
}