package xyz.flirora.caxton.dll;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.Platform;
import org.slf4j.Logger;
import xyz.flirora.caxton.CaxtonModClient;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;
import java.util.Scanner;

public class LibraryLoading {
    private static final Text UNSUPPORTED_PLATFORM = Text.translatable("caxton.gui.unsupportedPlatform").formatted(Formatting.BOLD);
    private static final Text LIBRARY_LOADING_FAILED = Text.translatable("caxton.gui.libraryLoadingFailed").formatted(Formatting.BOLD);
    private static final Text UNSUPPORTED_PLATFORM_DESC = Text.translatable("caxton.gui.unsupportedPlatform.description");
    private static final Text UNSUPPORTED_PLATFORM_DESC_OSX =
            Text.translatable("caxton.gui.unsupportedPlatform.description.osx")
                    .append(Text.translatable("caxton.gui.unsupportedPlatform.description.osx2")
                            .formatted(Formatting.BOLD));
    private static final Text LIBRARY_LOADING_FAILED_DESC = Text.translatable("caxton.gui.libraryLoadingFailed.description");
    public static String ISSUES_URL = "https://gitlab.com/Kyarei/caxton/-/issues";
    private static final Text LIBRARY_LOADING_FAILED_LINK = Text.translatable("caxton.gui.libraryLoadingFailed.link", ISSUES_URL);
    public static String OS_SUPPORT_URL = "https://gitlab.com/Kyarei/caxton#os-support";
    private static final Text UNSUPPORTED_PLATFORM_LINK = Text.translatable("caxton.gui.unsupportedPlatform.link", OS_SUPPORT_URL);

    private static Exception loadingException = new IllegalStateException("library not yet loaded");

    public static Exception getLoadingException() {
        return loadingException;
    }

    public static boolean isLibraryLoaded() {
        return loadingException == null;
    }

    public static void loadNativeLibrary(Logger logger) {
        logger.info(
                "Extracting and loading library for {} / {}",
                System.getProperty("os.name"),
                System.getProperty("os.arch"));

        File runDirectory = MinecraftClient.getInstance().runDirectory;

        String soName = System.mapLibraryName("caxton_impl");
        String soNameWithPlatform = RustPlatform.forCurrent() + "-" + soName;
        Metadata metadata = readFromChecksumsTxt(soNameWithPlatform, logger);

        try (InputStream libStream = CaxtonModClient.class.getResourceAsStream("/natives/" + soNameWithPlatform)) {
            if (libStream == null) {
                throw new UnsupportedPlatformException("Could not find " + soNameWithPlatform);
            }

            File tmp = new File(runDirectory, soName);
            if (shouldRewrite(tmp, metadata, logger)) {
                logger.info("Writing native library to {}", tmp);
                try (var output = new FileOutputStream(tmp)) {
                    libStream.transferTo(output);
                }
            } else {
                logger.info("Skipping copying native library because size and hash matched");
            }

            // Some launchers pass a relative path for Minecraft’s run directory;
            // make sure to convert to an absolute path to satisfy System.load.
            System.load(tmp.getAbsolutePath());
            loadingException = null;
        } catch (Exception e) {
            logger.error("Failed to load library", e);
            loadingException = e;
        }
    }

    private static boolean isMacOs() {
        return Platform.get() == Platform.MACOSX;
    }

    private static Text getTitle(Exception e) {
        return e instanceof UnsupportedPlatformException ? UNSUPPORTED_PLATFORM : LIBRARY_LOADING_FAILED;
    }

    private static Text getDescriptionText(Exception e) {
        boolean unsupported = e instanceof UnsupportedPlatformException;
        Text description = unsupported ? (isMacOs() ? UNSUPPORTED_PLATFORM_DESC_OSX : UNSUPPORTED_PLATFORM_DESC) : LIBRARY_LOADING_FAILED_DESC;
        Text link = unsupported ? UNSUPPORTED_PLATFORM_LINK : LIBRARY_LOADING_FAILED_LINK;
        return Text.translatable("caxton.gui.failDescription",
                description,
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                link);
    }

    public static Screen nativeLibraryLoadFailedScreen(BooleanConsumer callback, Exception e) {
        return new ConfirmLinkScreen(callback, getTitle(e), getDescriptionText(e), OS_SUPPORT_URL, ScreenTexts.ACKNOWLEDGE, true);
    }

    public static void showNativeLibraryLoadFailedScreen(MinecraftClient client, Exception e) {
        Screen screen = client.currentScreen;
        client.setScreen(nativeLibraryLoadFailedScreen(confirmed -> {
            if (confirmed) {
                String url = e instanceof UnsupportedPlatformException ? OS_SUPPORT_URL : ISSUES_URL;
                Util.getOperatingSystem().open(url);
            }
            client.setScreen(screen);
        }, e));
    }

    private record Metadata(int size, String sha256Base64) {
    }

    private static @Nullable Metadata readFromChecksumsTxt(String soName, Logger logger) {
        try (InputStream checksums = CaxtonModClient.class.getResourceAsStream("/natives/checksums.txt")) {
            Scanner scanner = new Scanner(Objects.requireNonNull(checksums, "checksums.txt not found"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.isEmpty()) continue;
                String[] comps = line.split("\t");
                if (comps.length != 3) throw new IOException("Malformed line in checksums.txt: " + line);
                if (comps[0].equals(soName)) {
                    return new Metadata(Integer.parseInt(comps[1]), comps[2]);
                }
            }
        } catch (IOException e) {
            logger.error("Could not read checksums.txt", e);
        }
        return null;
    }

    private static boolean shouldRewrite(File dest, @Nullable Metadata metadata, Logger logger) {
        try {
            if (metadata == null) return true;
            if (!dest.exists()) return true;
            if (Files.size(dest.toPath()) != metadata.size) return true;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream stream = new FileInputStream(dest)) {
                while (true) {
                    byte[] contents = stream.readNBytes(4096);
                    if (contents.length == 0) break;
                    digest.update(contents);
                }
            }
            String digestStr = Base64.getEncoder().encodeToString(digest.digest());
            return !digestStr.equals(metadata.sha256Base64);
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Could not determine whether we need to overwrite existing file in .minecraft; assuming that we do", e);
            return true;
        }
    }
}
