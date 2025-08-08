package de.verdox.voxel.client.level.mesh.terrain;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.Array;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.util.LODUtil;
import de.verdox.voxel.shared.level.world.World;
import gaiasky.util.gdx.mesh.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BufferedTerrainMesh {
    public static final int CHECKER_BOARD_FACTOR = 2;
    public static final int BASE_BLOCK_AMOUNT_FACES = 6;
    public static final int BASE_BLOCK_FACE_INDICES = 6;
    public static final int BASE_BLOCK_FACE_VERTICES = 4;

    private final TerrainRegion terrainRegion;
    private final IntVertexData vertices;
    private final IntIndexData indices;
    private boolean autoBind = true;

    public BufferedTerrainMesh(TerrainRegion terrainRegion, VertexAttribute... attributes) {
        int maxLod = 4;

        int viewDistanceX = 128;
        int viewDistanceY = 128;
        int viewDistanceZ = 128;

        VertexAttributes vertexAttributes = new VertexAttributes(attributes);

        System.out.println(terrainRegion);
        for (int i = 0; i <= maxLod; i++) {
            int lodLevel = i;
            int lodScale = LODUtil.getLodScale(lodLevel);

            World world = terrainRegion.getTerrainManager().getWorld();

            int maxBlocksPerChunk = world.getChunkSizeX() * world.getChunkSizeY() * world.getChunkSizeZ() / lodScale;
            int maxRenderedBlocksPerChunk = maxBlocksPerChunk / CHECKER_BOARD_FACTOR;
            int maxRenderedFacesPerChunk = maxRenderedBlocksPerChunk * BASE_BLOCK_AMOUNT_FACES;
            int maxRenderedVertices = maxRenderedFacesPerChunk * BASE_BLOCK_FACE_VERTICES;
            int maxRenderedIndices = maxRenderedFacesPerChunk * BASE_BLOCK_FACE_INDICES;
            int neededVRAMForVertices = maxRenderedVertices * vertexAttributes.vertexSize;
            int neededVRAMForIndices = maxRenderedIndices * Integer.SIZE / 8;
            int amountChunksPerRegion = terrainRegion.getTerrainManager().getBounds().regionSizeX() * terrainRegion.getTerrainManager().getBounds().regionSizeY() + terrainRegion.getTerrainManager().getBounds().regionSizeZ();

            int maxChunksRendered = viewDistanceX * viewDistanceY * viewDistanceZ / maxLod;
            long maxRegionsPossible = Math.round(maxChunksRendered / (amountChunksPerRegion * 1d));


            System.out.println("\tLOD: " + lodLevel);
            System.out.println("\t\t Rendered Chunks: " + maxChunksRendered + " chunks => " + maxRegionsPossible + " regions");
            System.out.println("\t\t Blocks per Chunk: " + maxBlocksPerChunk);
            System.out.println("\t\t Rendered Blocks per Chunk: " + maxRenderedBlocksPerChunk);
            System.out.println("\t\t Rendered Faces per Chunk: " + maxRenderedFacesPerChunk);
            System.out.println("\t\t Rendered Vertices per Chunk: " + maxRenderedVertices);
            System.out.println("\t\t Rendered Indices per Chunk: " + maxRenderedIndices);
            System.out.println("\t\tNeeded VRAM per Chunk: " + formatBytes(neededVRAMForVertices) + " + " + formatBytes(neededVRAMForIndices) + " = " + formatBytes(neededVRAMForVertices + neededVRAMForIndices));
            System.out.println("\t\tChunks per Region: " + amountChunksPerRegion);
            System.out.println("\t\tNeeded VRAM per Region: " + formatBytes(neededVRAMForVertices * amountChunksPerRegion) + " + " + formatBytes(neededVRAMForIndices * amountChunksPerRegion) + " = " + formatBytes((neededVRAMForVertices + neededVRAMForIndices) * amountChunksPerRegion));
            System.out.println("\t\tNeeded VRAM for Render distance: " + formatBytes((long) neededVRAMForVertices * maxChunksRendered) + " + " + formatBytes((long) neededVRAMForIndices * maxChunksRendered) + " = " + formatBytes((long) (neededVRAMForVertices + neededVRAMForIndices) * maxChunksRendered));


        }

        this.terrainRegion = terrainRegion;
        this.vertices = makeVertexBuffer(true, 1, vertexAttributes);
        this.indices = new IntIndexBufferObject(true, 1);
        BufferedTerrainMesh.addManagedMesh(Gdx.app, this);
    }

    private IntVertexData makeVertexBuffer(boolean isStatic, int maxVertices, VertexAttributes va) {
        if (Gdx.gl30 != null) {
            return new VertexBufferObjectWithVAO(isStatic, maxVertices, va);
        } else {
            return new VertexBufferObjectSubData(isStatic, maxVertices, va);
        }
    }


    static Map<Application, Array<BufferedTerrainMesh>> meshes = new HashMap<>();

    static void addManagedMesh(Application app, BufferedTerrainMesh mesh) {
        Array<BufferedTerrainMesh> managedResources = meshes.get(app);
        if (managedResources == null) {
            managedResources = new Array<>();
        }
        managedResources.add(mesh);
        meshes.put(app, managedResources);
    }

    /**
     * Will clear the managed mesh cache. I wouldn't use this if i was you :)
     */
    static void clearAllMeshes(Application app) {
        meshes.remove(app);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        // Einheiten-Labels für 1024er-Potenzen
        String[] units = {"KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};
        // Berechne, welche Potenz von 1024 wir brauchen
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        double value = bytes / Math.pow(1024, exp);
        // Format mit zwei Nachkommastellen
        return String.format(Locale.US, "%.2f %s", value, units[exp - 1]);
    }
}
