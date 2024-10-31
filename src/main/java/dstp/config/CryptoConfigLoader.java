package main.java.dstp.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class CryptoConfigLoader {
    private Map<String, String> config;

    public CryptoConfigLoader(String configFilePath) throws Exception {
        config = new HashMap<>();
        loadConfig(configFilePath);
    }

    private void loadConfig(String configFilePath) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(configFilePath));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(":")) {
                String[] parts = line.split(":");
                config.put(parts[0].trim(), parts[1].trim());
            }
        }
        reader.close();
    }

    public String getConfidentialityAlgorithm() {
        return config.get("CONFIDENTIALITY");
    }

    public String getSymmetricKey() {
        return config.get("SYMMETRIC_KEY");
    }

    public int getSymmetricKeySize() {
        return Integer.parseInt(config.get("SYMMETRIC_KEY_SIZE"));
    }

    public int getIvSize() {
        return Integer.parseInt(config.get("IV_SIZE"));
    }

    public String getIv() {
        return config.get("IV");
    }

    public String getIntegrityAlgorithm() {
        return config.get("INTEGRITY");
    }

    public String getHashAlgorithm() {
        return config.get("H");
    }

    public String getMacKey() {
        return config.get("MACKEY");
    }

    public int getMacKeySize() {
        return Integer.parseInt(config.get("MACKEY_SIZE"));
    }
}

