package de.verdox.voxel.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Null;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Server;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.play.ClientInterfaceImpl;
import de.verdox.voxel.client.play.singleplayer.LocalServerInterface;
import de.verdox.voxel.shared.VoxelBase;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.network.packet.client.ClientInterface;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameSession {
    private static final ExecutorService clientMain = Executors.newSingleThreadExecutor();

    public static void postRunnable(Runnable runnable) {
        Gdx.app.postRunnable(runnable);
    }

    @Getter
    private static GameSession instance;

    public static void startLocalHostSession(Server localServer) {
        ClientInterface clientInterface = new ClientInterfaceImpl(null);
        LocalServerInterface serverInterface = new LocalServerInterface(localServer, clientInterface);
        instance = new GameSession(clientInterface, serverInterface);
        serverInterface.localConnect();
        ;
    }

    public static void startRemoteSession(Client client) {
        ClientInterface clientInterface = new ClientInterfaceImpl(client);
        instance = new GameSession(clientInterface, null);
    }

    @Getter
    private ClientWorld currentWorld;

    private final Map<UUID, ClientWorld> worlds = new ConcurrentHashMap<>();

    public GameSession(ClientInterface clientInterface, @Null LocalServerInterface serverInterface) {
        VoxelBase.getInstance().setClientInterface(clientInterface);
        if (serverInterface != null) {
            VoxelBase.getInstance().setServerInterface(serverInterface);
        }
    }

    public void addWorld(World world) {
        ClientWorld clientWorld = new ClientWorld(world);
        worlds.put(world.getUuid(), clientWorld);
    }

    public void selectCurrentWorld(UUID uuid) {
        if (worlds.containsKey(uuid)) {
            currentWorld = worlds.get(uuid);
        }
    }
}
