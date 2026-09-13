/**
 * Cliente HTTP mínimo da API do ReadRace.
 *
 * Nenhuma chamada envia identificação do usuário: o backend resolve quem é
 * pelo `CurrentUser` (#10). A URL base vem de `EXPO_PUBLIC_API_URL`
 * (ver `.env.example`); sem ela, assume o backend local na porta 8080.
 */
const BASE_URL = (
  process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080'
).replace(/\/+$/, '');

/** Envelope padrão de erro do backend (#10): `code` estável, `message` exibível. */
export type ApiErrorBody = {
  code: string;
  message: string;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, body: Partial<ApiErrorBody>) {
    super(body.message ?? `Erro ${status} ao chamar a API.`);
    this.name = 'ApiError';
    this.status = status;
    this.code = body.code ?? 'UNKNOWN';
  }
}

export async function apiGet<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { Accept: 'application/json', ...init?.headers },
  });

  if (!response.ok) {
    let body: Partial<ApiErrorBody> = {};
    try {
      body = (await response.json()) as Partial<ApiErrorBody>;
    } catch {
      // Corpo não-JSON (proxy, gateway): a mensagem padrão do ApiError cobre.
    }
    throw new ApiError(response.status, body);
  }

  return (await response.json()) as T;
}
