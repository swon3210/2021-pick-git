package com.woowacourse.pickgit.common.mockapi;

import com.woowacourse.pickgit.exception.platform.PlatformHttpErrorException;
import com.woowacourse.pickgit.tag.infrastructure.PlatformTagApiRequester;

public class MockTagApiRequester implements PlatformTagApiRequester {

    private static final String TESTER_ACCESS_TOKEN = "oauth.access.token";
    private static final String USER_NAME = "jipark3";
    private static final String REPOSITORY_NAME = "doms-react";

    @Override
    public String requestTags(String url, String accessToken) {
        String validUrl =
            "https://api.github.com/repos/" + USER_NAME + "/" + REPOSITORY_NAME + "/languages";
        if (!accessToken.equals(TESTER_ACCESS_TOKEN)) {
            throw new PlatformHttpErrorException();
        }
        if (!url.equals(validUrl)) {
            throw new PlatformHttpErrorException();
        }
        return "{\"JavaScript\": \"91949\", \"HTML\": \"13\", \"CSS\": \"9\", \"Other\": \"13\"}";
    }
}
