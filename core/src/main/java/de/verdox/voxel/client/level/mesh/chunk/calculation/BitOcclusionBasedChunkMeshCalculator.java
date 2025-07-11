package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.chunk.ChunkOccupancyMask;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.BlockRenderer;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.util.Direction;

public class BitOcclusionBasedChunkMeshCalculator implements ChunkMeshCalculator {

    @Override
    public BlockFaceStorage calculateChunkMesh(ClientChunk chunk) {

        if (chunk.isEmpty()) {
            return null;
        }

        int chunkSizeX = chunk.getWorld().getChunkSizeX();
        int chunkSizeY = chunk.getWorld().getChunkSizeY();
        int chunkSizeZ = chunk.getWorld().getChunkSizeZ();

        ChunkOccupancyMask com = chunk.getOccupancyMask();
        com.initOccupancyMask();
        ChunkOccupancyMask.FaceMasks masks = com.computeFaceMasks();

        BlockFaceStorage result = new BlockFaceStorage(chunkSizeX, chunkSizeY, chunkSizeZ);

        ChunkOccupancyMask[] neighborMasks = new ChunkOccupancyMask[Direction.values().length];

        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];

            extractNeighborOccupancyMask(chunk, direction, neighborMasks);

            long[][] directionOcclusionMask = masks.getMaskForDirection(direction);

            for (int localX = 0; localX < chunkSizeX; localX++) {
                for (int localY = 0; localY < chunkSizeY; localY++) {

                    long occlusionBitsAtCoordinates = directionOcclusionMask[localX][localY];
                    calculateFacesForCoordinates(chunk, result, directionOcclusionMask, neighborMasks, direction, localX, localY, occlusionBitsAtCoordinates, chunkSizeX, chunkSizeY, chunkSizeZ);
                }
            }
        }

        return result;
    }

    protected void calculateFacesForCoordinates(ClientChunk chunk, BlockFaceStorage result, long[][] directionOcclusionMask, ChunkOccupancyMask[] neighborMasks, Direction faceDir, int localX, int localY, long occlusionBitsAtCoordinates, int chunkSizeX, int chunkSizeY, int chunkSizeZ) {
        while (occlusionBitsAtCoordinates != 0L) {
            int localZ = Long.numberOfTrailingZeros(occlusionBitsAtCoordinates);
            occlusionBitsAtCoordinates &= ~(1L << localZ);

            boolean atBoundary = isAtBoundary(faceDir.getId(), localX, chunkSizeX, localY, chunkSizeY, localZ, chunkSizeZ);

            if (atBoundary) {
                ChunkOccupancyMask neighborMaskForDirection = neighborMasks[faceDir.getId()];

                int localXInNeighborChunk = (localX + faceDir.getOffsetX() + chunkSizeX) % chunkSizeX;
                int localYInNeighborChunk = (localY + faceDir.getOffsetY() + chunkSizeY) % chunkSizeY;
                int localZInNeighborChunk = (localZ + faceDir.getOffsetZ() + chunkSizeZ) % chunkSizeZ;

                // If the chunk is not loaded we treat it as fully occluded.
                if (neighborMaskForDirection == null || neighborMaskForDirection.isOpaque(localXInNeighborChunk, localYInNeighborChunk, localZInNeighborChunk)) {
                    continue;
                }
            }

            BlockBase block = chunk.getBlockAt(localX, localY, localZ);

            var faces = block.getModel().getBlockModelType().findByNormal(faceDir.getOffsetX(), faceDir.getOffsetY(), faceDir.getOffsetZ());
            for (var face : faces) {
                ResourceLocation tex = block == Blocks.AIR ? null : block.getModel().getTextureOfFace(block.getModel().getBlockModelType().getNameOfFace(face));
                result.addFace(BlockRenderer.generateBlockFace(tex, face, localX, localY, localZ));
            }
        }
    }

    private boolean isAtBoundary(int faceDir, int localX, int chunkSizeX, int localY, int chunkSizeY, int localZ, int chunkSizeZ) {
        return (faceDir == 0 && localX == 0)
            || (faceDir == 1 && localX == chunkSizeX - 1)
            || (faceDir == 2 && localY == 0)
            || (faceDir == 3 && localY == chunkSizeY - 1)
            || (faceDir == 4 && localZ == 0)
            || (faceDir == 5 && localZ == chunkSizeZ - 1);
    }
    private static void extractNeighborOccupancyMask(ClientChunk chunk, Direction faceDir, ChunkOccupancyMask[] neighborMasks) {
        int relativeChunkX = chunk.getChunkX() + faceDir.getOffsetX();
        int relativeChunkY = chunk.getChunkY() + faceDir.getOffsetY();
        int relativeChunkZ = chunk.getChunkZ() + faceDir.getOffsetZ();
        ClientChunk relativeChunk = chunk.getWorld().getChunk(relativeChunkX, relativeChunkY, relativeChunkZ);
        if (relativeChunk != null) {
            neighborMasks[faceDir.getId()] = relativeChunk.getOccupancyMask();
        }
    }
}
