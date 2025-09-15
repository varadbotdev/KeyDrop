package com.p2pportal.p2p_portal.repository;

import com.p2pportal.p2p_portal.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SharedContentRepository extends JpaRepository<SharedContent, Long> {
    Optional<SharedContent> findByShareCode(String shareCode);
    boolean existsByShareCode(String shareCode);
}