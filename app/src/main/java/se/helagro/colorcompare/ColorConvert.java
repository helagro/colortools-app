package se.helagro.colorcompare;

import java.util.Locale;

import static android.graphics.Color.alpha;
import static android.graphics.Color.argb;
import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.parseColor;
import static android.graphics.Color.red;

import com.hlag.colorcompare.R;

class ColorConvert {
    private static final String TAG = "ColorConvert";
    private static final float ERR_CODE = 0.1f;

    static String ColorIntToString(int selectedId, int color) {
        if (selectedId == R.id.color_hex_btn)
            return isOpaque(color) ? String.format("#%06X", (0xFFFFFF & color)) : String.format("#%08X", color);

        else if (selectedId == R.id.color_rgb_btn)
            return String.format("%s%s %s %s%s",
                    getAlpha(color, true),
                    red(color),
                    green(color),
                    blue(color),
                    getAlpha(color, false));

        else if (selectedId == R.id.color_int_btn)
            return Integer.toString(color);

        else
            return "No mode selected, please restart the app";

    }

    private static String getAlpha(int color, boolean first) {
        if (!isOpaque(color) && first == MainActivity.is_argb) {
            final int alpha = alpha(color);
            final String alphaString = MainActivity.byte_alpha ? Integer.toString(alpha)
                    : String.format(Locale.US, "%.2f", alpha / 255f);

            if (MainActivity.is_argb) {
                return alphaString + " ";
            } else {
                return " " + alphaString;
            }
        }

        return "";
    }

    static boolean isOpaque(int color) {
        return alpha(color) == 255;
    }

    static double ColorIntFromString(String colorString) {
        int color;
        try {
            if (colorString.startsWith("#")) { // hex
                if (colorString.length() < 7) {
                    colorString = colorString.replaceAll("([0-9a-fA-F])", "$1$1");
                }
                color = parseColor(colorString);
            } else if (colorString.contains(" ")) { // rgb / argb / rgba
                final String[] colorStrings = colorString.split(" ");
                int alphaInt;
                int stringI = 0;
                final int[] colorInts = new int[3];

                if (colorStrings.length == 4) { // alpha entered
                    float alpha;

                    // yoinks alpha value
                    if (MainActivity.is_argb) {
                        alpha = Float.parseFloat(colorStrings[0]);
                        stringI = 1;
                    } else {
                        alpha = Float.parseFloat(colorStrings[3]);
                    }

                    if (alpha % 1 != 0)
                        alpha = alpha * 255;

                    alphaInt = Math.round(alpha);
                    if (badRgbValue(alphaInt))
                        return ERR_CODE;
                } else {
                    alphaInt = 255;
                }

                for (int i = 0; i < 3; i++) {
                    colorInts[i] = Integer.parseInt(colorStrings[stringI]);
                    if (badRgbValue(colorInts[i])) {
                        return ERR_CODE;
                    }
                    stringI++;
                }
                color = argb(alphaInt, colorInts[0], colorInts[1], colorInts[2]);

            } else {
                color = Integer.parseInt(colorString);
            }

            return color;
        } catch (Exception ignore) {
            return ERR_CODE;
        }
    }

    private static boolean badRgbValue(int value) {
        return value > 255 || value < 0;
    }

    static boolean noErr(double color) {
        return color % 1 == 0;
    }

}
