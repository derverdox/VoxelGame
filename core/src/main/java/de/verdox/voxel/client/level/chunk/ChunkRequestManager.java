package de.verdox.voxel.client.level.chunk;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.LongQueue;
import com.esotericsoftware.kryonet.Client;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.network.packet.client.ClientLoadChunkPacket;
import de.verdox.voxel.shared.util.ThreadUtil;
import it.unimi.dsi.fastutil.longs.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChunkRequestManager implements DebuggableOnScreen {
    private static final Logger LOGGER = Logger.getLogger(ChunkRequestManager.class.getSimpleName());

    private final Client client;
    private final ClientWorld clientWorld;

    private static final int CHUNKS_PER_TICK = 150;

    private final LongQueue pendingQueue = new LongQueue();
    private final Long2FloatMap requestedChunks = new Long2FloatOpenHashMap();

    private final LongSet received = LongSets.synchronize(new LongOpenHashSet());
    private final LongSet requested = LongSets.synchronize(new LongOpenHashSet());

    private int requestedChunksThisTick;

    private int centerX;
    private int centerY;
    private int centerZ;
    private final AtomicBoolean needRebuild = new AtomicBoolean();
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(ThreadUtil.createFactoryForName("ChunkRequestManager", true));

    public ChunkRequestManager(Client client, ClientWorld clientWorld) {
        this.client = client;

        this.clientWorld = clientWorld;
        if (ClientBase.clientRenderer != null) {
            ClientBase.clientRenderer.getDebugScreen().attach(this);
        }
        service.scheduleAtFixedRate(() -> {
            try {
                this.update();
            } catch (Throwable e) {
                LOGGER.log(Level.INFO, "Could not request new chunks.", e);
            }
        }, 0L, 50L, TimeUnit.MILLISECONDS);
    }

    public void changeCenter(int centerX, int centerY, int centerZ) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        needRebuild.set(true);
        Gdx.app.log("ChunkRequestManager", "Marked for new requests [" + centerX + ", " + centerY + ", " + centerZ + "]");
    }

    public void update() {
        if (needRebuild.get()) {

            received.clear();
            requested.clear();

            Gdx.app.log("ChunkRequestManager", "Rebuilding queue");
            pendingQueue.clear();
            requestedChunks.clear();

            int horizontalRadius = ClientBase.clientSettings.horizontalViewDistance;
            int verticalRadius = ClientBase.clientSettings.verticalViewDistance;

            int maxRadius = Math.max(verticalRadius, horizontalRadius);
            int counter = 0;

            for (int r = 0; r <= maxRadius; r++) {
                // dx, dy, dz in [-r..r]
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            // nur die Punkte auf der Oberfläche der Schale r
                            if (Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) != r) {
                                continue;
                            }
                            int x = centerX + dx;
                            int y = centerY + dy;
                            int z = centerZ + dz;

                            // nur innerhalb der erlaubten View‐Distanz
                            if (Math.abs(dx) > horizontalRadius ||
                                Math.abs(dy) > verticalRadius ||
                                Math.abs(dz) > horizontalRadius) {
                                continue;
                            }

                            if (isChunkLoaded(x, y, z)) {
                                continue;
                            }

                            long key = ChunkBase.computeChunkKey(x, y, z);
                            pendingQueue.addLast(key);
                            counter++;
                        }
                    }
                }
            }
            needRebuild.set(false);
            Gdx.app.log("ChunkRequestManager", "Rebuild done to now request " + counter + " chunks from server");
        }

        requestedChunksThisTick = 0;
        float now = getCurrentTimeSeconds();

        int requestCounter = CHUNKS_PER_TICK;

        while (requestCounter > 0 && !pendingQueue.isEmpty()) {
            long key = pendingQueue.removeFirst();

            int chunkX = ChunkBase.unpackChunkX(key);
            int chunkY = ChunkBase.unpackChunkY(key);
            int chunkZ = ChunkBase.unpackChunkZ(key);

            if (isChunkLoaded(chunkX, chunkY, chunkZ)) {
                continue;
            }
            sendRequest(key, chunkX, chunkY, chunkZ, now);
            requested.add(key);
            requestCounter--;
        }
    }

    /**
     * Muss aufgerufen werden, wenn ein Chunk vom Server empfangen wurde.
     */
    public void notifyChunkReceived(long key) {
        received.add(key);
    }

    private boolean isChunkLoaded(long key) {
        int chunkKeyX = ChunkBase.unpackChunkX(key);
        int chunkKeyY = ChunkBase.unpackChunkY(key);
        int chunkKeyZ = ChunkBase.unpackChunkZ(key);
        return isChunkLoaded(chunkKeyX, chunkKeyY, chunkKeyZ);
    }

    private boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ) {
        return clientWorld.getChunk(chunkX, chunkY, chunkZ) != null;
    }

    private void sendRequest(long chunkKey, float time) {
        int chunkKeyX = ChunkBase.unpackChunkX(chunkKey);
        int chunkKeyY = ChunkBase.unpackChunkY(chunkKey);
        int chunkKeyZ = ChunkBase.unpackChunkZ(chunkKey);
        sendRequest(chunkKey, chunkKeyX, chunkKeyY, chunkKeyZ, time);
    }

    private void sendRequest(long chunkKey, int chunkX, int chunkY, int chunkZ, float time) {
        client.sendTCP(new ClientLoadChunkPacket(clientWorld.getUuid(), chunkX, chunkY, chunkZ));
        //requestedChunks.put(chunkKey, time);
        requestedChunksThisTick++;
    }

    private float getCurrentTimeSeconds() {
        return System.nanoTime() / 1_000_000_000f;
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine("Requested chunks per tick: " + requestedChunksThisTick);
        debugScreen.addDebugTextLine("Request status: [" + requested.size() + " / " + received.size() + "]");
    }
}

