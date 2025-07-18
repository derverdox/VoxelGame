package de.verdox.voxel.test.light;

import de.verdox.voxel.shared.lighting.ChunkLightEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LightingTests {
    @Test
    public void testSimpleSkylightPropagation() {
        int CX = 16, CY = 16, CZ = 16;
        byte worldSky = 15;
        DummyLightAccessor chunk = new DummyLightAccessor(CX, CY, CZ, worldSky);
        DummyLightAccessor above = new DummyLightAccessor(CX, CY, CZ, worldSky);

        for (byte x=0; x<CX; x++)
            for (byte z=0; z<CZ; z++)
                above.setSkyLight(x, (byte)0, z, worldSky);

        ChunkLightEngine.computeSkylight(chunk, false);

        byte expected = (byte)(worldSky - 1);
        for (byte x=0; x<CX; x++)
            for (byte z=0; z<CZ; z++)
                assertEquals(expected, chunk.getSkyLight(x, (byte)(CY-1), z));
    }
}
