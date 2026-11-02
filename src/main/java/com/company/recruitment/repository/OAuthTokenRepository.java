package com.company.recruitment.repository;

import com.company.recruitment.entity.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, String> {
}
