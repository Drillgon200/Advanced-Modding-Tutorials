#version 330 compatibility

in vec2 texCoord;
in vec2 lightCoord;
in vec4 color;
in vec3 lighting;
out vec4 FragColor;

uniform sampler2D texture;
uniform sampler2D lightmap;

void main(){
	vec4 col = color * texture2D(texture, texCoord) * texture2D(lightmap, lightCoord);
	FragColor = vec4(col.rgb * lighting, col.a);
}