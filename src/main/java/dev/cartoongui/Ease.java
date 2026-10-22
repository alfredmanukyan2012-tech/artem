package dev.cartoongui;

/** Кривые плавности (как в iOS / Material). */
public final class Ease {
    private Ease() {}

    public static float outCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Мягкий «пружинный» выход с небольшим перелётом. */
    public static float outBack(float t) {
        final float c1 = 1.25f;
        final float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }
}
