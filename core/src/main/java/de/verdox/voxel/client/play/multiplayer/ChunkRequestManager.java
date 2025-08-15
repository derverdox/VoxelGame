package de.verdox.voxel.client.play.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.LongQueue;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.renderer.debug.DebugScreen;
import de.verdox.voxel.client.renderer.debug.DebuggableOnScreen;
import de.verdox.voxel.shared.VoxelBase;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.network.packet.client.ClientRequestChunkPacket;
import de.verdox.voxel.shared.util.ThreadUtil;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChunkRequestManager implements DebuggableOnScreen {
    private static final Logger LOGGER = Logger.getLogger(ChunkRequestManager.class.getSimpleName());

    private final ClientWorld clientWorld;

    private static final int CHUNKS_PER_TICK = 150;

    private final LongQueue pendingQueue = new LongQueue();

    private long received = 0;
    private long requested = 0;

    private int requestedChunksThisTick;

    private int centerX;
    private int centerY;
    private int centerZ;
    private final AtomicBoolean needRebuild = new AtomicBoolean();
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(ThreadUtil.createFactoryForName("ChunkRequestManager", true));

    public ChunkRequestManager(ClientWorld clientWorld) {

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

            received = 0;
            requested = 0;

            Gdx.app.log("ChunkRequestManager", "Rebuilding queue");
            pendingQueue.clear();

            int horizontalRadius = ClientBase.clientSettings.horizontalViewDistance;
            int verticalRadius = ClientBase.clientSettings.verticalViewDistance;

            VoxelBase.getInstance().clientInterface(clientInterface -> {

            });

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

                            long key = Chunk.computeChunkKey(x, y, z);
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

            int chunkX = Chunk.unpackChunkX(key);
            int chunkY = Chunk.unpackChunkY(key);
            int chunkZ = Chunk.unpackChunkZ(key);

            if (isChunkLoaded(chunkX, chunkY, chunkZ)) {
                continue;
            }
            sendRequest(key, chunkX, chunkY, chunkZ, now);
            requested++;
            requestCounter--;
        }
    }

    /**
     * Muss aufgerufen werden, wenn ein Chunk vom Server empfangen wurde.
     */
    public void notifyChunkReceived(long key) {
        received++;
    }

    private boolean isChunkLoaded(long key) {
        int chunkKeyX = Chunk.unpackChunkX(key);
        int chunkKeyY = Chunk.unpackChunkY(key);
        int chunkKeyZ = Chunk.unpackChunkZ(key);
        return isChunkLoaded(chunkKeyX, chunkKeyY, chunkKeyZ);
    }

    private boolean isChunkLoaded(int chunkX, int chunkY, int chunkZ) {
        return clientWorld.getChunkNow(chunkX, chunkY, chunkZ) != null;
    }

    private void sendRequest(long chunkKey, float time) {
        int chunkKeyX = Chunk.unpackChunkX(chunkKey);
        int chunkKeyY = Chunk.unpackChunkY(chunkKey);
        int chunkKeyZ = Chunk.unpackChunkZ(chunkKey);
        sendRequest(chunkKey, chunkKeyX, chunkKeyY, chunkKeyZ, time);
    }

    private void sendRequest(long chunkKey, int chunkX, int chunkY, int chunkZ, float time) {
        VoxelBase.getInstance().clientInterface(clientInterface -> clientInterface.sendToServer(new ClientRequestChunkPacket(clientWorld.getUuid(), chunkX, chunkY, chunkZ)));
        requestedChunksThisTick++;
    }

    private float getCurrentTimeSeconds() {
        return System.nanoTime() / 1_000_000_000f;
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine("Requested chunks per tick: " + requestedChunksThisTick);
        debugScreen.addDebugTextLine("Request status: [" + requested + " / " + received + "]");
    }
}

