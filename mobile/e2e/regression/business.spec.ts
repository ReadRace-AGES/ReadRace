import { test, expect, get, home, tab, button, text } from '../support/test';
import type { BibliotecaResponse } from '../../src/features/biblioteca/api';
import type { LivroDetalhe } from '../../src/features/livro/api';
import type { Perfil } from '../../src/features/perfil/api';
import type { ProgressoLeituraResponse } from '../../src/features/progresso/api';
import type { FeedComunidades } from '../../src/features/feed/api';
import type { Clube } from '../../src/features/clube/api';
import type { Mascote } from '../../src/features/mascotes/api';

test.describe('Regras e regressões das sprints @regression', () => {
  test('Leitura: retrocesso e repetição não pagam XP novamente #33 #199', async ({
    api,
  }) => {
    const biblioteca = await get<BibliotecaResponse>(api, '/api/biblioteca');
    const livro = biblioteca.lendo.itens[0];
    const detalhe = await get<LivroDetalhe>(
      api,
      `/api/livros/${livro.livroId}`
    );
    const maxima = detalhe.progresso!.paginaMaximaAlcancada;
    expect(maxima + 2).toBeLessThan(detalhe.livro.totalPaginas);
    for (const [pagina, xp] of [
      [maxima + 1, 1],
      [maxima, 0],
      [maxima + 1, 0],
      [maxima + 2, 1],
    ]) {
      const response = await api.post(
        `/api/livros/${livro.livroId}/progresso`,
        { data: { pagina } }
      );
      expect(response.ok()).toBeTruthy();
      const result: ProgressoLeituraResponse = await response.json();
      expect(result.xpPaginas).toBe(xp);
      expect(result.xpConclusao).toBe(0);
      expect(result.paginaAtual).toBe(pagina);
      expect(result.paginaMaximaAlcancada).toBeGreaterThanOrEqual(maxima + 1);
    }
  });

  test('Conclusão: bônus único, inclusive após retroceder #95 #199', async ({
    api,
  }) => {
    const biblioteca = await get<BibliotecaResponse>(api, '/api/biblioteca');
    const id = biblioteca.lendo.itens[0].livroId;
    const detalhe = await get<LivroDetalhe>(api, `/api/livros/${id}`);
    const total = detalhe.livro.totalPaginas;
    for (const [pagina, bonus] of [
      [total, 150],
      [total - 1, 0],
      [total, 0],
    ]) {
      const response = await api.post(`/api/livros/${id}/progresso`, {
        data: { pagina },
      });
      expect(response.ok()).toBeTruthy();
      expect((await response.json()).xpConclusao).toBe(bonus);
    }
  });

  test('Página inválida não altera perfil nem livro #33 #95', async ({
    api,
  }) => {
    const biblioteca = await get<BibliotecaResponse>(api, '/api/biblioteca');
    const id = biblioteca.lendo.itens[0].livroId;
    const before = await get<LivroDetalhe>(api, `/api/livros/${id}`);
    const perfil = await get<Perfil>(api, '/api/me/perfil');
    for (const pagina of [0, -1, 1.5, before.livro.totalPaginas + 1]) {
      const response = await api.post(`/api/livros/${id}/progresso`, {
        data: { pagina },
      });
      expect(response.status()).toBe(422);
      expect((await response.json()).code).toBe('PAGINA_INVALIDA');
    }
    expect(
      (await get<LivroDetalhe>(api, `/api/livros/${id}`)).progresso
    ).toEqual(before.progresso);
    expect((await get<Perfil>(api, '/api/me/perfil')).xpAtual).toBe(
      perfil.xpAtual
    );
  });

  test('Biblioteca: marcar lido sem leitura não dá XP; remoção protege histórico #219', async ({
    api,
  }) => {
    const biblioteca = await get<BibliotecaResponse>(api, '/api/biblioteca');
    const id = biblioteca.desejo.itens[0].livroId;
    const perfil = await get<Perfil>(api, '/api/me/perfil');
    expect(
      (
        await api.patch(`/api/biblioteca/${id}`, { data: { lista: 'lido' } })
      ).ok()
    ).toBeTruthy();
    const after = await get<Perfil>(api, '/api/me/perfil');
    expect(after.xpAtual).toBe(perfil.xpAtual);
    expect(after.estatisticas.livrosLidos).toBe(
      perfil.estatisticas.livrosLidos
    );
    expect((await api.delete(`/api/biblioteca/${id}`)).status()).toBe(204);
    const protectedId = biblioteca.lendo.itens[0].livroId;
    const blocked = await api.delete(`/api/biblioteca/${protectedId}`);
    expect(blocked.status()).toBe(409);
    expect((await blocked.json()).code).toBe('LIVRO_COM_LEITURA');
  });

  test('Busca preserva termo e aba ao voltar; ausência é diferente de erro #102 #222', async ({
    page,
  }) => {
    await home(page);
    await tab(page, 'Buscar');
    await page.getByRole('tab', { name: 'Livros', exact: true }).click();
    await page.getByRole('textbox').fill('Hobbit');
    await button(page, 'O Hobbit').click();
    await expect(page).toHaveURL(/buscar-livro/);
    await button(page, 'Voltar').click();
    await expect(page.getByRole('textbox')).toHaveValue('Hobbit');
    await expect(button(page, 'O Hobbit')).toBeVisible();
    await page.getByRole('textbox').fill('xyzxyz');
    await expect(text(page, 'Nenhum resultado para "xyzxyz".')).toBeVisible();
  });

  test('Falha no progresso preserva formulário e não grava #33 #203', async ({
    page,
    api,
  }) => {
    const biblioteca = await get<BibliotecaResponse>(api, '/api/biblioteca');
    const livro = biblioteca.lendo.itens[0];
    const before = await get<LivroDetalhe>(api, `/api/livros/${livro.livroId}`);
    await home(page);
    await tab(page, 'Buscar');
    await page.getByRole('tab', { name: 'Livros', exact: true }).click();
    await page.getByRole('textbox').fill(livro.titulo);
    await button(page, livro.titulo).click();
    await button(page, 'Seu progresso. Registrar progresso de leitura').click();
    await page.getByLabel('Página atual', { exact: true }).fill('2');
    await page.route('**/progresso', (route) =>
      route.fulfill({
        status: 503,
        json: { code: 'INDISPONIVEL', message: 'Falha E2E recuperável' },
      })
    );
    await button(page, 'Atualizar Progresso').click();
    await expect(text(page, 'Falha E2E recuperável')).toBeVisible();
    await expect(page.getByLabel('Página atual', { exact: true })).toHaveValue(
      '2'
    );
    expect(
      (await get<LivroDetalhe>(api, `/api/livros/${livro.livroId}`)).progresso
    ).toEqual(before.progresso);
  });

  test('Clube: saída do líder transfere liderança #237', async ({ api }) => {
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    const clubes = await Promise.all(
      feed.clubes.map((c) => get<Clube>(api, `/api/clubes/${c.id}`))
    );
    const clube = clubes.find(
      (c) => c.meuCargo === 'administrador' && c.proximoLider
    )!;
    expect(clube).toBeTruthy();
    const response = await api.delete(`/api/clubes/${clube.id}/membros/eu`);
    expect(response.ok()).toBeTruthy();
    expect((await response.json()).novoLider.id).toBe(clube.proximoLider!.id);
    expect(
      (await get<FeedComunidades>(api, '/api/feed/comunidades')).clubes.some(
        (c) => c.id === clube.id
      )
    ).toBeFalsy();
    expect(
      (await get<Clube>(api, `/api/clubes/${clube.id}`)).meuCargo
    ).toBeNull();
  });

  test('Mascote bloqueado é rejeitado pela API #164', async ({ api }) => {
    const { mascotes } = await get<{ mascotes: Mascote[] }>(
      api,
      '/api/me/mascotes'
    );
    const bloqueado = mascotes.find((m) => !m.desbloqueado)!;
    expect(bloqueado).toBeTruthy();
    const before = await get<Perfil>(api, '/api/me/perfil');
    const response = await api.put('/api/me/mascote', {
      data: { mascoteId: bloqueado.id },
    });
    expect(response.status()).toBe(409);
    expect((await response.json()).code).toBe('MASCOTE_BLOQUEADO');
    expect((await get<Perfil>(api, '/api/me/perfil')).mascote).toEqual(
      before.mascote
    );
  });
});
