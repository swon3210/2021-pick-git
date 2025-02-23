package com.woowacourse.pickgit.user.infrastructure.extractor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woowacourse.pickgit.exception.user.ContributionParseException;
import com.woowacourse.pickgit.user.infrastructure.dto.CountDto;
import com.woowacourse.pickgit.user.infrastructure.dto.ItemDto;
import com.woowacourse.pickgit.user.infrastructure.requester.PlatformContributionApiRequester;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GithubContributionExtractor implements PlatformContributionExtractor {

    private final ObjectMapper objectMapper;
    private final PlatformContributionApiRequester platformContributionApiRequester;
    private final String apiUrlFormatForStar;
    private final String apiUrlFormatForCount;

    public GithubContributionExtractor(
        ObjectMapper objectMapper,
        PlatformContributionApiRequester platformContributionApiRequester,
        @Value("${github.contribution.star-url}") String apiUrlFormatForStar,
        @Value("${github.contribution.count-url}") String apiUrlFormatForCount
    ) {
        this.objectMapper = objectMapper;
        this.platformContributionApiRequester = platformContributionApiRequester;
        this.apiUrlFormatForStar = apiUrlFormatForStar;
        this.apiUrlFormatForCount = apiUrlFormatForCount;
    }

    @Override
    public ItemDto extractStars(String accessToken, String username) {
        String apiUrl = generateUrl(username);
        String response = platformContributionApiRequester.request(apiUrl, accessToken);

        return parseToStars(response);
    }

    private String generateUrl(String username) {
        return String.format(apiUrlFormatForStar, username);
    }

    private ItemDto parseToStars(String response) {
        try {
            return objectMapper.readValue(response, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new ContributionParseException();
        }
    }

    @Override
    public CountDto extractCount(String restUrl, String accessToken, String username) {
        String apiUrl = generateUrl(restUrl, username);
        String response = platformContributionApiRequester.request(apiUrl, accessToken);

        return parseToCount(response);
    }

    private String generateUrl(String restUrl, String username) {
        return apiUrlFormatForCount + String.format(restUrl, username);
    }

    private CountDto parseToCount(String response) {
        try {
            return objectMapper.readValue(response, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new ContributionParseException();
        }
    }
}
