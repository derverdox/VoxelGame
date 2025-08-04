package de.verdox.voxel.shared.util.buffer;

public interface DynamicFloatBuffer {
    /**
     * Gibt die aktuelle Anzahl an Elementen zurück.
     */
    int size();

    /**
     * Gibt die aktuelle Kapazität des Puffers zurück.
     */
    int capacity();
    /**
     * Fügt am Ende den Wert ein.
     */
    void append(float value);

    /**
     * Setzt den Wert an Index. Vergrößert und füllt Lücken mit 0, wenn Index >= size.
     */
    void set(int index, float value);

    void fill(int start, int length, float value);

    /**
     * Entfernt den Wert an Index, setzt ihn auf 0. Verkleinert Kapazität, wenn möglich.
     */
    void remove(int index);

    /**
     * Fügt an Index ein und verschiebt alle folgenden Werte nach hinten.
     */
    void insert(int index, float value);

    /**
     * Überschreibt ab Index 'pos' die nächsten src.length Werte
     *
     * @param pos  Startposition im Buffer (0 ≤ pos ≤ size-src.length)
     * @param src  Quelle-Array mit float-Werten
     */
    void set(int pos, float[] src);

    /**
     * Ersetzt den Bereich [startIndex, endIndex) durch newData.
     * - Ist newData kürzer, wird der Rest des alten Bereichs entfernt.
     * - Ist newData länger, wird der Puffer entsprechend erweitert
     *   und die nachfolgenden Elemente nach hinten geschoben.
     * Gibt den neuen End-Index (exclusive) zurück.
     */

    int update(int startIndex, int endIndex, float[] newData);

    /**
     * Fügt ab index 'pos' das komplette Array 'src' ein
     */
    void insert(int pos, float[] src);

    /**
     * Sorgt dafür, dass mindestens minCapacity Platz da ist.
     */
    void ensureCapacity(int minCapacity, boolean exact);

    /**
     * Ändert die Puffergröße und kopiert bestehende Elemente vektorisiert.
     */
    void resizeBuffer(int newCapacity);

    /**
     * Liefert den Roh-Array (nur zum Debuggen).
     */
    float[] getSnapshot();
}
