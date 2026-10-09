import { apiGet, apiRequest } from '@/api/client';

export type AutorMensagemClube = {
  id: string;
  nome: string;
  avatarUrl: string | null;
};

export type MensagemClube = {
  id: string;
  texto: string;
  enviadaEm: string;
  minha: boolean;
  autor: AutorMensagemClube;
};

export type MensagensClubeResponse = {
  mensagens: MensagemClube[];
};

export type EnviarMensagemClubeRequest = {
  texto: string;
};

/**
 * Busca as mensagens do chat do clube.
 *
 * Sem `depois`, o backend devolve até as 100 mensagens mais recentes,
 * ordenadas da mais antiga para a mais nova.
 *
 * Com `depois`, devolve somente as mensagens enviadas depois daquele instante.
 */
export function buscarMensagensDoClube(
  clubeId: string,
  depois?: string,
  signal?: AbortSignal
) {
  const clube = encodeURIComponent(clubeId);

  const query = depois
    ? `?depois=${encodeURIComponent(depois)}`
    : '';

  return apiGet<MensagensClubeResponse>(
    `/api/clubes/${clube}/mensagens${query}`,
    { signal }
  );
}

/**
 * Envia uma nova mensagem para o chat do clube.
 */
export function enviarMensagemDoClube(
  clubeId: string,
  texto: string,
  signal?: AbortSignal
) {
  const clube = encodeURIComponent(clubeId);

  const body: EnviarMensagemClubeRequest = {
    texto,
  };

  return apiRequest<MensagemClube>(
    `/api/clubes/${clube}/mensagens`,
    {
      method: 'POST',
      signal,
      body: JSON.stringify(body),
    }
  );
}