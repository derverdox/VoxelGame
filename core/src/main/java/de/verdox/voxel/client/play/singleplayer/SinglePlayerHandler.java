package de.verdox.voxel.client.play.singleplayer;

import com.esotericsoftware.kryonet.Server;
import de.verdox.voxel.client.GameSession;
import de.verdox.voxel.shared.Bootstrap;
import de.verdox.voxel.shared.VoxelBase;

import java.util.UUID;

public class SinglePlayerHandler {
    public static void createNewWorldAndJoin() {
        VoxelBase.createNewVoxelBase();
        int writeBufferSize  = 1024 * 1024 * 16;
        int objectBufferSize = 1024 * 1024 * 16;

        Server server = new Server(writeBufferSize, objectBufferSize);
        server.start();
        Bootstrap.bootstrap(server.getKryo());

        VoxelBase.getInstance().createWorld(UUID.randomUUID());
        GameSession.startLocalHostSession(server);
    }
}
