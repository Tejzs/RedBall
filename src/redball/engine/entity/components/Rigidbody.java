package redball.engine.entity.components;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.contact.ContactConstraint;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import redball.engine.core.PhysicsSystem;

import java.util.List;

import static redball.engine.core.PhysicsSystem.PPM;

public class Rigidbody extends Component {
    private transient Body body = new Body();
    public int mass = 0;

    public Rigidbody() {
        this.body.setUserData(this);
    }

    @Override
    public void start() {
        Transform transform = this.gameObject.getComponent(Transform.class);

        body.getTransform().setTranslation(transform.position.x / PPM, transform.position.y / PPM);
        body.getTransform().setRotation(Math.toRadians(transform.rotation));
        setRectangleFixture();
        setBodyType(BodyType.DYNAMIC);
        PhysicsSystem.getWorld().addBody(body);
        super.markAsDirty();
    }

    @Override
    public void update(float dt) {
    }

    public void setBodyType(BodyType type) {
        this.body.setMass(type.getMassType());
    }

    public BodyType getBodyType() {
        MassType massType = body.getMass().getType();
        for (BodyType type : BodyType.values()) {
            if (type.getMassType().equals(massType)) {
                return type;
            }
        }
        return BodyType.DYNAMIC;
    }

    public void setCircleFixture() {
        Transform transform = this.gameObject.getComponent(Transform.class);
        body.removeAllFixtures();
        body.addFixture(Geometry.createCircle((transform.scale.x / PPM) / 2));
        body.updateMass();
    }

    public void setRectangleFixture() {
        Transform transform = this.gameObject.getComponent(Transform.class);
        body.removeAllFixtures();
        body.addFixture(Geometry.createRectangle(transform.scale.x / PPM, transform.scale.y / PPM));
        body.updateMass();
    }

    // detects overlaps but has no collision response
    public void setSensor(boolean set) {
        body.getFixture(0).setSensor(set);
    }

    public void setMass(int mass) {
        Mass m = new Mass(new Vector2(0, 0), mass, 1.0);
        body.setMass(m);
    }

    public int getMass() {
        return (int) body.getMass().getMass();
    }

    public void setBounce(double value) {
        body.getFixture(0).setRestitution(value);
    }

    public float getBounce() {
        return (float) body.getFixture(0).getRestitution();
    }

    public float getFriction() {
        return (float) body.getFixture(0).getFriction();
    }

    public void setFriction(double value) {
        body.getFixture(0).setFriction(value);
    }

    public Rigidbody isColliding() {
        List<ContactConstraint<Body>> contacts = PhysicsSystem.getWorld().getContacts(this.body);
        for (ContactConstraint<Body> constraint : contacts) {
            Body collidedBody = constraint.getBody2();
            return (Rigidbody) collidedBody.getUserData();
        }
        return null;
    }

    public Body getBody() {
        return body;
    }
}