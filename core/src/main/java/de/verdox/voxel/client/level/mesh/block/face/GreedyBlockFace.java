package de.verdox.voxel.client.level.mesh.block.face;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

public class GreedyBlockFace extends SingleBlockFace {
    @Getter
    private short deltaU, deltaUBack, deltaV, deltaVBack;

    public GreedyBlockFace(BlockModelType.BlockFace blockFace, byte blockXInChunk, byte blockYInChunk, byte blockZInChunk, ResourceLocation textureId, float lightPacked, byte aoPacked, int deltaU, int deltaUBack, int deltaV, int deltaVBack) {
        super(blockFace, blockXInChunk, blockYInChunk, blockZInChunk, (byte) 0, textureId, lightPacked, aoPacked);
        this.deltaU = (short) deltaU;
        this.deltaUBack = (short) deltaUBack;
        this.deltaV = (short) deltaV;
        this.deltaVBack = (short) deltaVBack;
    }

    @Override
    protected float getULength() {
        return super.getULength() + deltaU + deltaUBack;
    }


    @Override
    protected float getVLength() {
        return super.getVLength() + deltaV + deltaVBack;
    }

    @Override
    public BlockFace addOffset(int offsetX, int offsetY, int offsetZ) {
        return new GreedyBlockFace(
                getBlockFace(),
                (byte) (blockXInChunk + offsetX), (byte) (blockYInChunk + offsetX), (byte) (blockZInChunk + offsetX),
                textureId, lightPacked, aoPacked,
                this.deltaU, this.deltaUBack,
                this.deltaV, this.deltaVBack
        );
    }

    @Override
    public BlockFace addOffset(float offsetX, float offsetY, float offsetZ) {
        return new GreedyBlockFace(
                getBlockFace(),
                (byte) (blockXInChunk + offsetX), (byte) (blockYInChunk + offsetX), (byte) (blockZInChunk + offsetX),
                textureId, lightPacked, aoPacked,
                this.deltaU, this.deltaUBack,
                this.deltaV, this.deltaVBack
        );
    }

    @Override
    public GreedyBlockFace expandU(int extra) {
        this.deltaU += (short) extra;
        return this;
    }

    @Override
    public GreedyBlockFace expandV(int extra) {
        this.deltaV += (short) extra;
        return this;
    }

    @Override
    public GreedyBlockFace expandUBackward(int extra) {
        this.deltaUBack += (short) extra;
        return this;
    }

    @Override
    public GreedyBlockFace expandVBackward(int extra) {
        this.deltaVBack += (short) extra;
        return this;
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

        boolean flip = getBlockFace().direction().isNegative();

        int c0 = 0;
        int c1 = 1;
        int c2 = 2;
        int c3 = 3;
        if (cId == c0) {
            return base;
        }

        float uShift = (uDir * deltaU);

        if (cId == c1) {
            return (base + uShift);
        }
        float vShift = (vDir * deltaV);

        if (cId == c2) {
            return (base + vShift);
        }
        if (cId == c3) {
            return base + uShift + vShift;
        }
        return base;
    }

    /*    @Override
    protected float getCornerX(byte cId, BlockModelType.BlockFace.RelativeCoordinate rc) {
        float base = super.getCornerX(cId, rc);
        int uOff = blockFace.getUDirection().getOffsetX();
        int vOff = blockFace.getVDirection().getOffsetX();
        boolean isU = blockFace.getUDirection().isOnX();
        boolean isV = blockFace.getVDirection().isOnX();
        return computeCorner(base, uOff, vOff, deltaU, deltaV, isU, isV);
    }

    @Override
    protected float getCornerY(byte cId, BlockModelType.BlockFace.RelativeCoordinate rc) {
        float base = super.getCornerY(cId, rc);
        int uOff = blockFace.getUDirection().getOffsetY();
        int vOff = blockFace.getVDirection().getOffsetY();
        boolean isU = blockFace.getUDirection().isOnY();
        boolean isV = blockFace.getVDirection().isOnY();
        return computeCorner(base, uOff, vOff, deltaU, deltaV, isU, isV);
    }

    @Override
    protected float getCornerZ(byte cId, BlockModelType.BlockFace.RelativeCoordinate rc) {
        float base = super.getCornerZ(cId, rc);
        int uOff = blockFace.getUDirection().getOffsetZ();
        int vOff = blockFace.getVDirection().getOffsetZ();
        boolean isU = blockFace.getUDirection().isOnZ();
        boolean isV = blockFace.getVDirection().isOnZ();
        return computeCorner(base, uOff, vOff, deltaU, deltaV, isU, isV);
    }*/

    private float computeCorner(
            float base,
            int uOff, int vOff,
            float deltaU, float deltaV,
            boolean isU, boolean isV
    ) {

        if (uOff == 0 && vOff == 0) {
            return base;
        }


        float du = uOff >= 0 ? deltaU : deltaUBack;
        float dv = vOff >= 0 ? deltaV : deltaVBack;

        // Wenn

        // Eck-Koordinate + U/V-Anteil
        float coord = base + uOff * (isU ? du : 0) + vOff * (isV ? dv : 0);
/*        if (uOff < 0 || vOff < 0) {
            coord = 1 - coord;
        }*/
        return coord;
    }

    @Override
    protected byte[] getIndexOrderByFaceDirection(Direction direction) {
/*        if (direction.equals(Direction.DOWN) || direction.equals(Direction.WEST) || direction.equals(Direction.SOUTH)) {
            return new byte[]{0, 3, 1, 3, 0, 2};
        }*/
        return new byte[]{0, 1, 3, 3, 2, 0};
    }

    // EXPAND U
    //     /**
    //     * Expands this face by {@code delta} blocks along the U axis (corner1 → corner2),
    //     * growing the quad in its U direction.
    //     */
    //    public BlockFace expandU(int delta) {
    //        float lengthU = getULength();
    //        if (lengthU == 0) return this;
    //        float ux = (corner2X - corner1X) / lengthU;
    //        float uy = (corner2Y - corner1Y) / lengthU;
    //        float uz = (corner2Z - corner1Z) / lengthU;
    //        return new BlockFace(
    //            blockXInChunk, blockYInChunk, blockZInChunk,
    //            corner1X, corner1Y, corner1Z,
    //            corner2X + ux * delta, corner2Y + uy * delta, corner2Z + uz * delta,
    //            corner3X + ux * delta, corner3Y + uy * delta, corner3Z + uz * delta,
    //            corner4X, corner4Y, corner4Z,
    //            normalX, normalY, normalZ,
    //            textureId
    //        );
    //    }

    // EXPAND V
    //     /**
    //     * Expands this face by {@code delta} blocks along the V axis (corner1 → corner4),
    //     * growing the quad in its V direction.
    //     */
    //    public BlockFace expandV(int delta) {
    //        float lengthV = getVLength();
    //        if (lengthV == 0) return this;
    //        float vx = (corner4X - corner1X) / lengthV;
    //        float vy = (corner4Y - corner1Y) / lengthV;
    //        float vz = (corner4Z - corner1Z) / lengthV;
    //        return new BlockFace(
    //            blockXInChunk, blockYInChunk, blockZInChunk,
    //            corner1X, corner1Y, corner1Z,
    //            corner2X, corner2Y, corner2Z,
    //            corner3X + vx * delta, corner3Y + vy * delta, corner3Z + vz * delta,
    //            corner4X + vx * delta, corner4Y + vy * delta, corner4Z + vz * delta,
    //            normalX, normalY, normalZ,
    //            textureId
    //        );
    //    }

    // EXPAND U BACKWARD
    //     /**
    //     * Expands this face by {@code delta} blocks in the opposite U direction
    //     * (from corner2 back towards corner1).
    //     */
    //    public BlockFace expandUBackward(int delta) {
    //        float lengthU = getULength();
    //        if (lengthU == 0) return this;
    //        float ux = (corner1X - corner2X) / lengthU;
    //        float uy = (corner1Y - corner2Y) / lengthU;
    //        float uz = (corner1Z - corner2Z) / lengthU;
    //        return new BlockFace(
    //            blockXInChunk, blockYInChunk, blockZInChunk,
    //            corner1X + ux * delta, corner1Y + uy * delta, corner1Z + uz * delta,
    //            corner2X, corner2Y, corner2Z,
    //            corner3X, corner3Y, corner3Z,
    //            corner4X + ux * delta, corner4Y + uy * delta, corner4Z + uz * delta,
    //            normalX, normalY, normalZ,
    //            textureId
    //        );
    //    }

    // EXPAND V BACKWARD
    //     public BlockFace expandVBackward(int delta) {
    //        float lengthV = getVLength();
    //        if (lengthV == 0) return this;
    //        float vx = (corner1X - corner4X) / lengthV;
    //        float vy = (corner1Y - corner4Y) / lengthV;
    //        float vz = (corner1Z - corner4Z) / lengthV;
    //        return new BlockFace(
    //            blockXInChunk, blockYInChunk, blockZInChunk,
    //            corner1X + vx * delta, corner1Y + vy * delta, corner1Z + vz * delta,
    //            corner2X + vx * delta, corner2Y + vy * delta, corner2Z + vz * delta,
    //            corner3X, corner3Y, corner3Z,
    //            corner4X, corner4Y, corner4Z,
    //            normalX, normalY, normalZ,
    //            textureId
    //        );
    //    }
}
