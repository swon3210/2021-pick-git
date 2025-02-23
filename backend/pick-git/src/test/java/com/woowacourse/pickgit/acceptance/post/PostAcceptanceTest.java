package com.woowacourse.pickgit.acceptance.post;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.woowacourse.pickgit.acceptance.AcceptanceTest;
import com.woowacourse.pickgit.authentication.application.dto.OAuthProfileResponse;
import com.woowacourse.pickgit.authentication.domain.OAuthClient;
import com.woowacourse.pickgit.authentication.presentation.dto.OAuthTokenResponse;
import com.woowacourse.pickgit.common.factory.FileFactory;
import com.woowacourse.pickgit.config.InfrastructureTestConfiguration;
import com.woowacourse.pickgit.exception.authentication.UnauthorizedException;
import com.woowacourse.pickgit.exception.dto.ApiErrorResponse;
import com.woowacourse.pickgit.exception.post.CannotUnlikeException;
import com.woowacourse.pickgit.exception.post.DuplicatedLikeException;
import com.woowacourse.pickgit.post.application.dto.response.PostResponseDto;
import com.woowacourse.pickgit.post.application.dto.response.RepositoryResponseDto;
import com.woowacourse.pickgit.post.presentation.dto.request.PostUpdateRequest;
import com.woowacourse.pickgit.post.presentation.dto.response.LikeResponse;
import com.woowacourse.pickgit.post.presentation.dto.response.PostUpdateResponse;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

class PostAcceptanceTest extends AcceptanceTest {

    private static final String ANOTHER_USERNAME = "pick-git-login";
    private static final String USERNAME = "jipark3";

    private String githubRepoUrl;
    private String content;
    private Map<String, Object> request;

    @MockBean
    private OAuthClient oAuthClient;

    @BeforeEach
    void setUp() {
        githubRepoUrl = "https://github.com/woowacourse-teams/2021-pick-git";
        List<String> tags = List.of("java", "spring");
        content = "this is content";

        Map<String, Object> body = new HashMap<>();
        body.put("githubRepoUrl", githubRepoUrl);
        body.put("tags", tags);
        body.put("content", content);
        request = body;
    }

    @DisplayName("사용자는 게시글을 등록한다.")
    @Test
    void write_LoginUser_Success() {
        // given
        String token = 로그인_되어있음(ANOTHER_USERNAME).getToken();

        // when
        requestWrite(token);
    }

    @DisplayName("사용자는 태그 없이 게시글을 작성할 수 있다.")
    @Test
    void write_LoginUserWithNoneTags_Success() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        // when, then
        given().log().all()
            .auth().oauth2(token)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .formParams("githubRepoUrl", githubRepoUrl)
            .formParams("content", content)
            .multiPart("images", FileFactory.getTestImage1File())
            .multiPart("images", FileFactory.getTestImage2File())
            .when()
            .post("/api/posts")
            .then().log().all()
            .statusCode(HttpStatus.CREATED.value())
            .extract();
    }

    @DisplayName("잘못된 태그 이름을 가진 게시글을 작성할 수 없다.")
    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "  ", "abcdeabcdeabcdeabcdea"})
    void write_LoginUserWithInvalidTags_Fail(String tagName) {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();
        List<String> invalidTags = List.of("Java", "JavaScript", tagName);

        // when
        ApiErrorResponse response = given().log().all()
            .auth().oauth2(token)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .formParams("githubRepoUrl", githubRepoUrl)
            .formParams("content", content)
            .formParams("tags", invalidTags)
            .multiPart("images", FileFactory.getTestImage1File())
            .multiPart("images", FileFactory.getTestImage2File())
            .when()
            .post("/api/posts")
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .as(ApiErrorResponse.class);

        // then
        assertThat(response.getErrorCode()).isEqualTo("F0003");
    }

    @DisplayName("사용자는 중복된 태그를 가진 게시글을 작성할 수 없다.")
    @Test
    void write_LoginUserWithDuplicatedTags_Fail() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();
        List<String> duplicatedTags = List.of("Java", "JavaScript", "Java");

        // when
        ApiErrorResponse response = given().log().all()
            .auth().oauth2(token)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .formParams("githubRepoUrl", githubRepoUrl)
            .formParams("content", content)
            .formParams("tags", duplicatedTags)
            .multiPart("images", FileFactory.getTestImage1File())
            .multiPart("images", FileFactory.getTestImage2File())
            .when()
            .post("/api/posts")
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .as(ApiErrorResponse.class);

        // then
        assertThat(response.getErrorCode()).isEqualTo("P0001");
    }

    @DisplayName("로그인일때 홈 피드를 조회한다. - 게시글 좋아요 여부 true/false")
    @Test
    void readHomeFeed_LoginUser_Success() {
        String token = 로그인_되어있음(ANOTHER_USERNAME).getToken();

        requestToWritePostApi(token, HttpStatus.CREATED);
        requestToWritePostApi(token, HttpStatus.CREATED);
        requestToWritePostApi(token, HttpStatus.CREATED);

        List<PostResponseDto> response = given().log().all()
            .auth().oauth2(token)
            .when()
            .get("/api/posts?page=0&limit=3")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        assertThat(response)
            .hasSize(3)
            .extracting("liked")
            .containsExactly(false, false, false);
    }

    @DisplayName("비 로그인이어도 홈 피드 조회가 가능하다. - 게시물 좋아요 여부는 항상 null")
    @Test
    void read_GuestUser_Success() {
        String token = 로그인_되어있음(ANOTHER_USERNAME).getToken();

        requestToWritePostApi(token, HttpStatus.CREATED);
        requestToWritePostApi(token, HttpStatus.CREATED);
        requestToWritePostApi(token, HttpStatus.CREATED);

        List<PostResponseDto> response = given().log().all()
            .when()
            .get("/api/posts?page=0&limit=3")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        assertThat(response)
            .hasSize(3)
            .extracting("liked")
            .containsExactly(null, null, null);
    }

    @DisplayName("로그인 상태에서 내 피드 조회가 가능하다.")
    @Test
    void readMyFeed_LoginUser_Success() {
        String token = 로그인_되어있음(ANOTHER_USERNAME).getToken();

        requestToWritePostApi(token, HttpStatus.CREATED);
        requestToWritePostApi(token, HttpStatus.CREATED);
        requestToWritePostApi(token, HttpStatus.CREATED);

        List<PostResponseDto> response = given().log().all()
            .auth().oauth2(token)
            .when()
            .get("/api/posts/me?page=0&limit=3")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        assertThat(response)
            .hasSize(3)
            .extracting("liked")
            .containsExactly(false, false, false);
    }

    @DisplayName("비로그인 상태에서는 내 피드 조회가 불가능하다.")
    @Test
    void readMyFeed_GuestUser_Success() {
        String token = 로그인_되어있음(ANOTHER_USERNAME).getToken();

        requestToWritePostApi(token, HttpStatus.CREATED);
        requestToWritePostApi(token, HttpStatus.CREATED);
        requestToWritePostApi(token, HttpStatus.CREATED);

        given().log().all()
            .when()
            .get("/api/posts/me?page=0&limit=3")
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @DisplayName("로그인 상태에서 다른 유저 피드 조회가 가능하다.")
    @Test
    void readUserFeed_LoginUser_Success() {
        String loginUserToken = 로그인_되어있음(USERNAME).getToken();
        String targetUserToken = 로그인_되어있음(ANOTHER_USERNAME).getToken();

        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);
        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);
        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);

        List<PostResponseDto> response = given().log().all()
            .auth().oauth2(loginUserToken)
            .when()
            .get("/api/posts/" + ANOTHER_USERNAME + "?page=0&limit=3")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        assertThat(response)
            .hasSize(3)
            .extracting("liked")
            .containsExactly(false, false, false);
    }

    @DisplayName("비 로그인 상태에서 다른 유저 피드 조회가 가능하다.")
    @Test
    void readUserFeed_GuestUser_Success() {
        String targetUserToken = 로그인_되어있음(ANOTHER_USERNAME).getToken();

        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);
        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);
        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);

        List<PostResponseDto> response = given().log().all()
            .when()
            .get("/api/posts/" + ANOTHER_USERNAME + "?page=0&limit=3")
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        assertThat(response)
            .hasSize(3)
            .extracting("liked")
            .containsExactly(null, null, null);
    }

    @DisplayName("게스트는 게시글을 등록할 수 없다. - 유효하지 않은 토큰이 있는 경우 (Authorization header O)")
    @Test
    void write_GuestUserWithToken_Fail() {
        // given
        String token = "Bearer guest";

        // when
        requestToWritePostApi(token, HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("게스트는 게시글을 등록할 수 없다. - 토큰이 없는 경우 (Authorization header X)")
    @Test
    void write_GuestUserWithoutToken_Fail() {
        // when
        given().log().all()
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .formParams(request)
            .multiPart("images", FileFactory.getTestImage1File())
            .multiPart("images", FileFactory.getTestImage2File())
            .when()
            .post("/api/posts")
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .extract();
    }

    private ExtractableResponse<Response> requestToWritePostApi(String token,
        HttpStatus httpStatus) {
        return given().log().all()
            .auth().oauth2(token)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .formParams(request)
            .multiPart("images", FileFactory.getTestImage1File())
            .multiPart("images", FileFactory.getTestImage2File())
            .when()
            .post("/api/posts")
            .then().log().all()
            .statusCode(httpStatus.value())
            .extract();
    }

    @DisplayName("사용자는 Repository 목록을 가져올 수 있다.")
    @Test
    void userRepositories_LoginUser_Success() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        // when
        List<RepositoryResponseDto> response = requestUserRepositories(token, HttpStatus.OK.value())
            .as(new TypeRef<>() {
            });

        // then
        assertThat(response).hasSize(2);
    }

    @DisplayName("토큰이 유효하지 않은 경우 예외가 발생한다. - 500 예외")
    @Test
    void userRepositories_InvalidAccessToken_500Exception() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        // when
        requestUserRepositories(token + "hi", HttpStatus.UNAUTHORIZED.value());
    }

    private ExtractableResponse<Response> requestUserRepositories(String token, int statusCode) {
        return given().log().all()
            .auth().oauth2(token)
            .when()
            .get("/api/github/repositories?page=" + 0L + "&limit=" + 50L)
            .then().log().all()
            .statusCode(statusCode)
            .extract();
    }

    @DisplayName("로그인 사용자는 게시물을 좋아요 할 수 있다. - 성공")
    @Test
    void likePost_LoginUser_Success() {
        // given
        String loginUserToken = 로그인_되어있음(USERNAME).getToken();
        String targetUserToken = 로그인_되어있음(ANOTHER_USERNAME).getToken();
        Long postId = 1L;

        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);

        // when
        LikeResponse response = given().log().all()
            .auth().oauth2(loginUserToken)
            .when()
            .put("/api/posts/{postId}/likes", postId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(response.getLikesCount()).isEqualTo(1);
        assertThat(response.getLiked()).isTrue();
    }

    @DisplayName("로그인 사용자는 게시물을 좋아요 취소 할 수 있다. - 성공")
    @Test
    void unlikePost_LoginUser_Success() {
        // given
        String loginUserToken = 로그인_되어있음(USERNAME).getToken();
        String targetUserToken = 로그인_되어있음(ANOTHER_USERNAME).getToken();
        Long postId = 1L;

        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);

        LikeResponse likePostResponse = given().log().all()
            .auth().oauth2(loginUserToken)
            .when()
            .put("/api/posts/{postId}/likes", postId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        assertThat(likePostResponse.getLikesCount()).isEqualTo(1);
        assertThat(likePostResponse.getLiked()).isTrue();

        // when
        LikeResponse unlikePostResponse = given().log().all()
            .auth().oauth2(loginUserToken)
            .when()
            .delete("/api/posts/{postId}/likes", postId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(unlikePostResponse.getLikesCount()).isEqualTo(0);
        assertThat(unlikePostResponse.getLiked()).isFalse();
    }

    @DisplayName("게스트는 게시물을 좋아요 할 수 없다. - 실패")
    @Test
    void likePost_GuestUser_401ExceptionThrown() {
        // given
        String targetUserToken = 로그인_되어있음(ANOTHER_USERNAME).getToken();
        Long postId = 1L;

        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);

        // when
        UnauthorizedException response = given().log().all()
            .when()
            .put("/api/posts/{postId}/likes", postId)
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .extract()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(response.getErrorCode()).isEqualTo("A0001");
    }

    @DisplayName("게스트는 게시물을 좋아요 취소 할 수 없다. - 실패")
    @Test
    void unlikePost_GuestUser_401ExceptionThrown() {
        // given
        String targetUserToken = 로그인_되어있음(ANOTHER_USERNAME).getToken();
        Long postId = 1L;

        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);

        // when
        UnauthorizedException response = given().log().all()
            .when()
            .delete("/api/posts/{postId}/likes", postId)
            .then().log().all()
            .statusCode(HttpStatus.UNAUTHORIZED.value())
            .extract()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(response.getErrorCode()).isEqualTo("A0001");
    }

    @DisplayName("로그인 사용자는 이미 좋아요한 게시물을 좋아요 할 수 없다. - 실패")
    @Test
    void likePost_DuplicatedLike_400ExceptionThrown() {
        // given
        String loginUserToken = 로그인_되어있음(USERNAME).getToken();
        String targetUserToken = 로그인_되어있음(ANOTHER_USERNAME).getToken();
        Long postId = 1L;

        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);

        LikeResponse likePostResponse = given().log().all()
            .auth().oauth2(loginUserToken)
            .when()
            .put("/api/posts/{postId}/likes", postId)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .as(new TypeRef<>() {
            });

        assertThat(likePostResponse.getLikesCount()).isEqualTo(1);
        assertThat(likePostResponse.getLiked()).isTrue();

        // when
        DuplicatedLikeException secondLikeResponse = given().log().all()
            .auth().oauth2(loginUserToken)
            .when()
            .put("/api/posts/{postId}/likes", postId)
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(secondLikeResponse.getErrorCode()).isEqualTo("P0003");
    }

    @DisplayName("로그인 사용자는 좋아요 하지 않은 게시물을 좋아요 취소 할 수 없다. - 실패")
    @Test
    void unlikePost_cannotUnlike_400ExceptionThrown() {
        // given
        String loginUserToken = 로그인_되어있음(USERNAME).getToken();
        String targetUserToken = 로그인_되어있음(ANOTHER_USERNAME).getToken();
        Long postId = 1L;

        requestToWritePostApi(targetUserToken, HttpStatus.CREATED);

        // when
        CannotUnlikeException unlikeResponse = given().log().all()
            .auth().oauth2(loginUserToken)
            .when()
            .delete("/api/posts/{postId}/likes", postId)
            .then().log().all()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .extract()
            .as(new TypeRef<>() {
            });

        // then
        assertThat(unlikeResponse.getErrorCode()).isEqualTo("P0004");
    }

    @DisplayName("사용자는 게시물을 수정한다.")
    @Test
    void update_LoginUser_Success() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        requestWrite(token);

        PostUpdateRequest updateRequest = PostUpdateRequest.builder()
            .tags(List.of("java", "spring"))
            .content("hello")
            .build();

        PostUpdateResponse response = PostUpdateResponse.builder()
            .tags(List.of("java", "spring"))
            .content("hello")
            .build();

        // when
        PostUpdateResponse updateResponse =
            putApiForUpdate(token, updateRequest, HttpStatus.CREATED)
                .as(PostUpdateResponse.class);

        // then
        assertThat(updateResponse)
            .usingRecursiveComparison()
            .isEqualTo(response);
    }

    @DisplayName("유효하지 않은 내용(null)의 게시물은 수정할 수 없다. - 400 예외")
    @Test
    void update_NullContent_400Exception() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        requestWrite(token);

        PostUpdateRequest updateRequest = PostUpdateRequest.builder()
            .tags(List.of("java", "spring"))
            .content(null)
            .build();

        // when
        ApiErrorResponse response =
            putApiForUpdate(token, updateRequest, HttpStatus.BAD_REQUEST)
                .as(ApiErrorResponse.class);

        // then
        assertThat(response.getErrorCode()).isEqualTo("F0001");
    }

    @DisplayName("유효하지 않은 내용(500자 초과)의 게시물은 수정할 수 없다. - 400 예외")
    @Test
    void update_Over500Content_400Exception() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        requestWrite(token);

        PostUpdateRequest updateRequest = PostUpdateRequest.builder()
            .tags(List.of("java", "spring"))
            .content("a".repeat(501))
            .build();

        // when
        ApiErrorResponse response =
            putApiForUpdate(token, updateRequest, HttpStatus.BAD_REQUEST)
                .as(ApiErrorResponse.class);

        // then
        assertThat(response.getErrorCode()).isEqualTo("F0004");
    }

    @DisplayName("유효하지 않은 토큰으로 게시물을 수정할 수 없다. - 401 예외")
    @Test
    void update_InvalidToken_401Exception() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        requestWrite(token);

        PostUpdateRequest updateRequest = PostUpdateRequest.builder()
            .tags(List.of("java", "spring"))
            .content("hello")
            .build();

        // when
        ApiErrorResponse response =
            putApiForUpdate("invalidToken", updateRequest, HttpStatus.UNAUTHORIZED)
                .as(ApiErrorResponse.class);

        // then
        assertThat(response.getErrorCode()).isEqualTo("A0001");
    }

    private ExtractableResponse<Response> putApiForUpdate(
        String token,
        PostUpdateRequest updateRequest,
        HttpStatus httpStatus
    ) {
        return given().log().all()
            .auth().oauth2(token)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(updateRequest)
            .when()
            .put("/api/posts/{postId}", 1L)
            .then().log().all()
            .statusCode(httpStatus.value())
            .extract();
    }

    @DisplayName("사용자는 게시물을 삭제한다.")
    @Test
    void delete_LoginUser_Success() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        requestWrite(token);

        // when
        deleteApiForUpdate(token, HttpStatus.NO_CONTENT);
    }

    @DisplayName("유효하지 않은 토큰으로 게시물을 삭제할 수 없다. - 401 예외")
    @Test
    void delete_invalidToken_401Exception() {
        // given
        String token = 로그인_되어있음(USERNAME).getToken();

        requestWrite(token);

        // when
        ApiErrorResponse response =
            deleteApiForUpdate("invalidToken", HttpStatus.UNAUTHORIZED)
                .as(ApiErrorResponse.class);

        // then
        assertThat(response.getErrorCode()).isEqualTo("A0001");
    }

    private ExtractableResponse<Response> deleteApiForUpdate(
        String token,
        HttpStatus httpStatus) {
        return given().log().all()
            .auth().oauth2(token)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .delete("/api/posts/{postId}", 1L)
            .then().log().all()
            .statusCode(httpStatus.value())
            .extract();
    }

    private OAuthTokenResponse 로그인_되어있음(String name) {
        OAuthTokenResponse response = 로그인_요청(name)
            .as(OAuthTokenResponse.class);

        assertThat(response.getToken()).isNotBlank();

        return response;
    }

    private ExtractableResponse<Response> 로그인_요청(String name) {
        // given
        String oauthCode = "1234";
        String accessToken = "oauth.access.token";

        OAuthProfileResponse oAuthProfileResponse = new OAuthProfileResponse(
            name, "image", "hi~", "github.com/",
            null, null, null, null
        );

        given(oAuthClient.getAccessToken(oauthCode))
            .willReturn(accessToken);
        given(oAuthClient.getGithubProfile(accessToken))
            .willReturn(oAuthProfileResponse);

        // when
        return given().log().all()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when()
            .get("/api/afterlogin?code=" + oauthCode)
            .then().log().all()
            .statusCode(HttpStatus.OK.value())
            .extract();
    }

    private void requestWrite(String token) {
        given().log().all()
            .auth().oauth2(token)
            .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
            .formParams(request)
            .multiPart("images", FileFactory.getTestImage1File())
            .multiPart("images", FileFactory.getTestImage2File())
            .when()
            .post("/api/posts")
            .then().log().all()
            .statusCode(HttpStatus.CREATED.value())
            .extract();
    }
}
