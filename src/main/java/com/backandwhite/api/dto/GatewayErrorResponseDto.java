package com.backandwhite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * DTO de respuesta estándar para errores del API Gateway.
 *
 * <p>
 * Proporciona una estructura uniforme para todos los errores que
 * genera el gateway, tanto a nivel de controlador como de enrutamiento.
 *
 * @author NX036-ALFA
 */
@Data
@With
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayErrorResponseDto {

    private String code;
    private String message;
    private List<String> details;
    private ZonedDateTime dateTime;
}
