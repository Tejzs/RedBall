#version 400 core
out vec4 FragColor;

in vec4 ourColor;
in vec2 TexCoord;
flat in int ourTId;

uniform sampler2D u_Textures[16];

void main()
{
    if (ourTId == 0) {
        FragColor = ourColor;
    } else {
        FragColor = texture(u_Textures[ourTId - 1], TexCoord) * ourColor;
    }
}