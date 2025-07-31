package de.verdox.voxel.client.level.mesh.block.face;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

public class GreedyBlockFace extends SingleBlockFace {
    @Getter
    private final short deltaU, deltaV;

    public GreedyBlockFace(BlockModelType.BlockFace blockFace,
                           byte blockXInChunk, byte blockYInChunk, byte blockZInChunk,
                           byte lodLevel, ResourceLocation textureId, float lightPacked, byte aoPacked, int deltaU, int deltaV) {
        super(blockFace, blockXInChunk, blockYInChunk, blockZInChunk, lodLevel, textureId, lightPacked, aoPacked);
        this.deltaU = (short) deltaU;
        this.deltaV = (short) deltaV;
    }

    @Override
    protected float getULength() {
        return super.getULength() + deltaU;
    }


    @Override
    protected float getVLength() {
        return super.getVLength() + deltaV;
    }

    @Override
    public BlockFace addOffset(int offsetX, int offsetY, int offsetZ) {
        return new GreedyBlockFace(
                getBlockFace(),
                (byte) (blockXInChunk + offsetX), (byte) (blockYInChunk + offsetY), (byte) (blockZInChunk + offsetZ),
                lodLevel,
                textureId, lightPacked, aoPacked,
                this.deltaU,
                this.deltaV
        );
    }

    @Override
    public BlockFace addOffset(float offsetX, float offsetY, float offsetZ) {
        return new GreedyBlockFace(
                getBlockFace(),
                (byte) (blockXInChunk + offsetX), (byte) (blockYInChunk + offsetY), (byte) (blockZInChunk + offsetZ),
                lodLevel,
                textureId, lightPacked, aoPacked,
                this.deltaU,
                this.deltaV
        );
    }

    @Override
    public GreedyBlockFace expandU(int extra) {

        int deltaUNew = this.deltaU + extra;
        int deltaVNew = this.deltaV;

        // Flip
        if (getBlockFace().direction().equals(Direction.EAST) || getBlockFace().direction().equals(Direction.WEST)) {
            deltaVNew += (short) extra;
            deltaUNew = this.deltaU;
        }
        return new GreedyBlockFace(
                getBlockFace(),
                blockXInChunk, blockYInChunk, blockZInChunk,
                lodLevel,
                textureId, lightPacked, aoPacked,
                deltaUNew,
                deltaVNew
        );
    }

    @Override
    public GreedyBlockFace expandV(int extra) {

        int deltaUNew = this.deltaU;
        int deltaVNew = this.deltaV + extra;

        // Flip
        if (getBlockFace().direction().equals(Direction.EAST) || getBlockFace().direction().equals(Direction.WEST)) {
            deltaUNew += (short) extra;
            deltaVNew = this.deltaV;

        }
        return new GreedyBlockFace(
                getBlockFace(),
                blockXInChunk, blockYInChunk, blockZInChunk,
                lodLevel,
                textureId, lightPacked, aoPacked,
                deltaUNew,
                deltaVNew
        );
    }

    @Override
    protected float getCornerX(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        return calculate(cId, super.getCornerX(cId, relativeCoordinate), getBlockFace().getUDirection().getNx(), getBlockFace().getVDirection().getNx());
    }

    @Override
    protected float getCornerY(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        return calculate(cId, super.getCornerY(cId, relativeCoordinate), getBlockFace().getUDirection().getNy(), getBlockFace().getVDirection().getNy());
    }

    @Override
    protected float getCornerZ(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        return calculate(cId, super.getCornerZ(cId, relativeCoordinate), getBlockFace().getUDirection().getNz(), getBlockFace().getVDirection().getNz());
    }

    private float calculate(byte cId, float base, float uDir, float vDir) {
        //TODO: Calculate with LOD scale

        int dU = (int) (deltaU * getLODScale());
        int dV = (int) (deltaV * getLODScale());

        int moveU = uDir < 0 ? dU : 0;
        int moveV = vDir < 0 ? dV : 0;

        int c0 = 0;
        int c1 = 1;
        int c2 = 2;
        int c3 = 3;
        if (cId == c0) {
            return base + moveU + moveV;
        }

        float uShift = (uDir * dU);

        if (cId == c1) {
            return (base + uShift) + moveU + moveV;
        }
        float vShift = (vDir * dV);

        if (cId == c2) {
            return (base + vShift) + moveU + moveV;
        }
        if (cId == c3) {
            return base + uShift + vShift + moveU + moveV;
        }
        return base;
    }

    @Override
    protected byte[] getIndexOrderByFaceDirection(Direction direction) {
        return new byte[]{0, 1, 3, 3, 2, 0};
    }
}
