package com.uagrm.si2g2.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordRecoveryChallengeRepository extends JpaRepository<PasswordRecoveryChallenge, UUID> {

    Optional<PasswordRecoveryChallenge> findByIdAndUsadoFalse(UUID id);
}
