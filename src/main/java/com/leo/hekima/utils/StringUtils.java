package com.leo.hekima.utils;


import com.leo.hekima.exception.UnrecoverableServiceException;
import org.bouncycastle.util.encoders.Hex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class StringUtils {
    private StringUtils() {}
    public static final String toHex(final String string) {
        return Hex.toHexString(string.getBytes(UTF_8));
    }
    public static final String toHex(final byte[] bytes) {
        return Hex.toHexString(bytes);
    }
    public static final String base64Encode(final byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes), UTF_8);
    }
    public static final String base64Decode(final byte[] bytes) {
        return new String(Base64.getDecoder().decode(bytes), UTF_8);
    }
    public static final String sha1InHex(final String string) {
        byte[] bytes = string.toLowerCase().getBytes(UTF_8);
        return sha1InHex(bytes);
    }
    public static final String sha1InHex(final  byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return Hex.toHexString(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new UnrecoverableServiceException("sha1.not.found");
        }
    }
    public static final String md5InHex(final String string) {
        byte[] bytesEmailAddress = string.toLowerCase().getBytes(UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return Hex.toHexString(md.digest(bytesEmailAddress));
        } catch (NoSuchAlgorithmException e) {
            throw new UnrecoverableServiceException("md5.not.found");
        }
    }
    public static final String md5(final String string) {
        byte[] bytesEmailAddress = string.toLowerCase().getBytes(UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return base64Encode(md.digest(bytesEmailAddress));
        } catch (NoSuchAlgorithmException e) {
            throw new UnrecoverableServiceException("md5.not.found");
        }
    }

    public static final SplittedString splitByLastIndexOfCharacter(final char splitCar, final String source) {
        final SplittedString splitted;
        if(org.springframework.util.StringUtils.hasText(source)) {
            final String prefix, suffix;
            int lastIdx = source.lastIndexOf(splitCar);
            if(lastIdx == 0) {
                prefix = null;
                suffix = source.substring(1);
            } else if(lastIdx < 0){
                prefix = source;
                suffix = null;
            } else {
                prefix = source.substring(0, lastIdx);
                if(lastIdx < source.length() - 1) {
                    suffix = source.substring(lastIdx + 1);
                } else {
                    suffix = null;
                }
            }
            splitted = new SplittedString(prefix, suffix);
        } else {
            splitted = new SplittedString(null, null);
        }
        return splitted;
    }
    public static class SplittedString {
        private final String prexix;
        private final String suffix;

        public SplittedString(String prexix, String suffix) {
            this.prexix = prexix;
            this.suffix = suffix;
        }

        public Optional<String> getPrexix() {
            return Optional.ofNullable(prexix);
        }

        public Optional<String> getSuffix() {
            return Optional.ofNullable(suffix);
        }
    }
    public static final boolean isNotEmpty(final String seq) {
        return seq != null && !seq.trim().isEmpty();
    }

}
