package co.edu.unicesar.sierap.service.export;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import co.edu.unicesar.sierap.dto.response.report.GroupReportData;
import co.edu.unicesar.sierap.dto.response.report.IndividualReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.entity.GradeScale;

/** Datos de reportes ya calculados para probar los generadores de archivos. */
final class ExportFixtures {

    static final LocalDateTime GENERATED_AT = LocalDateTime.of(2026, 11, 25, 9, 30);
    static final GradeScale SCALE = GradeScale.of("60", "Bajo:0;Básico:60;Alto:76;Sobresaliente:90");

    private ExportFixtures() {
    }

    static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    static MomentReportData moment(boolean finalResults) {
        return new MomentReportData(GENERATED_AT,
                new MomentReportData.PeriodSummary(1L, "2026-2"),
                new MomentReportData.ProgramSummary(1L, "108", "Ingeniería de Sistemas"),
                new MomentReportData.MomentSummary(1L, 1, "Momento 1", "Programación II", n("5.00")),
                finalResults, 2, 1, 3, 2, n("4.00"), n("3.55"), n("2.80"), n("4.30"), 1, n("50.00"),
                List.of(new MomentReportData.OutcomeSummary(10L, "RAP1", "Diseña algoritmos", n("0.48"), n("0.60"),
                        n("80.00"))),
                List.of(new MomentReportData.DistributionBucket("0,00 - 2,99", "Bajo", n("0"), n("2.99"), 1, n("50.00")),
                        new MomentReportData.DistributionBucket("3,80 - 4,49", "Alto", n("3.80"), n("4.49"), 1,
                                n("50.00"))));
    }

    static GroupReportData group(boolean finalResults) {
        return new GroupReportData(GENERATED_AT,
                new GroupReportData.PeriodSummary(1L, "2026-2"),
                new GroupReportData.ProgramSummary(1L, "108", "Ingeniería de Sistemas"),
                new GroupReportData.MomentSummary(1L, 1, "Momento 1", "Programación II", n("5.00")),
                new GroupReportData.GroupSummary(7L, "Grupo 1", "SIERAP", "Marta Díaz"),
                finalResults, n("4.00"), n("80.00"),
                List.of(new GroupReportData.OutcomeScore(10L, "RAP1", "Diseña algoritmos", n("0.48"), n("0.60"),
                        n("80.00"))),
                List.of(new GroupReportData.MemberSummary(40L, "2026001", "Ana Gómez", n("4.30"), false,
                                List.of(new GroupReportData.MemberOutcomeScore(10L, "RAP1", n("0.52"), n("0.60"),
                                        n("86.67")))),
                        new GroupReportData.MemberSummary(41L, "2026002", "Luis Martínez", n("2.80"), true,
                                List.of(new GroupReportData.MemberOutcomeScore(10L, "RAP1", n("0.30"), n("0.60"),
                                        n("50.00")))),
                        new GroupReportData.MemberSummary(42L, "2026003", "Sara Díaz", null, false, List.of())),
                2, 1);
    }

    static IndividualReportData individual(boolean finalResults) {
        return new IndividualReportData(GENERATED_AT,
                new IndividualReportData.PeriodSummary(1L, "2026-2"),
                new IndividualReportData.ProgramSummary(1L, "108", "Ingeniería de Sistemas"),
                new IndividualReportData.MomentSummary(1L, 1, "Momento 1", "Programación II", n("5.00")),
                new IndividualReportData.StudentSummary(40L, "2026001", "Ana Gómez", 3),
                new IndividualReportData.GroupSummary(7L, "Grupo 1", "SIERAP", "Marta Díaz"),
                finalResults, n("4.30"), n("4.00"), n("86.00"),
                List.of(new IndividualReportData.OutcomeScore(10L, "RAP1", "Diseña algoritmos", n("0.52"), n("0.48"),
                        n("0.60"), n("86.67"))),
                3, 0);
    }
}
