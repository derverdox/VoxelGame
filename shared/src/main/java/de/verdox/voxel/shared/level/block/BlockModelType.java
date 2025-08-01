package de.verdox.voxel.shared.level.block;

import de.verdox.voxel.shared.util.Direction;
import lombok.Getter;
import org.joml.Vector3f;

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

    public static class BlockFace {

        public static BlockFace full(Direction direction) {
            return full(direction, 1, 1);
        }

        public static BlockFace full(Direction direction, int uGrowth, int vGrowth) {
            if(uGrowth < 0 || vGrowth < 0) {
                throw new IllegalArgumentException("u and v growth must be larger than one");
            }
            Vector3f normal = new Vector3f(direction.getNx(), direction.getNy(), direction.getNz());
            Vector3f uDir = new Vector3f(direction.getUDirection()[0], direction.getUDirection()[1], direction.getUDirection()[2]);
            Vector3f vDir = new Vector3f(direction.getVDirection()[0], direction.getVDirection()[1], direction.getVDirection()[2]);

            var c0 = new Vector3f(normal).mul(0.5f).add(new Vector3f(uDir).mul(-0.5f)).add(new Vector3f(vDir).mul(-0.5f));
            var c1 = new Vector3f(c0).add(new Vector3f(uDir).mul(uGrowth));
            var c2 = new Vector3f(c0).add(new Vector3f(vDir).mul(vGrowth));
            var c3 = new Vector3f(c0).add(new Vector3f(uDir).mul(uGrowth)).add(new Vector3f(vDir).mul(vGrowth));


            return new BlockFace(
                    new RelativeCoordinate(c0.x, c0.y, c0.z),
                    new RelativeCoordinate(c1.x, c1.y, c1.z),
                    new RelativeCoordinate(c2.x, c2.y, c2.z),
                    new RelativeCoordinate(c3.x, c3.y, c3.z),
                    direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ(),
                    Direction.fromOffsets((int) direction.getUDirection()[0], (int) direction.getUDirection()[1], (int) direction.getUDirection()[2]),
                    Direction.fromOffsets((int) direction.getVDirection()[0], (int) direction.getVDirection()[1], (int) direction.getVDirection()[2])
            );
        }

        public static BlockFace of(
                RelativeCoordinate a, RelativeCoordinate b,
                RelativeCoordinate c, RelativeCoordinate d,
                Direction direction
        ) {
            RelativeCoordinate[] corners = {a, b, c, d};


            float[] uDir = direction.getUDirection();
            float[] vDir = direction.getVDirection();

            return new BlockFace(
                    corners[0], corners[1], corners[2], corners[3],
                    direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ(),
                    Direction.fromOffsets((int) uDir[0], (int) uDir[1], (int) uDir[2]),
                    Direction.fromOffsets((int) vDir[0], (int) vDir[1], (int) vDir[2])
            );
        }


        private final RelativeCoordinate c1;
        private final RelativeCoordinate c2;
        private final RelativeCoordinate c3;
        private final RelativeCoordinate c4;
        private final float normalX;
        private final float normalY;
        private final float normalZ;
        @Getter
        private final Direction uDirection;
        @Getter
        private final Direction vDirection;

        private BlockFace(
                RelativeCoordinate c1, RelativeCoordinate c2, RelativeCoordinate c3, RelativeCoordinate c4,
                float normalX, float normalY, float normalZ,
                Direction uDirection,
                Direction vDirection
        ) {
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.c4 = c4;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
            this.uDirection = uDirection;
            this.vDirection = vDirection;
        }

        public float normalX() {
            return normalX;
        }

        public float normalY() {
            return normalY;
        }

        public float normalZ() {
            return normalZ;
        }

        public RelativeCoordinate c1() {
            return c1;
        }

        public RelativeCoordinate c2() {
            return c2;
        }

        public RelativeCoordinate c3() {
            return c3;
        }

        public RelativeCoordinate c4() {
            return c4;
        }

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

        public static BlockFace south() {
            return full(Direction.SOUTH, 1, 1);
        }

        public static BlockFace north() {
            return full(Direction.NORTH, 1, 1);
        }

        public static BlockFace east() {
            return full(Direction.EAST, 1, 1);
        }

        public static BlockFace west() {
            return full(Direction.WEST, 1, 1);
        }

        public static BlockFace top() {
            return full(Direction.UP, 1, 1);
        }

        public static BlockFace bottom() {
            return full(Direction.DOWN, 1, 1);
        }

        @Override
        public String toString() {
            return "BlockFace{" +
                    "c1=" + c1 +
                    ", c2=" + c2 +
                    ", c3=" + c3 +
                    ", c4=" + c4 +
                    ", normalX=" + normalX +
                    ", normalY=" + normalY +
                    ", normalZ=" + normalZ +
                    ", uDirection=" + uDirection +
                    ", vDirection=" + vDirection +
                    '}';
        }

        /**
         * Cut this face into left half (along U axis).
         *//*
        public BlockFace leftHalf() {
            RelativeCoordinate m1 = midpoint(c1, c2);
            RelativeCoordinate m4 = midpoint(c4, c3);
            return of(c1, m1, m4, c4, normalX, normalY, normalZ);
        }

        *//**
         * Cut this face into right half (along U axis).
         *//*
        public BlockFace rightHalf() {
            RelativeCoordinate m1 = midpoint(c1, c2);
            RelativeCoordinate m4 = midpoint(c4, c3);
            return of(m1, c2, c3, m4, normalX, normalY, normalZ);
        }

        *//**
         * Cut this face into top half (along V axis).
         *//*
        public BlockFace topHalf() {
            RelativeCoordinate m2 = midpoint(c2, c3);
            RelativeCoordinate m1 = midpoint(c1, c4);
            return of(m1, m2, c3, c4, normalX, normalY, normalZ);
        }

        */

        /**
         * Cut this face into bottom half (along V axis).
         *//*
        public BlockFace bottomHalf() {
            RelativeCoordinate m2 = midpoint(c2, c3);
            RelativeCoordinate m1 = midpoint(c1, c4);
            return of(c1, c2, m2, m1, normalX, normalY, normalZ);
        }*/
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
