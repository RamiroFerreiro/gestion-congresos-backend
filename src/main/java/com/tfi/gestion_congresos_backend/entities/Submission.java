package com.tfi.gestion_congresos_backend.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import com.tfi.gestion_congresos_backend.enums.SubmissionStatus;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long submissionId;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "submission_date", nullable = false)
    private LocalDateTime submissionDate;

    @Column(name = "feedback", columnDefinition = "TEXT") 
    private String feedback;

    @Column(name = "new_deadline")
    private LocalDateTime newDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubmissionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;
}