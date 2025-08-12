package de.verdox.voxel.client.level.chunk.proto;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.mesh.block.face.SingleBlockFace;
import de.verdox.voxel.shared.util.lod.LODUtil;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.util.BitPackingUtil;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.LightUtil;
import lombok.Getter;

public class Single extends ProtoMask {
    public static final byte DIRECTION_ID_SIZE_BITS = 3;
    public static final byte AO_SIZE_BITS = 8;
    public static final byte SKY_LIGHT_B = 4;
    public static final byte RED_LIGHT_B = 4;
    public static final byte GREEN_LIGHT_B = 4;
    public static final byte BLUE_LIGHT_B = 4;

    @Getter
    private final byte maskId;

    Single(byte maskId) {
        this.maskId = maskId;
    }

    /**
     * Speichert ein Face bit-kompakt und effizient, ohne globalen Cursor.
     */
    public void storeFace(
            ChunkProtoMesh chunkProtoMesh,
            Type faceType,
            byte x, byte y, byte z,
            Direction dir, byte ao,
            byte skyLight, byte redLight,
            byte greenLight, byte blueLight
    ) {
        ProtoMeshStorage storage = chunkProtoMesh.getStorage(faceType, this);
        byte localXByteSize = (byte) chunkProtoMesh.getLocalXByteSize();
        byte localYByteSize = (byte) chunkProtoMesh.getLocalYByteSize();
        byte localZByteSize = (byte) chunkProtoMesh.getLocalZByteSize();

        int bitsPerFace = getBitSizePerFace(localXByteSize, localYByteSize, localZByteSize);
        long bitOffset = (long) storage.getFaceCount() * bitsPerFace;

        storage.writeBitsAt(bitOffset, x, localXByteSize);
        bitOffset += localXByteSize;

        storage.writeBitsAt(bitOffset, y, localYByteSize);
        bitOffset += localYByteSize;

        storage.writeBitsAt(bitOffset, z, localZByteSize);
        bitOffset += localZByteSize;

        storage.writeBitsAt(bitOffset, dir.getId(), DIRECTION_ID_SIZE_BITS);
        bitOffset += DIRECTION_ID_SIZE_BITS;

        storage.writeBitsAt(bitOffset, ao, AO_SIZE_BITS);
        bitOffset += AO_SIZE_BITS;

        storage.writeBitsAt(bitOffset, skyLight, SKY_LIGHT_B);
        bitOffset += SKY_LIGHT_B;

        storage.writeBitsAt(bitOffset, redLight, RED_LIGHT_B);
        bitOffset += RED_LIGHT_B;

        storage.writeBitsAt(bitOffset, greenLight, GREEN_LIGHT_B);
        bitOffset += GREEN_LIGHT_B;

        storage.writeBitsAt(bitOffset, blueLight, BLUE_LIGHT_B);

        storage.faceCount++;
    }

    public ChunkProtoMesh.FaceData get(ChunkProtoMesh chunkProtoMesh, Type faceType, int index) {
        ProtoMeshStorage storage = chunkProtoMesh.getStorage(faceType, this);
        byte localXByteSize = (byte) chunkProtoMesh.getLocalXByteSize();
        byte localYByteSize = (byte) chunkProtoMesh.getLocalYByteSize();
        byte localZByteSize = (byte) chunkProtoMesh.getLocalZByteSize();

        int bitsPerFace = getBitSizePerFace(localXByteSize, localYByteSize, localZByteSize);
        int offset = index * bitsPerFace;

        byte x = (byte) storage.readBits(offset, localXByteSize);
        offset += localXByteSize;

        byte y = (byte) storage.readBits(offset, localYByteSize);
        offset += localYByteSize;

        byte z = (byte) storage.readBits(offset, localZByteSize);
        offset += localZByteSize;

        Direction d = Direction.values()[(int) storage.readBits(offset, DIRECTION_ID_SIZE_BITS)];
        offset += DIRECTION_ID_SIZE_BITS;

        byte ao = (byte) storage.readBits(offset, AO_SIZE_BITS);
        offset += AO_SIZE_BITS;

        byte sky = (byte) storage.readBits(offset, SKY_LIGHT_B);
        offset += SKY_LIGHT_B;

        byte red = (byte) storage.readBits(offset, RED_LIGHT_B);
        offset += RED_LIGHT_B;

        byte green = (byte) storage.readBits(offset, GREEN_LIGHT_B);
        offset += GREEN_LIGHT_B;

        byte blue = (byte) storage.readBits(offset, BLUE_LIGHT_B);

        return new ChunkProtoMesh.FaceData(x, y, z, d, ao, sky, red, green, blue);
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
    public void appendToBuffers(ChunkProtoMesh chunkProtoMesh, Type faceType, FloatArray vertices, IntArray indices, int baseVertexIndex, TextureAtlas textureAtlas, byte lodLevel, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks) {
        ProtoMeshStorage storage = chunkProtoMesh.getStorage(faceType, this);
        byte localXByteSize = (byte) chunkProtoMesh.getLocalXByteSize();
        byte localYByteSize = (byte) chunkProtoMesh.getLocalYByteSize();
        byte localZByteSize = (byte) chunkProtoMesh.getLocalZByteSize();

        int vertsPerFace = getVerticesPerFace();

        for (int faceId = 0; faceId < storage.getFaceCount(); faceId++) {
            int faceBaseVertexIdx = baseVertexIndex + faceId * vertsPerFace;

            int bitsPerFace = getBitSizePerFace(localXByteSize, localYByteSize, localZByteSize);
            int offset = faceId * bitsPerFace;

            byte localX = (byte) storage.readBits(offset, localXByteSize);
            offset += localXByteSize;

            byte localY = (byte) storage.readBits(offset, localYByteSize);
            offset += localYByteSize;

            byte localZ = (byte) storage.readBits(offset, localZByteSize);
            offset += localZByteSize;

            Direction faceDir = Direction.values()[(int) storage.readBits(offset, DIRECTION_ID_SIZE_BITS)];
            offset += DIRECTION_ID_SIZE_BITS;

            byte ambientOcclusion = (byte) storage.readBits(offset, AO_SIZE_BITS);
            offset += AO_SIZE_BITS;

            byte sky = (byte) storage.readBits(offset, SKY_LIGHT_B);
            offset += SKY_LIGHT_B;

            byte red = (byte) storage.readBits(offset, RED_LIGHT_B);
            offset += RED_LIGHT_B;

            byte green = (byte) storage.readBits(offset, GREEN_LIGHT_B);
            offset += GREEN_LIGHT_B;

            byte blue = (byte) storage.readBits(offset, BLUE_LIGHT_B);

            BlockBase blockBase = chunkProtoMesh.getParent().getBlockAt(localX, localY, localZ);
            BlockModelType.BlockFace blockFaceDefinition = blockBase.getModel().getBlockModelType().getBlockFace(faceDir).getFirst();
            ResourceLocation textureName = blockBase.getModel().getTextureOfFace(blockBase.getModel().getBlockModelType().getNameOfFace(blockFaceDefinition));

            TextureRegion region = null;
            if (textureName != null) {
                region = textureAtlas.findRegion(textureName.toString());
            }

            float lodScale = LODUtil.getLodScale(lodLevel);


            float[][] corners = new float[][]{
                    {blockFaceDefinition.c1().getCornerX(localX, lodScale), blockFaceDefinition.c1().getCornerY(localY, lodScale), blockFaceDefinition.c1().getCornerZ(localZ, lodScale)},
                    {blockFaceDefinition.c2().getCornerX(localX, lodScale), blockFaceDefinition.c2().getCornerY(localY, lodScale), blockFaceDefinition.c2().getCornerZ(localZ, lodScale)},
                    {blockFaceDefinition.c3().getCornerX(localX, lodScale), blockFaceDefinition.c3().getCornerY(localY, lodScale), blockFaceDefinition.c3().getCornerZ(localZ, lodScale)},
                    {blockFaceDefinition.c4().getCornerX(localX, lodScale), blockFaceDefinition.c4().getCornerY(localY, lodScale), blockFaceDefinition.c4().getCornerZ(localZ, lodScale)}
            };

            float uLen = (int) (Math.abs(corners[1][0] - corners[0][0]) + Math.abs(corners[1][1] - corners[0][1]) + Math.abs(corners[1][2] - corners[0][2]));
            float vLen = Math.abs(corners[3][0] - corners[0][0]) + Math.abs(corners[3][1] - corners[0][1]) + Math.abs(corners[3][2] - corners[0][2]);

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

            for (int i = 0; i < getVerticesPerFace(); i++) {
                float u = uv[i][0], v = uv[i][1];

                writeCornerPositionToBuffer(vertices, corners, i, ambientOcclusion, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);
                writeCornerUVAndLightToBuffer(vertices, u, v, uLen, vLen, uStart, vStart, tileU, tileV, sky, red, green, blue);
            }

            indices.add(faceBaseVertexIdx + 0);
            indices.add(faceBaseVertexIdx + 1);
            indices.add(faceBaseVertexIdx + 3);
            indices.add(faceBaseVertexIdx + 3);
            indices.add(faceBaseVertexIdx + 2);
            indices.add(faceBaseVertexIdx + 0);
        }
    }

    @Override
    public void appendToInstances(ChunkProtoMesh chunkProtoMesh, Type faceType, FloatArray floatBuffer, TextureAtlas textureAtlas, byte lodLevel, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks) {
        ProtoMeshStorage storage = chunkProtoMesh.getStorage(faceType, this);
        byte localXByteSize = (byte) chunkProtoMesh.getLocalXByteSize();
        byte localYByteSize = (byte) chunkProtoMesh.getLocalYByteSize();
        byte localZByteSize = (byte) chunkProtoMesh.getLocalZByteSize();
        int blockAtlasSize = TextureAtlasManager.getInstance().getBlockTextureAtlasSize();

        for (int faceId = 0; faceId < storage.getFaceCount(); faceId++) {
            int bitsPerFace = getBitSizePerFace(localXByteSize, localYByteSize, localZByteSize);
            int offset = faceId * bitsPerFace;

            byte localX = (byte) storage.readBits(offset, localXByteSize);
            offset += localXByteSize;

            byte localY = (byte) storage.readBits(offset, localYByteSize);
            offset += localYByteSize;

            byte localZ = (byte) storage.readBits(offset, localZByteSize);
            offset += localZByteSize;

            Direction faceDir = Direction.values()[(int) storage.readBits(offset, DIRECTION_ID_SIZE_BITS)];
            offset += DIRECTION_ID_SIZE_BITS;

            byte ambientOcclusion = (byte) storage.readBits(offset, AO_SIZE_BITS);
            offset += AO_SIZE_BITS;

            byte sky = (byte) storage.readBits(offset, SKY_LIGHT_B);
            offset += SKY_LIGHT_B;

            byte red = (byte) storage.readBits(offset, RED_LIGHT_B);
            offset += RED_LIGHT_B;

            byte green = (byte) storage.readBits(offset, GREEN_LIGHT_B);
            offset += GREEN_LIGHT_B;

            byte blue = (byte) storage.readBits(offset, BLUE_LIGHT_B);

            BlockBase blockBase = chunkProtoMesh.getParent().getBlockAt(localX, localY, localZ);
            BlockModelType.BlockFace blockFaceDefinition = blockBase.getModel().getBlockModelType().getBlockFace(faceDir).getFirst();
            ResourceLocation textureName = blockBase.getModel().getTextureOfFace(blockBase.getModel().getBlockModelType().getNameOfFace(blockFaceDefinition));

            TextureRegion region = null;
            if (textureName != null) {
                region = textureAtlas.findRegion(textureName.toString());
            }

            float uStart = region.getU(), vStart = region.getV();

            writeFaceToInstances(
                    floatBuffer, faceDir,
                    (int) (uStart * blockAtlasSize), (int) (vStart * blockAtlasSize),
                    localX, localY, localZ,
                    ambientOcclusion,
                    sky, red, green, blue,
                    offsetXInBlocks, offsetYInBlocks, offsetZInBlocks
            );
        }
    }

    private void writeFaceToInstances(
            FloatArray instanceBuffer,
            Direction faceDir,
            int u, int v,
            byte localX, byte localY, byte localZ,
            byte aoPacked, byte skyLight, byte redLight, byte greenLight, byte blueLight,
            int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks
    ) {
        int meshX = localX + offsetXInBlocks;
        int meshY = localY + offsetYInBlocks;
        int meshZ = localZ + offsetZInBlocks;

        int offset = 0;
        float packedCoordsAndAO = BitPackingUtil.packToFloat(offset, meshX, 8);
        offset += 8;

        packedCoordsAndAO = BitPackingUtil.packToFloat(packedCoordsAndAO, offset, meshY, 8);
        offset += 8;

        packedCoordsAndAO = BitPackingUtil.packToFloat(packedCoordsAndAO, offset, meshZ, 8);
        offset += 8;

        packedCoordsAndAO = BitPackingUtil.packToFloat(packedCoordsAndAO, offset, aoPacked, AO_SIZE_BITS);

        offset = 0;

        float packedDirUVAndLight = BitPackingUtil.packToFloat(offset, faceDir.getId(), DIRECTION_ID_SIZE_BITS);
        offset += DIRECTION_ID_SIZE_BITS;

        packedDirUVAndLight = BitPackingUtil.packToFloat(packedDirUVAndLight, offset, u, 6);
        offset += 6;

        packedDirUVAndLight = BitPackingUtil.packToFloat(packedDirUVAndLight, offset, v, 6);
        offset += 6;

        packedDirUVAndLight = BitPackingUtil.packToFloat(packedDirUVAndLight, offset, skyLight, SKY_LIGHT_B);
        offset += SKY_LIGHT_B;

        packedDirUVAndLight = BitPackingUtil.packToFloat(packedDirUVAndLight, offset, redLight, RED_LIGHT_B);
        offset += RED_LIGHT_B;

        packedDirUVAndLight = BitPackingUtil.packToFloat(packedDirUVAndLight, offset, greenLight, GREEN_LIGHT_B);
        offset += GREEN_LIGHT_B;

        packedDirUVAndLight = BitPackingUtil.packToFloat(packedDirUVAndLight, offset, blueLight, BLUE_LIGHT_B);
        offset += BLUE_LIGHT_B;

        instanceBuffer.add(packedCoordsAndAO);
        instanceBuffer.add(packedDirUVAndLight);
    }

    private void writeCornerPositionToBuffer(FloatArray vertices, float[][] corners, int cornerIndex, byte aoPacked, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks) {
        int x = (int) corners[cornerIndex][0] + offsetXInBlocks;
        int y = (int) corners[cornerIndex][1] + offsetYInBlocks;
        int z = (int) corners[cornerIndex][2] + offsetZInBlocks;


        byte aoAtCorner = LightUtil.unpackAo(aoPacked, cornerIndex);

        float packedPositionAndAo = SingleBlockFace.packPositionAndAOForCorner(x, y, z, aoAtCorner);

        vertices.add(packedPositionAndAo);
    }

    private void writeCornerUVAndLightToBuffer(
            FloatArray vertices,
            float u, float v,
            float uLen, float vLen,
            float uStart, float vStart,
            float tileU, float tileV,
            byte sky, byte red, byte green, byte blue
    ) {
        float localU = u / uLen;
        float localV = v / vLen;

        float atlasU = uStart + localU * tileU;
        float atlasV = vStart + localV * tileV;

        float packed = SingleBlockFace.packTileUVAndLightsForCorner(TextureAtlasManager.getInstance().getBlockTextureAtlasSize(), TextureAtlasManager.getInstance().getBlockTextureSize(), atlasU, atlasV,
                sky, red, green, blue
        );
        vertices.add(packed);
    }

    private int getBitSizePerFace(byte localXByteSize, byte localYByteSize, byte localZByteSize) {
        return localXByteSize
                + localYByteSize
                + localZByteSize
                + DIRECTION_ID_SIZE_BITS
                + AO_SIZE_BITS
                + SKY_LIGHT_B + RED_LIGHT_B + GREEN_LIGHT_B + BLUE_LIGHT_B;
    }
}
