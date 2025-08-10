package de.verdox.voxel.shared.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class TerrainRenderStats {
    public long amountFacesDrawn;
    public long amountVerticesDrawn;
    public long amountShortIndicesVRAM;
    public long amountIntIndicesVRAM;
    public long amountFloatVerticesVRAM;
    public long drawnMeshes;
    public long drawnChunks;
    public long maxDrawableChunks;

    public void reset() {
        amountFacesDrawn = 0;
        amountVerticesDrawn = 0;
        amountShortIndicesVRAM = 0;
        amountIntIndicesVRAM = 0;
        amountFloatVerticesVRAM = 0;
        drawnMeshes = 0;
        drawnChunks = 0;
        maxDrawableChunks = 0;
    }

    public List<String> printToLines() {
        List<String> print = new ObjectArrayList<>();

        if (drawnMeshes > 0) {
            print.add("Drawn meshes: " + drawnMeshes);
        }

        if (maxDrawableChunks > 0) {
            print.add(" - Max Chunks: " + maxDrawableChunks);
        }

        if (drawnChunks > 0 && drawnChunks != drawnMeshes) {
            print.add(" - Drawn chunks: " + drawnChunks + "("+(FormatUtil.formatPercent(1f * drawnChunks / maxDrawableChunks))+")");
        }

        if (amountFacesDrawn > 0) {
            print.add(" - Drawn block faces: " + amountFacesDrawn);
        }
        if (amountVerticesDrawn > 0) {
            print.add(" - Drawn vertices: " + amountVerticesDrawn);
        }
        long byteCostIndices = (amountShortIndicesVRAM * Short.BYTES) + (amountIntIndicesVRAM * Integer.BYTES);
        long byteCostVertices = amountFloatVerticesVRAM * Float.BYTES;

        if (byteCostIndices > 0 || byteCostVertices > 0) {
            print.add("VRAM terrain: " + FormatUtil.formatBytes(byteCostIndices + byteCostVertices));
        }

        if (amountShortIndicesVRAM > 0 || amountIntIndicesVRAM > 0) {
            print.add(" - VRAM Indices: " + FormatUtil.formatBytes(byteCostIndices));
        }
        if (amountFloatVerticesVRAM > 0) {

            print.add(" - VRAM vertex data: " + FormatUtil.formatBytes(byteCostVertices));
        }

        return print;
    }
}
