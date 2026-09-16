import { apiRequest } from '@/api/client';

export type ProgressoLeituraResponse = {
  paginaAtual: number;
  paginaMaximaAlcancada: number;
  totalPaginas: number;
  percentual: number;
  xpPaginas: number;
  xpConclusao: number;
  xpTotal: number;
  concluido: boolean;
};

export class ProgressoTimeoutError extends Error {
  constructor() {
    super(
      'Não foi possível confirmar o registro a tempo. Consulte o progresso do livro antes de tentar novamente.'
    );
    this.name = 'ProgressoTimeoutError';
  }
}

export async function registrarProgresso(livroId: string, pagina: number) {
  const controller = new AbortController();
  let timer: ReturnType<typeof setTimeout> | undefined;
  const limite = new Promise<never>((_, reject) => {
    timer = setTimeout(() => {
      reject(new ProgressoTimeoutError());
      controller.abort();
    }, 15000);
  });
  try {
    return await Promise.race([
      apiRequest<ProgressoLeituraResponse>(
        `/api/livros/${encodeURIComponent(livroId)}/progresso`,
        {
          method: 'POST',
          body: JSON.stringify({ pagina }),
          signal: controller.signal,
        }
      ),
      limite,
    ]);
  } finally {
    clearTimeout(timer);
  }
}
