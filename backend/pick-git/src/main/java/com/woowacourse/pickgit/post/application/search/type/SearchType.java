package com.woowacourse.pickgit.post.application.search.type;

import com.woowacourse.pickgit.post.domain.Post;
import java.util.List;
import org.springframework.data.domain.PageRequest;

public interface SearchType {
    boolean isSatisfiedBy(String searchType);
    List<Post> search(String[] keywords, PageRequest pageRequest);
}
