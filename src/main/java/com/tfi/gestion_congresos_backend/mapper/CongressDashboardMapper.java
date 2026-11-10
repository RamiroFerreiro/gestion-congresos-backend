package com.tfi.gestion_congresos_backend.mapper;

import com.tfi.gestion_congresos_backend.dtos.dashboard.*;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.repository.projection.AcademicMetricsProjection;
import com.tfi.gestion_congresos_backend.repository.projection.FinancialMetricsProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.math.RoundingMode;
@Mapper(componentModel = "spring")
public interface CongressDashboardMapper {

    // --- Mapeo Principal ---
    @Mapping(target = "congressCode", source = "congress.code")
    @Mapping(target = "congressName", source = "congress.name")
    @Mapping(target = "academicMetrics", expression = "java(toAcademicDashboardMetricsDTO(academicProj))")
    @Mapping(target = "financialMetrics", expression = "java(toFinancialDashboardMetricsDTO(financialProj))")
    CongressDashboardResponseDTO toDashboardResponseDTO(
            Congress congress,
            AcademicMetricsProjection academicProj,
            FinancialMetricsProjection financialProj
    );

    // --- Sub-DTO Académico ---
    default AcademicDashboardMetricsDTO toAcademicDashboardMetricsDTO(AcademicMetricsProjection proj) {
        long total = (proj != null && proj.getTotalSubmissions() != null) ? proj.getTotalSubmissions() : 0L;
        
        return AcademicDashboardMetricsDTO.builder()
                .totalSubmissions(total)
                .unassignedSubmissions(mapMetric(proj != null ? proj.getUnassignedSubmissions() : null, total))
                .underEvaluationSubmissions(mapMetric(proj != null ? proj.getUnderEvaluationSubmissions() : null, total))
                .needsRevisionSubmissions(mapMetric(proj != null ? proj.getNeedsRevisionSubmissions() : null, total))
                .acceptedSubmissions(mapMetric(proj != null ? proj.getAcceptedSubmissions() : null, total))
                .rejectedSubmissions(mapMetric(proj != null ? proj.getRejectedSubmissions() : null, total))
                .notSubmittedSubmissions(mapMetric(proj != null ? proj.getNotSubmittedSubmissions() : null, total))
                .build();
    }

    // --- Sub-DTO Financiero ---
    default FinancialDashboardMetricsDTO toFinancialDashboardMetricsDTO(FinancialMetricsProjection proj) {
        long total = (proj != null && proj.getTotalPayments() != null) ? proj.getTotalPayments() : 0L;
        
        return FinancialDashboardMetricsDTO.builder()
                .totalPayments(total)
                .pendingApprovalPayments(mapMetric(proj != null ? proj.getPendingApprovalPayments() : null, total))
                .rejectedPayments(mapMetric(proj != null ? proj.getRejectedPayments(): null, total))
                .approvedPayments(mapMetric(proj != null ? proj.getApprovedPayments() : null, total))
                .build();
    }

    // --- Método auxiliar tolerante a NULLs y 0 ---
    @Named("mapMetric")
    default MetricValueDTO mapMetric(Long count, long total) {
        long safeCount = (count != null) ? count : 0L;

        if (total == 0L || safeCount == 0L) {
            return MetricValueDTO.builder()
                    .count(safeCount)
                    .percentage(0.0)
                    .build();
        }

        double percentage = BigDecimal.valueOf((double) safeCount / total * 100)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return MetricValueDTO.builder()
                .count(safeCount)
                .percentage(percentage)
                .build();
    }
}