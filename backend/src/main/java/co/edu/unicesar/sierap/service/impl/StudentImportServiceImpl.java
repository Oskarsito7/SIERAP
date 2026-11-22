package co.edu.unicesar.sierap.service.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import co.edu.unicesar.sierap.dto.request.NewStudentRequest;
import co.edu.unicesar.sierap.dto.response.StudentImportPreviewResponse;
import co.edu.unicesar.sierap.dto.response.StudentImportResultResponse;
import co.edu.unicesar.sierap.dto.response.StudentImportResultResponse.RowError;
import co.edu.unicesar.sierap.dto.response.StudentImportRowResponse;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.AccountService;
import co.edu.unicesar.sierap.service.AuditService;
import co.edu.unicesar.sierap.service.StudentImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementación de {@link StudentImportService}: lee el CSV con {@link StudentCsvParser},
 * valida con {@link StudentImportValidator} y crea cada estudiante por el mismo camino
 * que {@code POST /api/students} ({@link AccountService#createImportedStudent}), de modo que
 * reutiliza el alcance, las validaciones y el envío del enlace de contraseña.
 */
@Service
@Transactional
public class StudentImportServiceImpl implements StudentImportService {

    /** Tamaño máximo del archivo: 1 MB. */
    static final long MAX_FILE_BYTES = 1024 * 1024;

    private final ProgramRepository programRepository;
    private final AccessScope accessScope;
    private final StudentImportValidator validator;
    private final AccountService accountService;
    private final AuditService auditService;

    /**
     * Crea el servicio.
     *
     * @param programRepository repositorio de programas
     * @param accessScope       reglas de alcance
     * @param validator         validador de filas
     * @param accountService    servicio de cuentas, por donde se crea cada estudiante
     * @param auditService      bitácora de auditoría (un evento por importación)
     */
    StudentImportServiceImpl(ProgramRepository programRepository, AccessScope accessScope,
                             StudentImportValidator validator, AccountService accountService,
                             AuditService auditService) {
        this.programRepository = programRepository;
        this.accessScope = accessScope;
        this.validator = validator;
        this.accountService = accountService;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentImportPreviewResponse preview(AuthenticatedUser actor, Long programId, MultipartFile file) {
        requireScope(actor, programId);
        List<StudentImportRowResponse> rows = readAndValidate(file);
        int valid = (int) rows.stream().filter(row -> row.errors().isEmpty()).count();
        return new StudentImportPreviewResponse(rows, valid, rows.size() - valid);
    }

    @Override
    public StudentImportResultResponse importStudents(AuthenticatedUser actor, Long programId, MultipartFile file) {
        Program program = requireScope(actor, programId);
        List<StudentImportRowResponse> rows = readAndValidate(file);
        int created = 0;
        List<RowError> errors = new ArrayList<>();
        for (StudentImportRowResponse row : rows) {
            if (!row.errors().isEmpty()) {
                errors.add(new RowError(row.row(), String.join(" ", row.errors())));
                continue;
            }
            accountService.createImportedStudent(actor, new NewStudentRequest(row.firstNames(), row.lastNames(),
                    StudentImportValidator.parseDocumentType(row.documentType()), row.documentNumber(),
                    row.email(), programId, row.studentCode(), Integer.valueOf(row.currentSemester())));
            created++;
        }
        if (created > 0) {
            auditService.record(AuditAction.ACCOUNTS_IMPORTED, "Program", programId, program.getFaculty().getId(),
                    "Importó " + created + (created == 1 ? " estudiante" : " estudiantes") + " al programa "
                            + program.getName() + " desde un archivo CSV.");
        }
        return new StudentImportResultResponse(created, errors);
    }

    private Program requireScope(AuthenticatedUser actor, Long programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Programa", programId));
        accessScope.requireFaculty(accessScope.loadActor(actor), program.getFaculty().getId());
        return program;
    }

    private List<StudentImportRowResponse> readAndValidate(MultipartFile file) {
        return validator.validate(StudentCsvParser.parse(readText(file)));
    }

    /** Comprueba nombre, tamaño y codificación del archivo y lo devuelve como texto. */
    private static String readText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Seleccione un archivo CSV con los estudiantes.");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BusinessRuleException("El archivo debe ser un .csv.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessRuleException("El archivo no puede pesar más de 1 MB.");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.getBytes()))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new BusinessRuleException("El archivo debe estar codificado en UTF-8.");
        } catch (IOException e) {
            throw new BusinessRuleException("No se pudo leer el archivo.");
        }
    }
}
