package de.verdox.voxel.shared;

import de.verdox.voxel.shared.level.world.LevelWorld;
import de.verdox.voxel.shared.level.world.World;
import de.verdox.voxel.shared.network.packet.client.ClientInterface;
import de.verdox.voxel.shared.network.packet.server.ServerInterface;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Getter
public class VoxelBase {

    public static void createNewVoxelBase() {
        Instance = new VoxelBase();
    }

    @Getter
    protected static VoxelBase Instance = null;

    public static final Logger LOGGER = Logger.getLogger(VoxelBase.class.getSimpleName());

    private final Map<UUID, World> worldStorage = new HashMap<>();
    @Setter
    private ClientInterface clientInterface;
    @Setter
    private ServerInterface serverInterface;
    private World standardWorld;

    public VoxelBase() {
        Instance = this;
    }

    public Optional<World> getWorld(UUID uuid) {
        return Optional.ofNullable(worldStorage.get(uuid));
    }

    public World createWorld(UUID uuid) {
        if (worldStorage.containsKey(uuid)) {
            return worldStorage.get(uuid);
        }
        LOGGER.info("New world is created " + uuid);
        World world = new LevelWorld(uuid);
        if (standardWorld == null) {
            standardWorld = world;
        }
        return worldStorage.put(uuid, world);
    }

    public Set<World> getWorlds() {
        return Set.copyOf(worldStorage.values());
    }

    public void clientInterface(Consumer<ClientInterface> consumer) {
        if (clientInterface != null) {
            consumer.accept(clientInterface);
        }
    }

    public void serverInterface(Consumer<ServerInterface> consumer) {
        if (serverInterface != null) {
            consumer.accept(serverInterface);
        }
    }
}
