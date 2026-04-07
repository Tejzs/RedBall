package redball.engine.renderer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glCreateProgram;

public class Shader {
    private final int ID;

    public Shader(String vertexShaderSource, String fragmentShaderSource) {
        int vertex, fragment;
        IntBuffer successBuffer = BufferUtils.createIntBuffer(1);

        vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex, vertexShaderSource);
        glCompileShader(vertex);

        fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment, fragmentShaderSource);
        glCompileShader(fragment);

        glGetShaderiv(vertex, GL_COMPILE_STATUS, successBuffer);
        if (successBuffer.get(0) != 1) {
            assert false : "ERROR::SHADER::VERTEX::COMPILATION_FAILED";
            System.err.println(glGetShaderInfoLog(vertex));
        }
        successBuffer.clear();

        glGetShaderiv(fragment, GL_COMPILE_STATUS, successBuffer);
        if (successBuffer.get(0) != 1) {
            assert false : "ERROR::SHADER::FRAGMENT::COMPILATION_FAILED";
            System.err.println(glGetShaderInfoLog(fragment));
        }
        successBuffer.clear();

        ID = glCreateProgram();
        glAttachShader(ID, vertex);
        glAttachShader(ID, fragment);
        glLinkProgram(ID);

        glGetProgramiv(ID, GL_LINK_STATUS, successBuffer);
        if (successBuffer.get(0) != 1) {
            assert false : "ERROR::SHADER::PROGRAM::LINKING_FAILED";
            System.err.println(glGetShaderInfoLog(ID));
        }

        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    public void use() {
        glUseProgram(ID);
    }

    public void initTextureSamplers() {
        int loc = GL20.glGetUniformLocation(getID(), "u_Textures");
        int[] samplers = { 0, 1, 2, 3, 4, 5, 6, 7 };
        GL20.glUniform1iv(loc, samplers);
    }

    public int getID() {
        return ID;
    }

    public void setMat4f(String name, Matrix4f value) {
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
        value.get(matrixBuffer);
        glUniformMatrix4fv(glGetUniformLocation(ID, name), false, matrixBuffer);
    }

    public void uploadInt(String name, int value) {
        int location = glGetUniformLocation(ID, name);
        glUniform1i(location, value);
    }
}