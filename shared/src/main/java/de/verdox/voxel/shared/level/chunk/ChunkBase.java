package de.verdox.voxel.shared.level.chunk;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.data.types.Registries;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;
import lombok.Getter;

@Getter
public abstract class ChunkBase<WORLD extends World<?>> implements Box {
    private final ChunkBlockPalette chunkBlockPalette;
    private final HeightMap heightmap;
    private final DepthMap depthMap;
    private final ChunkLightData chunkLightData;

    public ChunkBase(WORLD world, int chunkX, int chunkY, int chunkZ) {
        this(new ChunkBlockPalette(Blocks.AIR.findKey()), new HeightMap(), new DepthMap(), new ChunkLightData());
    }

    private ChunkBase(ChunkBlockPalette chunkBlockPalette, HeightMap heightMap, DepthMap depthMap, ChunkLightData chunkLightData) {
        this.chunkBlockPalette = chunkBlockPalette;
        this.heightmap = heightMap;
        this.depthMap = depthMap;
        this.chunkLightData = chunkLightData;

        this.chunkBlockPalette.setOwner(this);
        this.heightmap.setOwner(this);
        this.depthMap.setOwner(this);
        this.chunkLightData.setOwner(this);
    }

    public abstract int getChunkX();

    public abstract int getChunkY();

    public abstract int getChunkZ();

    public abstract WORLD getWorld();

    public boolean isEmpty() {
        return this.chunkBlockPalette.getPaletteSize() == 1;
    }

    public long getChunkKey() {
        return computeChunkKey(getChunkX(), getChunkY(), getChunkZ());
    }

    public void init() {

    }

    public BlockBase getBlockAt(int localX, int localY, int localZ) {
        ResourceLocation resourceLocation = chunkBlockPalette.get((short) localX, (short) localY, (short) localZ);
        return Registries.BLOCKS.get(resourceLocation);
    }

    public void setBlockAt(BlockBase newBlock, int localX, int localY, int localZ) {
        chunkBlockPalette.set((short) localX, (short) localY, (short) localZ, newBlock.findKey());

        if (!newBlock.equals(Blocks.AIR)) {

            if (heightmap != null) {
                byte maxHeight = heightmap.get(localX, localZ);
                if (localY > maxHeight) {
                    heightmap.set(localX, localZ, (byte) localY);
                }
            }


            if (depthMap != null) {
                byte minHeight = depthMap.get(localX, localZ);
                if (localY < minHeight) {
                    depthMap.set(localX, localZ, (byte) localY);
                }
            }
        } else {
            if (heightmap != null && heightmap.get(localX, localZ) == localY) {
                int newHeight = -1;

                // Find next solid block that is the highest that is not air

                for (int y = localY - 1; y >= 0; y--) {
                    BlockBase below = getBlockAt(localX, y, localZ);
                    if (!below.equals(Blocks.AIR)) {
                        newHeight = y;
                        break;
                    }
                }
                heightmap.set(localX, localZ, (byte) Math.max(newHeight, 0));
            }

            if (depthMap != null && depthMap.get(localX, localZ) == localY) {
                int newDepth = -1;

                int chunkHeight = getBlockSizeY();
                for (int y = localY + 1; y < chunkHeight; y++) {
                    BlockBase above = getBlockAt(localX, y, localZ);
                    if (!above.equals(Blocks.AIR)) {
                        newDepth = y;
                        break;
                    }
                }
                // wenn nichts gefunden: default auf 0

                depthMap.set(localX, localZ, (byte) Math.max(newDepth, 0));
            }
        }
    }

    public int getBlockSizeX() {
        return getWorld().getChunkSizeX();
    }

    public int getBlockSizeY() {
        return getWorld().getChunkSizeY();
    }

    public int getBlockSizeZ() {
        return getWorld().getChunkSizeZ();
    }

    public int localX(int globalX) {
        return Math.floorMod(globalX, getBlockSizeX());
    }

    public int localY(int globalY) {
        return Math.floorMod(globalY, getBlockSizeY());
    }

    public int localZ(int globalZ) {
        return Math.floorMod(globalZ, getBlockSizeZ());
    }

    public static int chunkX(World<?> world, int globalX) {
        return chunkX(world.getChunkSizeX(), globalX);
    }

    public static int chunkX(int chunkSizeX, int globalX) {
        return Math.floorDiv(globalX, Math.max(1, chunkSizeX));
    }

    public static int chunkY(World<?> world, int globalY) {
        return chunkY(world.getChunkSizeY(), globalY);
    }

    public static int chunkY(int chunkSizeY, int globalY) {
        return Math.floorDiv(globalY, Math.max(1, chunkSizeY));
    }

    public static int chunkZ(World<?> world, int globalZ) {
        return chunkZ(world.getChunkSizeZ(), globalZ);
    }

    public static int chunkZ(int chunkSizeZ, int globalZ) {
        return Math.floorDiv(globalZ, Math.max(1, chunkSizeZ));
    }

    public int globalX(int localX) {
        return getChunkX() * getBlockSizeX() + localX(localX);
    }

    public int globalY(int localY) {
        return getChunkY() * getBlockSizeY() + localY(localY);
    }

    public int globalZ(int localZ) {
        return getChunkZ() * getBlockSizeZ() + localZ(localZ);
    }

    @Override
    public String toString() {
        return "{" + getChunkX() + ", " + getChunkY() + ", " + getChunkZ() + "}";
    }

    public static long computeChunkKey(int chunkX, int chunkY, int chunkZ) {
        return (((long) chunkX & 0x1FFFFF) << 42)
                | (((long) chunkY & 0x1FFFFF) << 21)
                | (((long) chunkZ & 0x1FFFFF));
    }

    public static int unpackChunkX(long key) {
        int x = (int) ((key >>> 42) & 0x1FFFFF);
        // Wenn Bit 20 = 1 → negative Zahl → nach oben hin auffüllen
        if ((x & (1 << 20)) != 0) {
            x |= ~0x1FFFFF;
        }
        return x;
    }

    public static int unpackChunkY(long key) {
        int y = (int) ((key >>> 21) & 0x1FFFFF);
        if ((y & (1 << 20)) != 0) {
            y |= ~0x1FFFFF;
        }
        return y;
    }

    public static int unpackChunkZ(long key) {
        int z = (int) (key & 0x1FFFFF);
        if ((z & (1 << 20)) != 0) {
            z |= ~0x1FFFFF;
        }
        return z;
    }

    @Override
    public int getSizeX() {
        return getBlockSizeX();
    }

    @Override
    public int getSizeY() {
        return getBlockSizeY();
    }

    @Override
    public int getSizeZ() {
        return getBlockSizeZ();
    }
}
