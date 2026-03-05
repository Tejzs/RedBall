package redball.engine.entity.components;

import org.dyn4j.geometry.MassType;

import java.io.Serializable;

public enum BodyType implements Serializable {
    STATIC(MassType.INFINITE),
    DYNAMIC(MassType.NORMAL),
    KINEMATIC(MassType.FIXED_LINEAR_VELOCITY);

    private final MassType massType;

    BodyType(MassType massType) {
        this.massType = massType;
    }

    public MassType getMassType() { return massType; }
}
