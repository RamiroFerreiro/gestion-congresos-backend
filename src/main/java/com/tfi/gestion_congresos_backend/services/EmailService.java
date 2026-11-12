package com.tfi.gestion_congresos_backend.services;

import java.time.LocalDateTime;

import com.tfi.gestion_congresos_backend.entities.Evaluation;
import com.tfi.gestion_congresos_backend.entities.Congress;
import com.tfi.gestion_congresos_backend.entities.Paper;
import com.tfi.gestion_congresos_backend.entities.PaperPayment;
import com.tfi.gestion_congresos_backend.entities.User;
import com.tfi.gestion_congresos_backend.enums.RoleName;

public interface EmailService {

    void sendPasswordResetEmail(User user, String token);

    void sendCurrentEmailChangeVerificationEmail(User user, String token);
    
    void sendNewEmailChangeVerificationEmail(String newEmail, User user, String token);

    void sendTemporaryPasswordEmail(User user, String temporaryPassword, Congress congress, RoleName role);

    void sendPasswordChangedNotification(User user);

    void sendPasswordResetConfirmationNotification(User user);

    void sendEmailChangeCompletedNotification(String previousEmail, User user);

    void sendUserAccountDisabledNotification(User user);

    void sendUserAccountEnabledNotification(User user);

    void sendUserRegistrationNotification(User user);

    void sendCongressImportantDatesUpdatedNotification(User recipient, Congress congress);

    void sendCongressStatusChangedNotification(User recipient, Congress congress, boolean enabled);

    void sendCongressBankDetailsUpdatedNotification(User recipient, Congress congress);

    void sendNewAuthorJoinRequestNotification(User mainAuthor, Paper paper, User applicant);

    void sendAuthorJoinRequestAcceptedNotification(User recipient, Paper paper, boolean promotedToExpositor);

    void sendAuthorJoinRequestRejectedNotification(User recipient, Paper paper);

    void sendAuthorAddedByAdministratorNotification(User recipient, Paper paper, boolean promotedToExpositor);

    void sendAuthorAddedByAdministratorToMainAuthorNotification(User mainAuthor, Paper paper, User addedAuthor);

    void sendAuthorRemovedFromPaperNotification(User recipient, Paper paper);

    void sendMainAuthorTransferredNotification(User recipient, Paper paper, boolean isNewMainAuthor);

    void sendPaperPresenterChangedNotification(User recipient, Paper paper, boolean isNewPresenter);

    void sendDiscardedAuthorJoinRequestNotification(User recipient, Paper paper);

    void sendCongressRegistrationNotification(User recipient, Congress congress, RoleName role);

    void sendCongressParticipationCancelledNotification(User recipient, Congress congress);

    void sendCongressParticipationReactivatedNotification(User recipient, Congress congress, RoleName role);

    void sendCongressParticipantRoleChangedNotification(User recipient, Congress congress, RoleName previousRole, RoleName newRole);
    
    void sendNewPaperPendingAssignmentNotification(User administrator, Paper paper, LocalDateTime submissionDate);
    
    void sendPaymentReceiptReceivedNotification(User administrator, PaperPayment payment);
    
    void sendPaymentReceiptReplacedAfterRejectionNotification(User administrator, PaperPayment payment);
    
    void sendPaperAssignedToReviewerNotification(User reviewer, Paper paper, LocalDateTime assignmentDate);
    
    void sendCorrectedPaperVersionToReviewerNotification(User reviewer, Paper paper);

    void sendPaperReviewerChangedNotification(User previousReviewer, Paper paper, LocalDateTime reassignmentDate);
    
    void sendPaperSubmissionConfirmationNotification(User recipient, Paper paper, LocalDateTime submissionDate);
    
    void sendPaperReviewerAssignedToAuthorNotification(User recipient, Paper paper, User reviewer);
    
    void sendPaperCorrectionSubmissionNotification(User recipient, Paper paper, LocalDateTime submissionDate);
    
    void sendPaperEvaluationWithObservationsNotification(User recipient, Paper paper, Evaluation evaluation, User evaluator);
    
    void sendPaperApprovalNotification(User recipient, Paper paper, Evaluation evaluation, User evaluator);
    
    void sendPaperRejectionNotification(User recipient, Paper paper, Evaluation evaluation, User evaluator);
    
    void sendPaymentReceiptReceivedToAuthorNotification(User recipient, PaperPayment payment);
    
    void sendPaymentVerifiedToAuthorNotification(User recipient, PaperPayment payment, LocalDateTime verificationDate);
    
    void sendPaymentReceiptRejectedToAuthorNotification(User recipient, PaperPayment payment);
}
