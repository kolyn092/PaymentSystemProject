package com.paymentsystemproject.domain.infra.portone.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paymentsystemproject.domain.infra.portone.config.PortOneProperties;
import com.paymentsystemproject.domain.infra.portone.dto.PortOneConfigResponseDto;
import com.paymentsystemproject.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PortOneConfigController {

    private final PortOneProperties portOneProperties;

    @GetMapping("/api/config/portone")
    public ResponseEntity<ApiResponse<PortOneConfigResponseDto>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(new PortOneConfigResponseDto(
            portOneProperties.getStoreId(),
            portOneProperties.getChannelKey()
        )));
    }
}
