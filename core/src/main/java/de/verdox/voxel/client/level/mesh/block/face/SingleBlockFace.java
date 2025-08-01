package de.verdox.voxel.client.level.mesh.block.face;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.verdox.voxel.client.util.LODUtil;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.LightUtil;
import lombok.Getter;

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
@Getter
public class SingleBlockFace implements BlockFace {
    public static final float CUBE_BOUNDING_BOX_HALF = 0.5f;

    protected final BlockModelType.BlockFace blockFace;
    protected final byte lodLevel;
    protected final ResourceLocation textureId;
    protected final float lightPacked;
    protected final byte aoPacked;
    protected final byte blockXInChunk;
    protected final byte blockYInChunk;
    protected final byte blockZInChunk;

    public SingleBlockFace(
            BlockModelType.BlockFace blockFace,
            byte blockXInMesh, byte blockYInMesh, byte blockZInMesh,
            byte lodLevel, ResourceLocation textureId, float lightPacked, byte aoPacked
    ) {
        this.blockFace = blockFace;
        this.blockXInChunk = blockXInMesh;
        this.blockYInChunk = blockYInMesh;
        this.blockZInChunk = blockZInMesh;
        this.lodLevel = lodLevel;
        this.textureId = textureId;
        this.lightPacked = lightPacked;
        this.aoPacked = aoPacked;
    }

    public void appendToBuffers(
            float[] vertices,
            short[] shortIndices,
            int[] intIndices,
            int vertexOffsetFloats,
            int indexOffset,
            int baseVertexIndex,
            TextureAtlas textureAtlas,
            int floatsPerVertex,
            int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks
    ) {
        // Hole Texturregion
        TextureRegion region = null;
        if (textureId != null) {
            region = textureAtlas.findRegion(textureId.toString());
        }
        float lodScale = getLODScale();

        float uLen = getULength();
        float vLen = getVLength();

        float[][] corners = new float[][]{
                {getCorner1X(), getCorner1Y(), getCorner1Z()},
                {getCorner2X(), getCorner2Y(), getCorner2Z()},
                {getCorner3X(), getCorner3Y(), getCorner3Z()},
                {getCorner4X(), getCorner4Y(), getCorner4Z()}
        };

        float[] c0 = {0f, 0f};
        float[] c1 = {0f, vLen};
        float[] c2 = {uLen, 0f};
        float[] c3 = {uLen, vLen};

        float[][] uv = new float[][]{
                c0,
                c1,
                c2,
                c3,
        };


        // Atlas-Region
        float uStart = region.getU(), vStart = region.getV();
        float tileU = region.getU2() - uStart;
        float tileV = region.getV2() - vStart;

        for (int i = 0; i < 4; i++) {

            int offset = vertexOffsetFloats + i * floatsPerVertex;
            float u = uv[i][0], v = uv[i][1];

            offset = writePosition(vertices, corners, i, offset, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);
            offset = writeUV(vertices, offset, u, v, uLen, vLen, uStart, vStart, tileU, tileV);
            offset = writeLight(vertices, offset, i);

/*            vertices[o + 0] = corners[i][0];
            vertices[o + 1] = corners[i][1];
            vertices[o + 2] = corners[i][2];*/



/*            float localU = u / uLen;
            float localV = v / vLen;

            float atlasU = uStart + localU * tileU;
            float atlasV = vStart + localV * tileV;*/


                        /*            // GREEDY START
            vertices[o + 8] = uStart;
            vertices[o + 9] = vStart;

            // GREEDY END
            vertices[o + 10] = tileU;
            vertices[o + 11] = tileV;*/

/*            System.out.println(
                    "\t\tU: " + u + ", V: " + v + ", \n" +
                    "\t\tU Length: " + uLen + ", V Length: " + vLen + ",\n" +
                    "\t\tLocal U: " + localU + ", Local V: " + localV + ",\n" +
                    "\t\tTile U: " + tileU + ", Tile V: " + tileV + ",\n" +
                    "\t\tAtlas U: " + atlasU + ", Atlas V: " + atlasV
            );*/

/*            float bigU    = u * uLen;
            float bigV    = v * vLen;

            float localU   = bigU / uLen;
            float localV   = bigV / vLen;

            float atlasU  = uStart + localU * tileU;
            float atlasV  = vStart + localV * tileV;

            System.out.println("\t" +
                    "U: " + u + ", V: " + v + ", " +
                    "U Length: " + uLen + ", V Length: " + vLen + ", " +
                    "Local U: " + localU + ", Local V: " + localV + ", " +
                    "Big U: " + bigU + ", Big V: " + bigV + ", " +
                    "Tile U: " + tileU + ", Tile V: " + tileV + ", " +
                    "Atlas U: " + atlasU + ", Atlas V: " + atlasV);*/

/*            vertices[o + 3] = atlasU;
            vertices[o + 4] = atlasV;*/


        }

        byte[] indicesByFace = getIndexOrderByFaceDirection(blockFace.direction());

        // Indices für 2 Triangles

        if (intIndices != null) {
            intIndices[indexOffset + 0] = (baseVertexIndex + indicesByFace[0]);
            intIndices[indexOffset + 1] = (baseVertexIndex + indicesByFace[1]);
            intIndices[indexOffset + 2] = (baseVertexIndex + indicesByFace[2]);

            intIndices[indexOffset + 3] = (baseVertexIndex + indicesByFace[3]);
            intIndices[indexOffset + 4] = (baseVertexIndex + indicesByFace[4]);
            intIndices[indexOffset + 5] = (baseVertexIndex + indicesByFace[5]);
        } else {
            shortIndices[indexOffset + 0] = (short) (baseVertexIndex + indicesByFace[0]);
            shortIndices[indexOffset + 1] = (short) (baseVertexIndex + indicesByFace[1]);
            shortIndices[indexOffset + 2] = (short) (baseVertexIndex + indicesByFace[2]);

            shortIndices[indexOffset + 3] = (short) (baseVertexIndex + indicesByFace[3]);
            shortIndices[indexOffset + 4] = (short) (baseVertexIndex + indicesByFace[4]);
            shortIndices[indexOffset + 5] = (short) (baseVertexIndex + indicesByFace[5]);
        }
    }

    protected int writePosition(float[] vertices, float[][] corners, int cornerIndex, int offsetStart, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks) {
        int x = (int) corners[cornerIndex][0] + offsetXInBlocks;
        int y = (int) corners[cornerIndex][1] + offsetYInBlocks;
        int z = (int) corners[cornerIndex][2] + offsetZInBlocks;


        byte aoAtCorner = LightUtil.unpackAo(this.aoPacked, cornerIndex);

        float packedPositionAndAo = packPositionAndAO(x, y, z, aoAtCorner);

        vertices[offsetStart] = packedPositionAndAo;

/*        vertices[offsetStart] = x;
        vertices[offsetStart + 1] = y;
        vertices[offsetStart + 2] = z;
        return offsetStart + 3;*/
        return offsetStart + 1;
    }

    protected int writeUV(
            float[] vertices, int offsetStart,
            float u, float v,
            float uLen, float vLen,
            float uStart, float vStart,
            float tileU, float tileV
    ) {
        float localU = u / uLen;
        float localV = v / vLen;

        float atlasU = uStart + localU * tileU;
        float atlasV = vStart + localV * tileV;

        vertices[offsetStart] = atlasU;
        vertices[offsetStart + 1] = atlasV;
        return offsetStart + 2;
    }

    protected int writeLight(float[] vertices, int offset, int cornerIdx) {
        byte aoAtCorner = LightUtil.unpackAo(this.aoPacked, cornerIdx);
        vertices[offset] = LightUtil.packLightAndAoToFloat((byte) 15, (byte) 0, (byte) 0, (byte) 0, aoAtCorner);
        return offset + 1;
    }

    @Override
    public BlockModelType.BlockFace getFaceDefinition() {
        return blockFace;
    }


    @Override
    public BlockFace addOffset(float offsetX, float offsetY, float offsetZ) {
        return new SingleBlockFace(
                blockFace,
                ((byte) (blockXInChunk + offsetX)), ((byte) (blockYInChunk + offsetY)), ((byte) (blockZInChunk + offsetZ)),
                lodLevel,
                textureId,
                lightPacked, aoPacked
        );
    }

    @Override
    public BlockFace addOffset(int offsetX, int offsetY, int offsetZ) {
        return addOffset((float) offsetX, (float) offsetY, (float) offsetZ);
    }

    @Override
    public BlockFace addOffset(int u, int v) {
        int offsetX = 0;
        int offsetY = 0;
        int offsetZ = 0;

        if (u == 0 && v == 0) {
            return this;
        }

        if (u != 0) {
            switch (blockFace.direction()) {
                case UP, DOWN, SOUTH -> offsetX = u;
                case NORTH -> offsetX = -u;
                case EAST -> offsetZ = u;
                case WEST -> offsetZ = -u;
            }
            ;
        }

        if (v != 0) {
            switch (blockFace.direction()) {
                case UP, DOWN -> offsetZ = v;
                case EAST, WEST, NORTH, SOUTH -> offsetY = v;
            }
        }
        return addOffset(offsetX, offsetY, offsetZ);
    }

    @Override
    public int getFloatsPerVertex() {
        return 4;
    }

    @Override
    public int getVerticesPerFace() {
        return 4;
    }

    @Override
    public int getIndicesPerFace() {
        return 6;
    }

    @Override
    public Direction getDirection() {
        return Direction.fromOffsets((int) getNormalX(), (int) getNormalY(), (int) getNormalZ());
    }

    /**
     * Expands this face by {@code delta} blocks along the U axis (corner1 → corner2),
     * growing the quad in its U direction.
     */
    @Override
    public SingleBlockFace expandU(int delta) {
        float lengthU = getULength();
        if (lengthU == 0) return this;

        boolean flip = getBlockFace().direction().equals(Direction.EAST) || getBlockFace().direction()
                                                                                          .equals(Direction.WEST);

        int deltaU = flip ? 0 : delta;
        int deltaV = flip ? delta : 0;

        return new GreedyBlockFace(
                blockFace,
                blockXInChunk, blockYInChunk, blockZInChunk,
                lodLevel,
                textureId,
                lightPacked, aoPacked,
                deltaU, deltaV
        );
    }

    /**
     * Expands this face by {@code delta} blocks along the V axis (corner1 → corner4),
     * growing the quad in its V direction.
     */
    @Override
    public SingleBlockFace expandV(int delta) {
        float lengthV = getVLength();
        if (lengthV == 0) return this;

        boolean flip = getBlockFace().direction().equals(Direction.EAST) || getBlockFace().direction()
                                                                                          .equals(Direction.WEST);

        int deltaU = flip ? delta : 0;
        int deltaV = flip ? 0 : delta;

        return new GreedyBlockFace(
                blockFace,
                blockXInChunk, blockYInChunk, blockZInChunk,
                lodLevel,
                textureId,
                lightPacked, aoPacked,
                deltaU, deltaV
        );
    }

    @Override
    public int getUCoord(Direction dir) {
        return BlockFace.getUCoord(dir, blockXInChunk, blockYInChunk, blockZInChunk);
    }

    @Override
    public int getVCoord(Direction dir) {
        return BlockFace.getVCoord(dir, blockXInChunk, blockYInChunk, blockZInChunk);
    }

    @Override
    public int getWCoord(Direction dir) {
        return BlockFace.getWCoord(dir, (short) getCorner1X(), (short) getCorner1Y(), (short) getCorner1Z());
    }

    @Override
    public boolean isGreedyGroup(BlockFace other, Direction direction) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof SingleBlockFace singleBlockFace)) {
            return false;
        }
        return this.blockFace.equals(singleBlockFace.blockFace) && this.aoPacked == singleBlockFace.aoPacked && this.lightPacked == singleBlockFace.lightPacked && this.getWCoord(direction) == singleBlockFace.getWCoord(direction);
    }

    @Override
    public boolean isLodGroup(BlockFace other, Direction direction, int lodStep) {
        if (other == null) {
            return false;
        }

        return this.getWCoord(direction) / lodStep == other.getWCoord(direction) / lodStep;
    }

    /**
     * Computes the face length along the U axis (corner1 → corner2).
     *
     * @return the U-length in block units
     */
    protected float getULength() {
        return (int) (Math.abs(getCorner2X() - getCorner1X()) + Math.abs(getCorner2Y() - getCorner1Y()) + Math.abs(getCorner2Z() - getCorner1Z()));
    }

    /**
     * Computes the face length along the V axis (corner1 → corner4).
     *
     * @return the V-length in block units
     */
    protected float getVLength() {
        return Math.abs(getCorner4X() - getCorner1X()) + Math.abs(getCorner4Y() - getCorner1Y()) + Math.abs(getCorner4Z() - getCorner1Z());
    }

    @Override
    public String toString() {
        return "BlockFace{" +
                "c1X=" + getCorner1X() +
                ", c1Y=" + getCorner1Y() +
                ", c1Z=" + getCorner1Z() +
                ", c2X=" + getCorner2X() +
                ", c2Y=" + getCorner2Y() +
                ", c2Z=" + getCorner2Z() +
                ", c3X=" + getCorner3X() +
                ", c3Y=" + getCorner3Y() +
                ", c3Z=" + getCorner3Z() +
                ", c4X=" + getCorner4X() +
                ", c4Y=" + getCorner4Y() +
                ", c4Z=" + getCorner4Z() +
                ", nX=" + getNormalX() +
                ", nY=" + getNormalY() +
                ", nZ=" + getNormalZ() +
                ", textureId=" + textureId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SingleBlockFace blockFace = (SingleBlockFace) o;
        return Float.compare(getCorner1X(), blockFace.getCorner1X()) == 0 &&
                Float.compare(getCorner1Y(), blockFace.getCorner1Y()) == 0 &&
                Float.compare(getCorner1Z(), blockFace.getCorner1Z()) == 0 &&
                Float.compare(getCorner2X(), blockFace.getCorner2X()) == 0 &&
                Float.compare(getCorner2Y(), blockFace.getCorner2Y()) == 0 &&
                Float.compare(getCorner2Z(), blockFace.getCorner2Z()) == 0 &&
                Float.compare(getCorner3X(), blockFace.getCorner3X()) == 0 &&
                Float.compare(getCorner3Y(), blockFace.getCorner3Y()) == 0 &&
                Float.compare(getCorner3Z(), blockFace.getCorner3Z()) == 0 &&
                Float.compare(getCorner4X(), blockFace.getCorner4X()) == 0 &&
                Float.compare(getCorner4Y(), blockFace.getCorner4Y()) == 0 &&
                Float.compare(getCorner4Z(), blockFace.getCorner4Z()) == 0 &&
                Float.compare(getNormalX(), blockFace.getNormalX()) == 0 &&
                Float.compare(getNormalY(), blockFace.getNormalY()) == 0 &&
                Float.compare(getNormalZ(), blockFace.getNormalZ()) == 0 &&
                Objects.equals(textureId, blockFace.textureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getCorner1X(), getCorner1Y(), getCorner1Z(),
                getCorner2X(), getCorner2Y(), getCorner2Z(),
                getCorner3X(), getCorner3Y(), getCorner3Z(),
                getCorner4X(), getCorner4Y(), getCorner4Z(),
                getNormalX(), getNormalY(), getNormalZ(), textureId);
    }

    public float getCorner1X() {
        return getCornerX((byte) 0, blockFace.c1());
    }

    public float getCorner1Y() {
        return getCornerY((byte) 0, blockFace.c1());
    }

    public float getCorner1Z() {
        return getCornerZ((byte) 0, blockFace.c1());
    }

    public float getCorner2X() {
        return getCornerX((byte) 1, blockFace.c2());
    }

    public float getCorner2Y() {
        return getCornerY((byte) 1, blockFace.c2());
    }

    public float getCorner2Z() {
        return getCornerZ((byte) 1, blockFace.c2());
    }

    public float getCorner3X() {
        return getCornerX((byte) 2, blockFace.c3());
    }

    public float getCorner3Y() {
        return getCornerY((byte) 2, blockFace.c3());
    }

    public float getCorner3Z() {
        return getCornerZ((byte) 2, blockFace.c3());
    }

    public float getCorner4X() {
        return getCornerX((byte) 3, blockFace.c4());
    }

    public float getCorner4Y() {
        return getCornerY((byte) 3, blockFace.c4());
    }

    public float getCorner4Z() {
        return getCornerZ((byte) 3, blockFace.c4());
    }

    public float getNormalX() {
        return blockFace.normalX();
    }

    public float getNormalY() {
        return blockFace.normalY();
    }

    public float getNormalZ() {
        return blockFace.normalZ();
    }

    protected float getCornerX(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        return ((blockXInChunk + relativeCoordinate.x() + CUBE_BOUNDING_BOX_HALF) * getLODScale());
    }

    protected float getCornerY(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        return ((blockYInChunk + relativeCoordinate.y() + CUBE_BOUNDING_BOX_HALF) * getLODScale());
    }

    protected float getCornerZ(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        return ((blockZInChunk + relativeCoordinate.z() + CUBE_BOUNDING_BOX_HALF) * getLODScale());
    }

    protected float getLODScale() {
        return LODUtil.getLodScale(lodLevel);
    }

    protected byte[] getIndexOrderByFaceDirection(Direction direction) {
        return new byte[]{0, 1, 3, 3, 2, 0};
    }

    /**
     * Packt drei 10-Bit-Koordinaten (cx, cy, cz ∈ [0..1023]) und
     * einen 2-Bit AO-Wert (ao ∈ [0..3]) in einen 32-Bit Int,
     * und reinterpret castet ihn als Float.
     * <p>
     * Bit-Layout (0 = LSB, 31 = MSB):
     * [ ao:2 |   z:10   |   y:10   |   x:10   ]
     */
    protected float packPositionAndAO(int cx, int cy, int cz, int ao) {
        // Maske auf gültigen Wertebereich
        int x = cx & 0x3FF;   // 10 Bit
        int y = cy & 0x3FF;   // 10 Bit
        int z = cz & 0x3FF;   // 10 Bit
        int a = ao & 0x3;     // 2 Bit

        // Schiebe in 32-Bit-Wort: [a<<30] + [z<<20] + [y<<10] + [x]
        int packed = (a << 30) | (z << 20) | (y << 10) | x;

        // Reinterpret als Float, Bits bleiben erhalten
        return Float.intBitsToFloat(packed);
    }
}
