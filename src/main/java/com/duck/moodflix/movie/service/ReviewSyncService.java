package com.duck.moodflix.movie.service;

import com.duck.moodflix.movie.client.TMDbClient;
import com.duck.moodflix.movie.domain.entity.Movie;
import com.duck.moodflix.movie.domain.entity.TmdbReview;
import com.duck.moodflix.movie.dto.tmdb.reviews.ReviewDto;
import com.duck.moodflix.movie.dto.tmdb.reviews.ReviewsPageDto;
import com.duck.moodflix.movie.repository.TmdbReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSyncService {

    private final TMDbClient tmdb;
    private final TmdbReviewRepository reviewRepo;
    private final TransactionTemplate tx;

    public int syncForMovie(Movie movie) {
        ReviewsPageDto ko = tmdb.getReviews(movie.getTmdbId(), "ko-KR", 1);
        ReviewsPageDto en = tmdb.getReviews(movie.getTmdbId(), "en-US", 1);
        int upserted = tx.execute(status -> upsertAll(movie, ko) + upsertAll(movie, en));
        log.info("Synced TMDb reviews for movieId={}, upserted={}", movie.getId(), upserted);
        return upserted;
    }

    private int upsertAll(Movie movie, ReviewsPageDto page) {
        if (page == null || page.results() == null || page.results().isEmpty()) return 0;
        int cnt = 0;
        for (ReviewDto r : page.results()) cnt += upsert(movie, r);
        return cnt;
    }

    private int upsert(Movie movie, ReviewDto r) {
        if (r == null || r.id() == null) return 0;

        TmdbReview entity = reviewRepo.findById(r.id()).orElse(null);
        Double rating = (r.authorDetails() == null) ? null : r.authorDetails().rating();
        OffsetDateTime created = safeOffset(r.createdAt());

        boolean changed = false;
        if (entity == null) {
            reviewRepo.save(TmdbReview.of(r.id(), r.author(), r.content(), rating, r.url(), r.createdAt(), movie));
            return 1;
        }
        if (!Objects.equals(entity.getAuthor(), r.author())) { entity.setAuthor(r.author()); changed = true; }
        if (!Objects.equals(entity.getContent(), r.content())) { entity.setContent(r.content()); changed = true; }
        if (!Objects.equals(entity.getRating(), rating))       { entity.setRating(rating);       changed = true; }
        if (!Objects.equals(entity.getUrl(), r.url()))         { entity.setUrl(r.url());         changed = true; }
        if (!Objects.equals(entity.getCreatedAtTmdb(), created)) { entity.setCreatedAtTmdb(created); changed = true; }
        if (entity.getMovie() == null || !Objects.equals(entity.getMovie().getId(), movie.getId())) {
            entity.setMovie(movie); changed = true;
        }
        if (changed) reviewRepo.save(entity);
        return changed ? 1 : 0;
    }

    private OffsetDateTime safeOffset(String iso) {
        try { return iso == null ? null : OffsetDateTime.parse(iso); }
        catch (Exception e) { return null; }
    }
}
