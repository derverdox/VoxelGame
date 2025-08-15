package de.verdox.voxel.client.renderer.terrain.regions.mesh;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.chunk.RenderableChunk;
import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.client.level.chunk.proto.ProtoMask;
import de.verdox.voxel.client.renderer.terrain.regions.TerrainRegion;
import de.verdox.voxel.client.renderer.terrain.regions.TerrainMesh;

public class BufferedRegionMeshCalculator implements RegionMeshCalculator {
    @Override
    public void updateTerrainMesh(TerrainRegion terrainRegion, int lodLevel) {
        terrainRegion.setRenderedChunks(0);
        if(terrainRegion.isAirRegion()) {
            terrainRegion.setRenderedFaces(0);
            return;
        }

        long start = System.nanoTime();

        TerrainMesh terrainMesh = terrainRegion.getOrCreateMesh();

        TextureAtlas textureAtlas = TextureAtlasManager.getInstance().getBlockTextureAtlas();

        FloatArray verts = new FloatArray();
        IntArray idxs = new IntArray();

        FloatArray instances = new FloatArray();

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

            renderableChunk.getChunkProtoMesh().appendToBuffers(ProtoMask.FaceType.OPAQUE, verts, idxs, vertexIndexOffset, textureAtlas, (byte) lodLevel, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);
            renderableChunk.getChunkProtoMesh().appendToInstances(ProtoMask.FaceType.OPAQUE, instances, textureAtlas, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

            int storageVertices = renderableChunk.getChunkProtoMesh().getAmountVertices(ProtoMask.FaceType.OPAQUE);
            int storageIndices = renderableChunk.getChunkProtoMesh().getAmountIndices(ProtoMask.FaceType.OPAQUE);
            int storageFaces = renderableChunk.getChunkProtoMesh().getAmountFaces(ProtoMask.FaceType.OPAQUE);

            numVertices += storageVertices;
            numIndices += storageIndices;
            amountFaces += storageFaces;
            vertexIndexOffset += storageVertices;
            terrainRegion.setRenderedChunks(terrainRegion.getRenderedChunks() + 1);
        }

        if (amountFaces > 0) {
            verts.shrink();
            idxs.shrink();
            instances.shrink();
            //terrainMesh.setMeshData(verts.toArray(), idxs.toArray(), amountFaces, numVertices, numIndices, lodLevel);
            terrainMesh.setInstances(instances.toArray(), amountFaces, numVertices, lodLevel);
        }

        terrainRegion.setRenderedFaces(amountFaces);
        long duration = System.nanoTime() - start;
        regionCalculatorThroughput.add(duration);
    }
}
