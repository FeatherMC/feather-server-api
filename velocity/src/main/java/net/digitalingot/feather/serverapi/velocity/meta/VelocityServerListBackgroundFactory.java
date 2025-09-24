package net.digitalingot.feather.serverapi.velocity.meta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import net.digitalingot.feather.serverapi.api.meta.ServerListBackground;
import net.digitalingot.feather.serverapi.api.meta.ServerListBackgroundFactory;
import net.digitalingot.feather.serverapi.api.meta.exception.ImageSizeExceededException;
import net.digitalingot.feather.serverapi.api.meta.exception.InvalidImageException;
import net.digitalingot.feather.serverapi.api.meta.exception.UnsupportedImageFormatException;
import net.digitalingot.feather.serverapi.api.meta.format.ImageFormat;
import org.jetbrains.annotations.NotNull;

class VelocityServerListBackgroundFactory implements ServerListBackgroundFactory {

  @Override
  public @NotNull Set<ImageFormat> getSupportedFormats() {
    return EnumSet.allOf(ImageFormat.class);
  }

  @Override
  public @NotNull ServerListBackground byPath(@NotNull Path path)
      throws IOException,
      UnsupportedImageFormatException,
      ImageSizeExceededException,
      InvalidImageException {
    byte[] imageBytes = Files.readAllBytes(path);
    ServerListBackgroundValidator.validateImageBytes(imageBytes);
    byte[] imageHash = HashingUtils.sha1(imageBytes);
    return new SimpleServerListBackground(imageBytes, imageHash, ImageFormat.PNG);
  }

  @Override
  public @NotNull ServerListBackground fromBytes(byte[] bytes, @NotNull ImageFormat format)
      throws UnsupportedImageFormatException, ImageSizeExceededException, InvalidImageException {
    ServerListBackgroundValidator.validateImageBytes(bytes);
    byte[] imageHash = HashingUtils.sha1(bytes);
    return new SimpleServerListBackground(bytes, imageHash, format);
  }

  @Override
  public @NotNull ServerListBackground fromBytes(byte[] bytes)
      throws UnsupportedImageFormatException, ImageSizeExceededException, InvalidImageException {
    // Currently only PNG is supported, so we can assume PNG format
    return fromBytes(bytes, ImageFormat.PNG);
  }
}
