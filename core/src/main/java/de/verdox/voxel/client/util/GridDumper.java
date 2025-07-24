package de.verdox.voxel.client.util;

import de.verdox.voxel.client.level.mesh.block.face.BlockFace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GridDumper {

    /**
     * Dump das 2D-Grid in eine .txt-Datei mit Tabs.
     *
     * @param grid       2D-Array BlockFace[u][v]
     * @param outputFile Pfad zur Ausgabedatei (z.B. Paths.get("grid.txt"))
     * @throws IOException falls Schreiben fehlschlägt
     */
    public static void writeGridToFile(BlockFace[][] grid, Path outputFile) throws IOException {
        int sizeU = grid.length;
        int sizeV = grid[0].length;

        try (BufferedWriter bw = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            // Header
            bw.write("\t");
            for (int u = 0; u < sizeU; u++) {
                bw.write(u + "\t");
            }
            bw.newLine();

            // Zeilen für v
            for (int v = 0; v < sizeV; v++) {
                bw.write(v + "\t");
                for (int u = 0; u < sizeU; u++) {
                    BlockFace f = grid[u][v];
                    bw.write((f == null ? "." : "#") + "\t");
                }
                bw.newLine();
            }
        }
    }
}
