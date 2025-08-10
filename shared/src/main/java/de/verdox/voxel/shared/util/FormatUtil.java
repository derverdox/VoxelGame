package de.verdox.voxel.shared.util;

import java.util.Locale;

public class FormatUtil {
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        // Einheiten-Labels für 1024er-Potenzen
        String[] units = {"KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};
        // Berechne, welche Potenz von 1024 wir brauchen
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        double value = bytes / Math.pow(1024, exp);
        // Format mit zwei Nachkommastellen
        return String.format(Locale.US, "%.2f %s", value, units[exp - 1]);
    }

    public static String formatPercent(float percent) {
        if (Float.isNaN(percent) || Float.isInfinite(percent)) return "—";
        return String.format(Locale.US, "%.2f%%", (percent * 100));
    }
}
