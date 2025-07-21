package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.client.level.mesh.block.BlockRenderer;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.LightUtil;

public class BitOcclusionBasedChunkMeshCalculator implements ChunkMeshCalculator {

    @Override
    public BlockFaceStorage calculateChunkMesh(BlockFaceStorage blockFaces, ClientChunk chunk, float offsetX, float offsetY, float offsetZ) {
        if (chunk.isEmpty()) {
            return null;
        }

        int sx = chunk.getWorld().getChunkSizeX();
        int sy = chunk.getWorld().getChunkSizeY();
        int sz = chunk.getWorld().getChunkSizeZ();

        OccupancyMask occupancyMask = chunk.getChunkOccupancyMask();

        OccupancyMask[] neighOcc = new OccupancyMask[6];
        for (int i = 0; i < Direction.values().length; i++) {
            Direction d = Direction.values()[i];
            ClientChunk nc = chunk.getWorld().getChunk(chunk.getChunkX() + d.getOffsetX(), chunk.getChunkY() + d.getOffsetY(), chunk.getChunkZ() + d.getOffsetZ());
            if (nc != null) {
                neighOcc[i] = nc.getChunkOccupancyMask();
            } else {
                neighOcc[i] = null;
            }
        }

        // 3) Main mesh loop with early‐outs and inline bit‐ops
        for (int dirId = 0; dirId < Direction.values().length; dirId++) {
            // skip entire face‐direction if fully occluded
            /*            if ((occupancyMask.getSideMask() & (1L << dirId)) != 0) continue;*/

            Direction d = Direction.values()[dirId];
            OccupancyMask neighborOcclusionMap = neighOcc[dirId];
            int dx = d.getOffsetX(), dy = d.getOffsetY(), dz = d.getOffsetZ();

            for (int localX = 0; localX < sx; localX++) {
                for (int localY = 0; localY < sy; localY++) {
                    long zColumn = occupancyMask.getZColumn(localX, localY);

                    long bits = switch (d) {
                        case EAST ->
                                zColumn & ~((localX + 1 < sx ? occupancyMask.getZColumn(localX + 1, localY) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(0, localY) : 0L)));
                        case WEST ->
                                zColumn & ~((localX - 1 >= 0 ? occupancyMask.getZColumn(localX - 1, localY) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(sx - 1, localY) : 0L)));
                        case UP ->
                                zColumn & ~((localY + 1 < sy ? occupancyMask.getZColumn(localX, localY + 1) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(localX, 0) : 0L)));
                        case DOWN ->
                                zColumn & ~((localY - 1 >= 0 ? occupancyMask.getZColumn(localX, localY - 1) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(localX, sy - 1) : 0L)));
                        case SOUTH -> zColumn & ~(zColumn >>> 1);
                        case NORTH -> zColumn & ~(zColumn << 1);
                    };

                    // emit faces for each set bit in bits
                    while (bits != 0L) {
                        int localZ = Long.numberOfTrailingZeros(bits);
                        bits &= ~(1L << localZ);

                        // boundary neighbor check: if at chunk edge, consult nOcc
                        if ((localX + dx < 0 || localX + dx >= sx || localY + dy < 0 || localY + dy >= sy || localZ + dz < 0 || localZ + dz >= sz) && (neighborOcclusionMap == null || ((neighborOcclusionMap.getZColumn(chunk.localX(localX + dx), chunk.localY(localY + dy)) >>> chunk.localZ(localZ + dz)) & 1L) != 0L)) {
                            continue;
                        }

                        // actually add face
                        BlockBase block = chunk.getBlockAt(localX, localY, localZ);
                        var faces = block.getModel()
                                .getBlockModelType()
                                .findByNormal(dx, dy, dz);

                        Direction direction = Direction.fromOffsets(dx, dy, dz);

                        for (var face : faces) {

                            face.c1();

                            ResourceLocation name = block == Blocks.AIR ? null
                                    : block.getModel().getTextureOfFace(
                                    block.getModel()
                                            .getBlockModelType()
                                            .getNameOfFace(face));
                            blockFaces.addFace(BlockRenderer.generateBlockFace(chunk, name, face, localX, localY, localZ, (int) (localX + offsetX), (int) (localY + offsetY), (int) (localZ + offsetZ)));
                        }
                    }
                }
            }
        }
        return blockFaces;
    }

    private float getLightValueAt(Direction direction, int localX, int localY, int localZ, ChunkBase<?> chunkToAsk) {

        int relX = direction.getOffsetX() + localX;
        int relY = direction.getOffsetY() + localY;
        int relZ = direction.getOffsetZ() + localZ;

        // Get left neighbor
        if (
                relX < 0 || relX >= chunkToAsk.getBlockSizeX() ||
                        relY < 0 || relY >= chunkToAsk.getBlockSizeY() ||
                        relZ < 0 || relZ >= chunkToAsk.getBlockSizeZ()
        ) {
            chunkToAsk = chunkToAsk.getWorld().getChunkNow(chunkToAsk.getChunkX() + (direction.getOffsetX()), chunkToAsk.getChunkY() + (direction.getOffsetY()), chunkToAsk.getChunkZ() + (direction.getOffsetZ()));
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
}
