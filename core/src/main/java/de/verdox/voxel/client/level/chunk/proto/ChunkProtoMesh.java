package de.verdox.voxel.client.level.chunk.proto;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

import java.util.Arrays;

/**
 * Holds vertex information for every block in a compact format
 */

/**
 * Kompakter und effizienter Bit-Packer für Block-Faces
 */
public class ChunkProtoMesh {
    @Getter
    private final Chunk parent;
    private final ProtoMeshStorage[][] facesPerMask;

    public ChunkProtoMesh(Chunk parent) {
        this.parent = parent;
        this.facesPerMask = new ProtoMeshStorage[ProtoMask.FaceType.values().length][ProtoMasks.getAmountMasks()];
    }

    ProtoMeshStorage getStorage(ProtoMask.FaceType faceType, ProtoMask protoMask) {
        ProtoMeshStorage storage = this.facesPerMask[faceType.ordinal()][protoMask.getMaskId()];
        if (storage == null) {
            storage = new ProtoMeshStorage();
            this.facesPerMask[faceType.ordinal()][protoMask.getMaskId()] = storage;
        }
        return storage;
    }

    public void appendToBuffers(ProtoMask.FaceType faceType, FloatArray vertices, IntArray indices, int baseVertexIndexOffset, TextureAtlas textureAtlas, byte lodLevel, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks) {
        ProtoMeshStorage[] storagesForType = facesPerMask[faceType.ordinal()];
        if (storagesForType == null) {
            return;
        }

        int vertexIndexOffset = baseVertexIndexOffset;

        for (int i = 0; i < ProtoMasks.getMASKS().size(); i++) {
            ProtoMask mask = ProtoMasks.getMASKS().get(i);
            ProtoMeshStorage storage = storagesForType[i];
            if (storage == null) {
                continue;
            }

            int faceCount = storage.getFaceCount();

            mask.appendToBuffers(this, faceType, vertices, indices, vertexIndexOffset, textureAtlas, lodLevel, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

            vertexIndexOffset += faceCount * mask.getVerticesPerFace();
        }
    }

    public void appendToInstances(ProtoMask.FaceType faceType, FloatArray instances, TextureAtlas textureAtlas, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks) {
        ProtoMeshStorage[] storagesForType = facesPerMask[faceType.ordinal()];
        if (storagesForType == null) {
            return;
        }

        for (int i = 0; i < ProtoMasks.getMASKS().size(); i++) {
            ProtoMask mask = ProtoMasks.getMASKS().get(i);
            ProtoMeshStorage storage = storagesForType[i];
            if (storage == null) {
                continue;
            }
            mask.appendToInstances(this, faceType, instances, textureAtlas, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);
        }
    }

    public int getAmountFaces(ProtoMask.FaceType faceType) {
        ProtoMeshStorage[] storagesForType = facesPerMask[faceType.ordinal()];
        if (storagesForType == null) {
            return 0;
        }
        int amount = 0;
        for (int i = 0; i < ProtoMasks.getMASKS().size(); i++) {
            ProtoMask mask = ProtoMasks.getMASKS().get(i);
            ProtoMeshStorage storage = storagesForType[i];
            if (storage == null) {
                continue;
            }
            amount += storage.getFaceCount();
        }
        return amount;
    }

    public int getAmountVertices(ProtoMask.FaceType faceType) {
        ProtoMeshStorage[] storagesForType = facesPerMask[faceType.ordinal()];
        if (storagesForType == null) {
            return 0;
        }
        int amount = 0;
        for (int i = 0; i < ProtoMasks.getMASKS().size(); i++) {
            ProtoMask mask = ProtoMasks.getMASKS().get(i);
            ProtoMeshStorage storage = storagesForType[i];
            if (storage == null) {
                continue;
            }
            amount += storage.getFaceCount() * mask.getVerticesPerFace();
        }
        return amount;
    }

    public int getAmountIndices(ProtoMask.FaceType faceType) {
        ProtoMeshStorage[] storagesForType = facesPerMask[faceType.ordinal()];
        if (storagesForType == null) {
            return 0;
        }
        int amount = 0;
        for (int i = 0; i < ProtoMasks.getMASKS().size(); i++) {
            ProtoMask mask = ProtoMasks.getMASKS().get(i);
            ProtoMeshStorage storage = storagesForType[i];
            if (storage == null) {
                continue;
            }
            amount += storage.getFaceCount() * mask.getIndicesPerFace();
        }
        return amount;
    }

    public void clear() {
        for (int i = 0; i < this.facesPerMask.length; i++) {
            Arrays.fill(facesPerMask[i], null);
        }
    }

    public int getLocalXByteSize() {
        return parent.getLocalXByteSize();
    }

    public int getLocalYByteSize() {
        return parent.getLocalYByteSize();
    }

    public int getLocalZByteSize() {
        return parent.getLocalZByteSize();
    }

    /**
     * Container für die per-get() zurückgegebenen Face-Daten.
     */
    public static class FaceData {
        public final byte x, y, z;
        public final Direction direction;
        public final byte ao, sky, red, green, blue;

        public FaceData(
                byte x, byte y, byte z, Direction direction,
                byte ao, byte sky, byte red,
                byte green, byte blue
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.direction = direction;
            this.ao = ao;
            this.sky = sky;
            this.red = red;
            this.green = green;
            this.blue = blue;
        }
    }
}

