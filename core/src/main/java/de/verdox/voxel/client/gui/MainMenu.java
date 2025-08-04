package de.verdox.voxel.client.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MainMenu implements Screen {
    private Stage stage;

    public MainMenu() {
        Gdx.input.setCursorCatched(false);

        stage = new Stage(new ScreenViewport());

        //uiSkin = new Skin(Gdx.files.internal("/voxel/textures/"));

        Table table = new Table(null);
        table.setFillParent(true);
        table.center();


        TextButton btnSinglePlayer = new TextButton("Singeplayer", new TextButton.TextButtonStyle());
        btnSinglePlayer.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                //TODO: Open Singleplayer screen
            }
        });
        table.add(btnSinglePlayer).width(200).pad(10).row();

        TextButton btnMultiPlayer = new TextButton("Multiplayer", new TextButton.TextButtonStyle());
        btnMultiPlayer.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                //TODO: Open Multiplayer screen
            }
        });
        table.add(btnMultiPlayer).width(200).pad(10).row();


        TextButton btnOptions = new TextButton("Options", new TextButton.TextButtonStyle());
        btnOptions.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Open options screen
            }
        });
        table.add(btnOptions).width(200).pad(10).row();

        TextButton btnExit = new TextButton("Quit Game", new TextButton.TextButtonStyle());
        btnExit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        table.add(btnExit).width(200).pad(10);
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        stage.dispose();
        //skin.dipose();
    }

    private static class MenuInput implements InputProcessor {

        @Override
        public boolean keyDown(int keycode) {
            return false;
        }

        @Override
        public boolean keyUp(int keycode) {
            return false;
        }

        @Override
        public boolean keyTyped(char character) {
            return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            return false;
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            return false;
        }

        @Override
        public boolean scrolled(float amountX, float amountY) {
            return false;
        }
    }
}
