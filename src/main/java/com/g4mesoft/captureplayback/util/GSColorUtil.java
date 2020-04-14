package com.g4mesoft.captureplayback.util;

public class GSColorUtil {

	private static final double SRGB_2_XYZ_B11 = 0.4124;
	private static final double SRGB_2_XYZ_B12 = 0.3576;
	private static final double SRGB_2_XYZ_B13 = 0.1805;

	private static final double SRGB_2_XYZ_B21 = 0.2126;
	private static final double SRGB_2_XYZ_B22 = 0.7152;
	private static final double SRGB_2_XYZ_B23 = 0.0722;

	private static final double SRGB_2_XYZ_B31 = 0.0193;
	private static final double SRGB_2_XYZ_B32 = 0.1192;
	private static final double SRGB_2_XYZ_B33 = 0.9505;
	
	private static final double D65_XN =  95.0489;
	private static final double D65_YN = 100.0000;
	private static final double D65_ZN = 108.8840;
	
	private static final double LAB_DELTA = 6.0 / 29.0;
	private static final double LAB_DELTA_SQ = LAB_DELTA * LAB_DELTA;
	private static final double LAB_DELTA_CB = LAB_DELTA_SQ * LAB_DELTA;
	
	private static final double SIMILAR_COLOR_CIE76 = 2.3;
	
	/* @see https://en.wikipedia.org/wiki/SRGB#The_reverse_transformation */
	public static double[] rgb2xyz(int r, int g, int b) {
		double rf = toSRGB(r / 255.0);
		double gf = toSRGB(g / 255.0);
		double bf = toSRGB(b / 255.0);
	
		return new double[] { 
			SRGB_2_XYZ_B11 * rf + SRGB_2_XYZ_B12 * gf + SRGB_2_XYZ_B13 * bf,
			SRGB_2_XYZ_B21 * rf + SRGB_2_XYZ_B22 * gf + SRGB_2_XYZ_B23 * bf,
			SRGB_2_XYZ_B31 * rf + SRGB_2_XYZ_B32 * gf + SRGB_2_XYZ_B33 * bf
		};
	}
	
	private static double toSRGB(double u) {
		return (u < 0.04045) ? (25.0 * u / 323.0) : 
			Math.pow((200.0 * u + 11.0) / 211.0, 12.0 / 5.0);
	}

	/* @see https://en.wikipedia.org/wiki/CIELAB_color_space#Forward_transformation */
	public static double[] xyz2lab(double x, double y, double z) {
		double labY = labForward(y / D65_YN);
		
		double l = 116.0 * labY - 16.0;
		double a = 500.0 * (labForward(x / D65_XN) - labY);
		double b = 200.0 * (labY - labForward(z / D65_ZN));
	
		return new double[] { l, a, b };
	}
	
	private static double labForward(double t) {
		return (t > LAB_DELTA_CB) ? Math.cbrt(t) : 
			t / (3.0 * LAB_DELTA_SQ) + 4.0 / 29.0;
	}
	
	public static double[] rgb2lab(int r, int g, int b) {
		double[] xyz = rgb2xyz(r, g, b);
		return xyz2lab(xyz[0], xyz[1], xyz[2]);
	}

	/* @see https://en.wikipedia.org/wiki/Color_difference#CIE76 */
	public static double getColorDifferenceCIE76(double l1, double a1, double b1, double l2, double a2, double b2) {
		double dl = l2 - l1;
		double da = a2 - a1;
		double db = b2 - b1;
		return Math.sqrt(dl * dl + da * da + db * db);
	}
	
	public static boolean isRGBSimilar(int rgb1, int rgb2) {
		return isRGBSimilar((rgb1 >> 16) & 0xFF, (rgb1 >> 8) & 0xFF, rgb1 & 0xFF, 
		                    (rgb2 >> 16) & 0xFF, (rgb2 >> 8) & 0xFF, rgb2 & 0xFF);
	}

	public static boolean isRGBSimilar(int r1, int g1, int b1, int r2, int g2, int b2) {
		return getColorDifferenceCIE76(rgb2lab(r1, g1, b1), rgb2lab(r2, g2, b2)) < SIMILAR_COLOR_CIE76;
	}
	
	public static double getColorDifferenceCIE76(double[] lab1, double[] lab2) {
		return getColorDifferenceCIE76(lab1[0], lab1[1], lab1[2], lab2[0], lab2[1], lab2[2]);
	}
}
