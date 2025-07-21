package de.verdox.voxel.client.level.mesh.block;

import com.esotericsoftware.kryo.util.Null;
import de.verdox.voxel.client.level.mesh.block.face.BlockFace;
import de.verdox.voxel.client.level.mesh.block.face.SingleBlockFace;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.LightUtil;

public class BlockRenderer {
    public static BlockFace generateBlockFace(ChunkBase<?> chunk, @Null ResourceLocation textureKey, BlockModelType.BlockFace blockFace, int localX, int localY, int localZ, int blockXInMesh, int blockYInMesh, int blockZInMesh) {

        float lightPacked = getLightValueAt(chunk, blockFace.direction(), localX, localY, localZ);
        byte c1Ao = computeCornerOcclusion(chunk, blockFace.direction(), blockFace.c1(), localX, localY, localZ);
        byte c2Ao = computeCornerOcclusion(chunk, blockFace.direction(), blockFace.c2(), localX, localY, localZ);
        byte c3Ao = computeCornerOcclusion(chunk, blockFace.direction(), blockFace.c3(), localX, localY, localZ);
        byte c4Ao = computeCornerOcclusion(chunk, blockFace.direction(), blockFace.c4(), localX, localY, localZ);

        byte aoPacked = LightUtil.packAo(c1Ao, c2Ao, c3Ao, c4Ao);

        // Pack all 4 values into one byte

        return new SingleBlockFace(
                blockFace,
                (byte) blockXInMesh, (byte) blockYInMesh, (byte) blockZInMesh,
                textureKey,
                lightPacked,
                aoPacked
        );
    }

    private static float getLightValueAt(ChunkBase<?> chunkToAsk, Direction direction, int localX, int localY, int localZ) {
        int relX = direction.getOffsetX() + localX;
        int relY = direction.getOffsetY() + localY;
        int relZ = direction.getOffsetZ() + localZ;

        if (
                relX < 0 || relX >= chunkToAsk.getBlockSizeX() ||
                        relY < 0 || relY >= chunkToAsk.getBlockSizeY() ||
                        relZ < 0 || relZ >= chunkToAsk.getBlockSizeZ()
        ) {
            chunkToAsk = chunkToAsk.getWorld().getChunkNow(chunkToAsk.getChunkX() + (direction.getOffsetX()), chunkToAsk.getChunkY() + (direction.getOffsetY()), chunkToAsk.getChunkZ() + (direction.getOffsetZ()));
        }
        if (chunkToAsk == null) {
            return LightUtil.packLightToFloat((byte) 15, (byte) 0, (byte) 0, (byte) 0);
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

    private static byte computeCornerOcclusion(ChunkBase<?> chunkToAsk, Direction directionOfFace, BlockModelType.BlockFace.RelativeCoordinate coordinate, int localX, int localY, int localZ) {
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

            occ1 = isOccludedAt(chunkToAsk, localX + directionOfFace.getOffsetX(), relY, localZ);
            occ2 = isOccludedAt(chunkToAsk, localX + directionOfFace.getOffsetX(), localY, relZ);
            occ3 = isOccludedAt(chunkToAsk, localX + directionOfFace.getOffsetX(), relY, relZ);
        }
        // Up or down
        else if (directionOfFace.getOffsetY() != 0) {
            xToSearch = coordinate.x() > 0 ? 1 : -1;
            zToSearch = coordinate.z() > 0 ? 1 : -1;

            int relX = xToSearch + localX;
            int relZ = zToSearch + localZ;

            occ1 = isOccludedAt(chunkToAsk, relX, localY + directionOfFace.getOffsetY(), localZ);
            occ2 = isOccludedAt(chunkToAsk, localX, localY + directionOfFace.getOffsetY(), relZ);
            occ3 = isOccludedAt(chunkToAsk, relX, localY + directionOfFace.getOffsetY(), relZ);
        }
        // Front or back
        else if (directionOfFace.getOffsetZ() != 0) {
            xToSearch = coordinate.x() > 0 ? 1 : -1;
            yToSearch = coordinate.y() > 0 ? 1 : -1;

            int relX = xToSearch + localX;
            int relY = yToSearch + localY;

            occ1 = isOccludedAt(chunkToAsk, relX, localY, localZ + directionOfFace.getOffsetZ());
            occ2 = isOccludedAt(chunkToAsk, localX, relY, localZ + directionOfFace.getOffsetZ());
            occ3 = isOccludedAt(chunkToAsk, relX, relY, localZ + directionOfFace.getOffsetZ());
        }

        return (occ1 && occ2) ? 3 : (byte) ((occ1 ? 1 : 0) + (occ2 ? 1 : 0) + (occ3 ? 1 : 0));
    }

    private static boolean isOccludedAt(ChunkBase<?> chunkToAsk, int relX, int relY, int relZ) {
        int chunkOffsetX = relX < 0 ? -1 : relX >= chunkToAsk.getBlockSizeX() ? 1 : 0;
        int chunkOffsetY = relY < 0 ? -1 : relY >= chunkToAsk.getBlockSizeY() ? 1 : 0;
        int chunkOffsetZ = relZ < 0 ? -1 : relZ >= chunkToAsk.getBlockSizeZ() ? 1 : 0;

        if (chunkOffsetX != 0 || chunkOffsetY != 0 || chunkOffsetZ != 0) {
            chunkToAsk = chunkToAsk.getWorld().getChunkNow(
                    chunkToAsk.getChunkX() + chunkOffsetX,
                    chunkToAsk.getChunkY() + chunkOffsetY,
                    chunkToAsk.getChunkZ() + chunkOffsetZ
            );
        }
        if (chunkToAsk == null) {
            return false;
        }

        relX = chunkToAsk.localX(relX);
        relY = chunkToAsk.localY(relY);
        relZ = chunkToAsk.localZ(relZ);
        return chunkToAsk.getBlockAt(relX, relY, relZ).isOpaque();
    }
}
