package se.helagro.colorcompare;

import static android.graphics.Color.alpha;
import static android.graphics.Color.argb;
import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.parseColor;
import static android.graphics.Color.red;

import com.hlag.colorcompare.R;

import java.util.Locale;

public class ColorConvert {
    private static final String TAG = "ColorConvert";
    private static final float ERR_CODE = 0.1f;

    public static String ColorIntToString(final int selectedId, final int color) {
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

    private static String getAlpha(final int color, final boolean first) {
        if (!isOpaque(color) && first == MainActivity.is_argb) {
            final int alpha = alpha(color);
            final String alphaString = MainActivity.byte_alpha ? Integer.toString(alpha)
                    : String.format(Locale.US, "%.2f", alpha / 255f);

            return MainActivity.byte_alpha ? alphaString + " " : " " + alphaString;
        } else
            return "";
    }

    static boolean isOpaque(final int color) {
        return alpha(color) == 255;
    }

    public static int ColorIntFromString(String colorString) throws IllegalArgumentException {
        if (colorString.startsWith("#")) { // hex
            if (colorString.length() < 7)
                colorString = colorString.replaceAll("([0-9a-fA-F])", "$1$1");

            return parseColor(colorString);
        } else if (colorString.contains(" ")) {
            return parseRGB(colorString);
        } else {
            return Integer.parseInt(colorString);
        }
    }

    private static int parseRGB(final String colorString) throws IllegalArgumentException {
        final String[] colorStrings = colorString.split(" ");
        final int colorLength = colorStrings.length;
        if (colorLength > 4)
            throw new IllegalArgumentException("Too many arguments");

        int alphaInt = parseAlpha(colorStrings);
        final int[] colorInts = new int[3];

        final int startI = MainActivity.is_argb && MainActivity.wasOpaque ? 1 : 0;
        final int endI = MainActivity.is_argb && MainActivity.wasOpaque ? 4 : 3;

        for (int i = startI; i < endI; i++) {
            final int colorInt = Integer.parseInt(colorStrings[i]);
            if (badRgbValue(colorInt))
                throw new IllegalArgumentException("Bad rgb value");

            colorInts[i - startI] = colorInt;
        }

        return argb(alphaInt, colorInts[0], colorInts[1], colorInts[2]);
    }

    private static int parseAlpha(final String[] colorStrings) throws IllegalArgumentException {
        if (colorStrings.length != 4)
            return 255;

        float alpha = MainActivity.is_argb ? Float.parseFloat(colorStrings[0]) : Float.parseFloat(colorStrings[3]);

        if (alpha % 1 != 0) // if alpha is a decimal
            alpha *= 255;

        final int alphaInt = Math.round(alpha);
        if (badRgbValue(alphaInt))
            throw new IllegalArgumentException("Bad alpha value");

        return alphaInt;
    }

    private static boolean badRgbValue(final int value) {
        return value > 255 || value < 0;
    }

}
