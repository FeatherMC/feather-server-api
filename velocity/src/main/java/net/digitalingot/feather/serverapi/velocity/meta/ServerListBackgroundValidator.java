package net.digitalingot.feather.serverapi.velocity.meta;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.digitalingot.feather.serverapi.api.meta.ServerListBackground;
import net.digitalingot.feather.serverapi.api.meta.exception.ImageSizeExceededException;
import net.digitalingot.feather.serverapi.api.meta.exception.InvalidImageException;
import net.digitalingot.feather.serverapi.api.meta.exception.UnsupportedImageFormatException;
import net.digitalingot.feather.serverapi.api.meta.format.ImageFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Validates server list background images according to size and format requirements.
 */
class ServerListBackgroundValidator {

  private static final int MAX_WIDTH = 1009;
  private static final int MAX_HEIGHT = 202;
  private static final long MAX_FILE_SIZE = 512 * 1024; // 512KB

  private static final byte[] PNG_HEADER = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

  /**
   * Validates a server list background according to size and format requirements.
   *
   * <p>Note: This method performs I/O operations when validating images and should be called off
   * the main thread to avoid blocking.
   *
   * @param background the background to validate
   * @throws UnsupportedImageFormatException if the image format is not supported
   * @throws ImageSizeExceededException      if the image exceeds the maximum allowed dimensions or
   *                                         file size
   * @throws InvalidImageException           if the image is invalid or corrupted
   */
  static void validate(@NotNull ServerListBackground background)
      throws UnsupportedImageFormatException, ImageSizeExceededException, InvalidImageException {
    if (background instanceof SimpleServerListBackground) {
      return;
    }

    byte[] imageBytes = background.getImage();
    validateImageBytes(imageBytes);
    validateHash(imageBytes, background.getHash());

    if (background.getImageFormat() != ImageFormat.PNG) {
      throw new UnsupportedImageFormatException("Only PNG format is supported");
    }
  }

  /**
   * Validates raw image bytes according to size and format requirements.
   *
   * @param imageBytes the image bytes to validate
   * @throws UnsupportedImageFormatException if the image format is not supported
   * @throws ImageSizeExceededException      if the image exceeds the maximum allowed dimensions or
   *                                         file size
   * @throws InvalidImageException           if the image is invalid or corrupted
   */
  static void validateImageBytes(byte[] imageBytes)
      throws UnsupportedImageFormatException, ImageSizeExceededException, InvalidImageException {
    validateFileSize(imageBytes);
    validatePngFormat(imageBytes);
    validateDimensions(loadImage(imageBytes));
  }

  /**
   * Validates the file size of the image.
   */
  private static void validateFileSize(byte[] imageBytes) throws ImageSizeExceededException {
    if (imageBytes.length > MAX_FILE_SIZE) {
      throw new ImageSizeExceededException(
          ImageSizeExceededException.Type.FILE_SIZE, imageBytes.length, MAX_FILE_SIZE);
    }
  }

  /**
   * Validates that the image is in PNG format by checking the header bytes.
   */
  private static void validatePngFormat(byte[] imageBytes) throws UnsupportedImageFormatException {
    if (!isPngFormat(imageBytes)) {
      throw new UnsupportedImageFormatException("Only PNG format is supported");
    }
  }

  /**
   * Loads the image from bytes into a BufferedImage.
   */
  private static BufferedImage loadImage(byte[] imageBytes) throws InvalidImageException {
    try {
      Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("PNG");
      if (!readers.hasNext()) {
        throw new InvalidImageException("No PNG image reader available");
      }

      ImageReader reader = readers.next();
      try (ByteArrayInputStream input = new ByteArrayInputStream(imageBytes);
          ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
        reader.setInput(imageInput);
        return reader.read(0);
      } finally {
        reader.dispose();
      }
    } catch (IOException ioException) {
      throw new InvalidImageException("Failed to load image", ioException);
    }
  }

  /**
   * Validates the dimensions of the image.
   */
  private static void validateDimensions(BufferedImage image) throws ImageSizeExceededException {
    int width = image.getWidth();
    int height = image.getHeight();

    if (width > MAX_WIDTH) {
      throw new ImageSizeExceededException(ImageSizeExceededException.Type.WIDTH, width, MAX_WIDTH);
    }
    if (height > MAX_HEIGHT) {
      throw new ImageSizeExceededException(
          ImageSizeExceededException.Type.HEIGHT, height, MAX_HEIGHT);
    }
  }

  /**
   * Validates that the provided hash matches the image bytes.
   */
  private static void validateHash(byte[] imageBytes, byte[] providedHash)
      throws InvalidImageException {
    byte[] computedHash = HashingUtils.sha1(imageBytes);
    if (!Arrays.equals(computedHash, providedHash)) {
      throw new InvalidImageException("Image hash does not match provided hash");
    }
  }

  /**
   * Checks if the given bytes represent a PNG image by examining the file header.
   */
  private static boolean isPngFormat(byte[] bytes) {
    if (bytes.length < PNG_HEADER.length) {
      return false;
    }

    for (int iii = 0; iii < PNG_HEADER.length; iii++) {
      if (bytes[iii] != PNG_HEADER[iii]) {
        return false;
      }
    }

    return true;
  }
}
