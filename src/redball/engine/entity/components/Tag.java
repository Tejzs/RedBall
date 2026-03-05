package redball.engine.entity.components;

import java.io.Serial;

public class Tag extends Component {
    @Serial
    private static final long serialVersionUID = 1L;

    public String tag;

    public Tag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public void update(float dt) {

    }
}
