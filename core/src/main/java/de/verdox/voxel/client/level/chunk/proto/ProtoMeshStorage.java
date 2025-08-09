package de.verdox.voxel.client.level.chunk.proto;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Getter;

public class ProtoMeshStorage {
    @Getter
    int faceCount = 0;

    private final LongList faces = new LongArrayList();

    /**
     * Schreibt 'bits' Bits aus 'value' beginnend bei 'bitOffset' in unser LongList 'faces'.
     *
     * @param bitOffset Bit-Index relativ zum Anfang des Bit-Streams
     * @param value     Der Wert, dessen niederwertigste 'bits'-Bits wir schreiben wollen
     * @param bits      Anzahl der Bits, die wir schreiben
     */
    void writeBitsAt(long bitOffset, long value, int bits) {
        // 1) Berechne, in welchem 64-Bit-Block wir anfangen:
        int blockIdx = (int) (bitOffset >>> 6);    // div 64
        // 2) Berechne, an welchem Bit im aktuellen Block wir starten:
        int bitInBlk = (int) (bitOffset & 63);     // mod 64
        // 3) Wie viele Bits müssen wir insgesamt noch schreiben?
        int remaining = bits;

        // Falls wir genau am Ende der Liste stehen, füge einen neuen Block hinzu
        if (blockIdx == faces.size()) {
            faces.add(0L);
        }

        // 4) Loop, bis alle Bits von 'value' geschrieben sind:
        while (remaining > 0) {
            // a) Lese den aktuellen 64-Bit-Block:
            long block = faces.getLong(blockIdx);

            // b) Wie viele Bits sind im aktuellen Block noch frei?
            //    Wenn bitInBlk = 10, sind z.B. 54 Bits von 64 noch unbeschrieben.
            int freeBits = 64 - bitInBlk;

            // c) Wir schreiben entweder alle noch verbleibenden Bits, oder nur so viele,
            //    wie im aktuellen Block noch Platz haben:
            int toWrite = Math.min(freeBits, remaining);

            // d) Wie viele Bits von 'value' sind übrig, nachdem wir die 'toWrite'-höchsten Bits
            //    (bezogen auf das noch zu schreibende Segment) abgeschnitten haben?
            int shift = remaining - toWrite;

            // e) Maske für die Ziel-Bits im Block:
            //    Wir wollen genau 'toWrite' Bits an die Position
            //    [freeBits-toWrite .. freeBits-1] überschreiben.
            long mask = ((1L << toWrite) - 1) << (freeBits - toWrite);

            // f) Extrahiere die entsprechenden Bits aus 'value':
            //    (value >>> shift) holt die 'toWrite'-höchsten Bits des noch zu schreibenden
            //    Segmentes nach ganz rechts; mit & maskBits heben wir nur die toWrite Bits hervor.
            long chunk = (value >>> shift) & ((1L << toWrite) - 1);
            //    Und schiebe sie an ihre Ziel-Position im 64-Bit-Block:
            chunk <<= (freeBits - toWrite);

            // g) Schreibe die Bits in den Block:
            //    - Lösche mit ~mask die Ziel-Bits im Block
            //    - ODER (|) dann unser chunk-Feld hinein
            block = (block & ~mask) | chunk;
            faces.set(blockIdx, block);

            // h) Update remaining, blockIdx und bitInBlk für den nächsten Iterationsschritt:
            remaining -= toWrite;
            blockIdx++;
            bitInBlk = 0;  // ab dem zweiten Block immer am Anfang (Bit 0)

            // i) Falls wir noch Bits haben, aber im nächsten Index noch kein Block existiert:
            if (remaining > 0 && blockIdx == faces.size()) {
                faces.add(0L);
            }
        }
    }

    /**
     * Liest 'bits' Bits ab 'bitOffset' aus unserem LongList 'faces' und liefert sie als long.
     *
     * @param bitOffset Bit-Index relativ zum Anfang des Bit-Streams
     * @param bits      Anzahl der Bits, die wir lesen wollen
     * @return Die gelesenen Bits (rechtsbündig in einem long)
     */
    long readBits(int bitOffset, int bits) {
        // 1) Berechne den Start-Block und das Start-Bit im Block:
        int blockIdx = bitOffset >>> 6;     // div 64
        int shiftInBlk = bitOffset & 63;      // mod 64

        int remaining = bits;  // noch zu lesende Bits
        long result = 0L;    // Hier sammeln wir die gelesenen Bits

        // 2) Solange Bits fehlen, iteriere über die Blöcke:
        while (remaining > 0) {
            // a) Lese den aktuellen 64-Bit-Block:
            long block = faces.getLong(blockIdx);

            // b) Wie viele Bits ab shiftInBlk können wir in diesem Block höchstens lesen?
            int freeBits = 64 - shiftInBlk;
            // c) Wir lesen entweder alle noch verbleibenden Bits, oder nur freeBits, je nachdem,
            //    was zuerst aufgebraucht ist:
            int toRead = Math.min(freeBits, remaining);

            // d) Erstelle Maske für die toRead Bits rechtsbündig:
            long mask = (1L << toRead) - 1;

            // e) Extrahiere die Bits:
            //    - (block >>> (freeBits - toRead)) schiebt die gewünschten Bits
            //      an die ganz rechte Position.
            //    - & mask löscht alle anderen Bits
            long chunk = (block >>> (freeBits - toRead)) & mask;

            // f) Füge die Chunk-Bits an das Ergebnis an:
            //    Ergebnis <<= toRead verschiebt vorhandene Bits nach links,
            //    dann OR mit chunk hängt die neuen Bits unten an.
            result = (result << toRead) | chunk;

            // g) Update remaining und Block-Index:
            remaining -= toRead;
            blockIdx++;
            shiftInBlk = 0;  // ab dem nächsten Block wieder ab Bit 0 lesen
        }

        return result;
    }
}
