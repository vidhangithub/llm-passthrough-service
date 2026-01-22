package com.llm.passthrough.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SSL Configuration that supports loading PEM certificates directly.
 * This class handles tls.key, tls.crt, and ca.crt files without
 * requiring conversion to Java KeyStore format.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SslConfig {

    private final ApigeeProperties apigeeProperties;
    private final ResourceLoader resourceLoader;

    @Bean
    public RestClient restClient() throws Exception {
        HttpClient httpClient = createHttpClient();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(30000);
        requestFactory.setConnectionRequestTimeout(30000);

        return RestClient.builder()
                .baseUrl(apigeeProperties.getUrl())
                .defaultHeader("x-lbg-client-id", apigeeProperties.getClientId())
                .defaultHeader("x-lbg-client-secret", apigeeProperties.getClientSecret())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .requestFactory(requestFactory)
                .build();
    }

    private HttpClient createHttpClient() throws Exception {
        ApigeeProperties.Ssl ssl = apigeeProperties.getSsl();

        if (!ssl.isEnabled()) {
            log.info("SSL is disabled, using default HTTP client");
            return HttpClients.createDefault();
        }

        SSLContext sslContext = buildSslContext();

        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(sslContext)
                .build();

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    private SSLContext buildSslContext() throws Exception {
        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
        ApigeeProperties.Ssl ssl = apigeeProperties.getSsl();

        // Load from PEM files if paths are provided
        if (StringUtils.hasText(ssl.getTlsKeyPath()) && StringUtils.hasText(ssl.getTlsCertPath())) {
            log.info("Loading client certificate from PEM files");
            KeyStore keyStore = loadKeyStoreFromPem(ssl.getTlsKeyPath(), ssl.getTlsCertPath());
            sslContextBuilder.loadKeyMaterial(keyStore, "".toCharArray());
        }
        // Fallback to JKS keystore
        else if (StringUtils.hasText(ssl.getKeyStorePath())) {
            log.info("Loading KeyStore from: {}", ssl.getKeyStorePath());
            KeyStore keyStore = loadKeyStore(ssl.getKeyStorePath(), ssl.getKeyStorePassword());
            sslContextBuilder.loadKeyMaterial(keyStore,
                    ssl.getKeyStorePassword() != null ? ssl.getKeyStorePassword().toCharArray() : null);
        }

        // Load CA certificate for trust
        if (StringUtils.hasText(ssl.getCaCertPath())) {
            log.info("Loading CA certificate from: {}", ssl.getCaCertPath());
            KeyStore trustStore = createTrustStoreFromCaCert(ssl.getCaCertPath());
            sslContextBuilder.loadTrustMaterial(trustStore, null);
        }
        // Fallback to JKS truststore
        else if (StringUtils.hasText(ssl.getTrustStorePath())) {
            log.info("Loading TrustStore from: {}", ssl.getTrustStorePath());
            KeyStore trustStore = loadKeyStore(ssl.getTrustStorePath(), ssl.getTrustStorePassword());
            sslContextBuilder.loadTrustMaterial(trustStore, null);
        }

        return sslContextBuilder.build();
    }

    /**
     * Load KeyStore from PEM format private key and certificate files.
     */
    private KeyStore loadKeyStoreFromPem(String keyPath, String certPath) throws Exception {
        // Load private key
        PrivateKey privateKey = loadPrivateKey(keyPath);

        // Load certificate chain
        List<X509Certificate> certChain = loadCertificateChain(certPath);

        // Create KeyStore
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);

        // Add key entry with certificate chain
        Certificate[] chain = certChain.toArray(new Certificate[0]);
        keyStore.setKeyEntry("client", privateKey, "".toCharArray(), chain);

        return keyStore;
    }

    /**
     * Load a private key from PEM file (supports PKCS#8 format).
     */
    private PrivateKey loadPrivateKey(String keyPath) throws Exception {
        Resource resource = resourceLoader.getResource(keyPath);
        String keyContent;

        try (InputStream is = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            keyContent = reader.lines().collect(Collectors.joining("\n"));
        }

        // Remove PEM headers and whitespace
        String privateKeyPEM = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);

        // Try RSA first, then EC
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(keySpec);
        }
    }

    /**
     * Load certificate chain from PEM file.
     */
    private List<X509Certificate> loadCertificateChain(String certPath) throws Exception {
        Resource resource = resourceLoader.getResource(certPath);
        List<X509Certificate> certificates = new ArrayList<>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        try (InputStream is = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder certBuilder = new StringBuilder();
            String line;
            boolean inCert = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains("BEGIN CERTIFICATE")) {
                    inCert = true;
                    certBuilder = new StringBuilder();
                    certBuilder.append(line).append("\n");
                } else if (line.contains("END CERTIFICATE")) {
                    certBuilder.append(line).append("\n");
                    byte[] certBytes = certBuilder.toString().getBytes(StandardCharsets.UTF_8);
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(
                            new ByteArrayInputStream(certBytes));
                    certificates.add(cert);
                    inCert = false;
                } else if (inCert) {
                    certBuilder.append(line).append("\n");
                }
            }
        }

        if (certificates.isEmpty()) {
            // Try loading as single certificate
            try (InputStream is = resource.getInputStream()) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
                certificates.add(cert);
            }
        }

        return certificates;
    }

    /**
     * Create a TrustStore from CA certificate PEM file.
     */
    private KeyStore createTrustStoreFromCaCert(String caCertPath) throws Exception {
        List<X509Certificate> caCerts = loadCertificateChain(caCertPath);

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        int index = 0;
        for (X509Certificate cert : caCerts) {
            trustStore.setCertificateEntry("ca-cert-" + index++, cert);
        }

        return trustStore;
    }

    /**
     * Load a JKS KeyStore from file.
     */
    private KeyStore loadKeyStore(String path, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
            keyStore.load(is, password != null ? password.toCharArray() : null);
        }
        return keyStore;
    }
}
