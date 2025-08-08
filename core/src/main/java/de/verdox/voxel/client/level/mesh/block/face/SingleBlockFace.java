package de.verdox.voxel.client.level.mesh.block.face;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.verdox.voxel.client.assets.TextureAtlasManager;
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

    protected final BlockModelType.BlockFace blockFaceDefinition;
    protected final ResourceLocation textureId;
    protected final float lightPacked;
    protected final byte aoPacked;
    protected final byte blockXInChunk;
    protected final byte blockYInChunk;
    protected final byte blockZInChunk;

    public SingleBlockFace(
            BlockModelType.BlockFace blockFaceDefinition,
            byte blockXInMesh, byte blockYInMesh, byte blockZInMesh,
            ResourceLocation textureId, float lightPacked, byte aoPacked
    ) {
        this.blockFaceDefinition = blockFaceDefinition;
        this.blockXInChunk = blockXInMesh;
        this.blockYInChunk = blockYInMesh;
        this.blockZInChunk = blockZInMesh;
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
            byte lodLevel,
            int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks
    ) {
        // Hole Texturregion
        TextureRegion region = null;
        if (textureId != null) {
            region = textureAtlas.findRegion(textureId.toString());
        }
        float lodScale = LODUtil.getLodScale(lodLevel);

        float uLen = getULength();
        float vLen = getVLength();

        float[][] corners = new float[][]{
                {getCorner1X(lodScale), getCorner1Y(lodScale), getCorner1Z(lodScale)},
                {getCorner2X(lodScale), getCorner2Y(lodScale), getCorner2Z(lodScale)},
                {getCorner3X(lodScale), getCorner3Y(lodScale), getCorner3Z(lodScale)},
                {getCorner4X(lodScale), getCorner4Y(lodScale), getCorner4Z(lodScale)}
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

            int offset = vertexOffsetFloats + i * getFloatsPerVertex();
            float u = uv[i][0], v = uv[i][1];

            offset = writePosition(vertices, corners, i, offset, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);
            offset = writeUVAndLight(vertices, offset, u, v, uLen, vLen, uStart, vStart, tileU, tileV);
            //offset = writeLight(vertices, offset, i);

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

        byte[] indicesByFace = getIndexOrderByFaceDirection(blockFaceDefinition.direction());

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
        return offsetStart + 1;
    }

    protected int writeUVAndLight(
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

        float packed = packTileUVAndLights(TextureAtlasManager.getInstance().getBlockTextureAtlasSize(), TextureAtlasManager.getInstance().getBlockTextureSize(), atlasU, atlasV, (byte) 15, (byte) 0, (byte) 0, (byte) 0);
        vertices[offsetStart] = packed;
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
    public BlockFace addOffset(float offsetX, float offsetY, float offsetZ) {
        return new SingleBlockFace(
                blockFaceDefinition,
                ((byte) (blockXInChunk + offsetX)), ((byte) (blockYInChunk + offsetY)), ((byte) (blockZInChunk + offsetZ)),
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
            switch (blockFaceDefinition.direction()) {
                case UP, DOWN, SOUTH -> offsetX = u;
                case NORTH -> offsetX = -u;
                case EAST -> offsetZ = u;
                case WEST -> offsetZ = -u;
            }
            ;
        }

        if (v != 0) {
            switch (blockFaceDefinition.direction()) {
                case UP, DOWN -> offsetZ = v;
                case EAST, WEST, NORTH, SOUTH -> offsetY = v;
            }
        }
        return addOffset(offsetX, offsetY, offsetZ);
    }

    @Override
    public int getFloatsPerVertex() {
        return 2;
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

        boolean flip = getBlockFaceDefinition().direction().equals(Direction.EAST) || getBlockFaceDefinition().direction()
                .equals(Direction.WEST);

        int deltaU = flip ? 0 : delta;
        int deltaV = flip ? delta : 0;

        return new GreedyBlockFace(
                blockFaceDefinition,
                blockXInChunk, blockYInChunk, blockZInChunk,
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

        boolean flip = getBlockFaceDefinition().direction().equals(Direction.EAST) || getBlockFaceDefinition().direction()
                .equals(Direction.WEST);

        int deltaU = flip ? delta : 0;
        int deltaV = flip ? 0 : delta;

        return new GreedyBlockFace(
                blockFaceDefinition,
                blockXInChunk, blockYInChunk, blockZInChunk,
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
        return BlockFace.getWCoord(dir, (short) getCorner1X(1), (short) getCorner1Y(1), (short) getCorner1Z(1));
    }

    @Override
    public boolean isGreedyGroup(BlockFace other, Direction direction) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof SingleBlockFace singleBlockFace)) {
            return false;
        }
        return this.blockFaceDefinition.equals(singleBlockFace.blockFaceDefinition) && this.aoPacked == singleBlockFace.aoPacked && this.lightPacked == singleBlockFace.lightPacked && this.getWCoord(direction) == singleBlockFace.getWCoord(direction);
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
        return (int) (Math.abs(getCorner2X(1) - getCorner1X(1)) + Math.abs(getCorner2Y(1) - getCorner1Y(1)) + Math.abs(getCorner2Z(1) - getCorner1Z(1)));
    }

    /**
     * Computes the face length along the V axis (corner1 → corner4).
     *
     * @return the V-length in block units
     */
    protected float getVLength() {
        return Math.abs(getCorner4X(1) - getCorner1X(1)) + Math.abs(getCorner4Y(1) - getCorner1Y(1)) + Math.abs(getCorner4Z(1) - getCorner1Z(1));
    }

    @Override
    public String toString() {
        return "BlockFace{" +
                "c1X=" + getCorner1X(1) +
                ", c1Y=" + getCorner1Y(1) +
                ", c1Z=" + getCorner1Z(1) +
                ", c2X=" + getCorner2X(1) +
                ", c2Y=" + getCorner2Y(1) +
                ", c2Z=" + getCorner2Z(1) +
                ", c3X=" + getCorner3X(1) +
                ", c3Y=" + getCorner3Y(1) +
                ", c3Z=" + getCorner3Z(1) +
                ", c4X=" + getCorner4X(1) +
                ", c4Y=" + getCorner4Y(1) +
                ", c4Z=" + getCorner4Z(1) +
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
        return Float.compare(getCorner1X(1), blockFace.getCorner1X(1)) == 0 &&
                Float.compare(getCorner1Y(1), blockFace.getCorner1Y(1)) == 0 &&
                Float.compare(getCorner1Z(1), blockFace.getCorner1Z(1)) == 0 &&
                Float.compare(getCorner2X(1), blockFace.getCorner2X(1)) == 0 &&
                Float.compare(getCorner2Y(1), blockFace.getCorner2Y(1)) == 0 &&
                Float.compare(getCorner2Z(1), blockFace.getCorner2Z(1)) == 0 &&
                Float.compare(getCorner3X(1), blockFace.getCorner3X(1)) == 0 &&
                Float.compare(getCorner3Y(1), blockFace.getCorner3Y(1)) == 0 &&
                Float.compare(getCorner3Z(1), blockFace.getCorner3Z(1)) == 0 &&
                Float.compare(getCorner4X(1), blockFace.getCorner4X(1)) == 0 &&
                Float.compare(getCorner4Y(1), blockFace.getCorner4Y(1)) == 0 &&
                Float.compare(getCorner4Z(1), blockFace.getCorner4Z(1)) == 0 &&
                Float.compare(getNormalX(), blockFace.getNormalX()) == 0 &&
                Float.compare(getNormalY(), blockFace.getNormalY()) == 0 &&
                Float.compare(getNormalZ(), blockFace.getNormalZ()) == 0 &&
                Objects.equals(textureId, blockFace.textureId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getCorner1X(1), getCorner1Y(1), getCorner1Z(1),
                getCorner2X(1), getCorner2Y(1), getCorner2Z(1),
                getCorner3X(1), getCorner3Y(1), getCorner3Z(1),
                getCorner4X(1), getCorner4Y(1), getCorner4Z(1),
                getNormalX(), getNormalY(), getNormalZ(), textureId);
    }

    @Override
    public float getCornerX(BlockModelType.BlockFace.BlockModelCoordinate blockModelCoordinate, float lodScale) {
        return blockModelCoordinate.getCornerX(blockXInChunk, lodScale);
    }

    @Override
    public float getCornerY(BlockModelType.BlockFace.BlockModelCoordinate blockModelCoordinate, float lodScale) {
        return blockModelCoordinate.getCornerY(blockYInChunk, lodScale);
    }

    @Override
    public float getCornerZ(BlockModelType.BlockFace.BlockModelCoordinate blockModelCoordinate, float lodScale) {
        return blockModelCoordinate.getCornerZ(blockZInChunk, lodScale);
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
    public static float packPositionAndAO(int cx, int cy, int cz, int ao) {
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

    /**
     * Packt zwei Tile-Koordinaten (u,v in [0,1] in Steps von 1/64)
     * und vier Lichtwerte (0–15) in einen einzigen float.
     *
     * @param u          Tile-U-Koordinate, muss ein Vielfaches von 1/64 sein
     * @param v          Tile-V-Koordinate, muss ein Vielfaches von 1/64 sein
     * @param skyLight   Lichtwert Slot 0 (0–15)
     * @param redLight   Lichtwert Slot 1 (0–15)
     * @param greenLight Lichtwert Slot 2 (0–15)
     * @param blueLight  Lichtwert Slot 3 (0–15)
     * @return Float mit gepacktem Bitmuster
     */
    public static float packTileUVAndLights(
            int atlasSize, int textureSize,
            float u, float v,
            byte skyLight, byte redLight, byte greenLight, byte blueLight
    ) {
        int maxTileIndex = (atlasSize / textureSize) - 1;

        // 1) Tile-Index 0–63 berechnen
        int uIndex = Math.round(u * maxTileIndex);
        int vIndex = Math.round(v * maxTileIndex);

        // Clamp zur Sicherheit
        uIndex = Math.min(Math.max(uIndex, 0), maxTileIndex);
        vIndex = Math.min(Math.max(vIndex, 0), maxTileIndex);

        // 2) Lichtwerte auf 4 Bit maskieren
        skyLight &= 0xF;
        redLight &= 0xF;
        greenLight &= 0xF;
        blueLight &= 0xF;

        // 3) Bit-Layout (LSB … MSB):
        //    bits  0– 5: uIndex (6 Bit)
        //    bits  6–11: vIndex (6 Bit)
        //    bits 12–15: l0     (4 Bit)
        //    bits 16–19: l1     (4 Bit)
        //    bits 20–23: l2     (4 Bit)
        //    bits 24–27: l3     (4 Bit)
        int packedBits =
                (blueLight << 24)
                        | (greenLight << 20)
                        | (redLight << 16)
                        | (skyLight << 12)
                        | (vIndex << 6)
                        | (uIndex);
        return Float.intBitsToFloat(packedBits);
    }
}
