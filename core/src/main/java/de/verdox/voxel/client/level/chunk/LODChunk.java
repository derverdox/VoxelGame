package de.verdox.voxel.client.level.chunk;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.chunk.occupancy.BitsetBasedOccupancyMask;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.client.util.LODUtil;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import de.verdox.voxel.shared.util.palette.strategy.PaletteStrategy;
import lombok.Getter;

public class LODChunk extends ChunkBase<ClientWorld> {
    private final ClientChunk parent;
    @Getter
    private final OccupancyMask chunkOccupancyMask = new BitsetBasedOccupancyMask();
    @Getter
    private final int lodLevel;

    public static LODChunk of(ClientChunk parent, int lodLevel) {
        int lodStep = LODUtil.getLodScale(lodLevel);
        return new LODChunk(parent, lodLevel, lodStep, (parent.getBlockSizeX() / lodStep), (parent.getBlockSizeY() / lodStep), (parent.getBlockSizeZ() / lodStep));
    }

    private LODChunk(ClientChunk parent, int lodLevel, int lodStep, int sizeX, int sizeY, int sizeZ) {
        super(parent.getWorld(), parent.getChunkX(), parent.getChunkY(), parent.getChunkZ());
        this.parent = parent;
        this.lodLevel = lodLevel;
    }

    @Override
    public int getChunkX() {
        return parent.getChunkX();
    }

    @Override
    public int getChunkY() {
        return parent.getChunkY();
    }

    @Override
    public int getChunkZ() {
        return parent.getChunkZ();
    }

    @Override
    public ClientWorld getWorld() {
        return parent.getWorld();
    }

    @Override
    public void init() {
        initLodChunk();
        initLodLight();
        this.chunkOccupancyMask.setOwner(this);
        this.chunkOccupancyMask.initFromOwner();
    }

    @Override
    public void setBlockAt(BlockBase newBlock, int localX, int localY, int localZ) {
        int lodX = localX(localX);
        int lodY = localY(localY);
        int lodZ = localZ(localZ);
        updateBlockAt(lodX, lodY, lodZ, true, true);
        this.chunkOccupancyMask.updateOccupancyMask(newBlock, lodX, lodY, lodZ);
    }

    @Override
    public BlockBase getBlockAt(int localX, int localY, int localZ) {
        int lodX = localX(localX);
        int lodY = localY(localY);
        int lodZ = localZ(localZ);
        //System.out.println("Get block at " + lodX + ", " + lodY + ", " + lodZ);
        return super.getBlockAt(lodX, lodY, lodZ);
    }

    @Override
    public int getBlockSizeX() {
        return parent.getBlockSizeX() / getLodStep();
    }

    @Override
    public int getBlockSizeY() {
        return parent.getBlockSizeY() / getLodStep();
    }

    @Override
    public int getBlockSizeZ() {
        return parent.getBlockSizeZ() / getLodStep();
    }

    private void updateBlockAt(int lodX, int lodY, int lodZ, boolean updateBlocks, boolean updateLights) {
        BlockBase lodBlock = null;
        int counter = 0;
        int accuracy = getLodAccuracy();
        int lodStep = getLodStep();
        // Search for LOD block in lod radius in parent chunk.
        //System.out.println("Update Block at: " + lodX + ", " + lodY + ", " + lodZ);
        for (int x = 0; x < lodStep; x++) {
            for (int y = 0; y < lodStep; y++) {
                for (int z = 0; z < lodStep; z++) {
                    int relX = (lodX * lodStep) + x;
                    int relY = (lodY * lodStep) + y;
                    int relZ = (lodZ * lodStep) + z;
                    BlockBase blockInParent = parent.getBlockAt(relX, relY, relZ);

                    if (blockInParent != null && !blockInParent.equals(Blocks.AIR)) {
                        counter++;
                        if (lodBlock == null) {
                            lodBlock = blockInParent;
                        }
                    }


                    if (counter >= accuracy) {
                        if (updateBlocks && lodBlock != null) {
                            super.setBlockAt(lodBlock, lodX, lodY, lodZ);
                        }
                        if (updateLights) {
                            byte skyLight = parent.getChunkLightData().getSkyLight(lodX + x, lodY + y, lodZ + z);
                            byte red = parent.getChunkLightData().getBlockRed(lodX + x, lodY + y, lodZ + z);
                            byte green = parent.getChunkLightData().getBlockGreen(lodX + x, lodY + y, lodZ + z);
                            byte blue = parent.getChunkLightData().getBlockBlue(lodX + x, lodY + y, lodZ + z);
                            getChunkLightData().setSkyLight(lodX, lodY, lodZ, skyLight);
                            getChunkLightData().setBlockLight(lodX, lodY, lodZ, red, green, blue);
                        }
                        break;
                    }
                }
            }
        }
    }

    public int getLodStep() {
        return LODUtil.getLodScale(lodLevel);
    }

    public int getLodAccuracy() {
        int lodStep = getLodStep();
        return (int) (lodStep * lodStep * lodStep / 2f);
    }

    private void initLodChunk() {
        if (parent.getChunkBlockPalette().getState().equals(ThreeDimensionalPalette.State.EMPTY)) {
            this.getChunkBlockPalette().setStrategy(new PaletteStrategy.Empty<>(), ThreeDimensionalPalette.State.EMPTY);
        } else if (parent.getChunkBlockPalette().getState().equals(ThreeDimensionalPalette.State.UNIFORM) && parent.getChunkBlockPalette().getStrategy() instanceof PaletteStrategy.Uniform<?> strategy) {

            this.getChunkBlockPalette().setStrategy(new PaletteStrategy.Uniform<>((ResourceLocation) strategy.getUniformValue()), ThreeDimensionalPalette.State.UNIFORM);
        } else {
            for (int x = 0; x < getBlockSizeX(); x++) {
                for (int y = 0; y < getBlockSizeY(); y++) {
                    for (int z = 0; z < getBlockSizeZ(); z++) {
                        updateBlockAt(x, y, z, true, false);
                    }
                }
            }
        }
    }

    private void initLodLight() {
        if (parent.getChunkLightData().getState().equals(ChunkLightData.LightState.UNINITIALIZED)) {
            return;
        } else if (parent.getChunkLightData().getState().equals(ChunkLightData.LightState.UNIFORM)) {
            this.getChunkLightData().setUniform(parent.getChunkLightData().getUniformPacked());
        } else {
            for (int x = 0; x < getBlockSizeX(); x++) {
                for (int y = 0; y < getBlockSizeY(); y++) {
                    for (int z = 0; z < getBlockSizeZ(); z++) {
                        updateBlockAt(x, y, z, false, true);
                    }
                }
            }
        }
    }
}
