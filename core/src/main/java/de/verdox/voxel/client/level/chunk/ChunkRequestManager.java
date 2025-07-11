package de.verdox.voxel.client.level.chunk;

import com.esotericsoftware.kryonet.Client;
import de.verdox.voxel.client.ClientBase;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.renderer.DebugScreen;
import de.verdox.voxel.client.renderer.DebuggableOnScreen;
import de.verdox.voxel.shared.network.packet.client.ClientLoadChunkPacket;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkRequestManager implements DebuggableOnScreen {

    private final Client client;
    private final ClientWorld clientWorld;

    private static final float RESEND_INTERVAL = 1.0f;
    private static final int CHUNKS_PER_TICK = 150;

    private final Queue<Vector3i> pendingQueue = new ArrayDeque<>();
    private final Map<Vector3i, Float> requestedChunks = new ConcurrentHashMap<>();

    private Iterator<Map.Entry<Vector3i, Float>> resendIterator = null;

    private int requestedChunksThisTick;

    public ChunkRequestManager(Client client, ClientWorld clientWorld) {
        this.client = client;
        this.clientWorld = clientWorld;
        ClientBase.clientRenderer.getDebugScreen().attach(this);
    }

    /**
     * Setzt die Menge aller Chunks, die angefragt werden sollen.
     * Alte Requests werden verworfen.
     */
    public void setChunksToRequest(Set<Vector3i> chunks) {
        pendingQueue.clear();
        requestedChunks.clear();
        pendingQueue.addAll(chunks);
    }

    /**
     * Muss pro Tick aufgerufen werden.
     */
    public void update(float dt) {
        requestedChunksThisTick = 0;
        float now = getCurrentTimeSeconds();

        // Neue Requests aus der Queue

        int requestCounter = CHUNKS_PER_TICK;

        while (requestCounter > 0 && !pendingQueue.isEmpty()) {
            Vector3i coord = pendingQueue.poll();
            if (isChunkLoaded(coord)) {
                continue;
            }
            sendRequest(coord, now);
            requestCounter--;
        }
        // Resend-Iterator initialisieren falls leer
        if (resendIterator == null || !resendIterator.hasNext()) {
            resendIterator = requestedChunks.entrySet().iterator();
        }

        // Pro Tick maximal X Resends prüfen
        int resendsThisTick = 0;
        while (resendsThisTick < CHUNKS_PER_TICK && resendIterator.hasNext()) {
            Map.Entry<Vector3i, Float> entry = resendIterator.next();
            Vector3i coord = entry.getKey();
            float lastRequest = entry.getValue();

            if (isChunkLoaded(coord)) {
                // Nicht mehr nötig – wird nachher entfernt
                continue;
            }
            if ((now - lastRequest) >= RESEND_INTERVAL) {
                sendRequest(coord, now);
                resendsThisTick++;
            }
        }

        // Bereits erhaltene Chunks austragen
        requestedChunks.entrySet().removeIf(e -> isChunkLoaded(e.getKey()));
    }

    /**
     * Muss aufgerufen werden, wenn ein Chunk vom Server empfangen wurde.
     */
    public void notifyChunkReceived(int x, int y, int z) {
        requestedChunks.remove(new Vector3i(x, y, z));
    }

    private boolean isChunkLoaded(Vector3i coord) {
        return clientWorld.getChunk(coord.x, coord.y, coord.z) != null;
    }

    private void sendRequest(Vector3i coord, float time) {
        client.sendTCP(new ClientLoadChunkPacket(clientWorld.getUuid(), coord.x, coord.y, coord.z));
        requestedChunks.put(coord, time);
        requestedChunksThisTick++;
    }

    private float getCurrentTimeSeconds() {
        return System.nanoTime() / 1_000_000_000f;
    }

    @Override
    public void debugText(DebugScreen debugScreen) {
        debugScreen.addDebugTextLine("Requested chunks per tick: " + requestedChunksThisTick);
    }
}

