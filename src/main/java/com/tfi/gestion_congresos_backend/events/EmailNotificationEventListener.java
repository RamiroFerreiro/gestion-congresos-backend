package com.tfi.gestion_congresos_backend.events;

import com.tfi.gestion_congresos_backend.services.impl.AsyncEmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailNotificationEventListener {

    private final AsyncEmailSender asyncEmailSender;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT, // Le indica a Spring que procese el evento después de que la transacción haya confirmado sus cambios
            fallbackExecution = false // Todos los flujos deben ser transaccionales para publicar notificaciones
            
    )
    public void handleEmailNotification(EmailNotificationEvent event) {

        if (event.contentType() == EmailNotificationEvent.ContentType.HTML) {

            asyncEmailSender.sendHtmlEmail(
                    event.recipient(),
                    event.subject(),
                    event.content()
            );

        } else {

            asyncEmailSender.sendTextEmail(
                    event.recipient(),
                    event.subject(),
                    event.content()
            );
        }
    }
}
