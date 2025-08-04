package de.verdox.voxel.shared.lighting;

import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.ThreadUtil;
import de.verdox.voxel.shared.util.datastructure.LongQueue;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ChunkLightEngine {
    private static final Logger LOGGER = Logger.getLogger(ChunkLightEngine.class.getSimpleName());

    private volatile LongQueue longQueue = new LongQueue(512);
    private final Executor service = Executors.newSingleThreadExecutor(ThreadUtil.createFactoryForName("Chunk Light Engine", true));

    public void scheduleSkylightUpdateInSlice(World world, int regionSliceX, int regionSliceZ, LightAccessor startAccessor, int stepsToCalculateDown, LightUpdateCallback onDone) {
/*        service.execute(() -> {
            int steps = stepsToCalculateDown;

            long start = System.currentTimeMillis();
            //computeSkylight(startAccessor, true);
            onDone.regionLightCallback(regionSliceX, startAccessor.getRegionY(), regionSliceZ);

            int relCounter = -1;

            while (steps > 0) {
                var toCheck = startAccessor.getRelative(0, relCounter, 0);
                if (toCheck == null) {
                    onDone.regionLightCallback(regionSliceX, startAccessor.getRegionY() + relCounter, regionSliceZ);
                    continue;
                }
                //computeSkylight(toCheck, false);
                onDone.regionLightCallback(regionSliceX, toCheck.getRegionY(), regionSliceZ);
                steps--;
                relCounter--;
            }
            long end = System.currentTimeMillis() - start;
            //System.out.println("Took " + end + "ms to calculate sky light for region slice " + regionSliceX + ", " + regionSliceZ);
        });*/
    }

    public static boolean computeSkylight(LightAccessor lightAccessor, boolean isHighest) {
        boolean wasUpdated = false;
        LongQueue queue = new LongQueue();
        LightAccessor upperNeighbor = lightAccessor.getNeighbor(Direction.UP);
        if (upperNeighbor == null) {
            return false;
        }

        int sizeX = lightAccessor.sizeX();
        int sizeY = lightAccessor.sizeY();
        int sizeZ = lightAccessor.sizeZ();
        byte worldLight = lightAccessor.getWorldSkyLight();
        boolean isUpperAir = (isHighest || upperNeighbor.isAirRegion());

        // 1) Initial seeding per column
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                // Find highest non-air block
                int firstNonAir = lightAccessor.getHighestNonAirBlockAt(x, z);

                // Determine incoming sky light
                byte incoming;
                byte attenuation;
                if (isUpperAir || firstNonAir == -1) {
                    // No neighbor above or no blocks at all: full sky light
                    incoming = worldLight;
                    attenuation = 0;
                } else if (firstNonAir == sizeY - 1) {
                    // Our top slice, take from upperNeighbor
                    incoming = upperNeighbor.getSkyLight(x, 0, z);
                    attenuation = (byte) (upperNeighbor.isOpaque(x, 0, z) ? 1 : 0);
                } else {
                    // Take from our own cell just above the first block
                    incoming = lightAccessor.getSkyLight(x, firstNonAir + 1, z);
                    attenuation = (byte) (lightAccessor.isOpaque(x, firstNonAir + 1, z) ? 1 : 0);
                }

                // Fill all air blocks above the first non-air
                for (int y = sizeY - 1; y > firstNonAir; y--) {
                    if (lightAccessor.getSkyLight(x, y, z) != incoming) {
                        lightAccessor.setSkyLight(x, y, z, incoming);
                        queue.enqueue(Chunk.computeChunkKey(x, y, z));
                        wasUpdated = true;
                    }
                }

                if (firstNonAir == -1) {
                    continue;
                }

                // Seed the first non-air block itself (minus one attenuation)

                byte blockLevel = (byte) Math.max(0, incoming - attenuation);
                if (lightAccessor.getSkyLight(x, firstNonAir, z) != blockLevel) {
                    lightAccessor.setSkyLight(x, firstNonAir, z, blockLevel);
                    queue.enqueue(Chunk.computeChunkKey(x, firstNonAir, z));
                    wasUpdated = true;
                }
            }
        }
/*
        // 2) Flood-fill propagation
        while (!queue.isEmpty()) {
            long packed = queue.dequeue();
            int lx = ChunkBase.unpackChunkX(packed);
            int ly = ChunkBase.unpackChunkY(packed);
            int lz = ChunkBase.unpackChunkZ(packed);
            byte baseLight = lightAccessor.getSkyLight(lx, ly, lz);

            for (Direction dir : directions) {
                int nx = lx + dir.getOffsetX();
                int ny = ly + dir.getOffsetY();
                int nz = lz + dir.getOffsetZ();

                if (nx < 0 || ny < 0 || nz < 0
                    || nx >= sizeX || ny >= sizeY || nz >= sizeZ) {
                    continue;
                }

                // attenuation: full block = worldLight, else 1
                byte attenuation = lightAccessor.isOpaque(nx, ny, nz)
                    ? (byte) 1
                    : (byte) 1;
                byte newLevel = (byte) Math.max(0, baseLight - attenuation);

                if (newLevel > lightAccessor.getSkyLight(nx, ny, nz)) {
                    lightAccessor.setSkyLight(nx, ny, nz, newLevel);
                    queue.enqueue(ChunkBase.computeChunkKey(nx, ny, nz));
                    wasUpdated = true;
                }
            }
        }*/

        return wasUpdated;
    }


    /*    */

    /**
     * Füllt die farbigen Block-Licht-Quellen (z.B. Fackeln, Leuchtstein, etc.).
     * Jeder Block liefert chunk.getBlock(x,y,z).getEmissionRGB() zurück – ein
     * Tupel (r,g,b) im Bereich 0–15. Auch hier fällt das Licht um 1 pro Block ab.
     *//*
    public void computeColoredLight(Chunk chunk, ChunkLightData light) {
        int sizeX = chunk.getSizeX(), sizeY = chunk.getSizeY(), sizeZ = chunk.getSizeZ();
        Deque<LightNode> q = new ArrayDeque<>();

        // 1) Block-Licht-Quellen initialisieren
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    byte er = block.getEmissionRed();
                    byte eg = block.getEmissionGreen();
                    byte eb = block.getEmissionBlue();
                    if ((er | eg | eb) != 0) {
                        light.set(x, y, z, er, eg, eb);
                        q.add(new LightNode(x, y, z, er, eg, eb));
                    }
                }
            }
        }

        // 2) BFS-Loop für RGB
        while (!q.isEmpty()) {
            LightNode n = q.poll();
            for (int[] d : DIRS) {
                int nx = n.x + d[0], ny = n.y + d[1], nz = n.z + d[2];
                if (nx < 0 || ny < 0 || nz < 0
                    || nx >= sizeX || ny >= sizeY || nz >= sizeZ) continue;

                int attenuation = chunk.isOpaque(nx, ny, nz) ? 15 : 1;

                int newR = Math.max(n.r - attenuation, 0);
                int newG = Math.max(n.g - attenuation, 0);
                int newB = Math.max(n.b - attenuation, 0);

                boolean updated = false;
                if (newR > light.getR(nx, ny, nz)) updated = true;
                if (newG > light.getG(nx, ny, nz)) updated = true;
                if (newB > light.getB(nx, ny, nz)) updated = true;

                if (updated) {
                    light.set(nx, ny, nz, (byte) newR, (byte) newG, (byte) newB);
                    q.add(new LightNode(nx, ny, nz, newR, newG, newB));
                }
            }
        }
    }*/

    public interface LightUpdateCallback {
        void regionLightCallback(int regionX, int regionY, int regionZ);
    }

}
