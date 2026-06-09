package com.paulocordeiro.nfeai.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Tool exposta ao LLM via function calling.
 * O ponto importante: o modelo NÃO "sabe" a tabela NCM da sua empresa. Quando ele
 * precisa validar um NCM, ele PEDE para a aplicação consultar — e a aplicação responde
 * com o dado real. É assim que a IA fica ancorada nos dados do sistema, em vez de
 * alucinar uma resposta.
 * Num ERP real, este método consultaria o banco / cadastro fiscal em vez de um Map.
 */
@Service
public class TabelaFiscalTool {

    // Mock de uma base de NCM. Num sistema real: repositório JPA / tabela fiscal.
    private static final Map<String, NcmInfo> NCM_VALIDOS = Map.of(
            "84713012", new NcmInfo("84713012", "Máquinas automáticas para processamento de dados, portáteis", "17.5"),
            "85171231", new NcmInfo("85171231", "Telefones para redes celulares", "17.5"),
            "22021000", new NcmInfo("22021000", "Águas, incluídas as águas minerais, com adição de açúcar", "17.0"),
            "94036000", new NcmInfo("94036000", "Móveis de madeira", "12.0")
    );

    @Tool(description = "Consulta um código NCM na tabela fiscal da empresa. "
            + "Retorna a descrição oficial e a alíquota de ICMS de referência, "
            + "ou indica que o NCM não foi encontrado.")
    public NcmConsultaResult consultarNcm(
            @ToolParam(description = "Código NCM de 8 dígitos a ser consultado") String ncm) {

        return Optional.ofNullable(NCM_VALIDOS.get(ncm))
                .map(info -> new NcmConsultaResult(true, info.descricao(), info.aliquotaIcmsReferencia()))
                .orElse(new NcmConsultaResult(false, "NCM não encontrado na tabela fiscal", null));
    }

    @JsonClassDescription("Resultado da consulta de um NCM na tabela fiscal")
    public record NcmConsultaResult(boolean encontrado, String descricao, String aliquotaIcmsReferencia) {}

    private record NcmInfo(String codigo, String descricao, String aliquotaIcmsReferencia) {}
}
