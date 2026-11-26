package com.tfi.gestion_congresos_backend.repository.projection;

public interface ParticipantHomePaymentMetricsProjection {

    long getPaymentsWithoutValidReceipt();

    long getPaymentsPendingApproval();

    long getPaymentsVerified();
}
