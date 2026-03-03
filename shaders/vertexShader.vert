#version 400 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec2 aTexCoord;
layout (location = 3) in float tId;

out vec4 ourColor;
out vec2 TexCoord;
flat out int ourTId;

uniform mat4 view;
uniform mat4 projection;

void main()
{
    gl_Position = projection * view * vec4(aPos, 1.0);
    ourColor = aColor;
    TexCoord = aTexCoord;
    ourTId = int(tId);
}