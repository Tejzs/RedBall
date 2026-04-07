package redball.engine.entity.components;

import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;

import java.io.Serializable;

public enum BodyFixture implements Serializable {
    CIRCLE,
    RECTANGLE;

    public Convex getShape(double... params) {
        return switch (this) {
            case CIRCLE -> Geometry.createCircle(params[0] / 2);
            case RECTANGLE -> Geometry.createRectangle(params[0], params[1]);
        };
    }
}