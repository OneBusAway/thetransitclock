package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.junit.Test;

/**
 * Encryption wraps jasypt's BasicTextEncryptor. The encryptor is a static
 * singleton initialized lazily from the configured password, so these tests
 * treat it as a black box: encrypt/decrypt must round-trip, encrypt must be
 * salted (non-deterministic), and a tampered ciphertext must throw rather
 * than silently return bad plaintext.
 */
public class EncryptionTest {

	@Test
	public void encryptDecryptRoundTripsSimpleString() {
		String plaintext = "hunter2";
		String encrypted = Encryption.encrypt(plaintext);
		assertNotNull(encrypted);
		assertNotEquals(plaintext, encrypted);
		assertEquals(plaintext, Encryption.decrypt(encrypted));
	}

	@Test
	public void encryptDecryptRoundTripsEmptyString() {
		String encrypted = Encryption.encrypt("");
		assertEquals("", Encryption.decrypt(encrypted));
	}

	@Test
	public void encryptDecryptRoundTripsUnicode() {
		String plaintext = "pāsswørd ☃ 中文";
		String encrypted = Encryption.encrypt(plaintext);
		assertEquals(plaintext, Encryption.decrypt(encrypted));
	}

	@Test
	public void encryptionIsSaltedSoSameInputProducesDifferentCiphertexts() {
		// BasicTextEncryptor uses a random salt per call, so successive
		// encryptions of the same plaintext should produce different outputs.
		// This matters for security (otherwise ciphertexts are a dictionary).
		String a = Encryption.encrypt("sameInput");
		String b = Encryption.encrypt("sameInput");
		assertNotEquals(a, b);
		// But both decrypt back to the same plaintext.
		assertEquals(Encryption.decrypt(a), Encryption.decrypt(b));
	}

	@Test
	public void decryptOfTamperedCiphertextThrows() {
		String encrypted = Encryption.encrypt("original");
		// Flip a character in the middle of the ciphertext to corrupt it.
		char[] chars = encrypted.toCharArray();
		int mid = chars.length / 2;
		chars[mid] = (chars[mid] == 'A') ? 'B' : 'A';
		String tampered = new String(chars);

		try {
			Encryption.decrypt(tampered);
			fail("Expected EncryptionOperationNotPossibleException on tampered ciphertext");
		} catch (EncryptionOperationNotPossibleException expected) {
			// expected
		}
	}

	@Test
	public void decryptOfGarbageThrows() {
		try {
			Encryption.decrypt("not-a-real-ciphertext");
			fail("Expected EncryptionOperationNotPossibleException on garbage input");
		} catch (EncryptionOperationNotPossibleException expected) {
			// expected
		}
	}
}
