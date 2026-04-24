package redball.engine.renderer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import redball.engine.core.Engine;
import redball.engine.editor.EditorLayer;

import java.io.Serial;
import java.io.Serializable;

public class Camera implements Serializable {

    @Serial
    private static final long serialVersionUID = -6595029519555762330L;

    private Matrix4f projection, view;
    private Vector2f position;
    public Vector2f editorPosition;

    public Camera(Vector2f position) {
        this.position = position;
        this.editorPosition = position;
        this.projection = new Matrix4f();
        this.view = new Matrix4f();
    }

    public void adjustProjection(int width, int height) {
        projection.identity();
        if (!Engine.isPlaying()) {
            projection.ortho((float) -width / (2 * EditorLayer.zoom), (float) width / (2 * EditorLayer.zoom), (float) -height / (2 * EditorLayer.zoom), (float) height / (2 * EditorLayer.zoom), 0.1f, 100.0f);
        } else {
            projection.ortho((float) -width / 2, (float) width / 2, (float) -height / 2, (float) height / 2, 0.1f, 100.0f);
        }
    }

    public Matrix4f getViewMat() {
        Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f eye = null;
        Vector3f target = null;

        if (!Engine.isPlaying()) {
            eye = new Vector3f(editorPosition.x, editorPosition.y, 20.0f);
            target = new Vector3f(editorPosition.x, editorPosition.y, 0.0f);
        } else {
            eye = new Vector3f(position.x, position.y, 20.0f);
            target = new Vector3f(position.x, position.y, 0.0f);
        }

        view.identity();
        view = view.lookAt(eye, target, cameraUp);
        return view;
    }

    public Matrix4f getProjectionMat() {
        return projection;
    }

    public void setPosition(Vector2f position) {
        this.position = position;
    }

    public void setEditorPosition(Vector2f editorPosition) {
        this.editorPosition = editorPosition;
    }
}
