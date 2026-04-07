package redball.engine.entity.components;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.Serial;

import static redball.engine.core.PhysicsSystem.PPM;

public class Transform extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public Vector3f position;
    public float rotation;
    public Vector3f scale;
    private Matrix4f matrix;

    public Transform(Vector3f position, float rotation, Vector3f scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
        this.matrix = new Matrix4f();
        super.markAsDirty();
    }

    public void setXPosition(float xPos) {
        Rigidbody rb = this.gameObject.getComponent(Rigidbody.class);
        if (rb != null) {
            rb.getBody().getTransform().setTranslationX(xPos / PPM);
        }
        this.position.x = xPos;
        super.markAsDirty();
    }

    public void setYPosition(float yPos) {
        Rigidbody rb = this.gameObject.getComponent(Rigidbody.class);
        if (rb != null) {
            rb.getBody().getTransform().setTranslationY(yPos / PPM);
        }
        this.position.y = yPos;
        super.markAsDirty();
    }

    public void setRotation(float rotation) {
        Rigidbody rb = this.gameObject.getComponent(Rigidbody.class);
        if (rb != null) {
            rb.getBody().getTransform().setRotation(rotation);
        }
        this.rotation = rotation;
        super.markAsDirty();
    }

    public void setXScale(float xPos) {
        Rigidbody rb = this.gameObject.getComponent(Rigidbody.class);
        if (rb != null) {
            rb.getBody().getFixture(0);
        }
        this.scale.x = xPos;
        super.markAsDirty();
    }

    public void setYScale(float yPos) {
        this.scale.y = yPos;
        super.markAsDirty();
    }

    public Matrix4f getMatrix() {
        matrix.identity().translate(position).rotateZ((float) Math.toRadians(rotation)).scale(scale);
        return matrix;
    }

    public float getXPosition() {
        Rigidbody rb = this.gameObject.getComponent(Rigidbody.class);
        if (rb != null) {
            return (float) rb.getBody().getTransform().getTranslationX() * PPM;
        }
        return position.x;
    }

    public float getYPosition() {
        Rigidbody rb = this.gameObject.getComponent(Rigidbody.class);
        if (rb != null) {
            return (float) rb.getBody().getTransform().getTranslationY() * PPM;
        }
        return position.y;
    }

    public float getRotation() {
        Rigidbody rb = this.gameObject.getComponent(Rigidbody.class);
        if (rb != null) {
            return (float) rb.getBody().getTransform().getRotationAngle() * PPM;
        }
        return rotation;
    }

    public float getScaleX() {
        return scale.x;
    }

    public float getScaleY() {
        return scale.y;
    }

    @Override
    public void update(float dt) {
    }
}