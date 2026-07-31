package com.tfi.gestion_congresos_backend.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tfi.gestion_congresos_backend.enums.PaperStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)

@Entity
@Table(name = "papers")
public class Paper {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long paperId;
	
	@Column(name = "title", nullable = false)
	private String title;
	
	@EqualsAndHashCode.Include
	@Column(name = "code", nullable = false, unique = true)
	private String code;

	@Column(name = "version", nullable = false)
	private String version;
	
	@Column(name = "summary", nullable = false)
	private String summary;
	
	@Column(name = "keywords", nullable = false)
	private String keywords;
	
	@Column(name = "presentation_date", nullable = false)
	private LocalDateTime presentationDate;
	
	@Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaperStatus status;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "users_reviewer_id")
	private User userReviewer;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "congresses_id", nullable = false)
	private Congress congress;
	
	@OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("authorOrder ASC")
    @Builder.Default
    private List<PaperAuthor> authors = new ArrayList<>();

	@OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("evaluationDate DESC")
    @Builder.Default
    private List<Evaluation> evaluations = new ArrayList<>();
	
}
