package io.fortalis.fortalisauth.config;

import io.fortalis.fortalisauth.crypto.KeyFileGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.nio.file.Path;

@Slf4j
public class KeyFileInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var env = context.getEnvironment();
        var privateKeyPath = resolveKeyPath(env, "auth.jwt.key-file-private", "keys/private_key.pem");
        var publicKeyPath = resolveKeyPath(env, "auth.jwt.key-file-public", "keys/public_key.pem");

        try {
            var generator = new KeyFileGenerator();
            generator.ensureKeysExist(privateKeyPath, publicKeyPath);
        } catch (Exception e) {
            log.error("Failed to ensure key files exist", e);
            throw new IllegalStateException("Cannot start application without valid key files", e);
        }
    }

    private Path resolveKeyPath(Environment env, String propertyName, String defaultValue) {
        var pathString = env.getProperty(propertyName, defaultValue);
        return Path.of(pathString);
    }
}
