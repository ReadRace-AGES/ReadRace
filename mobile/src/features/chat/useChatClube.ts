import { useCallback, useEffect, useRef, useState } from 'react';
import { useFocusEffect } from 'expo-router';

import { ApiError } from '@/api/client';

import {
  buscarMensagensDoClube,
  enviarMensagemDoClube,
  type MensagemClube,
} from './api';

const INTERVALO_POLLING_MS = 5000;

const COPY = {
  erroCarregar: 'Não foi possível carregar as mensagens. Tente novamente.',
  erroEnviar: 'Não foi possível enviar a mensagem. Tente novamente.',
  soMembro: 'Só membros do clube participam do chat.',
} as const;

type EstadoChat =
  | {
      situacao: 'carregando';
      mensagens: MensagemClube[];
      mensagem: null;
    }
  | {
      situacao: 'sucesso';
      mensagens: MensagemClube[];
      mensagem: null;
    }
  | {
      situacao: 'erro';
      mensagens: MensagemClube[];
      mensagem: string;
    }
  | {
      situacao: 'nao_membro';
      mensagens: MensagemClube[];
      mensagem: string;
    };

function adicionarSemDuplicar(
  atuais: MensagemClube[],
  novas: MensagemClube[]
): MensagemClube[] {
  const porId = new Map<string, MensagemClube>();

  for (const mensagem of atuais) {
    porId.set(mensagem.id, mensagem);
  }

  for (const mensagem of novas) {
    porId.set(mensagem.id, mensagem);
  }

  return Array.from(porId.values()).sort(
    (a, b) =>
      new Date(a.enviadaEm).getTime() - new Date(b.enviadaEm).getTime()
  );
}

function mensagemDeErro(erro: unknown, fallback: string): string {
  if (erro instanceof ApiError && erro.code === 'SO_MEMBRO_NO_CHAT') {
    return COPY.soMembro;
  }

  if (erro instanceof ApiError && erro.message) {
    return erro.message;
  }

  return fallback;
}

export function useChatClube(clubeId: string) {
  const [chat, setChat] = useState<EstadoChat>({
    situacao: 'carregando',
    mensagens: [],
    mensagem: null,
  });

  const [enviando, setEnviando] = useState(false);
  const [erroEnvio, setErroEnvio] = useState<string | null>(null);

  const mensagensRef = useRef<MensagemClube[]>([]);
  const requisicaoInicialRef = useRef<AbortController | null>(null);
  const requisicaoPollingRef = useRef<AbortController | null>(null);
  const requisicaoEnvioRef = useRef<AbortController | null>(null);
  const pollingEmAndamentoRef = useRef(false);

  const atualizarMensagens = useCallback((mensagens: MensagemClube[]) => {
    mensagensRef.current = mensagens;

    setChat({
      situacao: 'sucesso',
      mensagens,
      mensagem: null,
    });
  }, []);

  const carregar = useCallback(async () => {
    requisicaoInicialRef.current?.abort();

    const controller = new AbortController();
    requisicaoInicialRef.current = controller;

    setChat({
      situacao: 'carregando',
      mensagens: mensagensRef.current,
      mensagem: null,
    });

    try {
      const resposta = await buscarMensagensDoClube(
        clubeId,
        undefined,
        controller.signal
      );

      if (controller.signal.aborted) return;

      mensagensRef.current = resposta.mensagens;

      setChat({
        situacao: 'sucesso',
        mensagens: resposta.mensagens,
        mensagem: null,
      });
    } catch (erro: unknown) {
      if (controller.signal.aborted) return;

      if (erro instanceof ApiError && erro.code === 'SO_MEMBRO_NO_CHAT') {
        setChat({
          situacao: 'nao_membro',
          mensagens: [],
          mensagem: COPY.soMembro,
        });

        return;
      }

      setChat({
        situacao: 'erro',
        mensagens: mensagensRef.current,
        mensagem: mensagemDeErro(erro, COPY.erroCarregar),
      });
    }
  }, [clubeId]);

  const buscarNovasMensagens = useCallback(async () => {
    if (pollingEmAndamentoRef.current) return;

    const mensagensAtuais = mensagensRef.current;

    if (mensagensAtuais.length === 0) {
      return;
    }

    const ultimaMensagem = mensagensAtuais[mensagensAtuais.length - 1];

    pollingEmAndamentoRef.current = true;

    const controller = new AbortController();
    requisicaoPollingRef.current = controller;

    try {
      const resposta = await buscarMensagensDoClube(
        clubeId,
        ultimaMensagem.enviadaEm,
        controller.signal
      );

      if (controller.signal.aborted || resposta.mensagens.length === 0) {
        return;
      }

      const atualizadas = adicionarSemDuplicar(
        mensagensRef.current,
        resposta.mensagens
      );

      atualizarMensagens(atualizadas);
    } catch {
      // Falhas de polling são silenciosas para não interromper o uso do chat.
    } finally {
      pollingEmAndamentoRef.current = false;
    }
  }, [atualizarMensagens, clubeId]);

  const enviar = useCallback(
    async (texto: string): Promise<boolean> => {
      const textoNormalizado = texto.trim();

      if (
        enviando ||
        textoNormalizado.length === 0 ||
        textoNormalizado.length > 1000
      ) {
        return false;
      }

      requisicaoEnvioRef.current?.abort();

      const controller = new AbortController();
      requisicaoEnvioRef.current = controller;

      setEnviando(true);
      setErroEnvio(null);

      try {
        const novaMensagem = await enviarMensagemDoClube(
          clubeId,
          textoNormalizado,
          controller.signal
        );

        if (controller.signal.aborted) {
          return false;
        }

        const atualizadas = adicionarSemDuplicar(
          mensagensRef.current,
          [novaMensagem]
        );

        atualizarMensagens(atualizadas);

        return true;
      } catch (erro: unknown) {
        if (controller.signal.aborted) {
          return false;
        }

        if (erro instanceof ApiError && erro.code === 'SO_MEMBRO_NO_CHAT') {
          setChat({
            situacao: 'nao_membro',
            mensagens: [],
            mensagem: COPY.soMembro,
          });

          return false;
        }

        setErroEnvio(mensagemDeErro(erro, COPY.erroEnviar));

        return false;
      } finally {
        if (!controller.signal.aborted) {
          setEnviando(false);
        }
      }
    },
    [atualizarMensagens, clubeId, enviando]
  );

  useFocusEffect(
    useCallback(() => {
      void carregar();

      return () => {
        requisicaoInicialRef.current?.abort();
        requisicaoPollingRef.current?.abort();
        requisicaoEnvioRef.current?.abort();
        pollingEmAndamentoRef.current = false;
      };
    }, [carregar])
  );

  useEffect(() => {
    if (chat.situacao !== 'sucesso') {
      return;
    }

    const intervalo = setInterval(() => {
      void buscarNovasMensagens();
    }, INTERVALO_POLLING_MS);

    return () => {
      clearInterval(intervalo);
      requisicaoPollingRef.current?.abort();
      pollingEmAndamentoRef.current = false;
    };
  }, [buscarNovasMensagens, chat.situacao]);

  return {
    chat,
    enviando,
    erroEnvio,
    enviar,
    recarregar: carregar,
  };
}