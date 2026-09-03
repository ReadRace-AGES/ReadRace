package com.readrace.api.service;

import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Service;

/**
 * Diz se a API consegue atender de verdade.
 *
 * <p>Reaproveita o {@link HealthEndpoint} do Actuator, que já está no classpath e já sabe checar
 * banco, disco e qualquer {@code HealthIndicator} que o projeto adicionar. Escrever a checagem de
 * banco na mão aqui duplicaria essa lógica e as duas sairiam de sincronia.
 *
 * <p>Esta classe existe para isolar o controller da API do Actuator, que é instável entre versões —
 * no Boot 4 ela mudou de pacote e {@code HealthDescriptor} virou classe {@code sealed}. Se mudar de
 * novo, muda só aqui.
 *
 * <p>Reduz o resultado a um booleano de propósito: o mobile não precisa saber qual dependência
 * caiu, e expor o detalhe seria dar mapa da infraestrutura para quem não está autenticado. O
 * diagnóstico fica no {@code /actuator/health}.
 */
@Service
public class HealthService {

    private final HealthEndpoint healthEndpoint;

    public HealthService(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    /** Só {@code UP} conta como saudável: DOWN, OUT_OF_SERVICE e UNKNOWN são indisponibilidade. */
    public boolean apiEstaSaudavel() {
        return Status.UP.equals(healthEndpoint.health().getStatus());
    }
}
