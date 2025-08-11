package de.verdox.voxel.client.level.chunk;

import de.verdox.voxel.client.level.chunk.occupancy.BitsetBasedOccupancyMask;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.client.level.chunk.proto.ChunkProtoMesh;
import de.verdox.voxel.client.util.LODUtil;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.level.chunk.DelegateChunk;
import de.verdox.voxel.shared.level.chunk.data.palette.ChunkBlockPalette;
import de.verdox.voxel.shared.level.chunk.data.sliced.DepthMap;
import de.verdox.voxel.shared.level.chunk.data.sliced.HeightMap;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.lighting.ChunkLightData;
import de.verdox.voxel.shared.util.Delegate;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.palette.ThreeDimensionalPalette;
import de.verdox.voxel.shared.util.palette.strategy.PaletteStrategy;
import lombok.Getter;

import java.util.List;

public class LODChunk implements Delegate<TerrainChunk>, RenderableChunk {
    private final TerrainChunk owner;
    @Getter
    private final OccupancyMask chunkOccupancyMask = new BitsetBasedOccupancyMask();
    @Getter
    private final int lodLevel;
    @Getter
    private final ChunkProtoMesh chunkProtoMesh;

    public static LODChunk of(TerrainChunk parent, int lodLevel) {
        return new LODChunk(parent, lodLevel);
    }

    private LODChunk(TerrainChunk parent, int lodLevel) {
        this.owner = parent;
        this.lodLevel = lodLevel;
        initLodChunk();
        initLodLight();
        this.chunkOccupancyMask.setOwner(this);
        this.chunkOccupancyMask.initFromOwner();
        this.chunkProtoMesh = new ChunkProtoMesh(this);
        this.subscribe(parent);
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
    public void init() {

    }

    @Override
    public int getChunkX() {
        return this.owner.getChunkX();
    }

    @Override
    public int getChunkY() {
        return this.owner.getChunkY();
    }

    @Override
    public int getChunkZ() {
        return this.owner.getChunkZ();
    }

    @Override
    public ChunkBlockPalette getChunkBlockPalette() {
        return null;
    }

    @Override
    public HeightMap getHeightMap() {
        return this.owner.getHeightMap();
    }

    @Override
    public DepthMap getDepthMap() {
        return this.owner.getDepthMap();
    }

    @Override
    public ChunkLightData getChunkLightData() {
        return null;
    }

    @Override
    public World getWorld() {
        return this.owner.getWorld();
    }

    @Override
    public boolean isEmpty() {
        return this.owner.isEmpty();
    }

    @Override
    public boolean hasNeighborsToAllSides() {
        return this.owner.hasNeighborsToAllSides();
    }

    @Override
    public <SELF extends Chunk> SELF getNeighborChunk(Direction direction) {
        return (SELF) ((TerrainChunk)this.owner.getNeighborChunk(direction)).getLodChunk(lodLevel);
    }

    @Override
    public BlockBase getBlockAt(int localX, int localY, int localZ) {
        int lodX = localX(localX);
        int lodY = localY(localY);
        int lodZ = localZ(localZ);
        return super.getBlockAt(lodX, lodY, lodZ);
    }

    @Override
    public int getSizeX() {
        return owner.getSizeX() / getLodStep();
    }

    @Override
    public int getSizeY() {
        return owner.getSizeY() / getLodStep();
    }

    @Override
    public int getSizeZ() {
        return owner.getSizeZ() / getLodStep();
    }

    private void updateBlockAt(int lodX, int lodY, int lodZ, boolean updateBlocks, boolean updateLights) {
        BlockBase lodBlock = null;
        int counter = 0;
        int accuracy = getLodAccuracy();
        int lodStep = getLodStep();

        for (int x = 0; x < lodStep; x++) {
            for (int y = 0; y < lodStep; y++) {
                for (int z = 0; z < lodStep; z++) {
                    int relX = (lodX * lodStep) + x;
                    int relY = (lodY * lodStep) + y;
                    int relZ = (lodZ * lodStep) + z;
                    BlockBase blockInParent = owner.getBlockAt(relX, relY, relZ);

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
                            byte skyLight = owner.getChunkLightData().getSkyLight(lodX + x, lodY + y, lodZ + z);
                            byte red = owner.getChunkLightData().getBlockRed(lodX + x, lodY + y, lodZ + z);
                            byte green = owner.getChunkLightData().getBlockGreen(lodX + x, lodY + y, lodZ + z);
                            byte blue = owner.getChunkLightData().getBlockBlue(lodX + x, lodY + y, lodZ + z);
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
        if (owner.getChunkBlockPalette().getState().equals(ThreeDimensionalPalette.State.EMPTY)) {
            this.getChunkBlockPalette().setStrategy(new PaletteStrategy.Empty<>(), ThreeDimensionalPalette.State.EMPTY);
        } else if (owner.getChunkBlockPalette().getState().equals(ThreeDimensionalPalette.State.UNIFORM) && owner.getChunkBlockPalette().getStrategy() instanceof PaletteStrategy.Uniform<?> strategy) {

            this.getChunkBlockPalette().setStrategy(new PaletteStrategy.Uniform<>((ResourceLocation) strategy.getUniformValue()), ThreeDimensionalPalette.State.UNIFORM);
        } else {
            for (int x = 0; x < getSizeX(); x++) {
                for (int y = 0; y < getSizeY(); y++) {
                    for (int z = 0; z < getSizeZ(); z++) {
                        updateBlockAt(x, y, z, true, false);
                    }
                }
            }
        }
    }

    private void initLodLight() {
        if (owner.getChunkLightData().getState().equals(ChunkLightData.LightState.UNINITIALIZED)) {
            return;
        } else if (owner.getChunkLightData().getState().equals(ChunkLightData.LightState.UNIFORM)) {
            this.getChunkLightData().setUniform(owner.getChunkLightData().getUniformPacked());
        } else {
            for (int x = 0; x < getSizeX(); x++) {
                for (int y = 0; y < getSizeY(); y++) {
                    for (int z = 0; z < getSizeZ(); z++) {
                        updateBlockAt(x, y, z, false, true);
                    }
                }
            }
        }
    }

    @Override
    public List<DelegateChunk> getDelegates() {
        return List.of();
    }

    @Override
    public TerrainChunk getOwner() {
        return this.owner;
    }
}
