package us.to.midensthings.dynamicTools.resourcepack;

import us.to.midensthings.dynamicTools.tools.MaterialColors;

import java.awt.image.BufferedImage;

/***
 * Recolors a texture by gradient-mapping each pixel's brightness onto a material's dark, medium and light colors.
 * The shading and transparency of the base texture are kept, only the colors change.
 * Based on TextureColorizer from TBMCTools.
 */
public final class TextureRecolorer {

    /***
     * Which of the material's colors a texture is recolored with.
     */
    public enum ColorRange {
        // Dark for the shadows, medium for the mid-tones and light for the highlights
        FULL,
        // Dark for the shadows up to medium for the highlights, leaving out the light color
        DARK_TO_MEDIUM,
        // Every pixel gets the dark color
        DARK_ONLY
    }

    private TextureRecolorer() {
    }

    public static BufferedImage recolor(BufferedImage source, MaterialColors colors) {
        float[] range = luminanceRange(source);
        return recolor(source, colors, range[0], range[1], ColorRange.FULL);
    }

    /***
     * @return the darkest and brightest luminance across all the images' visible pixels, as {min, max}
     */
    public static float[] luminanceRange(BufferedImage... images) {
        float minLum = 1f;
        float maxLum = 0f;
        for (BufferedImage image : images) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    if ((argb >>> 24) == 0) {
                        continue;
                    }
                    float lum = luminance(argb);
                    minLum = Math.min(minLum, lum);
                    maxLum = Math.max(maxLum, lum);
                }
            }
        }
        return new float[]{minLum, maxLum};
    }

    /***
     * Recolors a texture, stretching the given brightness range over the gradient instead of the texture's own,
     * so textures that are meant to match (e.g. the two halves of worn armor) get the same colors for the same shades.
     * @param colorRange which of the material's colors to use
     */
    public static BufferedImage recolor(BufferedImage source, MaterialColors colors, float minLum, float maxLum, ColorRange colorRange) {
        int width = source.getWidth();
        int height = source.getHeight();
        float range = maxLum - minLum;

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) {
                    continue;
                }
                float t = range > 0 ? Math.clamp((luminance(argb) - minLum) / range, 0f, 1f) : 1f;
                int rgb = switch (colorRange) {
                    case DARK_ONLY -> colors.dark();
                    case DARK_TO_MEDIUM -> mix(colors.dark(), colors.medium(), t);
                    case FULL -> t < 0.5f
                            ? mix(colors.dark(), colors.medium(), t * 2f)
                            : mix(colors.medium(), colors.light(), (t - 0.5f) * 2f);
                };
                out.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        return out;
    }

    private static float luminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f;
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }
}
