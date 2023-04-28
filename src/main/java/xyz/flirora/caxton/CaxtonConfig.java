package xyz.flirora.caxton;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public class CaxtonConfig {
    public static final String CONFIG_PATH_STR = "config/caxton.json";
    public static final Path CONFIG_PATH = Path.of(CONFIG_PATH_STR);
    public static final Path CONFIG_DIR = CONFIG_PATH.getParent();
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setLenient().create();
    public @Nullable String rustTarget = null;
    public boolean tweakExpText = true;
    public boolean sortTextRenderLayers = false;
    public boolean fatalOnBrokenMethodCall = false;
    public boolean debugRefcountChanges = false;

    public static CaxtonConfig readFromFile() {
        CaxtonConfig config;
        try (FileReader fh = new FileReader(CONFIG_PATH_STR, StandardCharsets.UTF_8)) {
            try {
                config = GSON.fromJson(fh, CaxtonConfig.class);
                // Perform validation (none currently)
            } catch (JsonIOException | JsonSyntaxException e) {
                CaxtonModClient.LOGGER.error("Invalid config: ", e);
                CaxtonModClient.LOGGER.error("Falling back to default config");
                return new CaxtonConfig();
            }
        } catch (IOException e) {
            // Create new config
            config = new CaxtonConfig();
            if (!Files.exists(CONFIG_PATH)) {
                try {
                    Files.createDirectories(CONFIG_DIR);
                } catch (IOException e2) {
                    CaxtonModClient.LOGGER.error("Could not create config directory: ", e);
                }
            } else {
                CaxtonModClient.LOGGER.error("Could not read from config file:  ", e);
            }
        }
        try (FileWriter fh = new FileWriter(CONFIG_PATH_STR, StandardCharsets.UTF_8)) {
            GSON.toJson(config, fh);
        } catch (IOException ex) {
            CaxtonModClient.LOGGER.error("Could not write to config file: ", ex);
        }
        return config;
    }
}
