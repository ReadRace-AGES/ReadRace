import { apiRequest } from "@/api/client";

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

export function registrarProgresso(livroId: string, pagina: number) {
  return apiRequest<ProgressoLeituraResponse>(
    `/api/livros/${livroId}/progresso`,
    {
      method: "POST",
      body: JSON.stringify({ pagina }),
    },
  );
}
