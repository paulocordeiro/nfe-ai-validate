package com.paulocordeiro.nfeai.service;

import com.paulocordeiro.nfeai.domain.AnaliseFiscal;
import com.paulocordeiro.nfeai.domain.NotaFiscal;
import com.paulocordeiro.nfeai.domain.Severidade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Guardrails determinísticos.
 * <p>
 * Princípio básico: a IA NÃO decide sozinha. Ela sugere; o código valida e decide.
 * <p>
 * - Pré-validação: checagens que não dependem de IA (e nem deveriam). Se a soma dos
 * itens não bate com o total declarado, isso é aritmética, não "interpretação".
 * Garante que erros óbvios sejam pegos mesmo que o LLM esteja indisponível.
 * <p>
 * - Pós-validação: nunca confiamos cegamente no que o modelo devolve. Se a IA
 * marcou a nota como aprovada, mas existe inconsistência de severidade ALTA,
 * o código sobrepõe a decisão. O LLM informa; a regra de negócio manda.
 */
@Component
public class GuardrailService {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01");

    /**
     * Validações determinísticas executadas ANTES de chamar o LLM.
     * Em uma implementação real, existe uma quantidade enorme de checagens
     */
    public List<AnaliseFiscal.Inconsistencia> preValidar(NotaFiscal nota) {
        List<AnaliseFiscal.Inconsistencia> achados = new ArrayList<>();

        if (nota.itens() == null || nota.itens().isEmpty()) {
            achados.add(new AnaliseFiscal.Inconsistencia("itens", Severidade.ALTA, "Nota sem itens.", "Incluir ao menos um item."));
            return achados;
        }

        BigDecimal somaItens = nota.itens().stream()
                .map(NotaFiscal.ItemNota::precoTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal valorProdutos = nota.totais() == null ? null : nota.totais().valorProdutos();
        if (valorProdutos != null && somaItens.subtract(valorProdutos).abs().compareTo(TOLERANCIA) > 0) {
            achados.add(new AnaliseFiscal.Inconsistencia("totais.valorProdutos", Severidade.ALTA, "Soma dos itens (%s) diverge do total de produtos declarado (%s).".formatted(somaItens, valorProdutos), "Recalcular o total da nota a partir dos itens."));
        }

        for (int i = 0; i < nota.itens().size(); i++) {
            var item = nota.itens().get(i);
            if (item.ncm() == null || item.ncm().length() != 8) {
                achados.add(new AnaliseFiscal.Inconsistencia(
                        "item[%d].ncm".formatted(i), Severidade.ALTA,
                        "NCM ausente ou com formato inválido (esperado 8 dígitos).",
                        "Informar um NCM válido de 8 dígitos."));
            }
        }
        return achados;
    }

    /**
     * Reconcilia a saída do LLM com as regras de negócio.
     * A decisão final de "aprovada" pertence ao código, não ao modelo.
     */
    public AnaliseFiscal posValidar(AnaliseFiscal analiseLlm, List<AnaliseFiscal.Inconsistencia> achadosDeterministicos) {

        List<AnaliseFiscal.Inconsistencia> consolidadas = new ArrayList<>(achadosDeterministicos);
        if (analiseLlm != null && analiseLlm.inconsistencias() != null) {
            consolidadas.addAll(analiseLlm.inconsistencias());
        }

        boolean temAlta = consolidadas.stream()
                .anyMatch(inc -> Severidade.ALTA == inc.severidade());

        // A regra sobrepõe o veredito da IA: havendo severidade ALTA, reprova.
        boolean aprovadaFinal = !temAlta;

        String resumoLlm = analiseLlm == null ? null : analiseLlm.resumo();
        String resumoFinal = aprovadaFinal
                ? "Nota apta a transmissão. " + safe(resumoLlm)
                : "Nota reprovada por inconsistências de severidade ALTA.";

        return new AnaliseFiscal(aprovadaFinal, resumoFinal.trim(), consolidadas);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
