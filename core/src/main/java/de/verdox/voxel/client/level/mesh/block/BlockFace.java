package de.verdox.voxel.client.level.mesh.block;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Vector3;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.lighting.LightAccessor;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.LightUtil;

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
    public byte blockXInChunk;
    public byte blockYInChunk;
    public byte blockZInChunk;
    public float corner1X;
    public float corner1Y;
    public float corner1Z;
    public float corner2X, corner2Y, corner2Z;
    public float corner3X, corner3Y, corner3Z;
    public float corner4X, corner4Y, corner4Z;
    public float normalX, normalY, normalZ;
    public ResourceLocation textureId;

    public BlockFace(byte blockXInChunk, byte blockYInChunk, byte blockZInChunk, float corner1X, float corner1Y, float corner1Z, float corner2X, float corner2Y, float corner2Z, float corner3X, float corner3Y, float corner3Z, float corner4X, float corner4Y, float corner4Z, float normalX, float normalY, float normalZ, ResourceLocation textureId) {
        this.blockXInChunk = blockXInChunk;
        this.blockYInChunk = blockYInChunk;
        this.blockZInChunk = blockZInChunk;
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

    // Corner positions in world space
    // Mapping corners to UV coordinates:
    //  c1 -> (u0, v0) bottom-left  (00)
    //  c2 -> (uEnd, v0) bottom-right (10)
    //  c3 -> (uEnd, vEnd) top-right   (11)
    //  c4 -> (u0, vEnd) top-left    (01)
    public void addToBuilder(MeshPartBuilder meshPartBuilder, TextureAtlas textureAtlas) {
        if (textureId != null) {
            TextureRegion region = textureAtlas.findRegion(textureId.toString());

            if (!isGreedyFace()) {
                meshPartBuilder.setUVRange(region);
                //meshPartBuilder.setColor(1,1,0,1);
            } else {
                float uStart = region.getU(), vStart = region.getV();
                float uEnd = region.getU2(), vEnd = region.getV2();

                float dStepSize = uEnd - uStart, vStepSize = vEnd - vStart;

                float greedyUEnd = uStart + dStepSize * getULength();
                float greedyVEnd = vStart + vStepSize * getVLength();

                meshPartBuilder.setUVRange(uStart, vStart, greedyUEnd, greedyVEnd);
                //meshPartBuilder.setColor(getULength(),getVLength(),0,1);
            }
        }
        meshPartBuilder.rect(
            corner1X, corner1Y, corner1Z,
            corner2X, corner2Y, corner2Z,
            corner3X, corner3Y, corner3Z,
            corner4X, corner4Y, corner4Z,
            normalX, normalY, normalZ
        );
    }

    /**
     * Determines orthogonal tangent offsets u and v for face normal (nx,ny,nz).
     */
    private static void getTangents(float nx, float ny, float nz,
                                    int[] uOffset, int[] vOffset) {
        if (nx != 0f) {
            // Face ±X → use Y and Z as tangents
            uOffset[0] = 0;
            uOffset[1] = 0;
            uOffset[2] = (nx > 0 ? 1 : -1);
            vOffset[0] = 0;
            vOffset[1] = 1;
            vOffset[2] = 0;
        } else if (ny != 0f) {
            // Face ±Y → use X and Z
            uOffset[0] = 1;
            uOffset[1] = 0;
            uOffset[2] = 0;
            vOffset[0] = 0;
            vOffset[1] = 0;
            vOffset[2] = (ny > 0 ? 1 : -1);
        } else {
            // Face ±Z → use X and Y
            uOffset[0] = 1;
            uOffset[1] = 0;
            uOffset[2] = 0;
            vOffset[0] = 0;
            vOffset[1] = 1;
            vOffset[2] = 0;
        }
    }

    /**
     * Computes ambient occlusion factor [0..1] for a vertex at block (x,y,z).
     */

    // Reusable arrays to avoid allocations
    private static final int[] uOff = new int[3];
    private static final int[] vOff = new int[3];

    private static float computeAO(int x, int y, int z, float nx, float ny, float nz, LightAccessor lightAccessor) {
        getTangents(nx, ny, nz, uOff, vOff);
        int x1 = x + uOff[0], y1 = y + uOff[1], z1 = z + uOff[2];
        int x2 = x + vOff[0], y2 = y + vOff[1], z2 = z + vOff[2];
        int xc = x1 + (x2 - x), yc = y1 + (y2 - y), zc = z1 + (z2 - z);

        boolean occ1 = false;
        boolean occ2 = false;
        boolean occc = false;
        if (lightAccessor.isInBounds(x1, y1, z1)) {
            occ1 = lightAccessor.isOpaque(x1, y1, z1);
        }
        if (lightAccessor.isInBounds(x2, y2, z2)) {
            occ2 = lightAccessor.isOpaque(x2, y2, z2);
        }
        if (lightAccessor.isInBounds(xc, yc, zc)) {
            occc = lightAccessor.isOpaque(xc, yc, zc);
        }
        int occ = (occ1 && occ2) ? 3 : ((occ1 ? 1 : 0) + (occ2 ? 1 : 0) + (occc ? 1 : 0));
        return 1f - occ / 3f;
    }


    public void appendToBuffers(
        float[] vertices,
        short[] indices,
        int vertexOffsetFloats,
        int indexOffset,
        short baseVertexIndex,
        TextureAtlas textureAtlas,
        int floatsPerVertex,
        LightAccessor lightAccessor
    ) {
        // Hole Texturregion
        TextureRegion region = null;
        if (textureId != null) {
            region = textureAtlas.findRegion(textureId.toString());
        }

        float uLen = isGreedyFace() ? getULength() : 1f;
        float vLen = isGreedyFace() ? getVLength() : 1f;

        float[][] uv = {
            {0f, 0f},
            {uLen, 0f},
            {uLen, vLen},
            {0f, vLen}
        };

        float[][] corners = new float[][]{
            {corner1X, corner1Y, corner1Z},
            {corner2X, corner2Y, corner2Z},
            {corner3X, corner3Y, corner3Z},
            {corner4X, corner4Y, corner4Z}
        };

        // Atlas-Region
        float uStart = region.getU(), vStart = region.getV();
        float tileU = region.getU2() - uStart;
        float tileV = region.getV2() - vStart;

        for (int i = 0; i < 4; i++) {
            int o = vertexOffsetFloats + i * floatsPerVertex;

            // Position
            vertices[o + 0] = corners[i][0];
            vertices[o + 1] = corners[i][1];
            vertices[o + 2] = corners[i][2];

            // Normal
            vertices[o + 3] = normalX;
            vertices[o + 4] = normalY;
            vertices[o + 5] = normalZ;

            // UV
            vertices[o + 6] = uv[i][0];
            vertices[o + 7] = uv[i][1];

            // GREEDY START
            vertices[o + 8] = uStart;
            vertices[o + 9] = vStart;

            // GREEDY END
            vertices[o + 10] = tileU;
            vertices[o + 11] = tileV;

            int bx = blockXInChunk;
            int by = blockYInChunk;
            int bz = blockZInChunk;

            int rx = (int) (blockXInChunk + normalX);
            int ry = (int) (blockYInChunk + normalY);
            int rz = (int) (blockZInChunk + normalZ);

            // 2) Aus LightAccessor abfragen
            byte sky;
            if (lightAccessor.isInBounds(rx, ry, rz)) {
                sky = lightAccessor.getSkyLight((byte) rx, (byte) ry, (byte) rz);
            } else {
                var neighborAccessor = lightAccessor.getNeighbor(Direction.fromOffsets((int) normalX, (int) normalY, (int) normalZ));
                if (neighborAccessor != null) {
                    sky = lightAccessor.getSkyLight((byte) normalX != 0 ? 0 : blockXInChunk, (byte) normalY != 0 ? 0 : blockYInChunk, (byte) normalZ != 0 ? 0 : blockZInChunk);
                } else {
                    sky = 15;
                }
            }


            byte r = lightAccessor.getBlockLightRed((byte) bx, (byte) by, (byte) bz);
            byte g = lightAccessor.getBlockLightGreen((byte) bx, (byte) by, (byte) bz);
            byte b = lightAccessor.getBlockLightBlue((byte) bx, (byte) by, (byte) bz);

            // Neues
            vertices[o + 12] = LightUtil.packLightToFloat((byte) sky, (byte) r, (byte) g, (byte) b);

            // Compute AO
            float ao = computeAO(bx, by, bz, normalX, normalY, normalZ, lightAccessor);
            vertices[o + 13] = ao;
        }

        // Indices für 2 Triangles
        indices[indexOffset + 0] = (short) (baseVertexIndex + 0);
        indices[indexOffset + 1] = (short) (baseVertexIndex + 1);
        indices[indexOffset + 2] = (short) (baseVertexIndex + 2);
        indices[indexOffset + 3] = (short) (baseVertexIndex + 2);
        indices[indexOffset + 4] = (short) (baseVertexIndex + 3);
        indices[indexOffset + 5] = (short) (baseVertexIndex + 0);
    }


    public BlockFace addOffset(float offsetX, float offsetY, float offsetZ) {
        return new BlockFace(
            (byte) (blockXInChunk + offsetX), (byte) (blockYInChunk + offsetY), (byte) (blockZInChunk + offsetZ),
            corner1X + offsetX, corner1Y + offsetY, corner1Z + offsetZ,
            corner2X + offsetX, corner2Y + offsetY, corner2Z + offsetZ,
            corner3X + offsetX, corner3Y + offsetY, corner3Z + offsetZ,
            corner4X + offsetX, corner4Y + offsetY, corner4Z + offsetZ,
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
            blockXInChunk, blockYInChunk, blockZInChunk,
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
            blockXInChunk, blockYInChunk, blockZInChunk,
            corner1X + ux * delta, corner1Y + uy * delta, corner1Z + uz * delta,
            corner2X, corner2Y, corner2Z,
            corner3X, corner3Y, corner3Z,
            corner4X + ux * delta, corner4Y + uy * delta, corner4Z + uz * delta,
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
            blockXInChunk, blockYInChunk, blockZInChunk,
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
            blockXInChunk, blockYInChunk, blockZInChunk,
            corner1X + vx * delta, corner1Y + vy * delta, corner1Z + vz * delta,
            corner2X + vx * delta, corner2Y + vy * delta, corner2Z + vz * delta,
            corner3X, corner3Y, corner3Z,
            corner4X, corner4Y, corner4Z,
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
            "c1X=" + corner1X +
            ", c1Y=" + corner1Y +
            ", c1Z=" + corner1Z +
            ", c2X=" + corner2X +
            ", c2Y=" + corner2Y +
            ", c2Z=" + corner2Z +
            ", c3X=" + corner3X +
            ", c3Y=" + corner3Y +
            ", c3Z=" + corner3Z +
            ", c4X=" + corner4X +
            ", c4Y=" + corner4Y +
            ", c4Z=" + corner4Z +
            ", nX=" + normalX +
            ", nY=" + normalY +
            ", nZ=" + normalZ +
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
