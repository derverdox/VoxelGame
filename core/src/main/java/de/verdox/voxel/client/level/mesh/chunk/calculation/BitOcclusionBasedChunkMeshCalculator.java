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
    public BlockFaceStorage calculateChunkMesh(BlockFaceStorage blockFaces, ClientChunk chunk, float chunkOffsetX, float chunkOffsetY, float chunkOffsetZ) {
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

            for (int lightLookupX = 0; lightLookupX < sx; lightLookupX++) {
                for (int lightLookupY = 0; lightLookupY < sy; lightLookupY++) {
                    long zColumn = occupancyMask.getZColumn(lightLookupX, lightLookupY);

                    long bits = switch (d) {
                        case EAST ->
                                zColumn & ~((lightLookupX + 1 < sx ? occupancyMask.getZColumn(lightLookupX + 1, lightLookupY) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(0, lightLookupY) : 0L)));
                        case WEST ->
                                zColumn & ~((lightLookupX - 1 >= 0 ? occupancyMask.getZColumn(lightLookupX - 1, lightLookupY) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(sx - 1, lightLookupY) : 0L)));
                        case UP ->
                                zColumn & ~((lightLookupY + 1 < sy ? occupancyMask.getZColumn(lightLookupX, lightLookupY + 1) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(lightLookupX, 0) : 0L)));
                        case DOWN ->
                                zColumn & ~((lightLookupY - 1 >= 0 ? occupancyMask.getZColumn(lightLookupX, lightLookupY - 1) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(lightLookupX, sy - 1) : 0L)));
                        case SOUTH -> zColumn & ~(zColumn >>> 1);
                        case NORTH -> zColumn & ~(zColumn << 1);
                    };

                    // emit faces for each set bit in bits
                    while (bits != 0L) {
                        int lightLookupZ = Long.numberOfTrailingZeros(bits);
                        bits &= ~(1L << lightLookupZ);

                        // boundary neighbor check: if at chunk edge, consult nOcc
                        if ((lightLookupX + dx < 0 || lightLookupX + dx >= sx || lightLookupY + dy < 0 || lightLookupY + dy >= sy || lightLookupZ + dz < 0 || lightLookupZ + dz >= sz) && (neighborOcclusionMap == null || ((neighborOcclusionMap.getZColumn(chunk.localX(lightLookupX + dx), chunk.localY(lightLookupY + dy)) >>> chunk.localZ(lightLookupZ + dz)) & 1L) != 0L)) {
                            continue;
                        }

                        // actually add face
                        BlockBase block = chunk.getBlockAt(lightLookupX, lightLookupY, lightLookupZ);
                        var faces = block.getModel()
                                .getBlockModelType()
                                .findByNormal(dx, dy, dz);

                        Direction direction = Direction.fromOffsets(dx, dy, dz);

                        int lodLevel = 0;
                        int lodStep = 1 << lodLevel;

                        boolean skip = false;
                        switch (direction) {
                            case UP:
                            case DOWN:
                                // Quad liegt in XZ-Ebene, Y konstant
                                if (lightLookupX % lodStep != 0 || lightLookupZ % lodStep != 0)
                                    skip = true;
                                break;
                            case NORTH:
                            case SOUTH:
                                // Quad liegt in XY-Ebene, Z konstant
                                if (lightLookupX % lodStep != 0 || lightLookupY % lodStep != 0)
                                    skip = true;
                                break;
                            case EAST:
                            case WEST:
                                // Quad liegt in YZ-Ebene, X konstant
                                if (lightLookupY % lodStep != 0 || lightLookupZ % lodStep != 0)
                                    skip = true;
                                break;
                        }
                        if (skip) continue;

                        for (var face : faces) {

                            ResourceLocation name = block == Blocks.AIR ? null
                                    : block.getModel().getTextureOfFace(
                                    block.getModel()
                                            .getBlockModelType()
                                            .getNameOfFace(face));

                            int faceXInMesh = (int) (lightLookupX + chunkOffsetX);
                            int faceYInMesh = (int) (lightLookupY + chunkOffsetY);
                            int faceZInMesh = (int) (lightLookupZ + chunkOffsetZ);


                            blockFaces.addFace(BlockRenderer.generateBlockFace(chunk, name, face,
                                    lightLookupX,
                                    lightLookupY,
                                    lightLookupZ,
                                    faceXInMesh,
                                    faceYInMesh,
                                    faceZInMesh,
                                    lodLevel)
                            );
                        }
                    }
                }
            }
        }
        return blockFaces;
    }
}
