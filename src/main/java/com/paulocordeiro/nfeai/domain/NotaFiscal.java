package com.paulocordeiro.nfeai.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Representação simplificada de uma NF-e que entra para validação.
 * Num ERP real isso viria do módulo fiscal já desserializado do XML; aqui mantemos só o
 * núcleo fiscal (sem IPI/PIS/COFINS, IBS/CBS, ST, transporte, pagamento) para o exemplo ficar legível.
 */
public record NotaFiscal(
        String numero,
        String serie,
        LocalDate dataEmissao,
        String naturezaOperacao,
        Emitente emitente,
        Destinatario destinatario,
        List<ItemNota> itens,
        Totais totais
) {
    public record Emitente(
            String cnpj,
            String razaoSocial,
            String inscricaoEstadual,
            String uf,
            String regimeTributario
    ) {}

    /** indicadorInscricaoEstadual: CONTRIBUINTE, ISENTO ou NAO_CONTRIBUINTE. */
    public record Destinatario(
            String cnpjCpf,
            String nome,
            String inscricaoEstadual,
            String uf,
            String indicadorInscricaoEstadual
    ) {}

    public record ItemNota(
            String codigoProduto,
            String produtoNome,
            String ncm,
            String cfop,
            String unidadeComercial,
            BigDecimal quantidade,
            BigDecimal precoUnitario,
            BigDecimal precoTotal,
            Icms icms
    ) {
        /** Tributação de ICMS do item (vBC / pICMS / vICMS), com a situação tributária. */
        public record Icms(
                String cstCsosn,
                BigDecimal baseCalculo,
                BigDecimal aliquota,
                BigDecimal valor
        ) {}
    }

    /** Totais da nota (≈ ICMSTot): vProd, vICMS e vNF. */
    public record Totais(
            BigDecimal valorProdutos,
            BigDecimal valorIcms,
            BigDecimal valorTotalNota
    ) {}
}
