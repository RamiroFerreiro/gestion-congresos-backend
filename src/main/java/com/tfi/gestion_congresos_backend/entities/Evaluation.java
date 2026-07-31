package com.tfi.gestion_congresos_backend.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import com.tfi.gestion_congresos_backend.enums.PaperStatus;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long evaluationId;

    @Column(name = "feedback", columnDefinition = "TEXT") 
    private String feedback;

    @Column(name = "new_deadline")
    private LocalDateTime newDeadline;

    @Enumerated(EnumType.STRING)
    @Column (name= "new_status")
    private PaperStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;
    
    @Column(name = "evaluation_date")
    private LocalDateTime evaluationDate;

    @Column(name = "evaluated_version", nullable = false)
	private String evaluatedVersion;
	
}