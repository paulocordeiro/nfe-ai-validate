package com.paulocordeiro.nfeai.service;

import com.paulocordeiro.nfeai.domain.AnaliseFiscal;
import com.paulocordeiro.nfeai.domain.NotaFiscal;
import com.paulocordeiro.nfeai.tool.TabelaFiscalTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestrador. É o "maestro" do diagrama do post: o Spring Boot coordena o fluxo,
 * o LLM é só uma das etapas — não o centro.
 * <p>
 * Fluxo:
 * 1. Guardrail PRÉ  → checagens determinísticas (aritmética, formato).
 * 2. LLM            → análise semântica, com tool calling para consultar a tabela NCM
 * e structured output mapeando direto para AnaliseFiscal.
 * 3. Guardrail PÓS  → reconcilia: a regra de negócio decide o veredito final.
 */
@Service
public class ValidacaoNfeService {

    private static final String SYSTEM_PROMPT = """
            Você é um analista fiscal especializado em NF-e brasileira.
            Analise a nota fiscal fornecida e identifique inconsistências fiscais:
            divergências de NCM, CFOP incompatível com a operação, alíquotas de ICMS
            fora do esperado e demais problemas tributários.
            
            Sempre que precisar confirmar a descrição oficial ou a alíquota de
            referência de um NCM, use a ferramenta de consulta à tabela fiscal —
            não presuma o valor. Baseie suas conclusões nos dados retornados pela
            ferramenta, não em conhecimento geral.
            
            Seja objetivo e técnico. Responda apenas com a análise estruturada solicitada.
            """;

    private final ChatClient chatClient;
    private final TabelaFiscalTool tabelaFiscalTool;
    private final GuardrailService guardrails;

    public ValidacaoNfeService(ChatClient.Builder builder,
                               TabelaFiscalTool tabelaFiscalTool,
                               GuardrailService guardrails) {
        this.chatClient = builder.build();
        this.tabelaFiscalTool = tabelaFiscalTool;
        this.guardrails = guardrails;
    }

    public AnaliseFiscal validar(NotaFiscal nota) {
        // 1. Guardrail PRÉ
        List<AnaliseFiscal.Inconsistencia> achadosDeterministicos = guardrails.preValidar(nota);

        // 2. LLM com tool calling + structured output
        AnaliseFiscal analiseLlm = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("Analise esta NF-e:\n{nota}").param("nota", nota.toString()))
                .tools(tabelaFiscalTool)          // function calling: o modelo consulta a tabela NCM
                .call()
                .entity(AnaliseFiscal.class);     // structured output: JSON Schema -> record Java

        // 3. Guardrail PÓS — a decisão final é do código
        return guardrails.posValidar(analiseLlm, achadosDeterministicos);
    }
}
