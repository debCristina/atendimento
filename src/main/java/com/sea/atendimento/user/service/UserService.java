package com.sea.atendimento.user.service;

import com.sea.atendimento.shared.audit.Audit;
import com.sea.atendimento.shared.domain.Uf;
import com.sea.atendimento.shared.exception.BusinessRuleException;
import com.sea.atendimento.shared.exception.ConflictException;
import com.sea.atendimento.shared.exception.NotFoundException;
import com.sea.atendimento.user.domain.User;
import com.sea.atendimento.user.infra.UserRepository;
import com.sea.atendimento.user.web.dto.CreateUserRequestDTO;
import com.sea.atendimento.user.web.dto.UserResponseDTO;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Audit("CREATE_INTERNAL_USER")
    public UserResponseDTO createInternalUser(CreateUserRequestDTO request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        boolean hasUfs = request.coveredUfs() != null && !request.coveredUfs().isEmpty();

        User user = switch (request.role()) {
            case ANALYST -> User.newAnalyst(request.name(), email, passwordHash, request.coveredUfs());
            case ADMIN -> {
                if (hasUfs) {
                    throw new BusinessRuleException("Apenas analistas possuem UFs de cobertura");
                }
                yield User.newAdmin(request.name(), email, passwordHash);
            }
            case CLIENT -> throw new BusinessRuleException(
                    "Clientes devem se cadastrar pelo endpoint /auth/register");
        };

        return UserResponseDTO.from(userRepository.save(user));
    }

    @Transactional
    @Audit("UPDATE_ANALYST_UFS")
    public UserResponseDTO updateCoveredUfs(Long userId, Set<Uf> coveredUfs) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        user.updateCoveredUfs(coveredUfs);
        return UserResponseDTO.from(user);
    }
}