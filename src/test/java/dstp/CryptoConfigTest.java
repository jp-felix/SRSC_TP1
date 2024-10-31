package test.java.dstp;

import main.java.dstp.config.CryptoConfigLoader;

//Just to test the config file and loader
public class CryptoConfigTest {
    public static void main(String[] args) {
        try {
            String configFilePath = "config/cryptoconfig.txt";

            CryptoConfigLoader loader = new CryptoConfigLoader(configFilePath);

            System.out.println("Confidentiality Algorithm: " + loader.getConfidentialityAlgorithm());
            System.out.println("Symmetric Key: " + loader.getSymmetricKey());
            System.out.println("Symmetric Key Size: " + loader.getSymmetricKeySize());
            System.out.println("IV Size: " + loader.getIvSize());
            System.out.println("IV: " + loader.getIv());
            System.out.println("Integrity Algorithm: " + loader.getIntegrityAlgorithm());
            System.out.println("Hash Algorithm: " + loader.getHashAlgorithm());
            System.out.println("MAC Key: " + loader.getMacKey());
            System.out.println("MAC Key Size: " + loader.getMacKeySize());

        } catch (Exception e) {
            System.err.println("Error loading configuration: " + e.getMessage());
        }
    }
}

//test results: (SUCCESS!!!)
/*
Confidentiality Algorithm: AES/CTR/NoPadding
Symmetric Key: 00112233445566778899AABBCCDDEEFF
Symmetric Key Size: 128
IV Size: 16
IV: 0102030405060708090A0B0C0D0E0F10
Integrity Algorithm: HMAC
Hash Algorithm: SHA256
MAC Key: A1B2C3D4E5F607182A3B4C5D6E7F8090
MAC Key Size: 128
*/