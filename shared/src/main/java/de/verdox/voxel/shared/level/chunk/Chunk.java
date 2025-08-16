package de.verdox.voxel.shared.level.chunk;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.data.types.Registries;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.util.DelegateBase;
import de.verdox.voxel.shared.util.Direction;

public interface Chunk extends Box, DelegateBase<DelegateChunk> {
    void init();

    int getChunkX();

    int getChunkY();

    int getChunkZ();

    ChunkBlockPalette getChunkBlockPalette();

    HeightMap getHeightMap();

    DepthMap getDepthMap();

    ChunkLightData getChunkLightData();

    World getWorld();

    boolean isEmpty();

    boolean hasNeighborsToAllSides();

    <SELF extends Chunk> SELF getNeighborChunk(Direction direction);

    default BlockBase getBlockAt(int localX, int localY, int localZ) {
        return getChunkBlockPalette().get((short) localX, (short) localY, (short) localZ);
    }

    default void setBlockAt(BlockBase newBlock, int localX, int localY, int localZ) {
        boolean isEmptyBefore = isEmpty();
        boolean didHeightMapChange = false;

        getChunkBlockPalette().set((short) localX, (short) localY, (short) localZ, newBlock);

        HeightMap heightMap = getHeightMap();
        DepthMap depthMap = getDepthMap();

        if (!newBlock.equals(Blocks.AIR)) {

            if (heightMap != null) {
                byte maxHeight = heightMap.get(localX, localZ);
                if (localY > maxHeight) {
                    heightMap.set(localX, localZ, (byte) localY);
                    didHeightMapChange = true;
                }
            }


            if (depthMap != null) {
                byte minHeight = depthMap.get(localX, localZ);
                if (localY < minHeight) {
                    depthMap.set(localX, localZ, (byte) localY);
                    didHeightMapChange = true;
                }
            }
        } else {
            if (heightMap != null && heightMap.get(localX, localZ) == localY) {
                int newHeight = -1;

                // Find next solid block that is the highest that is not air

                for (int y = localY - 1; y >= 0; y--) {
                    BlockBase below = getBlockAt(localX, y, localZ);
                    if (!below.equals(Blocks.AIR)) {
                        newHeight = y;
                        break;
                    }
                }
                didHeightMapChange = true;
                heightMap.set(localX, localZ, (byte) Math.max(newHeight, 0));
            }

            if (depthMap != null && depthMap.get(localX, localZ) == localY) {
                int newDepth = -1;

                int chunkHeight = getSizeY();
                for (int y = localY + 1; y < chunkHeight; y++) {
                    BlockBase above = getBlockAt(localX, y, localZ);
                    if (!above.equals(Blocks.AIR)) {
                        newDepth = y;
                        break;
                    }
                }
                // wenn nichts gefunden: default auf 0
                didHeightMapChange = true;
                depthMap.set(localX, localZ, (byte) Math.max(newDepth, 0));
            }
        }

        for (int i = 0; i < getDelegates().size(); i++) {
            getDelegates().get(i).notifySetBlock(newBlock, localX, localY, localZ);
        }

        //getWorld().chunkUpdate(this, (byte) localX, (byte) localY, (byte) localZ, isEmptyBefore);
/*        if(didHeightMapChange) {
            getWorld().getChunkMap().notifyHeightmapChange(this);
        }*/
    }

    default long getChunkKey() {
        return computeChunkKey(getChunkX(), getChunkY(), getChunkZ());
    }

    default int localX(int globalX) {
        return Math.floorMod(globalX, getSizeX());
    }

    default int localY(int globalY) {
        return Math.floorMod(globalY, getSizeY());
    }

    default int localZ(int globalZ) {
        return Math.floorMod(globalZ, getSizeZ());
    }

    default int globalX(int localX) {
        return getChunkX() * getSizeX() + localX(localX);
    }

    default int globalY(int localY) {
        return getChunkY() * getSizeY() + localY(localY);
    }

    default int globalZ(int localZ) {
        return getChunkZ() * getSizeZ() + localZ(localZ);
    }

    static int chunkX(World world, int globalX) {
        return chunkX(world.getChunkSizeX(), globalX);
    }

    static int chunkX(int chunkSizeX, int globalX) {
        return Math.floorDiv(globalX, Math.max(1, chunkSizeX));
    }

    static int chunkY(World world, int globalY) {
        return chunkY(world.getChunkSizeY(), globalY);
    }

    static int chunkY(int chunkSizeY, int globalY) {
        return Math.floorDiv(globalY, Math.max(1, chunkSizeY));
    }

    static int chunkZ(World world, int globalZ) {
        return chunkZ(world.getChunkSizeZ(), globalZ);
    }

    static int chunkZ(int chunkSizeZ, int globalZ) {
        return Math.floorDiv(globalZ, Math.max(1, chunkSizeZ));
    }

    static long computeChunkKey(int chunkX, int chunkY, int chunkZ) {
        return (((long) chunkX & 0x1FFFFF) << 42)
                | (((long) chunkY & 0x1FFFFF) << 21)
                | (((long) chunkZ & 0x1FFFFF));
    }

    static int unpackChunkX(long key) {
        int x = (int) ((key >>> 42) & 0x1FFFFF);
        // Wenn Bit 20 = 1 → negative Zahl → nach oben hin auffüllen
        if ((x & (1 << 20)) != 0) {
            x |= ~0x1FFFFF;
        }
        return x;
    }

    static int unpackChunkY(long key) {
        int y = (int) ((key >>> 21) & 0x1FFFFF);
        if ((y & (1 << 20)) != 0) {
            y |= ~0x1FFFFF;
        }
        return y;
    }

    static int unpackChunkZ(long key) {
        int z = (int) (key & 0x1FFFFF);
        if ((z & (1 << 20)) != 0) {
            z |= ~0x1FFFFF;
        }
        return z;
    }

    default int getLocalXByteSize() {
        return Integer.SIZE - Integer.numberOfLeadingZeros(getSizeX() - 1);
    }

    default int getLocalYByteSize() {
        return Integer.SIZE - Integer.numberOfLeadingZeros(getSizeY() - 1);
    }

    default int getLocalZByteSize() {
        return Integer.SIZE - Integer.numberOfLeadingZeros(getSizeZ() - 1);
    }
}
