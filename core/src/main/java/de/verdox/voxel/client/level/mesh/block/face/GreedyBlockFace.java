package de.verdox.voxel.client.level.mesh.block.face;

import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;

public class GreedyBlockFace extends SingleBlockFace {

    private final short deltaU;
    private final short deltaUBackward;
    private final short deltaV;
    private final short deltaVBackward;

    public GreedyBlockFace(BlockModelType.BlockFace blockFace, byte blockXInChunk, byte blockYInChunk, byte blockZInChunk, ResourceLocation textureId, float lightPacked, byte aoPacked, int deltaU, int deltaUBackward, int deltaV, int deltaVBackward) {
        super(blockFace, blockXInChunk, blockYInChunk, blockZInChunk, textureId, lightPacked, aoPacked);
        this.deltaU = (short) deltaU;
        this.deltaUBackward = (short) deltaUBackward;
        this.deltaV = (short) deltaV;
        this.deltaVBackward = (short) deltaVBackward;
    }

    @Override
    protected float getULength() {
        // Basis-Länge zwischen Eckpunkt1 und Eckpunkt2 (euklidisch)
        float dx = getBlockFace().c2().x() - getBlockFace().c1().x();
        float dy = getBlockFace().c2().y() - getBlockFace().c1().y();
        float dz = getBlockFace().c2().z() - getBlockFace().c1().z();
        float base = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        return base + deltaU + deltaUBackward;
    }

    @Override
    protected float getVLength() {
        // Basis-Länge zwischen Eckpunkt1 und Eckpunkt4 (euklidisch)
        float dx = getBlockFace().c4().x() - getBlockFace().c1().x();
        float dy = getBlockFace().c4().y() - getBlockFace().c1().y();
        float dz = getBlockFace().c4().z() - getBlockFace().c1().z();
        float base = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Plus aller bisherigen V-Expansions in beide Richtungen
        return base + deltaV + deltaVBackward;
    }

    @Override
    public SingleBlockFace expandU(int delta) {
        return super.expandU(delta + deltaU);
    }

    @Override
    public SingleBlockFace expandV(int delta) {
        return super.expandV(delta + deltaV);
    }

    @Override
    public SingleBlockFace expandUBackward(int delta) {
        return super.expandUBackward(delta + deltaUBackward);
    }

    @Override
    public SingleBlockFace expandVBackward(int delta) {
        return super.expandVBackward(delta + deltaVBackward);
    }

    @Override
    protected float getCornerX(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        float cx = super.getCornerX(cId, relativeCoordinate);

        float uLen = getULength();
        float vLen = getVLength();

        return expand(cId, cx, uLen, vLen);
    }


    @Override
    protected float getCornerY(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        float cy = super.getCornerY(cId, relativeCoordinate);

        float uLen = getULength();
        float vLen = getVLength();

        return expand(cId, cy, uLen, vLen);
    }

    @Override
    protected float getCornerZ(byte cId, BlockModelType.BlockFace.RelativeCoordinate relativeCoordinate) {
        float cz = super.getCornerZ(cId, relativeCoordinate);

        float uLen = getULength();
        float vLen = getVLength();

        return expand(cId, cz, uLen, vLen);
    }

    private float expand(byte cId, float cx, float ux, float vx) {
        float du = (cId == 1 || cId == 2) ? deltaU : -deltaUBackward;
        float dv = (cId == 2 || cId == 3) ? deltaV : -deltaVBackward;

        return cx + ux * du + vx * dv;
    }
}
