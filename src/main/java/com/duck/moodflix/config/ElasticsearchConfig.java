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
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.duck.moodflix.movie.repository")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            // 1) URI 파싱 + 스킴 자동 인식 (http/https)
            String[] uriTokens = uris.split("\\s*,\\s*");  // 콤마로 구분된 URI들
            URI first = URI.create(uriTokens[0]);  // 첫 번째 URI로 scheme 추출
            String scheme = (first.getScheme() == null ? "http" : first.getScheme());
            org.apache.http.HttpHost[] hosts = java.util.Arrays.stream(uriTokens)
                    .map(URI::create)
                    .map(u -> new HttpHost(u.getHost(), (u.getPort() == -1 ? 9200 : u.getPort()), (u.getScheme() == null ? "http" : u.getScheme())))
                    .toArray(HttpHost[]::new);

            // 2) Basic 인증
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
            );

            // 3) (개발용) HTTPS일 때만 trust-all SSLContext 준비
            SSLContext trustAllSsl;
            String verificationMode = System.getProperty("spring.elasticsearch.ssl.verification-mode",
                    System.getenv().getOrDefault("SPRING_ELASTICSEARCH_SSL_VERIFICATION_MODE", "full"));
            if ("https".equalsIgnoreCase(scheme) && "none".equalsIgnoreCase(verificationMode)) {
                TrustManager[] trustAll = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };
                trustAllSsl = SSLContext.getInstance("TLSv1.2");
                trustAllSsl.init(null, trustAll, new java.security.SecureRandom());
            } else {
                trustAllSsl = null;
            }

            // 4) RestClient 빌더 — 여기서 Keep-Alive/Timeout 튜닝 추가
            RestClient restClient = RestClient.builder(hosts)
                    // (a) 요청 타임아웃
                    .setRequestConfigCallback(rc -> rc
                            .setConnectTimeout(5_000)     // 연결 수립
                            .setSocketTimeout(60_000)     // 응답 대기
                    )
                    .setHttpClientConfigCallback(hc -> {
                        hc.setDefaultCredentialsProvider(credentialsProvider)
                                .setMaxConnTotal(100)
                                .setMaxConnPerRoute(100)
                                // 서버가 끊기기 전에 더 짧게 재협상: 15~30초 권장
                                .setKeepAliveStrategy((resp, ctx) -> java.util.concurrent.TimeUnit.SECONDS.toMillis(20))
                                // 너무 오래된 커넥션은 강제로 수명 제한(예: 60초)
                                .setConnectionTimeToLive(60, TimeUnit.SECONDS)
                                // TCP keepalive ON
                                .setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(true).build()
                                );
                        // HTTPS일 때만 SSLContext 적용 (기존 분기 유지)
                        if ("https".equalsIgnoreCase(scheme) && trustAllSsl != null) hc.setSSLContext(trustAllSsl);
                        return hc;
                    })
                    .build();

            ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
            return new ElasticsearchClient(transport);

        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch 클라이언트 생성 중 오류 발생", e);
        }
    }
}
