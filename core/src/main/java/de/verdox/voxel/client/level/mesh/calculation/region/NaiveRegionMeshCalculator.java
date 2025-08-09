package de.verdox.voxel.client.level.mesh.calculation.region;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.chunk.RenderableChunk;
import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.client.level.chunk.proto.ProtoMask;
import de.verdox.voxel.client.level.mesh.TerrainRegion;
import de.verdox.voxel.client.renderer.classic.TerrainMesh;

public class NaiveRegionMeshCalculator implements RegionMeshCalculator {
    @Override
    public void updateTerrainMesh(TerrainRegion terrainRegion, int lodLevel) {
        long start = System.nanoTime();
        if(terrainRegion.isAirRegion()) {
            return;
        }

        TerrainMesh terrainMesh = terrainRegion.getOrCreateMesh();

        TextureAtlas textureAtlas = TextureAtlasManager.getInstance().getBlockTextureAtlas();

        FloatArray verts = new FloatArray();
        IntArray idxs = new IntArray();

        int numVertices = 0;
        int numIndices = 0;
        int amountFaces = 0;
        int vertexIndexOffset = 0;

        TerrainChunk[] chunks = terrainRegion.getChunksInRegion();
        for (int i = 0; i < chunks.length; i++) {
            TerrainChunk terrainChunk = chunks[i];
            if (terrainChunk == null || terrainChunk.isEmpty()) {
                continue;
            }
            RenderableChunk renderableChunk = terrainChunk;
            if (lodLevel != 0) {
                renderableChunk = terrainChunk.getLodChunk(lodLevel);
            }

            int offsetXInBlocks = terrainRegion.getBounds().getOffsetX(terrainChunk.getChunkX()) * terrainRegion.getTerrainManager().getWorld().getChunkSizeX();
            int offsetYInBlocks = terrainRegion.getBounds().getOffsetY(terrainChunk.getChunkY()) * terrainRegion.getTerrainManager().getWorld().getChunkSizeY();
            int offsetZInBlocks = terrainRegion.getBounds().getOffsetZ(terrainChunk.getChunkZ()) * terrainRegion.getTerrainManager().getWorld().getChunkSizeZ();

            float[] vertices = renderableChunk.getChunkProtoMesh().createArrayForVertices(ProtoMask.Type.OPAQUE);
            int[] indices = renderableChunk.getChunkProtoMesh().createArrayForIndices(ProtoMask.Type.OPAQUE);

            renderableChunk.getChunkProtoMesh().appendToArrays(ProtoMask.Type.OPAQUE, vertices, indices, vertexIndexOffset, textureAtlas, (byte) lodLevel, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

            int storageVertices = renderableChunk.getChunkProtoMesh().getAmountVertices(ProtoMask.Type.OPAQUE);
            int storageIndices = renderableChunk.getChunkProtoMesh().getAmountIndices(ProtoMask.Type.OPAQUE);
            int storageFaces = renderableChunk.getChunkProtoMesh().getAmountFaces(ProtoMask.Type.OPAQUE);

            numVertices += storageVertices;
            numIndices += storageIndices;
            amountFaces += storageFaces;
            vertexIndexOffset += storageVertices;

            verts.addAll(vertices);
            idxs.addAll(indices);
        }

        if (amountFaces > 0) {
            terrainMesh.setMeshData(verts.toArray(), idxs.toArray(), amountFaces, numVertices, numIndices);
        }

        long duration = System.nanoTime() - start;
        regionCalculatorThroughput.add(duration);
    }
}
