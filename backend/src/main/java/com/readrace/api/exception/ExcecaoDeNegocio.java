package com.readrace.api.exception;

/**
 * Base de toda exceção de regra de negócio do ReadRace.
 *
 * <p>A exceção carrega o próprio {@link CodigoErro}. Assim o {@link GlobalExceptionHandler} precisa
 * de UM handler para todas elas, em vez de um {@code @ExceptionHandler} novo a cada exceção criada.
 *
 * <p>Regra do projeto: service não devolve {@code Optional} vazio para o controller decidir — ele
 * lança uma filha desta classe, e o tratamento acontece num lugar só.
 */
public abstract class ExcecaoDeNegocio extends RuntimeException {

    private final CodigoErro codigo;

    protected ExcecaoDeNegocio(CodigoErro codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
