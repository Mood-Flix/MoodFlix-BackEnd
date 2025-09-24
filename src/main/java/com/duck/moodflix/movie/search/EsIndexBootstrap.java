package com.duck.moodflix.movie.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EsIndexBootstrap implements CommandLineRunner {
    private final ElasticsearchOperations esOps;

    @Override
    public void run(String... args) {
        IndexOperations io = esOps.indexOps(MovieDoc.class);
        if (!io.exists()) {
            // 인덱스 생성
            io.createWithMapping();      // 매핑 생성
            io.refresh();
            log.info("[ES] movies index created with mapping.");
        } else {
            log.info("[ES] movies index already exists.");
        }
        }
    }

