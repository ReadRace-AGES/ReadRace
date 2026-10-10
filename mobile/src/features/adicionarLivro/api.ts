import { apiGet, apiRequest } from '@/api/client';

type IndustryIdentifier = { type: string; identifier: string };

type VolumeInfo = {
  title: string;
  authors?: string[];
  industryIdentifiers?: IndustryIdentifier[];
  pageCount?: number;
  imageLinks?: { thumbnail?: string; smallThumbnail?: string };
};

type GoogleBookVolume = { id: string; volumeInfo: VolumeInfo };

type GoogleBooksResponse = { items?: GoogleBookVolume[] };

export type ResultadoCatalogo = {
  volumeId: string;
  titulo: string;
  autor: string | null;
  capaUrl: string | null;
  isbn: string;
  paginas: number;
};

function isbnDe(info: VolumeInfo): string | null {
  const identificadores = info.industryIdentifiers ?? [];
  return (
    identificadores.find((i) => i.type === 'ISBN_13')?.identifier ??
    identificadores.find((i) => i.type === 'ISBN_10')?.identifier ??
    null
  );
}

/** Sem ISBN ou sem páginas, o livro não cabe na biblioteca (decisão da sprint, #155). */
function paraResultado(volume: GoogleBookVolume): ResultadoCatalogo | null {
  const isbn = isbnDe(volume.volumeInfo);
  const paginas = volume.volumeInfo.pageCount;
  if (!isbn || !paginas) return null;

  return {
    volumeId: volume.id,
    titulo: volume.volumeInfo.title,
    autor: volume.volumeInfo.authors?.join(', ') ?? null,
    capaUrl:
      volume.volumeInfo.imageLinks?.thumbnail ??
      volume.volumeInfo.imageLinks?.smallThumbnail ??
      null,
    isbn,
    paginas,
  };
}

/** `GET /api/books/volumes?q=` — a mesma rota da busca de livros, já existente. */
export async function buscarCatalogo(
  termo: string,
  signal?: AbortSignal
): Promise<ResultadoCatalogo[]> {
  const resposta = await apiGet<GoogleBooksResponse>(
    `/api/books/volumes?q=${encodeURIComponent(termo)}`,
    { signal }
  );

  return (resposta.items ?? [])
    .map(paraResultado)
    .filter((item): item is ResultadoCatalogo => item !== null);
}

export type ListaDestino = 'lido' | 'desejo' | 'favorito';

export type LivroAdicionado = {
  livroId: string;
  titulo: string;
  capaUrl: string | null;
  status: 'lendo' | 'lido' | 'desejo';
  favorito: boolean;
};

/** `POST /api/biblioteca` — cria ou atualiza o item, sem duplicar (#155). */
export function adicionarNaBiblioteca(
  volumeId: string,
  lista: ListaDestino,
  signal?: AbortSignal
) {
  return apiRequest<LivroAdicionado>('/api/biblioteca', {
    method: 'POST',
    body: JSON.stringify({ volumeId, lista }),
    signal,
  });
}
