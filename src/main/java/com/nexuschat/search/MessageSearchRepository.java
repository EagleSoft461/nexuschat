package com.nexuschat.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageSearchRepository extends ElasticsearchRepository<MessageDocument, Long> {

    Page<MessageDocument> findByContentContainingAndDeletedFalse(String content, Pageable pageable);

    Page<MessageDocument> findByRoomIdAndContentContainingAndDeletedFalse(Long roomId, String content, Pageable pageable);

    Page<MessageDocument> findBySenderUsernameAndContentContainingAndDeletedFalse(String username, String content, Pageable pageable);
}
