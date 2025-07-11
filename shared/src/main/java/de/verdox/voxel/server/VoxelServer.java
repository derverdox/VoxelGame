package de.verdox.voxel.server;

import de.verdox.voxel.server.level.ServerWorld;
import de.verdox.voxel.shared.VoxelBase;
import lombok.Getter;

import java.util.UUID;

@Getter
public class VoxelServer extends VoxelBase<ServerWorld> {
    @Getter
    private static final VoxelServer instance = new VoxelServer();

    @Override
    protected ServerWorld constructNewWorld(UUID uuid) {
        return new ServerWorld(uuid);
    }
}
