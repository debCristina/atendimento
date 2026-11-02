package com.sea.atendimento.solicitation.service;

import com.sea.atendimento.shared.exception.BusinessRuleException;
import com.sea.atendimento.shared.exception.FieldErrorDetail;
import com.sea.atendimento.shared.exception.NotFoundException;
import com.sea.atendimento.solicitation.domain.Address;
import com.sea.atendimento.solicitation.domain.Solicitation;
import com.sea.atendimento.solicitation.infra.SolicitationRepository;
import com.sea.atendimento.solicitation.web.dto.AddressRequestDTO;
import com.sea.atendimento.solicitation.web.dto.BasicDataRequestDTO;
import com.sea.atendimento.solicitation.web.dto.ConfirmationRequestDTO;
import com.sea.atendimento.solicitation.web.dto.SolicitationResponseDTO;
import com.sea.atendimento.user.domain.User;
import com.sea.atendimento.user.infra.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final CepLookup cepLookup;

    public SolicitationService(SolicitationRepository solicitationRepository,
                               UserRepository userRepository,
                               CepLookup cepLookup) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.cepLookup = cepLookup;
    }


    @Transactional
    public SolicitationResponseDTO create(Long clientId, BasicDataRequestDTO request) {
        User client = userRepository.getReferenceById(clientId);
        Solicitation solicitation = Solicitation.create(
                client, request.serviceType(), request.title(), request.description());
        return SolicitationResponseDTO.from(solicitationRepository.save(solicitation));
    }

    @Transactional
    public SolicitationResponseDTO updateBasicData(Long id, Long clientId, BasicDataRequestDTO request) {
        Solicitation solicitation = findOwnedByClient(id, clientId);
        solicitation.updateBasicData(request.serviceType(), request.title(), request.description());
        return SolicitationResponseDTO.from(solicitation);
    }


    @Transactional
    public SolicitationResponseDTO updateAddress(Long id, Long clientId, AddressRequestDTO request) {
        Solicitation solicitation = findOwnedByClient(id, clientId);

        String cep = Address.normalizeCep(request.cep());
        CepAddress found = cepLookup.findByCep(cep)
                .orElseThrow(() -> new BusinessRuleException("CEP não encontrado",
                        List.of(FieldErrorDetail.ofStep(2, "cep", "CEP não encontrado"))));

        Address address = new Address(found.cep(), found.street(), found.neighborhood(),
                found.city(), found.uf(), request.number(), request.complement());

        solicitation.updateAddress(address);
        return SolicitationResponseDTO.from(solicitation);
    }


    @Transactional
    public SolicitationResponseDTO updateConfirmation(Long id, Long clientId, ConfirmationRequestDTO request) {
        Solicitation solicitation = findOwnedByClient(id, clientId);
        solicitation.updateConfirmation(request.priority(), request.preferredDate(),
                request.estimatedValue(), request.termsAccepted());
        return SolicitationResponseDTO.from(solicitation);
    }


    @Transactional
    public SolicitationResponseDTO submit(Long id, Long clientId) {
        Solicitation solicitation = findOwnedByClient(id, clientId);
        solicitation.submit();
        return SolicitationResponseDTO.from(solicitation);
    }


    @Transactional(readOnly = true)
    public SolicitationResponseDTO findMine(Long id, Long clientId) {
        return SolicitationResponseDTO.from(findOwnedByClient(id, clientId));
    }

    @Transactional(readOnly = true)
    public List<SolicitationResponseDTO> listMine(Long clientId) {
        return solicitationRepository.findAllByClientIdOrderByCreatedAtDesc(clientId).stream()
                .map(SolicitationResponseDTO::from)
                .toList();
    }


    private Solicitation findOwnedByClient(Long id, Long clientId) {
        return solicitationRepository.findById(id)
                .filter(solicitation -> solicitation.isOwnedBy(clientId))
                .orElseThrow(() -> new NotFoundException("Solicitação não encontrada"));
    }
}