package com.tfi.gestion_congresos_backend.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "congresses")
public class Congress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long congressId;
	
	@Column(name = "name", nullable = false)
	private String name;
	
	@Column(name = "place", nullable = false)
	private String place;
	
	@Column(name = "registration_start_date", nullable = false)
	private LocalDateTime registrationStartDate;
	
	@Column(name = "registration_end_date", nullable = false)
	private LocalDateTime registrationEndDate;
	
	@Column(name = "presentation_start_date", nullable = false)
	private LocalDateTime presentationStartDate;
	
	@Column(name = "presentation_end_date", nullable = false)
	private LocalDateTime presentationEndDate;
	
	@Column(name = "start_date", nullable = false)
	private LocalDateTime startDate;
	
	@Column(name = "end_date", nullable = false)
	private LocalDateTime endDate;
	
	@Column(name = "max_number_of_authors")
	private Integer maxNumberOfAuthors;
	
	@Column(name = "keyword_repetition", nullable = false)
	private boolean keywordRepetition;
	
	@Column(name = "min_keywords")
	private Integer minKeywords;
	
	@Column(name = "max_keywords")
	private Integer maxKeywords;
	
	@ManyToMany
	@JoinTable(
		name = "congress_participants",
		joinColumns = @JoinColumn(name = "congresses_id"),
		inverseJoinColumns = @JoinColumn(name = "users_id")
	)
	@Builder.Default
	private Set<User> participants = new HashSet<>();
}
