package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.shared.data.types.BlockModelTypes;
import de.verdox.voxel.shared.data.types.BlockModels;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.block.BlockModel;
import de.verdox.voxel.shared.level.block.BlockModelType;

import java.util.List;

public class NaiveChunkMeshCalculator implements ChunkMeshCalculator {
    @Override
    public void calculateChunkMesh(TerrainFaceStorage.ChunkFaceStorage blockFaces, ClientChunk chunk, int lodLevel) {
        if (chunk.isEmpty()) {
            return;
        }
        int chunkSizeX = chunk.getWorld().getChunkSizeX();
        int chunkSizeY = chunk.getWorld().getChunkSizeY();
        int chunkSizeZ = chunk.getWorld().getChunkSizeZ();

        for (int x = 0; x < chunkSizeX; x++) {
            for (int y = 0; y < chunkSizeY; y++) {
                for (int z = 0; z < chunkSizeZ; z++) {
                    BlockBase block = chunk.getBlockAt(x, y, z);
                    if (block == Blocks.AIR) continue;
                    drawBlock(block, chunk, x, y, z, blockFaces, lodLevel);
                }
            }
        }
    }

    private void drawBlock(BlockBase blockBase, ClientChunk chunk, int blockXInMesh, int blockYInMesh, int blockZInMesh, TerrainFaceStorage.ChunkFaceStorage blockFaceStorage, int lodLevel) {
        paintFaceIfVisible(blockBase, chunk, blockXInMesh, blockYInMesh, blockZInMesh, 0, 0, -1, blockFaceStorage, lodLevel);
        paintFaceIfVisible(blockBase, chunk, blockXInMesh, blockYInMesh, blockZInMesh, 0, 0, +1, blockFaceStorage, lodLevel);
        paintFaceIfVisible(blockBase, chunk, blockXInMesh, blockYInMesh, blockZInMesh, 0, +1, 0, blockFaceStorage, lodLevel);
        paintFaceIfVisible(blockBase, chunk, blockXInMesh, blockYInMesh, blockZInMesh, 0, -1, 0, blockFaceStorage, lodLevel);
        paintFaceIfVisible(blockBase, chunk, blockXInMesh, blockYInMesh, blockZInMesh, -1, 0, 0, blockFaceStorage, lodLevel);
        paintFaceIfVisible(blockBase, chunk, blockXInMesh, blockYInMesh, blockZInMesh, +1, 0, 0, blockFaceStorage, lodLevel);
    }

    private void paintFaceIfVisible(BlockBase blockBase, ClientChunk chunk,
                                    int localX, int localY, int localZ,
                                    int relativeX, int relativeY, int relativeZ,
                                    TerrainFaceStorage.ChunkFaceStorage faces, int lodLevel) {

        BlockModel blockModel = blockBase.equals(Blocks.AIR) ? null : BlockModels.STONE;
        if (blockModel == null) {
            return;
        }

        int neighbourX = localX + relativeX;
        int neighbourY = localY + relativeY;
        int neighbourZ = localZ + relativeZ;

        ClientWorld world = chunk.getWorld();
        int sizeX = world.getChunkSizeX();
        int sizeY = world.getChunkSizeY();
        int sizeZ = world.getChunkSizeZ();

        BlockBase neighbour;
        if (neighbourX < 0 || neighbourX >= sizeX ||
                neighbourY < 0 || neighbourY >= sizeY ||
                neighbourZ < 0 || neighbourZ >= sizeZ) {

            int deltaChunkX = (neighbourX < 0 ? -1 : neighbourX >= sizeX ? 1 : 0);
            int deltaChunkY = (neighbourY < 0 ? -1 : neighbourY >= sizeY ? 1 : 0);
            int deltaChunkZ = (neighbourZ < 0 ? -1 : neighbourZ >= sizeZ ? 1 : 0);

            int relativeChunkX = chunk.getChunkX() + deltaChunkX;
            int relativeChunkY = chunk.getChunkY() + deltaChunkY;
            int relativeChunkZ = chunk.getChunkZ() + deltaChunkZ;

            neighbourX = Math.floorMod(neighbourX, sizeX);
            neighbourY = Math.floorMod(neighbourY, sizeY);
            neighbourZ = Math.floorMod(neighbourZ, sizeZ);

            ClientChunk adjacent = world.getChunk(relativeChunkX, relativeChunkY, relativeChunkZ);

            if (adjacent != null) {
                neighbour = adjacent.getBlockAt(neighbourX, neighbourY, neighbourZ);
            } else {
                // Chunk noch nicht generiert oder außerhalb der Welt → Stone
                neighbour = Blocks.STONE;
            }
        } else {
            neighbour = chunk.getBlockAt(neighbourX, neighbourY, neighbourZ);
        }
        BlockModelType neighbourModel = neighbour.equals(Blocks.AIR) ? null : BlockModelTypes.CUBE;

        boolean shouldPaint = true;

        for (BlockModelType.BlockFace relevantBlockFace : BlockModelTypes.CUBE.findByNormal(relativeX, relativeY, relativeZ)) {
            boolean canBeOccluded = relevantBlockFace.canBeOccluded();


            if (neighbourModel != null && canBeOccluded) {
                List<BlockModelType.BlockFace> opposingBlockFaces = relevantBlockFace.findOpposingBlockFaces(neighbourModel);

                for (BlockModelType.BlockFace opposingBlockFace : opposingBlockFaces) {
                    if (opposingBlockFace.canBeOccluded() && opposingBlockFace.isLargerOrEqual(relevantBlockFace)) {
                        shouldPaint = false;
                        break;
                    }
                }
            }

            if (!shouldPaint) {
                continue;
            }

            String nameOfBlockFace = blockModel.getBlockModelType().getNameOfFace(relevantBlockFace);

            faces.generateFace(
                    chunk, blockModel.getTextureOfFace(nameOfBlockFace), relevantBlockFace, (byte) lodLevel,
                    localX, localY, localZ
            );
        }
    }
}
