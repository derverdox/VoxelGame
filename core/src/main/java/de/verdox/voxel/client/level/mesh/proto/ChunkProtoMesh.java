package de.verdox.voxel.client.level.mesh.proto;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
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
    private static final float[] DUMMY_FLOAT_ARRAY = new float[0];
    private static final int[] DUMMY_INT_ARRAY = new int[0];

    @Getter
    private final Chunk parent;
    private final ProtoMeshStorage[][] facesPerMask;

    public ChunkProtoMesh(Chunk parent) {
        this.parent = parent;
        this.facesPerMask = new ProtoMeshStorage[ProtoMask.Type.values().length][ProtoMasks.getAmountMasks()];
    }

    ProtoMeshStorage getStorage(ProtoMask.Type type, ProtoMask protoMask) {
        ProtoMeshStorage storage = this.facesPerMask[type.ordinal()][protoMask.getMaskId()];
        if (storage == null) {
            storage = new ProtoMeshStorage();
            this.facesPerMask[type.ordinal()][protoMask.getMaskId()] = storage;
        }
        return storage;
    }

    public void appendToBuffers(ProtoMask.Type type, float[] vertices, int[] indices, int baseVertexIndexOffset, TextureAtlas textureAtlas, byte lodLevel, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks) {
        ProtoMeshStorage[] storagesForType = facesPerMask[type.ordinal()];
        if (storagesForType == null) {
            return;
        }

        int vertexOffset = 0;
        int indexOffset = 0;
        int vertexIndexOffset = baseVertexIndexOffset;

        for (int i = 0; i < ProtoMasks.getMASKS().size(); i++) {
            ProtoMask mask = ProtoMasks.getMASKS().get(i);
            ProtoMeshStorage storage = storagesForType[i];
            if (storage == null) {
                continue;
            }

            int faceCount = storage.getFaceCount();

            mask.appendToBuffers(this, type, vertices, indices, vertexOffset, indexOffset, vertexIndexOffset, textureAtlas, lodLevel, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

            vertexOffset += faceCount * mask.getVerticesPerFace() * mask.getFloatsPerVertex();
            indexOffset += faceCount * mask.getIndicesPerFace();
            vertexIndexOffset += faceCount * mask.getVerticesPerFace();
        }
    }

    public int getAmountFaces(ProtoMask.Type type) {
        ProtoMeshStorage[] storagesForType = facesPerMask[type.ordinal()];
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

    public int getAmountVertices(ProtoMask.Type type) {
        ProtoMeshStorage[] storagesForType = facesPerMask[type.ordinal()];
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

    public int getAmountIndices(ProtoMask.Type type) {
        ProtoMeshStorage[] storagesForType = facesPerMask[type.ordinal()];
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

    public float[] createArrayForVertices(ProtoMask.Type type) {
        ProtoMeshStorage[] storagesForType = facesPerMask[type.ordinal()];
        if (storagesForType == null) {
            return DUMMY_FLOAT_ARRAY;
        }
        int amount = 0;
        for (int i = 0; i < ProtoMasks.getMASKS().size(); i++) {
            ProtoMask mask = ProtoMasks.getMASKS().get(i);
            ProtoMeshStorage storage = storagesForType[i];
            if (storage == null) {
                continue;
            }
            amount += storage.getFaceCount() * mask.getVerticesPerFace() * mask.getFloatsPerVertex();
        }
        return new float[amount];
    }

    public int[] createArrayForIndices(ProtoMask.Type type) {
        ProtoMeshStorage[] storagesForType = facesPerMask[type.ordinal()];
        if (storagesForType == null) {
            return DUMMY_INT_ARRAY;
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
        return new int[amount];
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

