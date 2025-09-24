package net.digitalingot.feather.serverapi.velocity.meta;

import com.google.common.hash.Hashing;

/**
 * Utility class for consistent hashing across the server list background implementation.
 */
class HashingUtils {

  private HashingUtils() {
    throw new AssertionError();
  }

  /**
   * Computes the SHA-1 hash of the given bytes.
   *
   * @param bytes the bytes to hash
   * @return the SHA-1 hash as a byte array
   */
  static byte[] sha1(byte[] bytes) {
    return Hashing.sha1().hashBytes(bytes).asBytes();
  }
}
