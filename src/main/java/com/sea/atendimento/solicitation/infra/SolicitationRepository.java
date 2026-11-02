package com.sea.atendimento.solicitation.infra;

import com.sea.atendimento.solicitation.domain.Solicitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitationRepository extends JpaRepository<Solicitation, Long> {

    List<Solicitation> findAllByClientIdOrderByCreatedAtDesc(Long clientId);
}