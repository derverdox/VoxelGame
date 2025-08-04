package de.verdox.voxel.shared.util;

import java.util.List;

public interface DelegateBase<DELEGATE extends Delegate<?>> {
    default void subscribe(DELEGATE delegate) {
        getDelegates().add(delegate);
    }

    default void unsubscribe(DELEGATE delegate) {
        getDelegates().remove(delegate);
    }

    List<DELEGATE> getDelegates();
}
