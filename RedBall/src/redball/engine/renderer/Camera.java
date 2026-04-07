package redball.engine.renderer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.Serializable;

public class Camera implements Serializable {
    private Matrix4f projection, view;
    private Vector2f position;

    public Camera(Vector2f position) {
        this.position = position;
        this.projection = new Matrix4f();
        this.view = new Matrix4f();
    }

    public void adjustProjection(int width, int height) {
        projection.identity();
        projection.ortho(0.0f, width, 0.0f, height, 0.1f, 100.0f);
    }

    public Matrix4f getViewMat() {
        Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f eye = new Vector3f(position.x, position.y, 20.0f);
        Vector3f target = new Vector3f(position.x, position.y, 0.0f); // looking straight ahead

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
}
