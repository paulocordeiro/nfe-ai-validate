package com.paulocordeiro.nfeai.api;

import com.paulocordeiro.nfeai.domain.AnaliseFiscal;
import com.paulocordeiro.nfeai.domain.NotaFiscal;
import com.paulocordeiro.nfeai.service.ValidacaoNfeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nfe")
public class ValidacaoNfeController {

    private final ValidacaoNfeService service;

    public ValidacaoNfeController(ValidacaoNfeService service) {
        this.service = service;
    }

    @PostMapping("/validar")
    public ResponseEntity<AnaliseFiscal> validar(@RequestBody NotaFiscal nota) {
        return ResponseEntity.ok(service.validar(nota));
    }
}
