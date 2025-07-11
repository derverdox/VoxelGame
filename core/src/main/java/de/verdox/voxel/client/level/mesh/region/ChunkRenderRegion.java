package de.verdox.voxel.client.level.mesh.region;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import de.verdox.voxel.client.level.ClientWorld;
import lombok.Getter;
import lombok.Setter;

@Getter
@Deprecated
public class ChunkRenderRegion {
    private final ClientWorld world;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final int minChunkX, minChunkY, minChunkZ;
    @Setter
    private boolean isDirty;

    private final ModelCache cache;
    private final BoundingBox bounds;

    public ChunkRenderRegion(ClientWorld world, int sizeX, int sizeY, int sizeZ, int minChunkX, int minChunkY, int minChunkZ) {
        this.world = world;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.minChunkX = minChunkX;
        this.minChunkY = minChunkY;
        this.minChunkZ = minChunkZ;

        this.cache = new ModelCache();
        this.bounds = computeBounds();
        this.isDirty = true;
    }

    private BoundingBox computeBounds() {
        float x = minChunkX * world.getChunkSizeX();
        float y = minChunkY * world.getChunkSizeY();
        float z = minChunkZ * world.getChunkSizeZ();
        return new BoundingBox(
            new Vector3(x, y, z),
            new Vector3(
                x + sizeX * world.getChunkSizeX(),
                y + sizeY * world.getChunkSizeY(),
                z + sizeZ * world.getChunkSizeZ()
            )
        );
    }

    public boolean contains(int cx, int cy, int cz) {
        return cx >= minChunkX && cx < minChunkX + sizeX
            && cy >= minChunkY && cy < minChunkY + sizeY
            && cz >= minChunkZ && cz < minChunkZ + sizeZ;
    }

    public void renderBoundariesForDebug(ClientWorld world, Camera camera, ShapeRenderer shapeRenderer) {
        if (sizeX == 8) {
            Gdx.graphics.getGL20().glLineWidth(2);
            shapeRenderer.setColor(Color.WHITE);
        } else if (sizeX == 16) {
            Gdx.graphics.getGL20().glLineWidth(3);
            shapeRenderer.setColor(Color.LIGHT_GRAY);
        } else if (sizeX == 32) {
            Gdx.graphics.getGL20().glLineWidth(4);
            shapeRenderer.setColor(Color.GRAY);
        } else if (sizeX == 64) {
            Gdx.graphics.getGL20().glLineWidth(5);
            shapeRenderer.setColor(Color.BLACK);
        } else {
            Gdx.graphics.getGL20().glLineWidth(1);
            shapeRenderer.setColor(Color.RED);
        }

        shapeRenderer.setProjectionMatrix(camera.combined);

        float worldX = minChunkX * world.getChunkSizeX();
        float worldY = minChunkY * world.getChunkSizeY();
        float worldZ = minChunkZ * world.getChunkSizeZ();

        float dx = sizeX * world.getChunkSizeX();
        float dy = sizeY * world.getChunkSizeY();
        float dz = sizeZ * world.getChunkSizeZ();

        shapeRenderer.box(worldX, worldY, worldZ, dx, dy, dz);
    }

    @Override
    public String toString() {
        return "ChunkRenderRegion{" +
            "world=" + world +
            ", sizeX=" + sizeX +
            ", sizeY=" + sizeY +
            ", sizeZ=" + sizeZ +
            ", minChunkX=" + minChunkX +
            ", minChunkY=" + minChunkY +
            ", minChunkZ=" + minChunkZ +
            ", isDirty=" + isDirty +
            ", cache=" + cache +
            ", bounds=" + bounds +
            '}';
    }
}

