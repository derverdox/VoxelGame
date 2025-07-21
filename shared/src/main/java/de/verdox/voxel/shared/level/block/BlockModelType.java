package de.verdox.voxel.shared.level.block;

import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;

import java.util.*;

@Getter
public class BlockModelType {

    private final Map<String, BlockFace> faces = new HashMap<>();
    private final Map<BlockFace, String> facesToName = new HashMap<>();
    private final Map<Direction, List<BlockFace>> facesByDirection = new HashMap<>();

    /**
     * Adds a block face
     * Its coordinates are relative to (0, 0, 0)
     */
    public BlockModelType addFace(String faceName, BlockFace blockFace) {
        faces.put(faceName, blockFace);
        facesToName.put(blockFace, faceName);
        facesByDirection.computeIfAbsent(blockFace.direction(), direction -> new ArrayList<>(6)).add(blockFace);
        return this;
    }

    public String getNameOfFace(BlockFace blockFace) {
        return facesToName.get(blockFace);
    }

    public List<BlockFace> getBlockFace(Direction direction) {
        return facesByDirection.getOrDefault(direction, new ArrayList<>(0));
    }

    /**
     * Returns all faces whose normal matches the given direction.
     * Use for selecting the face(s) to render or check occlusion.
     */
    public List<BlockFace> findByNormal(int dx, int dy, int dz) {
        List<BlockFace> result = new ArrayList<>();
        for (BlockFace f : getFaces().values()) {
            if ((int) f.normalX == dx && (int) f.normalY == dy && (int) f.normalZ == dz) {
                result.add(f);
            }
        }
        return result;
    }

    /**
     * A Block face holds information about a face of a block.
     */
    public record BlockFace(RelativeCoordinate c1,
                            RelativeCoordinate c2,
                            RelativeCoordinate c3,
                            RelativeCoordinate c4,
                            float normalX, float normalY, float normalZ) {

        /**
         * Determines the cardinal direction of this face based on its normal vector.
         * <p>
         * Uses Math.signum to convert the float normal into integer offsets (-1, 0, or 1)
         * and dispatches to Direction.fromOffsets.
         *
         * @return the matching {@link Direction} enum value
         */
        public Direction direction() {
            int dx = (int) Math.signum(normalX);
            int dy = (int) Math.signum(normalY);
            int dz = (int) Math.signum(normalZ);
            return Direction.fromOffsets(dx, dy, dz);
        }

        /**
         * Given another BlockModel, find all faces on it that are opposite to this face.
         * Opposing faces have exactly opposite normals and lie in the same plane.
         */
        public List<BlockFace> findOpposingBlockFaces(BlockModelType blockModelType) {
            List<BlockFace> result = new ArrayList<>();
            for (BlockFace other : blockModelType.getFaces().values()) {
                if (normalX + other.normalX != 0f ||
                    normalY + other.normalY != 0f ||
                    normalZ + other.normalZ != 0f) continue;
                result.add(other);
            }
            return result;
        }

        /**
         * Checks whether this face lies on the boundary of the unit cube (±0.5),
         * and thus can potentially be occluded by an opposing face.
         */
        public boolean canBeOccluded() {
            if (Math.abs(normalX) == 1f) {
                float planeX = normalX * 0.5f;
                return c1.x() == planeX
                    && c2.x() == planeX
                    && c3.x() == planeX
                    && c4.x() == planeX;
            } else if (Math.abs(normalY) == 1f) {
                float planeY = normalY * 0.5f;
                return c1.y() == planeY
                    && c2.y() == planeY
                    && c3.y() == planeY
                    && c4.y() == planeY;
            } else {
                float planeZ = normalZ * 0.5f;
                return c1.z() == planeZ
                    && c2.z() == planeZ
                    && c3.z() == planeZ
                    && c4.z() == planeZ;
            }
        }

        /**
         * Returns true if this face has a strictly greater area than the given face.
         */
        public boolean isLarger(BlockFace other) {
            // Compute edge vectors in 3D
            RelativeCoordinate a = c1;
            RelativeCoordinate b = c2;
            RelativeCoordinate d = c4;
            float edge1 = distance(a, b);
            float edge2 = distance(a, d);
            float myArea = edge1 * edge2;

            RelativeCoordinate oa = other.c1;
            RelativeCoordinate ob = other.c2;
            RelativeCoordinate od = other.c4;
            float oEdge1 = distance(oa, ob);
            float oEdge2 = distance(oa, od);
            float otherArea = oEdge1 * oEdge2;

            return myArea > otherArea;
        }

        /**
         * Returns true if this face has a greater or equal area than the given face.
         */
        public boolean isLargerOrEqual(BlockFace other) {
            // Compute edge vectors in 3D
            RelativeCoordinate a = c1;
            RelativeCoordinate b = c2;
            RelativeCoordinate d = c4;
            float edge1 = distance(a, b);
            float edge2 = distance(a, d);
            float myArea = edge1 * edge2;

            RelativeCoordinate oa = other.c1;
            RelativeCoordinate ob = other.c2;
            RelativeCoordinate od = other.c4;
            float oEdge1 = distance(oa, ob);
            float oEdge2 = distance(oa, od);
            float otherArea = oEdge1 * oEdge2;

            return myArea >= otherArea;
        }

        public static boolean areOpposite(BlockFace a, BlockFace b) {
            if (a.normalX + b.normalX != 0f ||
                a.normalY + b.normalY != 0f ||
                a.normalZ + b.normalZ != 0f) {
                return false;
            }
            // Feste Achse (die nicht variiert) müssen gleich sein:
            // z.B. wenn |normalZ|==1, dann a.c1.z == b.c1.z
            if (Math.abs(a.normalX) == 1f)
                return a.c1().x() == b.c1().x();
            if (Math.abs(a.normalY) == 1f)
                return a.c1().y() == b.c1().y();
            // else Z-Faces
            return a.c1().z() == b.c1().z();
        }

        public static BlockFace front() {
            return new BlockFace(
                new RelativeCoordinate(-0.5f, -0.5f, +0.5f),
                new RelativeCoordinate(+0.5f, -0.5f, +0.5f),
                new RelativeCoordinate(+0.5f, +0.5f, +0.5f),
                new RelativeCoordinate(-0.5f, +0.5f, +0.5f),
                0f, 0f, +1f
            );
        }

        public static BlockFace back() {
            return new BlockFace(
                new RelativeCoordinate(+0.5f, -0.5f, -0.5f),
                new RelativeCoordinate(-0.5f, -0.5f, -0.5f),
                new RelativeCoordinate(-0.5f, +0.5f, -0.5f),
                new RelativeCoordinate(+0.5f, +0.5f, -0.5f),
                0f, 0f, -1f
            );
        }

        public static BlockFace left() {
            return new BlockFace(
                new RelativeCoordinate(-0.5f, -0.5f, -0.5f),
                new RelativeCoordinate(-0.5f, -0.5f, +0.5f),
                new RelativeCoordinate(-0.5f, +0.5f, +0.5f),
                new RelativeCoordinate(-0.5f, +0.5f, -0.5f),
                -1f, 0f, 0f
            );
        }

        public static BlockFace right() {
            return new BlockFace(
                new RelativeCoordinate(+0.5f, -0.5f, +0.5f),
                new RelativeCoordinate(+0.5f, -0.5f, -0.5f),
                new RelativeCoordinate(+0.5f, +0.5f, -0.5f),
                new RelativeCoordinate(+0.5f, +0.5f, +0.5f),
                +1f, 0f, 0f
            );
        }

        public static BlockFace top() {
            return new BlockFace(
                new RelativeCoordinate(-0.5f, +0.5f, +0.5f),
                new RelativeCoordinate(+0.5f, +0.5f, +0.5f),
                new RelativeCoordinate(+0.5f, +0.5f, -0.5f),
                new RelativeCoordinate(-0.5f, +0.5f, -0.5f),
                0f, +1f, 0f
            );
        }

        public static BlockFace bottom() {
            return new BlockFace(
                new RelativeCoordinate(-0.5f, -0.5f, -0.5f),
                new RelativeCoordinate(+0.5f, -0.5f, -0.5f),
                new RelativeCoordinate(+0.5f, -0.5f, +0.5f),
                new RelativeCoordinate(-0.5f, -0.5f, +0.5f),
                0f, -1f, 0f
            );
        }

        /**
         * Cut this face into left half (along U axis).
         */
        public BlockFace leftHalf() {
            RelativeCoordinate m1 = midpoint(c1, c2);
            RelativeCoordinate m4 = midpoint(c4, c3);
            return new BlockFace(c1, m1, m4, c4, normalX, normalY, normalZ);
        }

        /**
         * Cut this face into right half (along U axis).
         */
        public BlockFace rightHalf() {
            RelativeCoordinate m1 = midpoint(c1, c2);
            RelativeCoordinate m4 = midpoint(c4, c3);
            return new BlockFace(m1, c2, c3, m4, normalX, normalY, normalZ);
        }

        /**
         * Cut this face into top half (along V axis).
         */
        public BlockFace topHalf() {
            RelativeCoordinate m2 = midpoint(c2, c3);
            RelativeCoordinate m1 = midpoint(c1, c4);
            return new BlockFace(m1, m2, c3, c4, normalX, normalY, normalZ);
        }

        /**
         * Cut this face into bottom half (along V axis).
         */
        public BlockFace bottomHalf() {
            RelativeCoordinate m2 = midpoint(c2, c3);
            RelativeCoordinate m1 = midpoint(c1, c4);
            return new BlockFace(c1, c2, m2, m1, normalX, normalY, normalZ);
        }

        private static RelativeCoordinate midpoint(RelativeCoordinate a, RelativeCoordinate b) {
            return new RelativeCoordinate(
                (a.x() + b.x()) * 0.5f,
                (a.y() + b.y()) * 0.5f,
                (a.z() + b.z()) * 0.5f
            );
        }

        /**
         * Relative coordinates in a block model. Relative to 0,0,0
         */
        public record RelativeCoordinate(float x, float y, float z) {
            public boolean onPlane(float plane, float nx, float ny, float nz) {
                if (nx != 0) return x == plane;
                if (ny != 0) return y == plane;
                return z == plane;
            }

            @Override
            public String toString() {
                return "{" +
                    "x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
            }
        }

        private static float distance(RelativeCoordinate p1, RelativeCoordinate p2) {
            float dx = p2.x() - p1.x();
            float dy = p2.y() - p1.y();
            float dz = p2.z() - p1.z();
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}
