package de.verdox.voxel.client.level.mesh.block;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Vector3;
import de.verdox.voxel.shared.data.registry.ResourceLocation;

import java.util.Objects;

/**
 * Represents the geometric and texture data for a single block face or merged quad.
 * <p>
 * Four corner coordinates (corner1..4) define the face in 3D space, with normals indicating
 * the face orientation. The ordering of corners is assumed such that:
 * <ul>
 *   <li>corner1 to corner2 defines the U axis (first edge)</li>
 *   <li>corner1 to corner4 defines the V axis (second edge)</li>
 * </ul>
 */
public class BlockFace {
    public float corner1X, corner1Y, corner1Z;
    public float corner2X, corner2Y, corner2Z;
    public float corner3X, corner3Y, corner3Z;
    public float corner4X, corner4Y, corner4Z;
    public float normalX, normalY, normalZ;
    public ResourceLocation textureId;

    public BlockFace(float corner1X, float corner1Y, float corner1Z, float corner2X, float corner2Y, float corner2Z, float corner3X, float corner3Y, float corner3Z, float corner4X, float corner4Y, float corner4Z, float normalX, float normalY, float normalZ, ResourceLocation textureId) {
        this.corner1X = corner1X;
        this.corner1Y = corner1Y;
        this.corner1Z = corner1Z;
        this.corner2X = corner2X;
        this.corner2Y = corner2Y;
        this.corner2Z = corner2Z;
        this.corner3X = corner3X;
        this.corner3Y = corner3Y;
        this.corner3Z = corner3Z;
        this.corner4X = corner4X;
        this.corner4Y = corner4Y;
        this.corner4Z = corner4Z;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.textureId = textureId;
    }

    public int getLocalX() {
        return (int) Math.min(corner1X, Math.min(corner2X, Math.min(corner3X, corner4X)));
    }

    public int getLocalY() {
        return (int) Math.min(corner1Y, Math.min(corner2Y, Math.min(corner3Y, corner4Y)));
    }

    public int getLocalZ() {
        return (int) Math.min(corner1Z, Math.min(corner2Z, Math.min(corner3Z, corner4Z)));
    }

    public void addToBuilder(MeshPartBuilder meshPartBuilder, TextureAtlas textureAtlas) {
        if (textureId != null) {
            TextureRegion region = textureAtlas.findRegion(textureId.toString());

            if (!isGreedyFace()) {
                meshPartBuilder.setUVRange(region);
            } else {
                float uStart = region.getU(), vStart = region.getV();
                float uEnd = region.getU2(), vEnd = region.getV2();

                float dStepSize = uEnd - uStart, vStepSize = vEnd - vStart;

                float greedyUEnd = uStart + dStepSize * getULength();
                float greedyVEnd = vStart + vStepSize * getVLength();

                meshPartBuilder.setUVRange(uStart, vStart, greedyUEnd, greedyVEnd);
            }
        }


        // Corner positions in world space
        // Mapping corners to UV coordinates:
        //  c1 -> (u0, v0) bottom-left  (00)
        //  c2 -> (uEnd, v0) bottom-right (10)
        //  c3 -> (uEnd, vEnd) top-right   (11)
        //  c4 -> (u0, vEnd) top-left    (01)
        Vector3 c1 = new Vector3(corner1X, corner1Y, corner1Z);
        Vector3 c2 = new Vector3(corner2X, corner2Y, corner2Z);
        Vector3 c3 = new Vector3(corner3X, corner3Y, corner3Z);
        Vector3 c4 = new Vector3(corner4X, corner4Y, corner4Z);

        Vector3 normal = new Vector3(normalX, normalY, normalZ);
        meshPartBuilder.rect(c1, c2, c3, c4, normal);
    }

    public BlockFace addOffset(float offsetX, float offsetY, float offsetZ) {
        return new BlockFace(
            corner1X + offsetX, corner1Y + offsetY, corner1Z + offsetZ,
            corner2X + offsetX, corner2Y + offsetY, corner2Z + offsetZ,
            corner3X + offsetX, corner3Y + offsetY, corner3Z + offsetZ,
            corner4X + offsetX, corner4Y + offsetY, corner4Z + offsetZ,
            normalX, normalY, normalZ,
            textureId
        );
    }

    public BlockFace addOffsetX(float offset) {
        return new BlockFace(
            // nur X-Koordinaten plus Offset
            corner1X + offset, corner1Y, corner1Z,
            corner2X + offset, corner2Y, corner2Z,
            corner3X + offset, corner3Y, corner3Z,
            corner4X + offset, corner4Y, corner4Z,
            normalX, normalY, normalZ,
            textureId
        );
    }

    public BlockFace addOffsetY(float offset) {
        return new BlockFace(
            // nur Y-Koordinaten plus Offset
            corner1X, corner1Y + offset, corner1Z,
            corner2X, corner2Y + offset, corner2Z,
            corner3X, corner3Y + offset, corner3Z,
            corner4X, corner4Y + offset, corner4Z,
            normalX, normalY, normalZ,
            textureId
        );
    }

    public BlockFace addOffsetZ(float offset) {
        return new BlockFace(
            // nur Z-Koordinaten plus Offset
            corner1X, corner1Y, corner1Z + offset,
            corner2X, corner2Y, corner2Z + offset,
            corner3X, corner3Y, corner3Z + offset,
            corner4X, corner4Y, corner4Z + offset,
            normalX, normalY, normalZ,
            textureId
        );
    }

    /**
     * Expands this face by {@code delta} blocks along the U axis (corner1 → corner2),
     * growing the quad in its U direction.
     */
    public BlockFace expandU(int delta) {
        float lengthU = getULength();
        if (lengthU == 0) return this;
        float ux = (corner2X - corner1X) / lengthU;
        float uy = (corner2Y - corner1Y) / lengthU;
        float uz = (corner2Z - corner1Z) / lengthU;
        return new BlockFace(
            corner1X, corner1Y, corner1Z,
            corner2X + ux * delta, corner2Y + uy * delta, corner2Z + uz * delta,
            corner3X + ux * delta, corner3Y + uy * delta, corner3Z + uz * delta,
            corner4X, corner4Y, corner4Z,
            normalX, normalY, normalZ,
            textureId
        );
    }

    /**
     * Expands this face by {@code delta} blocks in the opposite U direction
     * (from corner2 back towards corner1).
     */
    public BlockFace expandUBackward(int delta) {
        float lengthU = getULength();
        if (lengthU == 0) return this;
        float ux = (corner1X - corner2X) / lengthU;
        float uy = (corner1Y - corner2Y) / lengthU;
        float uz = (corner1Z - corner2Z) / lengthU;
        return new BlockFace(
            corner1X + ux * delta, corner1Y + uy * delta, corner1Z + uz * delta,
            corner2X, corner2Y, corner2Z,
            corner3X, corner3Y, corner3Z,
            corner4X + ux * delta, corner4Y + uy * delta, corner4Z + uz * delta,
            normalX, normalY, normalZ,
            textureId
        );
    }

    /**
     * Rotates this face 90 degrees around its normal (right-hand rule),
     * returning a new BlockFace with rotated corners.
     */
    public BlockFace rotate90AroundNormal() {
        Vector3 axis = new Vector3(normalX, normalY, normalZ).nor();
        // compute center of the quad
        Vector3 center = new Vector3(corner1X, corner1Y, corner1Z)
            .add(corner2X, corner2Y, corner2Z)
            .add(corner3X, corner3Y, corner3Z)
            .add(corner4X, corner4Y, corner4Z)
            .scl(0.25f);
        // rotate each corner around center
        Vector3[] src = new Vector3[]{
            new Vector3(corner1X, corner1Y, corner1Z),
            new Vector3(corner2X, corner2Y, corner2Z),
            new Vector3(corner3X, corner3Y, corner3Z),
            new Vector3(corner4X, corner4Y, corner4Z)
        };
        Vector3[] dst = new Vector3[4];
        for (int i = 0; i < 4; i++) {
            Vector3 v = new Vector3(src[i]).sub(center);
            // v_rot = axis × v  (90° rotation)
            Vector3 vRot = new Vector3(axis).crs(v);
            dst[i] = vRot.add(center);
        }
        return new BlockFace(
            dst[0].x, dst[0].y, dst[0].z,
            dst[1].x, dst[1].y, dst[1].z,
            dst[2].x, dst[2].y, dst[2].z,
            dst[3].x, dst[3].y, dst[3].z,
            normalX, normalY, normalZ,
            textureId
        );
    }

    /**
     * Expands this face by {@code delta} blocks along the V axis (corner1 → corner4),
     * growing the quad in its V direction.
     */
    public BlockFace expandV(int delta) {
        float lengthV = getVLength();
        if (lengthV == 0) return this;
        float vx = (corner4X - corner1X) / lengthV;
        float vy = (corner4Y - corner1Y) / lengthV;
        float vz = (corner4Z - corner1Z) / lengthV;
        return new BlockFace(
            corner1X, corner1Y, corner1Z,
            corner2X, corner2Y, corner2Z,
            corner3X + vx * delta, corner3Y + vy * delta, corner3Z + vz * delta,
            corner4X + vx * delta, corner4Y + vy * delta, corner4Z + vz * delta,
            normalX, normalY, normalZ,
            textureId
        );
    }

    /**
     * Expands this face by {@code delta} blocks in the opposite V direction
     * (from corner4 back towards corner1).
     */
    public BlockFace expandVBackward(int delta) {
        float lengthV = getVLength();
        if (lengthV == 0) return this;
        float vx = (corner1X - corner4X) / lengthV;
        float vy = (corner1Y - corner4Y) / lengthV;
        float vz = (corner1Z - corner4Z) / lengthV;
        return new BlockFace(
            corner1X + vx * delta, corner1Y + vy * delta, corner1Z + vz * delta,
            corner2X + vx * delta, corner2Y + vy * delta, corner2Z + vz * delta,
            corner3X, corner3Y, corner3Z,
            corner4X, corner4Y, corner4Z,
            normalX, normalY, normalZ,
            textureId
        );
    }

    /**
     * Verschiebt das Face um `delta` Blöcke in U-Richtung (corner1 → corner2).
     */
    public BlockFace shiftU(int delta) {
        float lengthU = getULength();
        if (lengthU == 0) return this;  // kein valid U-Vector
        // Erzeuge den normierten U-Vektor
        float ux = (corner2X - corner1X) / lengthU;
        float uy = (corner2Y - corner1Y) / lengthU;
        float uz = (corner2Z - corner1Z) / lengthU;
        return offsetFace(ux * delta, uy * delta, uz * delta);
    }

    /**
     * Verschiebt das Face um `delta` Blöcke in V-Richtung (corner1 → corner4).
     */
    public BlockFace shiftV(int delta) {
        float lengthV = getVLength();
        if (lengthV == 0) return this;
        // Erzeuge den normierten V-Vektor
        float vx = (corner4X - corner1X) / lengthV;
        float vy = (corner4Y - corner1Y) / lengthV;
        float vz = (corner4Z - corner1Z) / lengthV;
        return offsetFace(vx * delta, vy * delta, vz * delta);
    }

    /**
     * Hilfsmethode: liefert ein neues BlockFace,
     * dessen alle vier Ecken um (dx,dy,dz) verschoben sind.
     */
    private BlockFace offsetFace(float dx, float dy, float dz) {
        return new BlockFace(
            corner1X + dx, corner1Y + dy, corner1Z + dz,
            corner2X + dx, corner2Y + dy, corner2Z + dz,
            corner3X + dx, corner3Y + dy, corner3Z + dz,
            corner4X + dx, corner4Y + dy, corner4Z + dz,
            normalX, normalY, normalZ,
            textureId
        );
    }

    /**
     * Computes the face length along the U axis (corner1 → corner2).
     *
     * @return the U-length in block units
     */
    public float getULength() {
        return (int) (Math.abs(corner2X - corner1X)
            + Math.abs(corner2Y - corner1Y)
            + Math.abs(corner2Z - corner1Z));
    }

    /**
     * Computes the face length along the V axis (corner1 → corner4).
     *
     * @return the V-length in block units
     */
    public float getVLength() {
        return Math.abs(corner4X - corner1X)
            + Math.abs(corner4Y - corner1Y)
            + Math.abs(corner4Z - corner1Z);
    }

    /**
     * Checks whether this face is a unit face (1×1), i.e. both U and V lengths equal 1.
     * Useful for identifying faces that should not be merged by greedy meshing.
     *
     * @return {@code true} if both dimensions are 1, {@code false} otherwise
     */
    public boolean isUnitFace() {
        return getULength() == 1 && getVLength() == 1;
    }

    public boolean isGreedyFace() {
        return getULength() > 1 || getVLength() > 1;
    }

    @Override
    public String toString() {
        return "BlockFace{" +
            "corner1X=" + corner1X +
            ", corner1Y=" + corner1Y +
            ", corner1Z=" + corner1Z +
            ", corner2X=" + corner2X +
            ", corner2Y=" + corner2Y +
            ", corner2Z=" + corner2Z +
            ", corner3X=" + corner3X +
            ", corner3Y=" + corner3Y +
            ", corner3Z=" + corner3Z +
            ", corner4X=" + corner4X +
            ", corner4Y=" + corner4Y +
            ", corner4Z=" + corner4Z +
            ", normalX=" + normalX +
            ", normalY=" + normalY +
            ", normalZ=" + normalZ +
            ", textureId=" + textureId +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BlockFace blockFace = (BlockFace) o;
        return Float.compare(corner1X, blockFace.corner1X) == 0 && Float.compare(corner1Y, blockFace.corner1Y) == 0 && Float.compare(corner1Z, blockFace.corner1Z) == 0 && Float.compare(corner2X, blockFace.corner2X) == 0 && Float.compare(corner2Y, blockFace.corner2Y) == 0 && Float.compare(corner2Z, blockFace.corner2Z) == 0 && Float.compare(corner3X, blockFace.corner3X) == 0 && Float.compare(corner3Y, blockFace.corner3Y) == 0 && Float.compare(corner3Z, blockFace.corner3Z) == 0 && Float.compare(corner4X, blockFace.corner4X) == 0 && Float.compare(corner4Y, blockFace.corner4Y) == 0 && Float.compare(corner4Z, blockFace.corner4Z) == 0 && Float.compare(normalX, blockFace.normalX) == 0 && Float.compare(normalY, blockFace.normalY) == 0 && Float.compare(normalZ, blockFace.normalZ) == 0 && Objects.equals(textureId, blockFace.textureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corner1X, corner1Y, corner1Z, corner2X, corner2Y, corner2Z, corner3X, corner3Y, corner3Z, corner4X, corner4Y, corner4Z, normalX, normalY, normalZ, textureId);
    }
}
