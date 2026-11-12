package com.tfi.gestion_congresos_backend.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;

import com.tfi.gestion_congresos_backend.entities.Evaluation;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperAuthor;
import com.tfi.gestion_congresos_backend.entities.PaperPayment;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.PaperAuthorStatus;
import com.tfi.gestion_congresos_backend.enums.RoleName;
import com.tfi.gestion_congresos_backend.events.EmailNotificationEvent;
import com.tfi.gestion_congresos_backend.services.EmailService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String TOKEN_EXPIRATION_MESSAGE = "Este enlace caducará en 30 minutos.";

    private final ApplicationEventPublisher eventPublisher;
    private final EmailTemplateRenderer emailTemplateRenderer;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;
    
    private static final String LOGIN_PATH  = "/auth/login";
    
    private static final DateTimeFormatter EMAIL_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "d 'de' MMMM 'de' yyyy",
                    Locale.forLanguageTag("es-AR")
            );
    
    /// ENVÍO DE NOTIFICACIONES
    @Override
		public void sendPasswordResetEmail(User user, String token) {
		
				String resetLink = buildLink("/auth/reset-password", token);
		
				String html = emailTemplateRenderer.render(
								"email_templates/auth/c4t-password-recovery.html",
								Map.of(
									"nombre_usuario", user.getFirstName(),
									"email_usuario", user.getEmail(),
									"tiempo_expiracion", "30 minutos",
									"url_recuperacion", resetLink
								)
				);
		
				sendHtmlEmail(
					user.getEmail(),
					"C4T · Recuperación de contraseña",
					html
				);
		}

    @Override
    public void sendCurrentEmailChangeVerificationEmail(User user, String token) {
        String verificationLink = buildLink("/confirm-email-change", token);
        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-email-confirm-current.html",
                Map.of("nombre", user.getFirstName(), "url", verificationLink,
                        "mensaje", "Confirmá el acceso a tu correo actual para continuar con el cambio.",
                        "aviso", TOKEN_EXPIRATION_MESSAGE)
        );
        sendHtmlEmail(user.getEmail(), "C4T · Confirmación de cambio de email", html);
    }

    @Override
    public void sendNewEmailChangeVerificationEmail(String newEmail, User user, String token) {

        String verificationLink = buildLink("/confirm-new-email", token);

        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-email-confirm-new.html",
                Map.of("nombre", user.getFirstName(), "url", verificationLink,
                        "mensaje", "Confirmá esta nueva dirección para completar el cambio.",
                        "aviso", TOKEN_EXPIRATION_MESSAGE)
        );
        sendHtmlEmail(newEmail, "C4T · Confirmación de nuevo email", html);
    }

    @Override
    public void sendTemporaryPasswordEmail(User user, String temporaryPassword, Congress congress, RoleName role) {
        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-email-temporary-password.html",
                Map.of("nombre", user.getFirstName(), "email", user.getEmail(),
                        "password", temporaryPassword, "congreso", congress.getName(),
                        "rol", formatRoleName(role), "url", frontendUrl + LOGIN_PATH)
        );
        sendHtmlEmail(user.getEmail(), "C4T · Credenciales de acceso", html);
    }

    @Override
    public void sendPasswordChangedNotification(User user) {
        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-password-changed.html",
                Map.of(
                        "nombre_destinatario", user.getFirstName(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(user.getEmail(), "C4T · Tu contrase\u00f1a fue modificada", html);
    }

    @Override
    public void sendPasswordResetConfirmationNotification(User user) {
        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-password-reset-confirmed.html",
                Map.of(
                        "nombre_destinatario", user.getFirstName(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(user.getEmail(), "C4T · Tu contrase\u00f1a fue restablecida", html);
    }

    @Override
    public void sendEmailChangeCompletedNotification(String previousEmail, User user) {
        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-email-change-completed.html",
                Map.of(
                        "nombre_destinatario", user.getFirstName(),
                        "nuevo_email", user.getEmail(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(previousEmail, "C4T · Tu direcci\u00f3n de email fue modificada", html);
    }

    @Override
    public void sendUserAccountDisabledNotification(User user) {
        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-account-disabled.html",
                Map.of("nombre_destinatario", user.getFirstName())
        );

        sendHtmlEmail(user.getEmail(), "C4T · Tu cuenta fue deshabilitada", html);
    }

    @Override
    public void sendUserAccountEnabledNotification(User user) {
        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-account-enabled.html",
                Map.of(
                        "nombre_destinatario", user.getFirstName(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(user.getEmail(), "C4T · Tu cuenta fue habilitada nuevamente", html);
    }

    @Override
    public void sendUserRegistrationNotification(User user) {
        String html = emailTemplateRenderer.render(
                "email_templates/auth/c4t-user-registration.html",
                Map.of(
                        "nombre_destinatario", user.getFirstName(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(user.getEmail(), "C4T · Bienvenido/a a Call for Talks", html);
    }

    @Override
    public void sendCongressImportantDatesUpdatedNotification(User recipient, Congress congress) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-participante-05-fechas-actualizadas.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "nombre_congreso", congress.getName(),
                        "lugar_congreso", congress.getPlace(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(recipient.getEmail(), "C4T · Se actualizaron fechas importantes del congreso", html);
    }

    @Override
    public void sendCongressStatusChangedNotification(User recipient, Congress congress, boolean enabled) {
        String templatePath = enabled
                ? "email_templates/participant/c4t-email-participante-06-congreso-reactivado.html"
                : "email_templates/participant/c4t-email-participante-06-congreso-deshabilitado.html";
        String subject = enabled
                ? "C4T · El congreso fue reactivado"
                : "C4T · El congreso fue deshabilitado";

        String html = emailTemplateRenderer.render(
                templatePath,
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "nombre_congreso", congress.getName(),
                        "lugar_congreso", congress.getPlace(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(recipient.getEmail(), subject, html);
    }

    @Override
    public void sendCongressBankDetailsUpdatedNotification(User recipient, Congress congress) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-19-datos-bancarios-actualizados.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "nombre_congreso", congress.getName(),
                        "lugar_congreso", congress.getPlace(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(recipient.getEmail(), "C4T · Se actualizaron los datos bancarios del congreso", html);
    }

    @Override
    public void sendNewAuthorJoinRequestNotification(User mainAuthor, Paper paper, User applicant) {
        String loginUrl = frontendUrl + LOGIN_PATH;
        String applicantFullName = applicant.getFirstName() + " " + applicant.getLastName();

        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-10-solicitud-coautoria.html",
                Map.of(
                        "nombre_destinatario", mainAuthor.getFirstName(),
                        "nombre_solicitante", applicantFullName,
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "url_login", loginUrl
                )
        );

        sendHtmlEmail(
                mainAuthor.getEmail(),
                "C4T · Nueva solicitud de coautoría",
                html
        );
    }

    @Override
    public void sendAuthorJoinRequestAcceptedNotification(
            User recipient,
            Paper paper,
            boolean promotedToExpositor
    ) {
        String roleInformation = promotedToExpositor
                ? "Además, tu rol de participación en el congreso fue actualizado de Oyente a Expositor."
                : "Tu rol de participación en el congreso se mantiene sin cambios.";

        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-11-solicitud-coautoria-aceptada.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "informacion_rol", roleInformation,
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(
                recipient.getEmail(),
                "C4T · Tu solicitud de coautoría fue aceptada",
                html
        );
    }

    @Override
    public void sendAuthorJoinRequestRejectedNotification(User recipient, Paper paper) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-12-solicitud-coautoria-rechazada.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(
                recipient.getEmail(),
                "C4T · Tu solicitud de coautoría no fue aceptada",
                html
        );
    }

    @Override
    public void sendAuthorAddedByAdministratorNotification(
            User recipient,
            Paper paper,
            boolean promotedToExpositor
    ) {
        String roleInformation = promotedToExpositor
                ? "Además, tu rol de participación en el congreso fue actualizado de Oyente a Expositor."
                : "Tu rol de participación en el congreso se mantiene sin cambios.";

        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-13-agregado-por-administrador.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "informacion_rol", roleInformation,
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(
                recipient.getEmail(),
                "C4T · Fuiste incorporado/a como autor/a de un trabajo",
                html
        );
    }

    @Override
    public void sendAuthorAddedByAdministratorToMainAuthorNotification(
            User mainAuthor,
            Paper paper,
            User addedAuthor
    ) {
        String addedAuthorFullName = addedAuthor.getFirstName() + " " + addedAuthor.getLastName();

        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-14-autor-agregado.html",
                Map.of(
                        "nombre_destinatario", mainAuthor.getFirstName(),
                        "nombre_autor_agregado", addedAuthorFullName,
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(
                mainAuthor.getEmail(),
                "C4T · Se incorporó un nuevo autor a tu trabajo",
                html
        );
    }

    @Override
    public void sendAuthorRemovedFromPaperNotification(User recipient, Paper paper) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-15-autor-eliminado.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(
                recipient.getEmail(),
                "C4T · Ya no formás parte del equipo autor de un trabajo",
                html
        );
    }

    @Override
    public void sendMainAuthorTransferredNotification(
            User recipient,
            Paper paper,
            boolean isNewMainAuthor
    ) {
        String heading = isNewMainAuthor
                ? "Ahora sos el/la autor/a principal del trabajo"
                : "La autoría principal del trabajo fue transferida";
        String roleInformation = isNewMainAuthor
                ? "A partir de ahora sos responsable de gestionar el trabajo y las solicitudes de coautoría."
                : "Seguís formando parte del equipo autor, pero ya no sos responsable de gestionar el trabajo.";

        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-16-autoria-principal-transferida.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "titulo_notificacion", heading,
                        "informacion_rol", roleInformation,
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(
                recipient.getEmail(),
                isNewMainAuthor
                        ? "C4T · Ahora sos responsable de un trabajo"
                        : "C4T · Transferencia de autoría principal",
                html
        );
    }

    @Override
    public void sendPaperPresenterChangedNotification(
            User recipient,
            Paper paper,
            boolean isNewPresenter
    ) {
        String heading = isNewPresenter
                ? "Ahora figurás como presentador/a del trabajo"
                : "Tu condición de presentador/a fue actualizada";
        String presenterInformation = isNewPresenter
                ? "Ahora figurás entre las personas designadas para presentar este trabajo en el congreso."
                : "Ya no figurás entre las personas designadas para presentar este trabajo.";

        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-17-cambio-presentador.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "titulo_notificacion", heading,
                        "informacion_presentador", presenterInformation,
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(
                recipient.getEmail(),
                isNewPresenter
                        ? "C4T · Fuiste designado/a como presentador/a"
                        : "C4T · Cambio en tu condición de presentador/a",
                html
        );
    }

    @Override
    public void sendDiscardedAuthorJoinRequestNotification(User recipient, Paper paper) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-presentador-18-solicitud-descartada-envio.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "codigo_trabajo", paper.getCode(),
                        "version_trabajo", paper.getVersion(),
                        "titulo_trabajo", paper.getTitle(),
                        "nombre_congreso", paper.getCongress().getName(),
                        "lugar_congreso", paper.getCongress().getPlace(),
                        "area_tematica", paper.getThematicArea(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(
                recipient.getEmail(),
                "C4T · Tu solicitud de coautoría fue descartada",
                html
        );
    }

    @Override
    public void sendCongressRegistrationNotification(User recipient, Congress congress, RoleName role) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-participante-01-inscripcion-confirmada.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "nombre_congreso", congress.getName(),
                        "lugar_congreso", congress.getPlace(),
                        "rol_participacion", formatRoleName(role),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(recipient.getEmail(), "C4T · Inscripción confirmada", html);
    }

    @Override
    public void sendCongressParticipationCancelledNotification(User recipient, Congress congress) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-participante-02-participacion-dada-de-baja.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "nombre_congreso", congress.getName(),
                        "lugar_congreso", congress.getPlace(),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(recipient.getEmail(), "C4T · Tu participación fue dada de baja", html);
    }

    @Override
    public void sendCongressParticipationReactivatedNotification(User recipient, Congress congress, RoleName role) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-participante-03-participacion-reactivada.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "nombre_congreso", congress.getName(),
                        "lugar_congreso", congress.getPlace(),
                        "rol_participacion", formatRoleName(role),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );

        sendHtmlEmail(recipient.getEmail(), "C4T · Tu participación fue reactivada", html);
    }

    @Override
    public void sendCongressParticipantRoleChangedNotification(User recipient, Congress congress, RoleName previousRole, RoleName newRole) {
        String html = emailTemplateRenderer.render(
                "email_templates/participant/c4t-email-participante-04-rol-actualizado.html",
                Map.of(
                        "nombre_destinatario", recipient.getFirstName(),
                        "nombre_congreso", congress.getName(),
                        "lugar_congreso", congress.getPlace(),
                        "rol_anterior", formatRoleName(previousRole),
                        "rol_nuevo", formatRoleName(newRole),
                        "url_login", frontendUrl + LOGIN_PATH
                )
        );
        sendHtmlEmail(recipient.getEmail(), "C4T · Tu rol en el congreso fue actualizado", html);
    }
    
	@Override
	public void sendNewPaperPendingAssignmentNotification(User administrator, Paper paper, LocalDateTime submissionDate) {
	
	    // Obtener el autor responsable del trabajo.
	    PaperAuthor mainAuthor = paper.getAuthors().stream()
	            .filter(PaperAuthor::isMainAuthor)
	            .filter(author -> author.getStatus() == PaperAuthorStatus.ACCEPTED)
	            .findFirst()
	            .orElseThrow(() -> new IllegalStateException(
	                    "El trabajo no tiene un autor responsable aceptado."
	            ));
	
	    String mainAuthorFullName =
	            mainAuthor.getAuthor().getFirstName() + " "
	            + mainAuthor.getAuthor().getLastName();
	    
	    String loginUrl = frontendUrl + LOGIN_PATH;
	
	    String html = emailTemplateRenderer.render(
	            "email_templates/admin/c4t-email-admin-01-nuevo-trabajo.html",
	            Map.of(
	                    "nombre_destinatario", administrator.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "nombre_autor_responsable", mainAuthorFullName,
	                    "fecha_envio", submissionDate.format(EMAIL_DATE_FORMATTER),
	                    "url_login", loginUrl
	            )
	    );
	
	    sendHtmlEmail(
	            administrator.getEmail(),
	            "C4T · Nuevo trabajo recibido para asignar",
	            html
	    );
	}
	
	@Override
	public void sendPaymentReceiptReceivedNotification(User administrator, PaperPayment payment) {

	    String html = renderPaymentAdministratorNotification(
	            "email_templates/admin/c4t-email-admin-02-comprobante-recibido.html",
	            administrator,
	            payment
	    );

	    sendHtmlEmail(
	            administrator.getEmail(),
	            "C4T · Nuevo comprobante de pago recibido",
	            html
	    );
	}
	
	@Override
	public void sendPaymentReceiptReplacedAfterRejectionNotification(User administrator, PaperPayment payment) {

	    String html = renderPaymentAdministratorNotification(
	            "email_templates/admin/c4t-email-admin-03-comprobante-reemplazado.html",
	            administrator,
	            payment
	    );

	    sendHtmlEmail(
	            administrator.getEmail(),
	            "C4T · Comprobante rechazado reemplazado",
	            html
	    );
	}
	
	@Override
	public void sendPaperAssignedToReviewerNotification(User reviewer, Paper paper, LocalDateTime assignmentDate) {
	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String html = emailTemplateRenderer.render(
	            "email_templates/evaluator/c4t-email-evaluador-01-trabajo-asignado.html",
	            Map.of(
	                    "nombre_destinatario", reviewer.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "fecha_asignacion", assignmentDate.format(EMAIL_DATE_FORMATTER),
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            reviewer.getEmail(),
	            "C4T · Nuevo trabajo asignado para evaluar",
	            html
	    );
	}
	
	@Override
	public void sendCorrectedPaperVersionToReviewerNotification(User reviewer, Paper paper) {
	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String html = emailTemplateRenderer.render(
	            "email_templates/evaluator/c4t-email-evaluador-02-version-corregida.html",
	            Map.of(
	                    "nombre_destinatario", reviewer.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            reviewer.getEmail(),
	            "C4T · Nueva versión de un trabajo para revisar",
	            html
	    );
	}
	
	@Override
	public void sendPaperReviewerChangedNotification(User previousReviewer, Paper paper, LocalDateTime reassignmentDate) {
	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String html = emailTemplateRenderer.render(
	            "email_templates/evaluator/c4t-email-evaluador-03-asignacion-cambiada.html",
	            Map.of(
	                    "nombre_destinatario", previousReviewer.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "fecha_reasignacion", reassignmentDate.format(EMAIL_DATE_FORMATTER),
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            previousReviewer.getEmail(),
	            "C4T · Trabajo reasignado a otro evaluador",
	            html
	    );
	}

	@Override
	public void sendPaperSubmissionConfirmationNotification(User recipient, Paper paper, LocalDateTime submissionDate) {
	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-01-trabajo-enviado.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "fecha_envio", submissionDate.format(EMAIL_DATE_FORMATTER),
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Confirmación de envío del trabajo",
	            html
	    );
	}
	
	@Override
	public void sendPaperReviewerAssignedToAuthorNotification(User recipient, Paper paper, User reviewer) {
	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String evaluatorInformation;

	    if (paper.getCongress().isSingleBlind()) {
	        String reviewerFullName =
	                reviewer.getFirstName() + " " + reviewer.getLastName();

	        evaluatorInformation =
	                "El evaluador asignado a tu trabajo es " + reviewerFullName + ".";
	    } else {
	        evaluatorInformation =
	                "La identidad del evaluador se mantiene confidencial "
	                + "debido a la modalidad de revisión doble ciego del congreso.";
	    }

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-02-evaluador-asignado.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "informacion_evaluador", evaluatorInformation,
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Evaluador asignado a tu trabajo",
	            html
	    );
	}
	
	@Override
	public void sendPaperCorrectionSubmissionNotification(User recipient, Paper paper, LocalDateTime submissionDate) {
	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-03-correccion-enviada.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "fecha_envio", submissionDate.format(EMAIL_DATE_FORMATTER),
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Confirmación de envío de versión corregida",
	            html
	    );
	}
	
	@Override
	public void sendPaperEvaluationWithObservationsNotification(User recipient, Paper paper, Evaluation evaluation, User evaluator) {
	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String observations = evaluation.getFeedback();

	    if (observations == null || observations.isBlank()) {
	        throw new IllegalStateException(
	                "La evaluación no contiene observaciones para notificar."
	        );
	    }

	    if (evaluator == null) {
	        throw new IllegalArgumentException(
	                "No se puede notificar una evaluación sin identificar al evaluador."
	        );
	    }

	    String evaluatorInformation = buildEvaluatorInformation(paper, evaluator);

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-04-con-observaciones.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", evaluation.getEvaluatedVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "observaciones", observations,
	                    "informacion_evaluador", evaluatorInformation,
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Tu trabajo tiene observaciones",
	            html
	    );
	}
	
	@Override
	public void sendPaperApprovalNotification(User recipient, Paper paper, Evaluation evaluation, User evaluator) {

	    String loginUrl = frontendUrl + LOGIN_PATH;
	    
	    if (evaluator == null) {
	        throw new IllegalArgumentException(
	                "No se puede notificar una evaluación sin identificar al evaluador."
	        );
	    }

	    String evaluatorInformation = buildEvaluatorInformation(paper, evaluator);

	    String nextStepInformation;

	    if (paper.getCongress().isFree()) {
	        nextStepInformation =
	                "Al tratarse de un congreso gratuito, no necesitás realizar ningún pago "
	                + "ni completar gestiones adicionales relacionadas con el pago de este trabajo.";
	    } else {
	        nextStepInformation =
	                "Ya podés continuar con la gestión del pago correspondiente. "
	                + "La incorporación del trabajo al circuito del congreso quedará "
	                + "sujeta a la verificación administrativa del pago.";
	    }

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-05-trabajo-aprobado.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", evaluation.getEvaluatedVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "informacion_evaluador", evaluatorInformation,
	                    "informacion_proxima_etapa", nextStepInformation,
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Tu trabajo fue aprobado",
	            html
	    );
	}
	
	@Override
	public void sendPaperRejectionNotification(User recipient, Paper paper, Evaluation evaluation, User evaluator) {
	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String observations = evaluation.getFeedback();

	    if (observations == null || observations.isBlank()) {
	        throw new IllegalStateException(
	                "La evaluación no contiene fundamentos para notificar."
	        );
	    }

	    String evaluatorInformation = buildEvaluatorInformation(paper, evaluator);

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-06-trabajo-desaprobado.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", evaluation.getEvaluatedVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "observaciones", observations,
	                    "informacion_evaluador", evaluatorInformation,
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Resultado de evaluación de tu trabajo",
	            html
	    );
	}
	
	@Override
	public void sendPaymentReceiptReceivedToAuthorNotification(User recipient, PaperPayment payment) {
	    Paper paper = payment.getPaper();

	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-07-comprobante-recibido.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "fecha_carga", payment.getUploadDate().format(EMAIL_DATE_FORMATTER),
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Comprobante de pago recibido",
	            html
	    );
	}
	
	@Override
	public void sendPaymentVerifiedToAuthorNotification(User recipient, PaperPayment payment, LocalDateTime verificationDate) {
	    Paper paper = payment.getPaper();

	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-08-pago-verificado.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "fecha_verificacion", verificationDate.format(EMAIL_DATE_FORMATTER),
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Tu pago fue verificado",
	            html
	    );
	}
	
	@Override
	public void sendPaymentReceiptRejectedToAuthorNotification(User recipient, PaperPayment payment) {
	    Paper paper = payment.getPaper();

	    String observations = payment.getObservations();

	    if (observations == null || observations.isBlank()) {
	        throw new IllegalStateException(
	                "El comprobante rechazado no contiene observaciones para notificar."
	        );
	    }

	    String loginUrl = frontendUrl + LOGIN_PATH;

	    String html = emailTemplateRenderer.render(
	            "email_templates/participant/c4t-email-presentador-09-comprobante-rechazado.html",
	            Map.of(
	                    "nombre_destinatario", recipient.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "observaciones", observations,
	                    "url_login", loginUrl
	            )
	    );

	    sendHtmlEmail(
	            recipient.getEmail(),
	            "C4T · Comprobante de pago rechazado",
	            html
	    );
	}
	
	/// MÉTODOS PRIVADOS
    private String buildLink(String path, String token) {
        return frontendUrl + path + "?token=" + token;
    }

    private void sendHtmlEmail(String to, String subject, String html) {

        eventPublisher.publishEvent(
                new EmailNotificationEvent(
                        to,
                        subject,
                        html,
                        EmailNotificationEvent.ContentType.HTML
                )
        );
    }
    

	private String renderPaymentAdministratorNotification(String templatePath, User administrator, PaperPayment payment) {
	
	    Paper paper = payment.getPaper();
	
	    // Obtener el autor responsable aceptado.
	    PaperAuthor mainAuthor = paper.getAuthors().stream()
	            .filter(PaperAuthor::isMainAuthor)
	            .filter(author -> author.getStatus() == PaperAuthorStatus.ACCEPTED)
	            .findFirst()
	            .orElseThrow(() -> new IllegalStateException(
	                    "El trabajo no tiene un autor responsable aceptado."
	            ));
	
	    String mainAuthorFullName =
	            mainAuthor.getAuthor().getFirstName() + " "
	            + mainAuthor.getAuthor().getLastName();
	
	    String loginUrl = frontendUrl + LOGIN_PATH;
	
	    return emailTemplateRenderer.render(
	            templatePath,
	            Map.of(
	                    "nombre_destinatario", administrator.getFirstName(),
	                    "codigo_trabajo", paper.getCode(),
	                    "version_trabajo", paper.getVersion(),
	                    "titulo_trabajo", paper.getTitle(),
	                    "nombre_congreso", paper.getCongress().getName(),
	                    "lugar_congreso", paper.getCongress().getPlace(),
	                    "area_tematica", paper.getThematicArea(),
	                    "nombre_autor_responsable", mainAuthorFullName,
	                    "fecha_carga", payment.getUploadDate().format(EMAIL_DATE_FORMATTER),
	                    "url_login", loginUrl
	            )
	    );
	}	
	
	private String buildEvaluatorInformation(Paper paper, User evaluator) {

	    if (evaluator == null) {
	        throw new IllegalArgumentException(
	                "No se puede notificar una evaluación sin identificar al evaluador."
	        );
	    }

	    if (paper.getCongress().isSingleBlind()) {
	        return evaluator.getFirstName() + " " + evaluator.getLastName();
	    }

	    return "Por la modalidad de revisión doble ciego, "
	            + "la identidad del evaluador se mantiene confidencial.";
	}

    private String formatRoleName(RoleName role) {
        return switch (role) {
            case UNKNOWN -> "Sin rol asignado";
            case LISTENER -> "Oyente";
            case EXPOSITOR -> "Expositor/a";
            case EVALUATOR -> "Evaluador/a";
            case ADMINISTRATOR -> "Administrador/a";
        };
    }

}
