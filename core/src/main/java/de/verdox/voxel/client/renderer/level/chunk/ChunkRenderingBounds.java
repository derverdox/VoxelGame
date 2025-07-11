package de.verdox.voxel.client.renderer.level.chunk;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import de.verdox.voxel.shared.level.World;
import de.verdox.voxel.shared.level.chunk.ChunkBase;

/** Hilfsklasse für die Rückgabe der Chunk‐Bounds */
@Deprecated
public class ChunkRenderingBounds {
    public final int startX, endX;
    public final int startY, endY;
    public final int startZ, endZ;

    public ChunkRenderingBounds(int sx, int ex, int sy, int ey, int sz, int ez) {
        this.startX = sx; this.endX = ex;
        this.startY = sy; this.endY = ey;
        this.startZ = sz; this.endZ = ez;
    }


    public static ChunkRenderingBounds computeChunkLoopBounds(
        PerspectiveCamera cam,
        World world,
        int renderDistanceInChunks
    )
    {
        // 1) Welt‐Einheiten bis zum äußersten Frustum‐Far‐Plane
        float chunkSize = 16; //TODO: Change       // z.B. 16
        float maxWorldDist = renderDistanceInChunks * chunkSize;

        // 2) Frustum‐Far‐Plane‐Corners in Weltkoordinaten
        Vector3[] corners = computeFrustumFarPlaneCorners(cam, maxWorldDist);

        // 3) Envelope inkl. Kameraposition
        float minX = cam.position.x, maxX = cam.position.x;
        float minY = cam.position.y, maxY = cam.position.y;
        float minZ = cam.position.z, maxZ = cam.position.z;
        for (Vector3 c : corners) {
            minX = Math.min(minX, c.x); maxX = Math.max(maxX, c.x);
            minY = Math.min(minY, c.y); maxY = Math.max(maxY, c.y);
            minZ = Math.min(minZ, c.z); maxZ = Math.max(maxZ, c.z);
        }

        int sx = ChunkBase.chunkX(world, (int) minX);
        int ex = ChunkBase.chunkX(world, (int) maxX);
        int sy = ChunkBase.chunkY(world, (int) minY);
        int ey = ChunkBase.chunkY(world, (int) maxY);
        int sz = ChunkBase.chunkZ(world, (int) minZ);
        int ez = ChunkBase.chunkZ(world, (int) maxZ);

        return new ChunkRenderingBounds(sx, ex, sy, ey, sz, ez);
    }

    /**
     * Liefert die 4 Ecken des Far‐Planes (in Weltkoordinaten),
     * basierend auf Kamera‐FOV, Aspect‐Ratio und einer Distanz.
     */
    private static Vector3[] computeFrustumFarPlaneCorners(PerspectiveCamera cam, float farDistance) {
        // Vertical half‐FOV in Radiant
        float vHalfFov = cam.fieldOfView * MathUtils.degreesToRadians * 0.5f;
        // Aspect‐Ratio
        float aspect = (float)cam.viewportWidth / cam.viewportHeight;
        // Far‐Plane‐Halb‐Ausdehnungen
        float halfH = (float)(Math.tan(vHalfFov) * farDistance);
        float halfW = halfH * aspect;

        // Mittelpunkt auf dem Far‐Plane
        Vector3 center = new Vector3(cam.direction).nor().scl(farDistance).add(cam.position);

        // Lokale Rechts‐ und Oben‐Vektoren
        Vector3 right = new Vector3(cam.direction).crs(cam.up).nor();
        Vector3 up    = new Vector3(right).crs(cam.direction).nor();

        // Die 4 Ecken
        return new Vector3[]{
            new Vector3(center).add(new Vector3(right).scl( halfW)).add(new Vector3(up).scl( halfH)),
            new Vector3(center).add(new Vector3(right).scl(-halfW)).add(new Vector3(up).scl( halfH)),
            new Vector3(center).add(new Vector3(right).scl(-halfW)).add(new Vector3(up).scl(-halfH)),
            new Vector3(center).add(new Vector3(right).scl( halfW)).add(new Vector3(up).scl(-halfH))
        };
    }
}
