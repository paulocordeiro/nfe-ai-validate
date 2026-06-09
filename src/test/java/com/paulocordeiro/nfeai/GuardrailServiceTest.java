package com.paulocordeiro.nfeai;

import com.paulocordeiro.nfeai.domain.AnaliseFiscal;
import com.paulocordeiro.nfeai.domain.NotaFiscal;
import com.paulocordeiro.nfeai.domain.Severidade;
import com.paulocordeiro.nfeai.service.GuardrailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes dos guardrails determinísticos.
 * <p>
 * De propósito NÃO chamam o LLM: rodam offline, sem API key. Demonstram que as
 * regras de negócio críticas não dependem da IA e que a decisão final é do código.
 */
class GuardrailServiceTest {

    private final GuardrailService guardrails = new GuardrailService();

    @Test
    @DisplayName("Pré-validação detecta divergência entre soma dos itens e total declarado")
    void detectaDivergenciaDeTotal() {
        var item = new NotaFiscal.ItemNota("PX-1", "Produto X", "94036000", "5102", "UN", BigDecimal.ONE, new BigDecimal("100.00"), new BigDecimal("100.00"),
                new NotaFiscal.ItemNota.Icms("00", new BigDecimal("100.00"), new BigDecimal("12.0"), new BigDecimal("12.00")));

        var nota = new NotaFiscal("001", "1", java.time.LocalDate.of(2025, 5, 20), "Venda", new NotaFiscal.Emitente("11222333000181", "Emit", "123456789", "BA", "Regime Normal"), new NotaFiscal.Destinatario("99888777000166", "Dest", "987654321", "BA", "CONTRIBUINTE"),
                List.of(item), new NotaFiscal.Totais(new BigDecimal("999.00"), new BigDecimal("12.00"), new BigDecimal("999.00")));

        var achados = guardrails.preValidar(nota);

        assertTrue(achados.stream()
                .anyMatch(i -> i.campo().equals("totais.valorProdutos") && Severidade.ALTA == i.severidade()));
    }

    @Test
    @DisplayName("Pós-validação reprova a nota mesmo se o LLM tiver aprovado, havendo severidade ALTA")
    void regraSobrepoeVereditoDaIa() {
        // LLM otimista: aprovou e não viu problema nenhum
        var analiseLlm = new AnaliseFiscal(true, "Tudo certo", List.of());

        // Mas o guardrail determinístico achou um problema ALTA
        var achadoDeterministico = new AnaliseFiscal.Inconsistencia(
                "item[0].ncm", Severidade.ALTA, "NCM inválido", "Corrigir NCM");

        var resultado = guardrails.posValidar(analiseLlm, List.of(achadoDeterministico));

        assertFalse(resultado.aprovada(), "Código deve sobrepor o 'aprovada=true' da IA");
        assertEquals(1, resultado.inconsistencias().size());
    }

    @Test
    @DisplayName("Nota limpa sem achados é aprovada")
    void notaLimpaAprovada() {
        var analiseLlm = new AnaliseFiscal(true, "Sem inconsistências", List.of());
        var resultado = guardrails.posValidar(analiseLlm, List.of());

        assertTrue(resultado.aprovada());
        assertTrue(resultado.inconsistencias().isEmpty());
    }
}
