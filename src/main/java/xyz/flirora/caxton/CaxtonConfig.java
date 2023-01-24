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
    public static final String CONFIG_PATH = "config/caxton.json";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setLenient().create();
    public @Nullable String rustTarget = null;
    public boolean fatalOnBrokenMethodCall = false;
    public boolean debugRefcountChanges = false;

    public static CaxtonConfig readFromFile() {
        try (FileReader fh = new FileReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            try {
                CaxtonConfig config = GSON.fromJson(fh, CaxtonConfig.class);
                // Perform validation (none currently)
                return config;
            } catch (JsonIOException | JsonSyntaxException e) {
                CaxtonModClient.LOGGER.error("Invalid config: ", e);
                CaxtonModClient.LOGGER.error("Falling back to default config");
                return new CaxtonConfig();
            }
        } catch (IOException e) {
            if (!Files.exists(Path.of(CONFIG_PATH))) {
                // Create new config
                CaxtonConfig config = new CaxtonConfig();
                try (FileWriter fh = new FileWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                    GSON.toJson(config, fh);
                    return config;
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException(e);
        }
    }
}
