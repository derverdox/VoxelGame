package de.verdox.voxel.shared.util;

public interface Delegate<PARENT extends DelegateBase<?>> {
    PARENT getOwner();
}
