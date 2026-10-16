package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.dto.request.ChangePasswordRequest;
import co.edu.unicesar.sierap.dto.request.ForgotPasswordRequest;
import co.edu.unicesar.sierap.dto.request.LoginRequest;
import co.edu.unicesar.sierap.dto.request.RegisterStudentRequest;
import co.edu.unicesar.sierap.dto.request.SetPasswordRequest;
import co.edu.unicesar.sierap.dto.response.TokenResponse;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.entity.RefreshToken;
import co.edu.unicesar.sierap.entity.RegistrationCode;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.exception.AuthenticationFailedException;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.mapper.UserMapper;
import co.edu.unicesar.sierap.repository.RefreshTokenRepository;
import co.edu.unicesar.sierap.repository.RegistrationCodeRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.security.JwtService;
import co.edu.unicesar.sierap.security.OpaqueTokens;
import co.edu.unicesar.sierap.service.AuthService;
import co.edu.unicesar.sierap.service.AuthSession;
import co.edu.unicesar.sierap.service.PasswordLinkService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link AuthService} (ADR-017).
 *
 * <p>El token de acceso es un JWT corto; el de renovación es un valor aleatorio del
 * que solo se guarda el hash. Si alguien presenta un token de renovación que ya se
 * rotó (señal de que pudo ser robado y usado por otro), se cierran todas las sesiones
 * de ese usuario.</p>
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    /** Mismo mensaje exista o no el correo, para no revelar qué cuentas hay. */
    private static final String INVALID_CREDENTIALS = "Correo o contraseña incorrectos.";

    private static final String SESSION_EXPIRED = "La sesión expiró. Inicie sesión de nuevo.";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegistrationCodeRepository registrationCodeRepository;
    private final PasswordLinkService passwordLinkService;
    private final AccountValidator accountValidator;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;
    private final Clock clock;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param userRepository             repositorio de usuarios
     * @param refreshTokenRepository     repositorio de sesiones
     * @param registrationCodeRepository repositorio de códigos de registro
     * @param passwordLinkService        enlaces por correo
     * @param accountValidator           validaciones de cuentas nuevas
     * @param passwordEncoder            codificador BCrypt
     * @param jwtService                 emisión de tokens de acceso
     * @param properties                 parámetros de autenticación
     * @param clock                      reloj de la aplicación
     */
    public AuthServiceImpl(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                           RegistrationCodeRepository registrationCodeRepository,
                           PasswordLinkService passwordLinkService, AccountValidator accountValidator,
                           PasswordEncoder passwordEncoder, JwtService jwtService, AuthProperties properties,
                           Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.registrationCodeRepository = registrationCodeRepository;
        this.passwordLinkService = passwordLinkService;
        this.accountValidator = accountValidator;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AuthSession login(LoginRequest request) {
        User user = userRepository.findByEmail(AccountValidator.normalizeEmail(request.email()))
                .filter(User::hasPassword)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));
        if (!user.isActive()) {
            throw new ForbiddenAccessException("La cuenta está desactivada. Comuníquese con quien la creó.");
        }
        return startSession(user);
    }

    /**
     * {@inheritDoc}
     *
     * <p>La {@link AuthenticationFailedException} no deshace la transacción: si se
     * detecta un token reutilizado, el cierre de todas las sesiones debe quedar
     * guardado aunque la petición responda 401.</p>
     */
    @Override
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthSession refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationFailedException(SESSION_EXPIRED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(refreshToken))
                .orElseThrow(() -> new AuthenticationFailedException(SESSION_EXPIRED));
        User user = stored.getUser();
        if (stored.isRotated()) {
            // Reutilizar un token ya rotado indica que pudo ser robado: se cierran todas
            // las sesiones del usuario.
            refreshTokenRepository.deleteAllByUserId(user.getId());
            throw new AuthenticationFailedException(SESSION_EXPIRED);
        }
        if (!stored.isActiveAt(now) || !user.isActive()) {
            throw new AuthenticationFailedException(SESSION_EXPIRED);
        }
        stored.markRotated(now);
        return startSession(user);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(refreshToken))
                .ifPresent(refreshTokenRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(Long userId) {
        return UserMapper.toSummary(findUser(userId));
    }

    @Override
    public AuthSession changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!user.hasPassword() || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("La contraseña actual no es correcta.");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.deleteAllByUserId(user.getId());
        return startSession(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> user = userRepository.findByEmail(AccountValidator.normalizeEmail(request.email()))
                .filter(User::isActive);
        user.ifPresent(passwordLinkService::sendPasswordResetLink);
    }

    @Override
    public void setPassword(SetPasswordRequest request) {
        User user = passwordLinkService.consume(request.token());
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.deleteAllByUserId(user.getId());
    }

    @Override
    public void registerStudent(RegisterStudentRequest request) {
        RegistrationCode code = registrationCodeRepository
                .findByCode(request.registrationCode().trim().toUpperCase(Locale.ROOT))
                .filter(candidate -> candidate.isUsableOn(LocalDate.now(clock)))
                .orElseThrow(() -> new BusinessRuleException(
                        "El código de registro no existe, venció o fue desactivado. Pídale uno nuevo a su docente."));
        String email = AccountValidator.normalizeEmail(request.email());
        String domain = "@" + properties.institutionalEmailDomain().toLowerCase(Locale.ROOT);
        if (!email.endsWith(domain)) {
            throw new BusinessRuleException("Para registrarse debe usar su correo institucional (" + domain + ").");
        }
        accountValidator.requireNewUser(email, request.documentNumber());
        accountValidator.requireNewStudentCode(request.studentCode());

        Student student = new Student(request.firstNames().trim(), request.lastNames().trim(),
                request.documentType(), request.documentNumber().trim(), email, null,
                LocalDateTime.now(clock), request.studentCode().trim(), request.currentSemester(),
                code.getProgram());
        student.setCreatedBy(code.getTeacher());
        student.setRegistrationCode(code);
        userRepository.save(student);
        passwordLinkService.sendAccountSetupLink(student);
    }

    private AuthSession startSession(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        refreshTokenRepository.deleteExpiredByUserId(user.getId(), now);
        String refreshToken = OpaqueTokens.newToken();
        refreshTokenRepository.save(new RefreshToken(user, OpaqueTokens.hash(refreshToken),
                now.plusDays(properties.refreshTokenDays()), now));
        TokenResponse token = new TokenResponse(jwtService.createAccessToken(user),
                jwtService.getAccessTokenSeconds(), UserMapper.toSummary(user));
        return new AuthSession(token, refreshToken);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
    }
}
