package de.verdox.voxel.shared;

import de.verdox.voxel.shared.level.World;
import lombok.Getter;

import java.util.*;
import java.util.logging.Logger;

@Getter
public abstract class VoxelBase<WORLD extends World> {

    @Getter
    protected static VoxelBase<?> INSTANCE = null;

    public static final Logger LOGGER = Logger.getLogger(VoxelBase.class.getSimpleName());

    private final Map<UUID, WORLD> worldStorage = new HashMap<>();
    private WORLD standardWorld;

    public VoxelBase() {
        INSTANCE = this;
    }

    public Optional<WORLD> getWorld(UUID uuid) {
        return Optional.ofNullable(worldStorage.get(uuid));
    }

    public WORLD createWorld(UUID uuid) {
        if (worldStorage.containsKey(uuid)) {
            return worldStorage.get(uuid);
        }
        LOGGER.info("New world is created " + uuid);
        WORLD world = constructNewWorld(uuid);
        if (standardWorld == null) {
            standardWorld = world;
        }
        return worldStorage.put(uuid, world);
    }

    public Set<WORLD> getWorlds() {
        return Set.copyOf(worldStorage.values());
    }

    public void insertWorld(WORLD world) {
        worldStorage.put(world.getUuid(), world);
    }

    protected abstract WORLD constructNewWorld(UUID uuid);
}
