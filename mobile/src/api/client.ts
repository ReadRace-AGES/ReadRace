/**
 * Cliente HTTP minimo da API do ReadRace.
 *
 * Nenhuma chamada envia identificação do usuário: o backend resolve quem é pelo `CurrentUser`
 * (#10). A URL base vem de `EXPO_PUBLIC_API_URL` (ver `.env.example`); sem ela, assume o backend
 * local na porta 8080.
 */
const BASE_URL = (process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080").replace(/\/+$/, "");

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
    this.name = "ApiError";
    this.status = status;
    this.code = body.code ?? "UNKNOWN";
  }
}

export async function apiGet<T>(path: string, init?: RequestInit): Promise<T> {
  return apiRequest<T>(path, {
    ...init,
    method: init?.method ?? "GET",
    headers: { Accept: "application/json", ...init?.headers },
  });
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");

  if (typeof init.body === "string" && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers,
  });

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorBody(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

async function readErrorBody(response: Response): Promise<Partial<ApiErrorBody>> {
  try {
    return (await response.json()) as Partial<ApiErrorBody>;
  } catch {
    // Corpo não-JSON (proxy, gateway): a mensagem padrão do ApiError cobre.
    return {};
  }
}
