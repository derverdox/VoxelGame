package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.*;

public class DebugScreen {
    private final Set<DebuggableOnScreen> attached = new LinkedHashSet<>();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont debugFont = new BitmapFont();
    private final List<String> cachedTextLines = new ArrayList<>();

    // UI-Kamera/Viewport in Pixelkoordinaten (0..width, 0..height)
    private final OrthographicCamera uiCamera = new OrthographicCamera();
    private final ScreenViewport uiViewport = new ScreenViewport(uiCamera);

    // Ränder in Pixeln
    private float marginLeft = 10f;
    private float marginTop  = 10f;

    // Zeilenhöhe in Font-Pixeln
    private float zPerLine = debugFont.getLineHeight();

    public DebugScreen() {
        // einmal initial an Fenstergröße anpassen
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    public void render() {
        // Viewport aktivieren und Batch auf UI-Kamera stellen
        uiViewport.apply();
        batch.setProjectionMatrix(uiCamera.combined);

        // Start-Y vom oberen Rand aus gerechnet
        final float startX = marginLeft;
        final float startY = uiViewport.getWorldHeight() - marginTop;

        batch.begin();
        cachedTextLines.clear();

        synchronized (attached) {
            attached.forEach(d -> {
                d.debugText(this);       // sammelt über addDebugTextLine(...)
                addDebugTextLine("");    // Leerzeile zwischen Blöcken
            });
        }

        // tatsächlich zeichnen
        for (int i = 0; i < cachedTextLines.size(); i++) {
            debugFont.draw(batch, cachedTextLines.get(i), startX, startY - i * zPerLine);
        }

        batch.end();
    }

    public void addDebugTextLine(String text) {
        cachedTextLines.add(text);
    }

    public void attach(DebuggableOnScreen debuggableOnScreen) {
        synchronized (attached) {
            this.attached.add(debuggableOnScreen);
        }
    }

    /** aus ApplicationListener.resize(...) aufrufen */
    public void resize(int width, int height) {
        uiViewport.update(width, height, true); // true = Recenter Kamera
    }

    /** Optional, falls du Font-Skalierung änderst */
    public void setFontScale(float scale) {
        debugFont.getData().setScale(scale);
        zPerLine = debugFont.getLineHeight();
    }

    public void setMargins(float left, float top) {
        this.marginLeft = left;
        this.marginTop = top;
    }
}