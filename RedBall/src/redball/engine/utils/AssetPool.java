package redball.engine.utils;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class AssetPool {
    private static final String VERTEX_SHADER = "#version 400 core\n" + "layout (location = 0) in vec3 aPos;\n" + "layout (location = 1) in vec4 aColor;\n" + "layout (location = 2) in vec2 aTexCoord;\n" + "layout (location = 3) in float tId;\n" + "\n" + "out vec4 ourColor;\n" + "out vec2 TexCoord;\n" + "flat out int ourTId;\n" + "\n" + "uniform mat4 view;\n" + "uniform mat4 projection;\n" + "\n" + "void main()\n" + "{\n" + "    gl_Position = projection * view * vec4(aPos, 1.0);\n" + "    ourColor = aColor;\n" + "    TexCoord = aTexCoord;\n" + "    ourTId = int(tId);\n" + "}";
    private static final String FRAGMENT_SHADER = "#version 400 core\n" + "out vec4 FragColor;\n" + "\n" + "in vec4 ourColor;\n" + "in vec2 TexCoord;\n" + "flat in int ourTId;\n" + "\n" + "uniform sampler2D u_Textures[16];\n" + "\n" + "void main()\n" + "{\n" + "    if (ourTId == 0) {\n" + "        FragColor = ourColor;\n" + "    } else {\n" + "        FragColor = texture(u_Textures[ourTId - 1], TexCoord) * ourColor;\n" + "    }\n" + "}";

    public static String getFragmentShaderSource() {
        return FRAGMENT_SHADER;
    }

    public static String getVertexShaderSource() {
        return VERTEX_SHADER;
    }
}