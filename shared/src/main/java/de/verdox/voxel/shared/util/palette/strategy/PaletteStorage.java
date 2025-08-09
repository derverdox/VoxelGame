package de.verdox.voxel.shared.util.palette.strategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.Getter;

import java.util.Arrays;

public interface PaletteStorage {

    static PaletteStorage create(PaletteStrategy.Paletted<?> owner, int bitsPerBlock) {
        if (bitsPerBlock < 8) return new ByteStore(owner, bitsPerBlock);
        if (bitsPerBlock < 16) return new ShortStore(owner, bitsPerBlock);
        if (bitsPerBlock < 32) return new IntStore(owner, bitsPerBlock);
        return new LongStore(owner, bitsPerBlock);
    }

    static PaletteStorage resizeIfNeeded(PaletteStrategy.Paletted<?> owner, PaletteStorage current) {
        int needed = computeRequiredBitsPerEntry(owner.getPaletteSize());
        if (needed < current.getMaxBitsPerEntry() && needed >= current.getMinBitsPerEntry()) {
            current.resizeIfNeeded();
            current.setBitsPerBlock(needed);
            return current;
        } else {
            PaletteStorage next = create(owner, needed);
            for (int i = 0; i < owner.getTotalSize(); i++) {
                next.write(i, current.read(i));
            }
            next.setBitsPerBlock(needed);
            return next;
        }
    }

    int read(int idx);

    void write(int idx, int value);

    int getMaxBitsPerEntry();

    int getMinBitsPerEntry();

    void resizeIfNeeded();

    boolean isInBounds(int idx);

    void write(Kryo kryo, Output output);

    void read(Kryo kryo, Input input);

    int bytesFor(int bits);

    void fill(int id);

    void setBitsPerBlock(int bitsPerBlock);

    int getBitsPerBlock();

    int getBitMask();

    abstract class AbstractPaletteStorage implements PaletteStorage {
        @Getter
        protected int bitsPerBlock;
        @Getter
        protected int bitMask;

        public AbstractPaletteStorage(int bitsPerBlock) {
            setBitsPerBlock(bitsPerBlock);
        }

        @Override
        public void setBitsPerBlock(int bitsPerBlock) {
            this.bitsPerBlock = bitsPerBlock;
            this.bitMask = (1 << bitsPerBlock) - 1;
        }
    }

    class ByteStore extends AbstractPaletteStorage {
        private final PaletteStrategy.Paletted<?> owner;
        private byte[] data;

        public ByteStore(PaletteStrategy.Paletted<?> owner, int bitsPerBlock) {
            super(bitsPerBlock);
            this.owner = owner;
            this.data = new byte[bytesFor(bitsPerBlock)];
        }

        public int bytesFor(int bits) {
            return ((owner.getTotalSize() * bits) + 7) >>> 3;
        }

        @Override
        public void fill(int id) {
            int total = owner.getTotalSize();
            int bpe = getBitsPerBlock();

            int totalBits = total * bpe;
            int bytesLen = (totalBits + 7) >>> 3;

            if (bpe == 8) {
                Arrays.fill(data, 0, bytesLen, (byte) id);
                return;
            }

            int g = gcd(bpe, 8);
            int pLen = bpe / g;

            int mask = (1 << bpe) - 1;
            id &= mask;

            byte[] pattern = new byte[pLen];
            for (int i = 0; i < pLen; i++) {
                int start = (i * 8) % bpe;
                pattern[i] = (byte) (((id >>> start) | (id << (bpe - start))) & 0xFF);
            }

            System.arraycopy(pattern, 0, data, 0, pLen);
            int filled = pLen;
            while (filled < bytesLen) {
                int copy = Math.min(filled, bytesLen - filled);
                System.arraycopy(data, 0, data, filled, copy);
                filled += copy;
            }
        }

        @Override
        public int read(int idx) {
            final int bpe = bitsPerBlock; // lokal cachen
            final int bitPos = idx * bpe;
            final int off = bitPos & 7;
            final int byteIdx = bitPos >>> 3;

            // Sicher lesen, High-Byte am Ende als 0 behandeln
            final int lo = data[byteIdx] & 0xFF;
            final int hi = (byteIdx + 1 < data.length) ? (data[byteIdx + 1] & 0xFF) : 0;

            final int two = lo | (hi << 8);
            return (two >>> off) & bitMask;
        }

        @Override
        public void write(int idx, int value) {
            int bitPos = idx * bitsPerBlock;
            int off = bitPos & 7;
            int byteIdx = bitPos >>> 3;
            int mask = bitMask << off;

            // clear + set im ersten Byte
            int b = data[byteIdx] & 0xFF;
            b = (b & ~mask) | ((value << off) & mask);
            data[byteIdx] = (byte) b;

            int overflow = off + bitsPerBlock - 8;
            if (overflow > 0) {
                int mask2 = (1 << overflow) - 1;
                int b2 = data[byteIdx + 1] & 0xFF;
                b2 = (b2 & ~mask2) | ((value >>> (bitsPerBlock - overflow)) & mask2);
                data[byteIdx + 1] = (byte) b2;
            }
        }

        @Override
        public void resizeIfNeeded() {
            int needed = computeRequiredBitsPerEntry(owner.getPaletteSize());
            if (needed == bitsPerBlock) return;

            int total = owner.getTotalSize();
            int bytesLen = ((total * needed) + 7) >>> 3;
            byte[] newData = new byte[bytesLen];

            // alle alten Werte umpacken
            for (int i = 0; i < total; i++) {
                int v = read(i);
                int bitPos = i * needed;
                int off = bitPos & 7;
                int byteIdx = bitPos >>> 3;
                int mask = ((1 << needed) - 1) << off;

                // write low part
                int b = newData[byteIdx] & 0xFF;
                b = (b & ~mask) | ((v << off) & mask);
                newData[byteIdx] = (byte) b;

                int overflow = off + needed - 8;
                if (overflow > 0) {
                    int mask2 = (1 << overflow) - 1;
                    int b2 = newData[byteIdx + 1] & 0xFF;
                    b2 = (b2 & ~mask2) | ((v >>> (needed - overflow)) & mask2);
                    newData[byteIdx + 1] = (byte) b2;
                }
            }

            this.data = newData;
            setBitsPerBlock(needed);
        }

        // Hilfsfunktion (wie gehabt)
        private static int gcd(int a, int b) {
            while (b != 0) {
                int t = a % b;
                a = b;
                b = t;
            }
            return a;
        }

        @Override
        public boolean isInBounds(int idx) {
            return idx > 0 && idx < data.length;
        }

        @Override
        public void write(Kryo kryo, Output output) {
            output.writeInt(data.length);
            output.writeBytes(data);
        }

        @Override
        public void read(Kryo kryo, Input input) {
            int dataLength = input.readInt();
            this.data = input.readBytes(dataLength);
        }

        @Override
        public int getMaxBitsPerEntry() {
            return 8;
        }

        @Override
        public int getMinBitsPerEntry() {
            return 0;
        }
    }

    class ShortStore extends AbstractPaletteStorage {
        private final PaletteStrategy.Paletted<?> owner;
        private byte[] data;

        public ShortStore(PaletteStrategy.Paletted<?> owner, int bitsPerBlock) {
            super(bitsPerBlock);
            this.owner = owner;
            this.data = new byte[bytesFor(bitsPerBlock)];
        }

        public int bytesFor(int bits) {
            return ((owner.getTotalSize() * bits) + 7) >>> 3;
        }

        @Override
        public void fill(int id) {
            int total = owner.getTotalSize();
            int bpe = bitsPerBlock;
            int mask = (1 << bpe) - 1;
            id &= mask;

            // Spezialfall: wenn bpe == 16 könnte man Arrays.fill(shortArray, (short)id) nutzen,
            // hier aber byte[]-Backing → voll bit-packed vorgehen.

            // 1) Pattern berechnen
            int g = gcd(bpe, 8);
            int pLen = bpe / g;               // patternBytes
            byte[] pattern = new byte[pLen];
            for (int i = 0; i < pLen; i++) {
                int start = (i * 8) % bpe;
                pattern[i] = (byte) (((id >>> start) | (id << (bpe - start))) & 0xFF);
            }

            // 2) Array in O(log N) befüllen
            int bitsNeeded = total * bpe;
            int bytesLen = (bitsNeeded + 7) >>> 3;
            System.arraycopy(pattern, 0, data, 0, pLen);
            int filled = pLen;
            while (filled < bytesLen) {
                int copy = Math.min(filled, bytesLen - filled);
                System.arraycopy(data, 0, data, filled, copy);
                filled += copy;
            }
        }

        @Override
        public int read(int idx) {
            int bitPos = idx * bitsPerBlock;
            int off = bitPos & 7;
            int byteIdx = bitPos >>> 3;
            // Lese bitsPerEntry Bits ab off in data[]
            int val = (data[byteIdx] & 0xFF) >>> off;
            int need = bitsPerBlock - (8 - off);
            int shift = 8 - off;
            while (need > 0) {
                byteIdx++;
                val |= (data[byteIdx] & 0xFF) << shift;
                shift += 8;
                need -= 8;
            }
            return val & bitMask;
        }

        @Override
        public void write(int idx, int value) {
            int bitPos = idx * bitsPerBlock;
            int off = bitPos & 7;
            int byteIdx = bitPos >>> 3;
            int mask = bitMask << off;

            // Clear+Set im ersten Byte
            int b = data[byteIdx] & 0xFF;
            b = (b & ~mask) | ((value << off) & mask);
            data[byteIdx] = (byte) b;

            int remaining = bitsPerBlock - (8 - off);
            int shiftOut = 8 - off;
            while (remaining > 0) {
                byteIdx++;
                int take = Math.min(8, remaining);
                int mask2 = (1 << take) - 1;
                int part = (value >>> shiftOut) & mask2;
                int b2 = data[byteIdx] & 0xFF;
                b2 = (b2 & ~mask2) | part;
                data[byteIdx] = (byte) b2;
                shiftOut += take;
                remaining -= take;
            }
        }

        @Override
        public void resizeIfNeeded() {
            int needed = computeRequiredBitsPerEntry(owner.getPaletteSize());
            if (needed == bitsPerBlock) return;

            byte[] newData = new byte[bytesFor(needed)];
            // alle alten Werte umpacken
            for (int i = 0; i < owner.getTotalSize(); i++) {
                int v = read(i);
                // Manuelles Schreiben in newData mit neuer Bit-Tiefe
                int bitPos = i * needed;
                int off = bitPos & 7;
                int byteIdx = bitPos >>> 3;
                int mask = ((1 << needed) - 1) << off;
                int b = newData[byteIdx] & 0xFF;
                b = (b & ~mask) | ((v << off) & mask);
                newData[byteIdx] = (byte) b;

                int rem = needed - (8 - off);
                int shiftOut = 8 - off;
                while (rem > 0) {
                    byteIdx++;
                    int take = Math.min(8, rem);
                    int mask2 = (1 << take) - 1;
                    int part = (v >>> shiftOut) & mask2;
                    int b2 = newData[byteIdx] & 0xFF;
                    b2 = (b2 & ~mask2) | part;
                    newData[byteIdx] = (byte) b2;
                    shiftOut += take;
                    rem -= take;
                }
            }

            setBitsPerBlock(needed);
            this.data = newData;
        }

        @Override
        public boolean isInBounds(int idx) {
            return idx > 0 && idx < data.length;
        }

        @Override
        public void write(Kryo kryo, Output output) {
            output.writeInt(data.length);
            output.writeBytes(data);
        }

        @Override
        public void read(Kryo kryo, Input input) {
            int dataLength = input.readInt();
            this.data = input.readBytes(dataLength);
        }

        @Override
        public int getMaxBitsPerEntry() {
            return 16;
        }

        @Override
        public int getMinBitsPerEntry() {
            return 9;
        }
    }

    class IntStore extends AbstractPaletteStorage {
        private final PaletteStrategy.Paletted<?> owner;
        private byte[] data;

        public IntStore(PaletteStrategy.Paletted<?> owner, int bitsPerBlock) {
            super(bitsPerBlock);
            this.owner = owner;
            this.data = new byte[bytesFor(bitsPerBlock)];
        }

        public int bytesFor(int bits) {
            return ((owner.getTotalSize() * bits) + 7) >>> 3;
        }

        @Override
        public void fill(int id) {
            int total = owner.getTotalSize();
            int bpe = bitsPerBlock;
            int mask = (1 << bpe) - 1;
            id &= mask;

            // 1) Pattern berechnen
            int g = gcd(bpe, 8);
            int pLen = bpe / g;
            byte[] pattern = new byte[pLen];
            for (int i = 0; i < pLen; i++) {
                int start = (i * 8) % bpe;
                pattern[i] = (byte) (((id >>> start) | (id << (bpe - start))) & 0xFF);
            }

            // 2) Array in O(log N) befüllen
            int bitsNeeded = total * bpe;
            int bytesLen = (bitsNeeded + 7) >>> 3;
            System.arraycopy(pattern, 0, data, 0, pLen);
            int filled = pLen;
            while (filled < bytesLen) {
                int copy = Math.min(filled, bytesLen - filled);
                System.arraycopy(data, 0, data, filled, copy);
                filled += copy;
            }
        }

        @Override
        public int read(int idx) {
            int bitPos = idx * bitsPerBlock;
            int off = bitPos & 7;
            int byteIdx = bitPos >>> 3;
            int val = (data[byteIdx] & 0xFF) >>> off;
            int need = bitsPerBlock - (8 - off);
            int shift = 8 - off;
            while (need > 0) {
                byteIdx++;
                val |= (data[byteIdx] & 0xFF) << shift;
                shift += 8;
                need -= 8;
            }
            return val & bitMask;
        }

        @Override
        public void write(int idx, int value) {
            int bitPos = idx * bitsPerBlock;
            int off = bitPos & 7;
            int byteIdx = bitPos >>> 3;
            int mask = bitMask << off;

            int b = data[byteIdx] & 0xFF;
            b = (b & ~mask) | ((value << off) & mask);
            data[byteIdx] = (byte) b;

            int remaining = bitsPerBlock - (8 - off);
            int shiftOut = 8 - off;
            while (remaining > 0) {
                byteIdx++;
                int take = Math.min(8, remaining);
                int mask2 = (1 << take) - 1;
                int part = (value >>> shiftOut) & mask2;
                int b2 = data[byteIdx] & 0xFF;
                b2 = (b2 & ~mask2) | part;
                data[byteIdx] = (byte) b2;
                shiftOut += take;
                remaining -= take;
            }
        }

        @Override
        public void resizeIfNeeded() {
            int needed = computeRequiredBitsPerEntry(owner.getPaletteSize());
            if (needed == bitsPerBlock) return;

            byte[] newData = new byte[bytesFor(needed)];
            for (int i = 0; i < owner.getTotalSize(); i++) {
                int v = read(i);
                int bitPos = i * needed;
                int off = bitPos & 7;
                int byteIdx = bitPos >>> 3;
                int mask = ((1 << needed) - 1) << off;
                int b = newData[byteIdx] & 0xFF;
                b = (b & ~mask) | ((v << off) & mask);
                newData[byteIdx] = (byte) b;

                int rem = needed - (8 - off);
                int shiftOut = 8 - off;
                while (rem > 0) {
                    byteIdx++;
                    int take = Math.min(8, rem);
                    int mask2 = (1 << take) - 1;
                    int part = (v >>> shiftOut) & mask2;
                    int b2 = newData[byteIdx] & 0xFF;
                    b2 = (b2 & ~mask2) | part;
                    newData[byteIdx] = (byte) b2;
                    shiftOut += take;
                    rem -= take;
                }
            }

            setBitsPerBlock(needed);
            this.data = newData;
        }

        @Override
        public boolean isInBounds(int idx) {
            return idx > 0 && idx < data.length;
        }

        @Override
        public void write(Kryo kryo, Output output) {
            output.writeInt(data.length);
            output.writeBytes(data);
        }

        @Override
        public void read(Kryo kryo, Input input) {
            int dataLength = input.readInt();
            this.data = input.readBytes(dataLength);
        }

        @Override
        public int getMaxBitsPerEntry() {
            return 32;
        }

        @Override
        public int getMinBitsPerEntry() {
            return 17;
        }
    }

    class LongStore extends AbstractPaletteStorage {
        private final PaletteStrategy.Paletted<?> owner;
        private long[] data;

        public LongStore(PaletteStrategy.Paletted<?> owner, int bitsPerBlock) {
            super(bitsPerBlock);
            this.owner = owner;
            this.data = new long[wordsFor(bitsPerBlock)];
        }

        private int wordsFor(int bits) {
            return ((owner.getTotalSize() * bits) + 63) >>> 6;
        }

        @Override
        public int read(int idx) {
            int bitPos = idx * bitsPerBlock;
            int off = bitPos & 63;
            int wordIdx = bitPos >>> 6;
            long seg = data[wordIdx] >>> off;
            if (off + bitsPerBlock > 64) {
                seg |= data[wordIdx + 1] << (64 - off);
            }
            return (int) (seg & bitMask);
        }

        @Override
        public void write(int idx, int value) {
            int bitPos = idx * bitsPerBlock;
            int off = bitPos & 63;
            int wordIdx = bitPos >>> 6;
            long mask = (long) bitMask << off;
            data[wordIdx] = (data[wordIdx] & ~mask)
                    | (((long) value << off) & mask);
            int overflow = off + bitsPerBlock - 64;
            if (overflow > 0) {
                long mask2 = (1L << overflow) - 1L;
                data[wordIdx + 1] = (data[wordIdx + 1] & ~mask2)
                        | ((long) value >>> (bitsPerBlock - overflow) & mask2);
            }
        }

        @Override
        public void resizeIfNeeded() {
            int needed = computeRequiredBitsPerEntry(owner.getPaletteSize());
            if (needed == bitsPerBlock) return;

            long[] newData = new long[wordsFor(needed)];
            for (int i = 0; i < owner.getTotalSize(); i++) {
                int v = read(i);
                // pack into newData per needed, analog zu write(...)
                int bitPos = i * needed;
                int off = bitPos & 63;
                int wi = bitPos >>> 6;
                long mask = ((1L << needed) - 1L) << off;
                newData[wi] = (newData[wi] & ~mask)
                        | (((long) v << off) & mask);
                int ov = off + needed - 64;
                if (ov > 0) {
                    long m2 = (1L << ov) - 1L;
                    newData[wi + 1] = (newData[wi + 1] & ~m2)
                            | ((long) v >>> (needed - ov) & m2);
                }
            }

            this.data = newData;
            setBitsPerBlock(needed);
        }

        @Override
        public boolean isInBounds(int idx) {
            return idx > 0 && idx < data.length;
        }

        @Override
        public void write(Kryo kryo, Output output) {
            output.writeInt(data.length, true);
            output.writeLongs(data, 0, data.length);
        }

        @Override
        public void read(Kryo kryo, Input input) {
            int len = input.readInt(true);
            data = input.readLongs(len);
        }

        @Override
        public int bytesFor(int bits) {
            return wordsFor(bits);
        }

        @Override
        public void fill(int id) {
            int total = owner.getTotalSize();
            int bpe = bitsPerBlock;
            long mask = ((1L << bpe) - 1L);
            long vid = id & mask;

            // 1) Pattern berechnen
            int g = gcd(bpe, 64);
            int pLenW = bpe / g;               // patternWords
            long[] pattern = new long[pLenW];
            for (int i = 0; i < pLenW; i++) {
                int start = (i * 64) % bpe;
                pattern[i] = ((vid >>> start) | (vid << (bpe - start)));
            }

            // 2) Array in O(log N) befüllen
            int bitsNeeded = total * bpe;
            int wordsLen = (bitsNeeded + 63) >>> 6;
            System.arraycopy(pattern, 0, data, 0, pLenW);
            int filled = pLenW;
            while (filled < wordsLen) {
                int copy = Math.min(filled, wordsLen - filled);
                System.arraycopy(data, 0, data, filled, copy);
                filled += copy;
            }
        }

        @Override
        public int getMaxBitsPerEntry() {
            return 64;
        }

        @Override
        public int getMinBitsPerEntry() {
            return 33;
        }
    }

    static byte computeRequiredBitsPerEntry(int amountOfEntries) {
        if (amountOfEntries == 0) {
            return 1;
        }
        return (byte) Math.max(1, 32 - java.lang.Integer.numberOfLeadingZeros(amountOfEntries - 1));
    }

    // Hilfsfunktion zum gcd
    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
