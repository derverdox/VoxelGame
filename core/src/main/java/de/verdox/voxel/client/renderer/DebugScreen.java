package de.verdox.voxel.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DebugScreen {
    private final Set<DebuggableOnScreen> attached = new LinkedHashSet<>();
    private final Batch batch = new SpriteBatch();
    private final BitmapFont debugFont = new BitmapFont();
    private final List<String> cachedTextLines = new ArrayList<>();

    private final float startX = 10;
    private final float startZ = 55;

    private final float zPerLine = debugFont.getLineHeight();

    public void render() {
        batch.begin();
        synchronized (attached) {
            attached.forEach(debuggableOnScreen -> {
                debuggableOnScreen.debugText(this);
                addDebugTextLine("");
            });
        }
        batch.end();
        cachedTextLines.clear();
    }

    public void addDebugTextLine(String text) {
        debugFont.draw(batch, text, startX, Gdx.graphics.getHeight() - 10 - (cachedTextLines.size() * zPerLine));
        cachedTextLines.add(text);
    }

    public void attach(DebuggableOnScreen debuggableOnScreen) {
        synchronized (attached) {
            this.attached.add(debuggableOnScreen);
        }
    }
}
