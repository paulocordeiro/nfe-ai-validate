package com.paulocordeiro.nfeai.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Saída ESTRUTURADA da análise da IA.
 * Esse é o ponto central do exemplo: o LLM não devolve texto livre, devolve um objeto
 * tipado. O Spring AI gera o JSON Schema a partir deste record e mapeia a resposta
 * de volta para Java via entity(AnaliseFiscal.class).
 * As anotações @JsonPropertyDescription viram parte do schema e orientam o modelo
 * sobre o que preencher em cada campo.
 */
@JsonClassDescription("Resultado da análise fiscal de uma NF-e, com inconsistências encontradas")
public record AnaliseFiscal(

        @JsonPropertyDescription("true se a nota está apta a ser transmitida sem ressalvas")
        boolean aprovada,

        @JsonPropertyDescription("Resumo objetivo em uma frase do estado fiscal da nota")
        String resumo,

        @JsonPropertyDescription("Lista de inconsistências fiscais encontradas. Vazia se não houver.")
        List<Inconsistencia> inconsistencias
) {
    @JsonClassDescription("Uma inconsistência fiscal específica identificada na nota")
    public record Inconsistencia(

            @JsonPropertyDescription("Campo ou item afetado, ex: 'item[0].ncm' ou 'cfop'")
            String campo,

            @JsonPropertyDescription("Severidade: ALTA, MEDIA ou BAIXA")
            Severidade severidade,

            @JsonPropertyDescription("Descrição clara do problema em português")
            String descricao,

            @JsonPropertyDescription("Correção sugerida, quando aplicável")
            String sugestao
    ) {}
}
