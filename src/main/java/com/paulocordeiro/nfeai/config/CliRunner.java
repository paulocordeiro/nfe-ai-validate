package com.paulocordeiro.nfeai.config;

import com.paulocordeiro.nfeai.domain.AnaliseFiscal;
import com.paulocordeiro.nfeai.domain.NotaFiscal;
import com.paulocordeiro.nfeai.service.ValidacaoNfeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Roda a validação via linha de comando (perfil "cli"):
 * ./gradlew bootRun --args='--spring.profiles.active=cli'
 * <p>
 * Demonstra a nota de exemplo com um NCM inexistente para forçar o LLM a usar a tool.
 */
@Configuration
@Profile("cli")
public class CliRunner {

    private static final Logger log = LoggerFactory.getLogger(CliRunner.class);

    @Bean
    CommandLineRunner runValidacao(ValidacaoNfeService service) {
        return args -> {
            NotaFiscal nota = NotaExemplo.comNcmInvalido();
            log.info("Validando NF-e {} ...", nota.numero());

            AnaliseFiscal resultado = service.validar(nota);

            log.info("=== RESULTADO ===");
            log.info("Aprovada: {}", resultado.aprovada());
            log.info("Resumo: {}", resultado.resumo());
            resultado.inconsistencias().forEach(inc ->
                    log.info("  [{}] {} -> {} (sugestão: {})", inc.severidade(), inc.campo(), inc.descricao(), inc.sugestao()));
        };
    }

    /**
     * Dados de exemplo reutilizados pelo CLI e pelos testes.
     */
    public static final class NotaExemplo {
        public static NotaFiscal comNcmInvalido() {
            var item = new NotaFiscal.ItemNota("NB-14", "Notebook 14 polegadas", "99999999", "5102", "UN", BigDecimal.ONE, new BigDecimal("3500.00"), new BigDecimal("3500.00"),
                    new NotaFiscal.ItemNota.Icms("00", new BigDecimal("3500.00"), new BigDecimal("17.5"), new BigDecimal("612.50")));

            return new NotaFiscal("000123", "1", LocalDate.of(2025, 5, 20), "Venda de mercadoria",
                    new NotaFiscal.Emitente("11222333000181", "Loja Exemplo LTDA", "123456789", "BA", "Regime Normal"),
                    new NotaFiscal.Destinatario("99888777000166", "Cliente Final SA", "987654321", "BA", "CONTRIBUINTE"), List.of(item),
                    new NotaFiscal.Totais(new BigDecimal("3500.00"), new BigDecimal("612.50"), new BigDecimal("3500.00")));
        }
    }
}
