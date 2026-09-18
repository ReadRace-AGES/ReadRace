import { useCallback, useEffect, useState } from 'react';

import { ApiError } from '@/api/client';

import { buscarClube, type Clube } from './api';

export type ClubeCarga =
  | { situacao: 'carregando' }
  | { situacao: 'erro'; mensagem: string }
  | { situacao: 'sucesso'; dados: Clube };

export type ClubeState = {
  clube: ClubeCarga;
  recarregar: () => void;
};

function mensagemDe(erro: unknown) {
  return erro instanceof ApiError
    ? erro.message
    : 'Não foi possível carregar o clube.';
}

/**
 * Cabeçalho e ranking chegam na mesma resposta, então a tela nunca mostra meia página.
 *
 * Recarregar depois de registrar leitura é o comportamento pedido pela #35: o ranking volta
 * igual porque nenhuma ação desta sprint cria `Pontos`. Por isso o recarregamento é discreto —
 * mantém a tela montada e só troca os dados quando eles chegam.
 */
export function useClube(clubeId: string): ClubeState {
  const [clube, setClube] = useState<ClubeCarga>({ situacao: 'carregando' });
  const [versao, setVersao] = useState(0);

  // Trocar de clube zera a tela; recarregar o mesmo clube, não — ver `recarregar`.
  useEffect(() => {
    setClube({ situacao: 'carregando' });
  }, [clubeId]);

  useEffect(() => {
    const controller = new AbortController();
    buscarClube(clubeId, controller.signal)
      .then((dados) => {
        if (controller.signal.aborted) return;
        setClube({ situacao: 'sucesso', dados });
      })
      .catch((erro: unknown) => {
        if (controller.signal.aborted) return;
        // Um recarregamento que falha não apaga o que já está na tela: quem acabou de
        // registrar leitura precisa ver que deu certo, não um erro de carregamento.
        setClube((atual) =>
          atual.situacao === 'sucesso'
            ? atual
            : { situacao: 'erro', mensagem: mensagemDe(erro) }
        );
      });
    return () => controller.abort();
  }, [clubeId, versao]);

  const recarregar = useCallback(() => {
    setClube((atual) =>
      atual.situacao === 'sucesso' ? atual : { situacao: 'carregando' }
    );
    setVersao((v) => v + 1);
  }, []);

  return { clube, recarregar };
}
