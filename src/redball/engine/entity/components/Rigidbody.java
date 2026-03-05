package redball.engine.entity.components;

import org.dyn4j.collision.Fixture;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.contact.ContactConstraint;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;
import redball.engine.core.PhysicsSystem;

import java.io.Serial;
import java.util.List;

import static redball.engine.core.PhysicsSystem.PPM;

public class Rigidbody extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    private transient Body body;

    private BodyType bodyType = BodyType.DYNAMIC;
    private BodyFixture bodyFixture = BodyFixture.RECTANGLE;

    private int mass = 1;
    private double bounciness = 0;
    private double friction = 0.5;

    public Rigidbody() {
        body = new Body();
        getBody().setUserData(this);
    }

    @Override
    public void start() {
    }

    @Override
    public void update(float dt) {
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public void physicsSystemSetBodyType(BodyType type) {
        getBody().setMass(type.getMassType());
    }

    public void physiosSystemSetBodyFixture(BodyFixture bodyFixture) {
        Transform transform = this.gameObject.getComponent(Transform.class);
        getBody().removeAllFixtures();
        getBody().addFixture(bodyFixture.getShape(transform.scale.x / PPM, transform.scale.y / PPM));
        getBody().updateMass();
    }

    public void physicsSystemSetMass(int mass) {
        getBody().setMass(MassType.NORMAL);
        Mass old = getBody().getMass();
        Mass m = new Mass(old.getCenter(), mass, old.getInertia());
        getBody().setMass(m);
    }

    public void physicsSystemSetBounce(double bounciness) {
        getBody().getFixture(0).setRestitution(bounciness);
    }

    public void physicsSystemSetFriction(double friction) {
        body.getFixture(0).setFriction(friction);
    }

    public BodyType getBodyType() {
        MassType massType = getBody().getMass().getType();
        for (BodyType type : BodyType.values()) {
            if (type.getMassType().equals(massType)) {
                return type;
            }
        }
        return BodyType.DYNAMIC;
    }

    public void setFixture(BodyFixture bodyFixture) {
        this.bodyFixture =  bodyFixture;
    }

    // detects overlaps but has no collision response
    public void setSensor(boolean set) {
        getBody().getFixture(0).setSensor(set);
    }

    public void setMass(int mass) {
        this.mass = mass;
    }

    public int getMass() {
        return mass;
    }

    public void setBounce(double value) {
        this.bounciness = value;
    }

    public float getBounce() {
        return (float) bounciness;
    }

    public float getFriction() {
        return (float) friction;
    }

    public void setFriction(double friction) {
        this.friction = friction;
    }

    public Rigidbody isColliding() {
        List<ContactConstraint<Body>> contacts = PhysicsSystem.getWorld().getContacts(getBody());
        for (ContactConstraint<Body> constraint : contacts) {
            Body collidedBody = constraint.getBody2();
            return (Rigidbody) collidedBody.getUserData();
        }
        return null;
    }

    public Body getBody() {
        return body;
    }

    public void createBody() {
        body = new Body();
        body.setUserData(this);
        Transform transform = this.gameObject.getComponent(Transform.class);

        getBody().getTransform().setTranslation(transform.position.x / PPM, transform.position.y / PPM);
        getBody().getTransform().setRotation(Math.toRadians(transform.rotation));

        physiosSystemSetBodyFixture(bodyFixture);
        physicsSystemSetMass(mass);
        physicsSystemSetBodyType(bodyType);
        physicsSystemSetBounce(bounciness);
        physicsSystemSetFriction(friction);
        PhysicsSystem.getWorld().addBody(body);
    }
}