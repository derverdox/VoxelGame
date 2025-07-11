package de.verdox.voxel.client;

import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.shared.VoxelBase;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
public class VoxelClient extends VoxelBase<ClientWorld> {

    @Setter
    private ClientWorld currentWorld;

    @Getter
    private static final VoxelClient instance = new VoxelClient();

    @Override
    protected ClientWorld constructNewWorld(UUID uuid) {
        return new ClientWorld(uuid);
    }
}
