package de.verdox.server.heart;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TickThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(TickThread.class.getName());

    /**
     * Anzahl Ticks pro Sekunde
     */
    private final int ticksPerSecond;
    /**
     * Dauer eines Ticks in Nanosekunden
     */
    private final long tickIntervalNanos;
    /**
     * Ob die Schleife läuft
     */
    private volatile boolean running = false;
    /**
     * Anzahl bisher ausgeführter Ticks
     */
    @Getter
    private long tickCount = 0;
    private final List<Ticking> tickSubscriber = new ArrayList<>();

    /**
     * Erzeugt eine neue TickLoop.
     *
     * @param ticksPerSecond Ziel-Tickrate (z.B. 20)
     */
    public TickThread(int ticksPerSecond) {
        this.ticksPerSecond = ticksPerSecond;
        this.tickIntervalNanos = 1_000_000_000L / ticksPerSecond;
    }

    @Override
    public void start() {
        super.start();
        running = true;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        running = false;
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        long accumulator = 0L;

        while (running) {
            long now = System.nanoTime();
            accumulator += now - lastTime;
            lastTime = now;

            // so viele Ticks abarbeiten wie möglich
            while (accumulator >= tickIntervalNanos) {
                tickCount++;
                long tickStart = System.nanoTime();
                try {
                    for (Ticking subscriber : tickSubscriber) {
                        try {
                            subscriber.onTick(tickCount);
                        } catch (Exception e) {
                            // Fehler eines Subscribers sollte andere nicht beeinflussen
                            e.printStackTrace();
                        }
                    }

                    long tickDuration = System.nanoTime() - tickStart;
                    if (tickDuration > tickIntervalNanos) {
                        long lagMillis = (tickDuration - tickIntervalNanos) / 1_000_000;
                        LOGGER.warning("Tick " + tickCount + " took " + lagMillis + " ms longer than "
                            + (tickIntervalNanos / 1_000_000) + " ms (Lag Detected)");
                    }

                } catch (Exception e) {
                    // Fehler in der Spiel-Logik nicht die gesamte Schleife beenden lassen
                    e.printStackTrace();
                }
                accumulator -= tickIntervalNanos;
            }

            // Restdauer ausnutzen (CPU schonen)
            long sleepNanos = tickIntervalNanos - accumulator;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    // Thread beendet oder unterbrochen
                }
            }
        }
    }
}
