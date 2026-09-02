package com.miloo.modules.ai.service;

import com.miloo.modules.ai.entity.UserEmbeddingEntity;
import com.miloo.modules.ai.repository.UserEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserEmbeddingRepository embeddingRepository;

    @Transactional
    public void updateUserInterests(UUID userId, List<String> tags) {
        UserEmbeddingEntity entity = embeddingRepository.findById(userId)
                .orElseGet(() -> UserEmbeddingEntity.builder().userId(userId).build());

        if (tags != null) {
            entity.setInterestTags(tags.toArray(new String[0]));
        }

        embeddingRepository.save(entity);
        log.info("[AI Module] Updated interest representation for user {}", userId);
    }

    public double calculateTagSimilarity(List<String> tagsA, List<String> tagsB) {
        if (tagsA == null || tagsB == null || tagsA.isEmpty() || tagsB.isEmpty()) {
            return 0.0;
        }

        Set<String> setA = new HashSet<>(tagsA);
        Set<String> setB = new HashSet<>(tagsB);

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        return (double) intersection.size() / union.size();
    }
}
