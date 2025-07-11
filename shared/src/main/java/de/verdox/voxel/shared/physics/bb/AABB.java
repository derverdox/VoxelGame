package de.verdox.voxel.shared.physics.bb;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class AABB {
    private final Vector3f min;
    private final Vector3f max;

    /**
     * Erzeugt eine leere (ungültige) Box.
     * Erst durch extend() wird sie definiert.
     */
    public AABB() {
        this.min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        this.max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    /**
     * Erzeugt eine Box mit vorgegebenen Min-/Max-Ecken.
     */
    public AABB(Vector3f min, Vector3f max) {
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
    }

    /**
     * Liefert die Minimum-Ecke.
     */
    public Vector3f getMin() {
        return new Vector3f(min);
    }

    /**
     * Liefert die Maximum-Ecke.
     */
    public Vector3f getMax() {
        return new Vector3f(max);
    }

    /**
     * Setzt beide Ecken neu.
     */
    public void set(Vector3f min, Vector3f max) {
        this.min.set(min);
        this.max.set(max);
    }

    /**
     * Erweitert die Box so, dass sie den Punkt einschließt.
     */
    public void extend(Vector3f point) {
        min.x = Math.min(min.x, point.x);
        min.y = Math.min(min.y, point.y);
        min.z = Math.min(min.z, point.z);
        max.x = Math.max(max.x, point.x);
        max.y = Math.max(max.y, point.y);
        max.z = Math.max(max.z, point.z);
    }

    /**
     * Erweitert die Box um eine andere AABB.
     */
    public void extend(AABB other) {
        extend(other.min);
        extend(other.max);
    }

    /**
     * Prüft, ob die Box gültig (min <= max) ist.
     */
    public boolean isValid() {
        return min.x <= max.x && min.y <= max.y && min.z <= max.z;
    }

    /**
     * Gibt eine Kopie der Box zurück.
     */
    public AABB cpy() {
        return new AABB(min, max);
    }

    /**
     * Prüft, ob diese AABB mit einer anderen schneidet.
     */
    public boolean intersects(AABB other) {
        return !(other.max.x < min.x || other.min.x > max.x
            || other.max.y < min.y || other.min.y > max.y
            || other.max.z < min.z || other.min.z > max.z);
    }

    /**
     * Transformiert die AABB mit der gegebenen Matrix (Axis-Aligned Ergebnis).
     * D.h. die transformierte Box wird als AABB um die 8 transformierten Ecken berechnet.
     */
    public AABB transform(Matrix4f matrix) {
        Vector3f[] corners = new Vector3f[]{
            new Vector3f(min.x, min.y, min.z),
            new Vector3f(max.x, min.y, min.z),
            new Vector3f(max.x, max.y, min.z),
            new Vector3f(min.x, max.y, min.z),
            new Vector3f(min.x, min.y, max.z),
            new Vector3f(max.x, min.y, max.z),
            new Vector3f(max.x, max.y, max.z),
            new Vector3f(min.x, max.y, max.z)
        };
        AABB result = new AABB();
        for (Vector3f corner : corners) {
            corner.mulPosition(matrix);
            result.extend(corner);
        }
        return result;
    }

    @Override
    public String toString() {
        return "AABB[min=" + min + ", max=" + max + "]";
    }
}
