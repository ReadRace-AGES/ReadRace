package com.readrace.api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.request.ExemploRequest;
import com.readrace.api.dto.response.ExemploResponse;
import com.readrace.api.exception.RecursoNaoEncontradoException;
import com.readrace.api.model.Exemplo;
import com.readrace.api.repository.ExemploRepository;

/**
 * Onde ficam a regra de negócio e o limite da transação.
 *
 * <p>Regra do projeto: controller não fala com repository. Sempre controller -> service ->
 * repository.
 */
@Service
@Transactional(readOnly = true)
public class ExemploService {

    private final ExemploRepository exemploRepository;

    // Injeção por construtor, sem @Autowired em campo.
    public ExemploService(ExemploRepository exemploRepository) {
        this.exemploRepository = exemploRepository;
    }

    public List<ExemploResponse> listar() {
        // A conversão para DTO acontece aqui dentro, com a transação aberta.
        return exemploRepository.findAll().stream().map(ExemploResponse::de).toList();
    }

    public ExemploResponse buscarPorId(Long id) {
        return ExemploResponse.de(buscarEntidade(id));
    }

    @Transactional
    public ExemploResponse criar(ExemploRequest request) {
        Exemplo exemplo = new Exemplo(request.nome(), request.descricao());

        return ExemploResponse.de(exemploRepository.save(exemplo));
    }

    @Transactional
    public ExemploResponse atualizar(Long id, ExemploRequest request) {
        Exemplo exemplo = buscarEntidade(id);

        exemplo.atualizar(request.nome(), request.descricao());

        // Repare que NÃO existe save() aqui. Dentro da transação o objeto está
        // gerenciado pelo Hibernate: ele compara com o estado original e dispara
        // o UPDATE sozinho no commit. Isso se chama dirty checking.
        return ExemploResponse.de(exemplo);
    }

    @Transactional
    public void excluir(Long id) {
        exemploRepository.delete(buscarEntidade(id));
    }

    /** Busca a entidade ou lança 404. Use sempre este, nunca o findById direto. */
    private Exemplo buscarEntidade(Long id) {
        return exemploRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new RecursoNaoEncontradoException(
                                        "Exemplo %d não encontrado".formatted(id)));
    }
}
