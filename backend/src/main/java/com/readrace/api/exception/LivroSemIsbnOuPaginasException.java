package com.readrace.api.exception;

public class LivroSemIsbnOuPaginasException extends ExcecaoDeNegocio {

    public LivroSemIsbnOuPaginasException() {
        super(
                CodigoErro.LIVRO_SEM_ISBN_OU_PAGINAS,
                CodigoErro.LIVRO_SEM_ISBN_OU_PAGINAS.mensagemPadrao());
    }
}
