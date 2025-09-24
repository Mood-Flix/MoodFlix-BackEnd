package com.duck.moodflix.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    /**
     * Low-level RestClient를 빈으로 등록 (종료 시 close)
     */
    @Bean(destroyMethod = "close")
    public RestClient esLowLevelClient() {
        // 콤마 구분 다중 URI 지원
        String[] uriTokens = uris.split("\\s*,\\s*");
        URI first = URI.create(uriTokens[0]);
        String scheme = first.getScheme() == null ? "http" : first.getScheme();

        HttpHost[] hosts = Arrays.stream(uriTokens)
                .map(URI::create)
                .map(u -> new HttpHost(
                        u.getHost(),
                        (u.getPort() == -1 ? 9200 : u.getPort()),
                        (u.getScheme() == null ? "http" : u.getScheme())))
                .toArray(HttpHost[]::new);

        CredentialsProvider cp = new BasicCredentialsProvider();
        cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        // (선택) 개발용 trust-all: 시스템/환경변수로 제어 (기본 full)
        SSLContext trustAllSsl = null;
        String verificationMode = System.getProperty(
                "spring.elasticsearch.ssl.verification-mode",
                System.getenv().getOrDefault("SPRING_ELASTICSEARCH_SSL_VERIFICATION_MODE", "full")
        );
        if ("https".equalsIgnoreCase(scheme) && "none".equalsIgnoreCase(verificationMode)) {
            try {
                TrustManager[] trustAll = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };
                trustAllSsl = SSLContext.getInstance("TLSv1.2"); // 가능하면 TLSv1.3
                trustAllSsl.init(null, trustAll, new java.security.SecureRandom());
            } catch (Exception ignore) {}
        }

        SSLContext finalTrustAllSsl = trustAllSsl;
        return RestClient.builder(hosts)
                .setRequestConfigCallback(rc -> rc
                        .setConnectTimeout(5_000)
                        .setSocketTimeout(60_000)
                        .setConnectionRequestTimeout(2_000)
                )
                .setHttpClientConfigCallback(hc -> {
                    hc.setDefaultCredentialsProvider(cp)
                            .setMaxConnTotal(100)
                            .setMaxConnPerRoute(100)
                            .setKeepAliveStrategy((resp, ctx) -> TimeUnit.SECONDS.toMillis(20))
                            .setConnectionTimeToLive(60, TimeUnit.SECONDS)
                            .setDefaultIOReactorConfig(
                                    IOReactorConfig.custom().setSoKeepAlive(true).build()
                            );
                    if (finalTrustAllSsl != null) hc.setSSLContext(finalTrustAllSsl);
                    return hc;
                })
                .build();
    }

    /**
     * Transport를 빈으로 등록 (종료 시 close)
     */
    @Bean(destroyMethod = "close")
    public ElasticsearchTransport esTransport(RestClient esLowLevelClient) {
        return new RestClientTransport(esLowLevelClient, new JacksonJsonpMapper());
    }

    /**
     * High-level ElasticsearchClient
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
