package br.sentineltrade.bff.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Corpos das requisicoes que chegam do frontend, com validacao de formato e tamanho.
 * Regras de negocio (politica de senha, saldo, limites...) continuam no backend,
 * que revalida tudo: o backend nunca confia no BFF.
 */
public final class Dtos {

    private Dtos() {
    }

    private static final String CODIGO_6 = "\\d{6}";

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.")
            @Size(max = 255, message = "E-mail muito longo.") String email,
            @NotBlank(message = "Informe a senha.") @Size(max = 128, message = "Senha muito longa.") String senha) {}

    public record MfaRequest(
            @NotBlank(message = "Sessão de login expirada. Recomece.") @Size(max = 512) String mfaToken,
            @NotBlank(message = "Informe o código.")
            @Pattern(regexp = CODIGO_6, message = "O código deve ter 6 dígitos.") String codigo) {}

    public record CadastroRequest(
            @NotBlank(message = "Informe seu nome completo.")
            @Size(min = 3, max = 150, message = "Informe seu nome completo.") String nome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.")
            @Size(max = 255, message = "E-mail muito longo.") String email,
            @NotBlank(message = "Informe a senha.")
            @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres.") String senha) {}

    public record CadastroMfaRequest(
            @NotBlank(message = "Cadastro expirado. Recomece.") @Size(max = 512) String cadastroToken,
            @NotBlank(message = "Informe o código.")
            @Pattern(regexp = CODIGO_6, message = "O código deve ter 6 dígitos.") String codigo) {}

    public record EsqueciSenhaRequest(
            @NotBlank(message = "Informe o e-mail.") @Email(message = "E-mail inválido.")
            @Size(max = 255, message = "E-mail muito longo.") String email) {}

    public record OrdemRequest(
            @NotBlank(message = "Informe o ativo.")
            @Pattern(regexp = "^[A-Z0-9]{4,12}$", message = "Ticker inválido.") String ticker,
            @NotBlank(message = "Informe o tipo da ordem.")
            @Pattern(regexp = "^(COMPRA|VENDA)$", message = "O tipo deve ser COMPRA ou VENDA.") String tipo,
            @NotNull(message = "Informe a quantidade.")
            @Positive(message = "A quantidade deve ser maior que zero.")
            @Max(value = 10_000_000, message = "Quantidade acima do permitido.") Integer quantidade,
            @NotNull(message = "Informe o preço limite.")
            @Positive(message = "O preço deve ser maior que zero.")
            @Digits(integer = 14, fraction = 4, message = "Preço inválido.") BigDecimal precoLimite) {}

    public record LimitesRequest(
            @NotNull(message = "Informe o limite por ordem.") @Positive(message = "Limites devem ser positivos.")
            @Digits(integer = 16, fraction = 2, message = "Valor inválido.") BigDecimal porOrdem,
            @NotNull(message = "Informe o limite diário.") @Positive(message = "Limites devem ser positivos.")
            @Digits(integer = 16, fraction = 2, message = "Valor inválido.") BigDecimal diario,
            @NotNull(message = "Informe o máximo de ordens por dia.") @Positive(message = "Limites devem ser positivos.")
            @Max(value = 100_000, message = "Valor acima do permitido.") Integer ordensDia) {}

    public record MercadoRequest(
            @NotBlank(message = "Informe o status do mercado.")
            @Pattern(regexp = "^(ABERTO|FECHADO)$", message = "Status deve ser ABERTO ou FECHADO.") String status) {}

    public record ModoSeguroRequest(
            @NotNull(message = "Informe se o modo seguro deve ficar ativo.") Boolean ativo) {}
}
