package com.readrace.api.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.readrace.api.dto.response.BuscaResponse;
import com.readrace.api.dto.response.ComunidadeBuscaResponse;
import com.readrace.api.dto.response.LivroBuscaResponse;
import com.readrace.api.dto.response.UsuarioBuscaResponse;
import com.readrace.api.exception.ParametroInvalidoException;
import com.readrace.api.model.Comunidade;
import com.readrace.api.model.Livro;
import com.readrace.api.model.TipoBusca;
import com.readrace.api.model.Usuario;
import com.readrace.api.repository.ComunidadeRepository;
import com.readrace.api.repository.LivroRepository;
import com.readrace.api.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class BuscaService {

    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComunidadeRepository comunidadeRepository;

    public BuscaService(
            LivroRepository livroRepository,
            UsuarioRepository usuarioRepository,
            ComunidadeRepository comunidadeRepository) {
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
        this.comunidadeRepository = comunidadeRepository;
    }

    public BuscaResponse<?> buscar(String termo, String tipo) {

        if (termo == null || termo.isEmpty()) {
            throw new ParametroInvalidoException(
                    "O parâmetro 'q' é obrigatório e deve ter pelo menos 1 caractere.");
        }

        TipoBusca tipoBusca = TipoBusca.de(tipo);

        if (tipoBusca == TipoBusca.LIVROS) {
            List<Livro> livros = livroRepository.findByTituloContainingIgnoreCase(termo);
            List<LivroBuscaResponse> itens = new ArrayList<>();

            for (Livro livro : livros) {
                itens.add(LivroBuscaResponse.de(livro));
            }

            return new BuscaResponse<>(tipoBusca.getValor(), itens);
        } else if (tipoBusca == TipoBusca.USUARIOS) {

            List<Usuario> usuarios =
                    usuarioRepository
                            .findByNomeContainingIgnoreCaseAndExcluidoEmIsNullOrNomeUsuarioContainingIgnoreCaseAndExcluidoEmIsNull(
                                    termo, termo);
            List<UsuarioBuscaResponse> itens = new ArrayList<>();

            for (Usuario usuario : usuarios) {
                itens.add(UsuarioBuscaResponse.de(usuario));
            }

            return new BuscaResponse<>(tipoBusca.getValor(), itens);
        } else {

            List<Comunidade> comunidades =
                    comunidadeRepository.findByNomeContainingIgnoreCaseAndExcluidoEmIsNull(termo);
            List<ComunidadeBuscaResponse> itens = new ArrayList<>();

            for (Comunidade comunidade : comunidades) {
                itens.add(ComunidadeBuscaResponse.de(comunidade));
            }

            return new BuscaResponse<>(tipoBusca.getValor(), itens);
        }
    }
}
