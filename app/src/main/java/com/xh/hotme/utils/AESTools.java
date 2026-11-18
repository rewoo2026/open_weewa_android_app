package com.xh.hotme.utils;

import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESTools {
	private static final String TAG = AESTools.class.getSimpleName();
	private static final String AES = "AES";
	private final static String INITIAL_VECTOR = "";
	private static final String MODE = "AES/ECB/PKCS5Padding";

	public static String encrypt(String cleartext, String password) {
		return encrypt(cleartext.getBytes(), password);
	}

	public static String encrypt(byte[] cleartext, String password) {
		IvParameterSpec zeroIv = new IvParameterSpec(INITIAL_VECTOR.getBytes());
		SecretKeySpec key = new SecretKeySpec(password.getBytes(), AES);
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance(MODE);
			if(MODE.contains("/ECB/")) {
				cipher.init(Cipher.ENCRYPT_MODE, key);
			} else {
				cipher.init(Cipher.ENCRYPT_MODE, key, zeroIv);
			}
			byte[] encryptedData = cipher.doFinal(cleartext);
			return new String(Base64.encode(encryptedData, Base64.DEFAULT));
		} catch(Throwable e) {
			AppTrace.e(TAG, e.getLocalizedMessage());
		}
		return null;
	}

	public static String decrypt(String encrypted, String password) {
		byte[] byteMi = Base64.decode(encrypted, Base64.DEFAULT);
		IvParameterSpec zeroIv = new IvParameterSpec(INITIAL_VECTOR.getBytes());
		SecretKeySpec key = new SecretKeySpec(password.getBytes(), AES);
		Cipher cipher = null;
		try {
			cipher = Cipher.getInstance(MODE);
			if(MODE.contains("/ECB/")) {
				cipher.init(Cipher.DECRYPT_MODE, key);
			} else {
				cipher.init(Cipher.DECRYPT_MODE, key, zeroIv);
			}
			byte[] decryptedData = cipher.doFinal(byteMi);
			return new String(decryptedData);
		} catch(Throwable e) {
			AppTrace.e(TAG, e.getLocalizedMessage());
		}
		return null;
	}
}
