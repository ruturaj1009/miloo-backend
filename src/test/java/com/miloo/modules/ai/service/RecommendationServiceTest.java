package com.miloo.modules.ai.service;

import com.miloo.modules.ai.repository.UserEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationServiceTest {

    private RecommendationService recommendationService;
    private UserEmbeddingRepository embeddingRepository;

    @BeforeEach
    void setUp() {
        embeddingRepository = Mockito.mock(UserEmbeddingRepository.class);
        recommendationService = new RecommendationService(embeddingRepository);
    }

    @Test
    @DisplayName("Should compute accurate Jaccard tag similarity between profiles")
    void testTagSimilarity() {
        List<String> userA = List.of("EDM", "Coffee", "Hiking", "Travel");
        List<String> userB = List.of("Photography", "Coffee", "Travel", "Art Galleries");

        // Intersection: Coffee, Travel (2)
        // Union: EDM, Coffee, Hiking, Travel, Photography, Art Galleries (6)
        // Expected: 2 / 6 = 0.3333...
        double similarity = recommendationService.calculateTagSimilarity(userA, userB);

        assertEquals(2.0 / 6.0, similarity, 0.001);
    }

    @Test
    @DisplayName("Should return 0.0 when there are no overlapping interest tags")
    void testNoOverlap() {
        List<String> userA = List.of("EDM", "Hiking");
        List<String> userB = List.of("Classical Music", "Cooking");

        double similarity = recommendationService.calculateTagSimilarity(userA, userB);

        assertEquals(0.0, similarity, 0.001);
    }
}
