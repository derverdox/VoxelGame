package de.verdox.voxel.client.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModel;
import de.verdox.voxel.shared.data.types.BlockModels;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TextureAtlasManager {
    private static final Logger LOGGER = Logger.getLogger(TextureAtlasManager.class.getSimpleName());
    private static TextureAtlasManager INSTANCE;

    public static TextureAtlasManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TextureAtlasManager();
        }
        return INSTANCE;
    }

    private final List<BlockModel> blockModels = new ArrayList<>();
    @Getter
    private TextureAtlas blockTextureAtlas;

    private TextureAtlasManager() {

    }

    public void build() {
        Gdx.app.log("Atlas", "Creating the Block atlas");
        PixmapPacker blockTexturePacker = new PixmapPacker(
            1024, 1024, Pixmap.Format.RGBA8888,
                0, false
        );
        BlockModels.getBlockModels().forEach(this::registerBlockModel);
        blockModels.stream().distinct().forEach(blockModel ->
            blockModel.getTexturesPerFace().values().stream().distinct().forEach(resourceLocation -> {
                Gdx.app.log("Atlas", "Including " + resourceLocation.toString());
                blockTexturePacker.pack(resourceLocation.toString(), new Pixmap(Gdx.files.internal(findFile(TextureType.BLOCKS, resourceLocation))));
            }));

        blockTextureAtlas = blockTexturePacker.generateTextureAtlas(
            Texture.TextureFilter.Nearest,
            Texture.TextureFilter.Nearest,
            false
        );
        blockTexturePacker.dispose();

        Gdx.app.log("Atlas", "Block Atlas created with " + blockTextureAtlas.getRegions().size + " regions on " + blockTextureAtlas.getTextures().size + " texture pages.");
    }

    public void registerBlockModel(BlockModel blockModel) {
        blockModels.add(blockModel);
    }

    public Texture findTexture(TextureType textureType, ResourceLocation resourceLocation) {
        return new Texture(findFile(textureType, resourceLocation));
    }

    public record TextureType(String path) {
        public static final TextureType BLOCKS = new TextureType("blocks");
    }

    private String findFile(TextureType textureType, ResourceLocation resourceLocation) {
        return resourceLocation.getNamespace() + "/textures/" + textureType.path() + "/" + resourceLocation.getPath() + ".png";
    }
}
