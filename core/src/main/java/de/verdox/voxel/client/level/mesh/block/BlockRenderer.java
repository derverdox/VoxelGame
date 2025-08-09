package de.verdox.voxel.client.level.mesh.block;

import com.esotericsoftware.kryo.util.Null;
import de.verdox.voxel.client.level.chunk.proto.ChunkProtoMesh;
import de.verdox.voxel.client.level.mesh.block.face.BlockFace;
import de.verdox.voxel.client.level.mesh.block.face.SingleBlockFace;
import de.verdox.voxel.client.level.chunk.proto.ProtoMask;
import de.verdox.voxel.client.level.chunk.proto.ProtoMasks;
import de.verdox.voxel.client.level.chunk.RenderableChunk;
import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.client.level.mesh.TerrainManager;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.LightUtil;

public class BlockRenderer {

    public static void saveBlockFaceToProtoMesh(
            TerrainManager terrainManager,
            RenderableChunk chunk, BlockModelType.BlockFace blockFace, byte lodLevel,
            int localX, int localY, int localZ
    ) {

        float lightPacked = getLightValueAt(terrainManager, chunk, blockFace.direction(), localX, localY, localZ, lodLevel);

        byte c1Ao = computeCornerOcclusion(terrainManager, chunk, blockFace.direction(), blockFace.c1(), localX, localY, localZ, lodLevel);
        byte c2Ao = computeCornerOcclusion(terrainManager, chunk, blockFace.direction(), blockFace.c2(), localX, localY, localZ, lodLevel);
        byte c3Ao = computeCornerOcclusion(terrainManager, chunk, blockFace.direction(), blockFace.c3(), localX, localY, localZ, lodLevel);
        byte c4Ao = computeCornerOcclusion(terrainManager, chunk, blockFace.direction(), blockFace.c4(), localX, localY, localZ, lodLevel);

        byte skyLight = LightUtil.unpackSkyFromFloat(lightPacked);
        byte redLight = LightUtil.unpackRedFromFloat(lightPacked);
        byte greenLight = LightUtil.unpackGreenFromFloat(lightPacked);
        byte blueLight = LightUtil.unpackBlueFromFloat(lightPacked);

        byte aoPacked = LightUtil.packAo(c1Ao, c2Ao, c3Ao, c4Ao);
        ChunkProtoMesh chunkProtoMesh = chunk.getChunkProtoMesh();
        ProtoMasks.SINGLE_PER_FACE.storeFace(chunkProtoMesh, ProtoMask.Type.OPAQUE, (byte) localX, (byte) localY, (byte) localZ, blockFace.direction(), aoPacked, skyLight, redLight, greenLight, blueLight);
    }

    public static BlockFace generateBlockFace(
            TerrainManager terrainManager,
            RenderableChunk chunk, @Null ResourceLocation textureKey, BlockModelType.BlockFace blockFace, byte lodLevel,
            int localX, int localY, int localZ
    ) {

        float lightPacked = getLightValueAt(terrainManager, chunk, blockFace.direction(), localX, localY, localZ, lodLevel);

        byte c1Ao = computeCornerOcclusion(terrainManager, chunk, blockFace.direction(), blockFace.c1(), localX, localY, localZ, lodLevel);
        byte c2Ao = computeCornerOcclusion(terrainManager, chunk, blockFace.direction(), blockFace.c2(), localX, localY, localZ, lodLevel);
        byte c3Ao = computeCornerOcclusion(terrainManager, chunk, blockFace.direction(), blockFace.c3(), localX, localY, localZ, lodLevel);
        byte c4Ao = computeCornerOcclusion(terrainManager, chunk, blockFace.direction(), blockFace.c4(), localX, localY, localZ, lodLevel);

        byte aoPacked = LightUtil.packAo(c1Ao, c2Ao, c3Ao, c4Ao);

        return new SingleBlockFace(
                blockFace,
                (byte) localX, (byte) localY, (byte) localZ,
                textureKey,
                lightPacked,
                aoPacked
        );
    }

    private static float getLightValueAt(TerrainManager terrainManager, Chunk chunkToAsk, Direction direction, int localX, int localY, int localZ, int lodLevel) {
        int relX = direction.getOffsetX() + localX;
        int relY = direction.getOffsetY() + localY;
        int relZ = direction.getOffsetZ() + localZ;

        if (
                relX < 0 || relX >= chunkToAsk.getSizeX() ||
                        relY < 0 || relY >= chunkToAsk.getSizeY() ||
                        relZ < 0 || relZ >= chunkToAsk.getSizeZ()
        ) {
            chunkToAsk = chunkToAsk.getWorld().getChunkNow(chunkToAsk.getChunkX() + (direction.getOffsetX()), chunkToAsk.getChunkY() + (direction.getOffsetY()), chunkToAsk.getChunkZ() + (direction.getOffsetZ()));
        }
        if (chunkToAsk == null) {
            return LightUtil.packLightToFloat((byte) 15, (byte) 0, (byte) 0, (byte) 0);
        }
        if (lodLevel > 0 && chunkToAsk instanceof TerrainChunk clientChunk) {
            chunkToAsk = clientChunk.getLodChunk(lodLevel);
        }
        relX = chunkToAsk.localX(relX);
        relY = chunkToAsk.localY(relY);
        relZ = chunkToAsk.localZ(relZ);

        var skyLight = chunkToAsk.getChunkLightData().getSkyLight(relX, relY, relZ);
        var blockRed = chunkToAsk.getChunkLightData().getBlockRed(relX, relY, relZ);
        var blockGreen = chunkToAsk.getChunkLightData().getBlockGreen(relX, relY, relZ);
        var blockBlue = chunkToAsk.getChunkLightData().getBlockBlue(relX, relY, relZ);
        return LightUtil.packLightToFloat(skyLight, blockRed, blockGreen, blockBlue);
    }

    private static byte computeCornerOcclusion(TerrainManager terrainManager, Chunk chunkToAsk, Direction directionOfFace, BlockModelType.BlockFace.BlockModelCoordinate coordinate, int localX, int localY, int localZ, int lodLevel) {
        int xToSearch;
        int yToSearch;
        int zToSearch;

        boolean occ1 = false;
        boolean occ2 = false;
        boolean occ3 = false;

        // Left or right
        if (directionOfFace.getOffsetX() != 0) {
            yToSearch = coordinate.y() > 0 ? 1 : -1;
            zToSearch = coordinate.z() > 0 ? 1 : -1;

            int relY = yToSearch + localY;
            int relZ = zToSearch + localZ;

            occ1 = isOccludedAt(terrainManager, chunkToAsk, localX + directionOfFace.getOffsetX(), relY, localZ, lodLevel);
            occ2 = isOccludedAt(terrainManager, chunkToAsk, localX + directionOfFace.getOffsetX(), localY, relZ, lodLevel);
            occ3 = isOccludedAt(terrainManager, chunkToAsk, localX + directionOfFace.getOffsetX(), relY, relZ, lodLevel);
        }
        // Up or down
        else if (directionOfFace.getOffsetY() != 0) {
            xToSearch = coordinate.x() > 0 ? 1 : -1;
            zToSearch = coordinate.z() > 0 ? 1 : -1;

            int relX = xToSearch + localX;
            int relZ = zToSearch + localZ;

            occ1 = isOccludedAt(terrainManager, chunkToAsk, relX, localY + directionOfFace.getOffsetY(), localZ, lodLevel);
            occ2 = isOccludedAt(terrainManager, chunkToAsk, localX, localY + directionOfFace.getOffsetY(), relZ, lodLevel);
            occ3 = isOccludedAt(terrainManager, chunkToAsk, relX, localY + directionOfFace.getOffsetY(), relZ, lodLevel);
        }
        // Front or back
        else if (directionOfFace.getOffsetZ() != 0) {
            xToSearch = coordinate.x() > 0 ? 1 : -1;
            yToSearch = coordinate.y() > 0 ? 1 : -1;

            int relX = xToSearch + localX;
            int relY = yToSearch + localY;

            occ1 = isOccludedAt(terrainManager, chunkToAsk, relX, localY, localZ + directionOfFace.getOffsetZ(), lodLevel);
            occ2 = isOccludedAt(terrainManager, chunkToAsk, localX, relY, localZ + directionOfFace.getOffsetZ(), lodLevel);
            occ3 = isOccludedAt(terrainManager, chunkToAsk, relX, relY, localZ + directionOfFace.getOffsetZ(), lodLevel);
        }

        return (occ1 && occ2) ? 3 : (byte) ((occ1 ? 1 : 0) + (occ2 ? 1 : 0) + (occ3 ? 1 : 0));
    }

    private static boolean isOccludedAt(TerrainManager terrainManager, Chunk chunkToAsk, int relX, int relY, int relZ, int lodLevel) {
        int chunkOffsetX = relX < 0 ? -1 : relX >= chunkToAsk.getSizeX() ? 1 : 0;
        int chunkOffsetY = relY < 0 ? -1 : relY >= chunkToAsk.getSizeY() ? 1 : 0;
        int chunkOffsetZ = relZ < 0 ? -1 : relZ >= chunkToAsk.getSizeZ() ? 1 : 0;

        if (chunkOffsetX != 0 || chunkOffsetY != 0 || chunkOffsetZ != 0) {
            chunkToAsk = terrainManager.getChunkNow(chunkToAsk.getChunkX() + chunkOffsetX,
                    chunkToAsk.getChunkY() + chunkOffsetY,
                    chunkToAsk.getChunkZ() + chunkOffsetZ);
        }
        if (chunkToAsk == null) {
            return false;
        }
        if (lodLevel > 0 && chunkToAsk instanceof TerrainChunk clientChunk) {
            chunkToAsk = clientChunk.getLodChunk(lodLevel);
        }

        relX = chunkToAsk.localX(relX);
        relY = chunkToAsk.localY(relY);
        relZ = chunkToAsk.localZ(relZ);
        return chunkToAsk.getBlockAt(relX, relY, relZ).isOpaque();
    }
}
