package de.verdox.voxel.server.level.generator.spline;

import org.joml.Math;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a 1D spline mapping from noise value [0,1] to height.
 */
public class NoiseHeightSpline {
    private final List<Vector2f> points;

    private NoiseHeightSpline(List<Vector2f> points) {
        // points must be sorted by x
        this.points = points;
    }

    /**
     * Evaluates the spline at noise value n (in [-1,1] or [0,1]).
     * First remaps n from [-1,1] to [0,1], then linearly interpolates.
     *
     * @param noise raw noise value in [-1,1]
     * @return interpolated height
     */
    public float evaluate(float noise) {
        // remap to [0,1]
        float t = (noise + 1f) * 0.5f;
        // clamp
        t = Math.clamp(t, 0f, 1f);

        // find segment
        int idx = Collections.binarySearch(points, new Vector2f(t, 0), (a, b) -> Float.compare(a.x, b.x));
        if (idx >= 0) {
            // exact match
            return points.get(idx).y;
        }
        int insert = -idx - 1;
        if (insert == 0) {
            return points.getFirst().y;
        }
        if (insert >= points.size()) {
            return points.getLast().y;
        }
        Vector2f p1 = points.get(insert - 1);
        Vector2f p2 = points.get(insert);
        float u = (t - p1.x) / (p2.x - p1.x);
        return Math.lerp(p1.y, p2.y, u);
    }

    /**
     * Builder for NoiseHeightSpline.
     */
    public static class Builder {
        private final List<Vector2f> pts = new ArrayList<>();

        /**
         * Adds a control point (noiseThreshold in [0,1], height).
         */
        public Builder addPoint(float noiseThreshold, float height) {
            pts.add(new Vector2f(noiseThreshold, height));
            return this;
        }

        /**
         * Builds the spline. Points will be sorted by noiseThreshold.
         */
        public NoiseHeightSpline build() {
            // sort by x
            pts.sort((a, b) -> Float.compare(a.x, b.x));
            // ensure first at 0 and last at 1
            if (pts.isEmpty() || pts.getFirst().x > 0f) {
                pts.addFirst(new Vector2f(0f, pts.isEmpty() ? 0f : pts.getFirst().y));
            }
            if (pts.getLast().x < 1f) {
                Vector2f last = pts.getLast();
                pts.add(new Vector2f(1f, last.y));
            }
            return new NoiseHeightSpline(new ArrayList<>(pts));
        }
    }
}
