package us.to.midensthings.dynamicTools.tools;

/***
 * The colors a material's part textures are recolored with. The darkest pixels of a base texture become dark,
 * the mid-tones medium and the highlights light.
 * @param light RGB color, e.g. 0xFFFFFF
 * @param medium RGB color
 * @param dark RGB color
 */
public record MaterialColors(int light, int medium, int dark) {
}
