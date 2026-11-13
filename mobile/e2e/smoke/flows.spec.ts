import {
  test,
  expect,
  button,
  text,
  tab,
  home,
  get,
  starts,
} from '../support/test';
import type { FeedComunidades } from '../../src/features/feed/api';
import type { Clube } from '../../src/features/clube/api';
import type { LivroDetalhe } from '../../src/features/livro/api';
import type { Perfil } from '../../src/features/perfil/api';
import type { Quiz, RespostaQuiz } from '../../src/features/quiz/api';
import type { Mascote } from '../../src/features/mascotes/api';
import type { OponentesResponse } from '../../src/features/desafios/api';

test.describe('Nove fluxos da #233 @smoke', () => {
  test('Feed: clubes, comunidades e sequência #34', async ({ page, api }) => {
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    expect(feed.clubes.length).toBeGreaterThan(0);
    expect(feed.comunidades.length).toBeGreaterThan(0);
    await home(page);
    await expect(text(page, `Olá, ${feed.usuario.nome}!`)).toBeVisible();
    await expect(
      text(
        page,
        `${feed.usuario.sequenciaDias} ${feed.usuario.sequenciaDias === 1 ? 'dia' : 'dias'}`
      )
    ).toBeVisible();
    for (const clube of feed.clubes)
      await expect(button(page, starts(clube.nome))).toBeVisible();
    await expect(text(page, 'Minhas comunidades')).toBeVisible();
    for (const comunidade of feed.comunidades)
      await expect(button(page, starts(comunidade.nome))).toBeVisible();
  });

  test('Biblioteca: adicionar e registrar leitura #30 #33 #155', async ({
    page,
    api,
  }) => {
    await home(page);
    await tab(page, 'Meus Livros');
    await button(page, /Adicionar livro/i).click();
    await page
      .getByPlaceholder('Busque por título, autor ou ISBN')
      .fill('Hobbit');
    await button(page, /^O Hobbit,/).click();
    const added = page.waitForResponse(
      (r) =>
        r.url().endsWith('/api/biblioteca') && r.request().method() === 'POST'
    );
    await button(page, 'Desejos').click();
    const response = await added;
    expect(response.ok()).toBeTruthy();
    const livro = await response.json();
    await expect(text(page, 'Livro adicionado com sucesso!')).toBeVisible();
    await button(page, 'Fechar').last().click();
    await page.getByRole('tab', { name: 'Quero ler', exact: true }).click();
    await button(page, livro.titulo).click();
    const detalhe = await get<LivroDetalhe>(
      api,
      `/api/livros/${livro.livroId}`
    );
    const pagina = (detalhe.progresso?.paginaMaximaAlcancada ?? 0) + 1;
    expect(pagina).toBeLessThanOrEqual(detalhe.livro.totalPaginas);
    await button(page, 'Seu progresso. Registrar progresso de leitura').click();
    await page.getByLabel('Página atual', { exact: true }).fill(String(pagina));
    const saved = page.waitForResponse(
      (r) => r.url().endsWith('/progresso') && r.request().method() === 'POST'
    );
    await button(page, 'Atualizar Progresso').click();
    expect((await saved).ok()).toBeTruthy();
    await expect(
      page.getByLabel('Página atual', { exact: true })
    ).not.toBeVisible();
    await expect(
      text(
        page,
        new RegExp(`${pagina} de ${detalhe.livro.totalPaginas} páginas`)
      )
    ).toBeVisible();
    await page.reload();
    await expect(
      text(
        page,
        new RegExp(`${pagina} de ${detalhe.livro.totalPaginas} páginas`)
      )
    ).toBeVisible();
    expect(
      (await get<LivroDetalhe>(api, `/api/livros/${livro.livroId}`)).progresso
        ?.paginaAtual
    ).toBe(pagina);
  });

  test('Busca: três abas, seguir e seguidores #43 #44 #154 #201', async ({
    page,
    api,
  }) => {
    const { itens } = await get<{ itens: { id: string; nome: string }[] }>(
      api,
      '/api/busca?tipo=usuarios&q=Ana'
    );
    const leitor = itens[0];
    expect(leitor).toBeTruthy();
    await home(page);
    await tab(page, 'Buscar');
    await page.getByRole('tab', { name: 'Livros', exact: true }).click();
    await page.getByRole('textbox').fill('Hobbit');
    await expect(button(page, 'O Hobbit')).toBeVisible();
    await page.getByRole('tab', { name: 'Comunidades', exact: true }).click();
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    await page.getByRole('textbox').fill(feed.comunidades[0].nome);
    await expect(button(page, feed.comunidades[0].nome)).toBeVisible();
    await page.getByRole('tab', { name: 'Usuários', exact: true }).click();
    await page.getByRole('textbox').fill(leitor.nome);
    await button(page, starts(`${leitor.nome}, @`)).click();
    const before = await get<Perfil>(api, `/api/usuarios/${leitor.id}/perfil`);
    await button(
      page,
      before.sigo ? 'Seguindo' : before.meSegue ? 'Seguir de volta' : 'Seguir'
    ).click();
    await expect
      .poll(
        async () =>
          (await get<Perfil>(api, `/api/usuarios/${leitor.id}/perfil`)).sigo
      )
      .toBe(!before.sigo);
    await button(
      page,
      before.sigo ? (before.meSegue ? 'Seguir de volta' : 'Seguir') : 'Seguindo'
    ).click();
    await expect
      .poll(
        async () =>
          (await get<Perfil>(api, `/api/usuarios/${leitor.id}/perfil`))
            .seguidores
      )
      .toBe(before.seguidores);
    await button(page, /^Seguidores/).click();
    await expect(text(page, 'Seguidores')).toBeVisible();
    await button(page, 'Voltar').click();
    await button(page, 'Voltar').click();
    await expect(page.getByRole('textbox')).toHaveValue(leitor.nome);
  });

  test('Comunidade: curtir, comentar e responder #101 #150', async ({
    page,
    api,
  }) => {
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    await home(page);
    await button(page, starts(feed.comunidades[0].nome)).click();
    // Deliberately exercise the first post in the visible, newest-first list.
    const like = button(page, /^\d+ curtida/).first();
    const before = await like.getAttribute('aria-label');
    await like.click();
    await expect(like).not.toHaveAttribute('aria-label', before!);
    await button(page, /^\d+ comentário/)
      .first()
      .click();
    await page
      .getByPlaceholder('Escreva um comentário...')
      .fill('Comentário E2E da comunidade');
    await button(page, 'Comentar').click();
    await expect(text(page, 'Comentário E2E da comunidade')).toBeVisible();
    const me = await get<Perfil>(api, '/api/me/perfil');
    await button(page, `Responder a ${me.nome}`).last().click();
    await page
      .getByPlaceholder('Escreva uma resposta...')
      .fill('Resposta E2E da comunidade');
    await button(page, 'Responder').click();
    await expect(text(page, 'Resposta E2E da comunidade')).toBeVisible();
  });

  test('Clube: ranking, fórum, chat e quiz #35 #149 #151 #153 #162', async ({
    page,
    api,
  }) => {
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    const clubes = await Promise.all(
      feed.clubes.map((c) => get<Clube>(api, `/api/clubes/${c.id}`))
    );
    const clube = clubes.find((c) => c.meuCargo === 'membro' && c.temQuizAtivo);
    expect(clube, 'Seed deve conter clube com quiz para membro').toBeTruthy();
    const path = `/api/clubes/${clube!.id}`;
    await home(page);
    await button(page, starts(clube!.nome)).click();
    await button(page, 'Ver todos').click();
    for (const linha of clube!.ranking)
      await expect(button(page, new RegExp(linha.usuario.nome))).toBeVisible();
    await button(page, 'Voltar').click();
    await button(page, 'Fórum de leitura').click();
    await button(page, 'Escrever post').click();
    await page
      .getByLabel('Texto do post', { exact: true })
      .fill('Post E2E no fórum do clube');
    await button(page, 'Publicar').click();
    await expect(text(page, 'Post E2E no fórum do clube')).toBeVisible();
    await button(page, 'Voltar').click();
    await button(page, 'Chat do clube').click();
    await page
      .getByPlaceholder('Escreva uma mensagem...')
      .fill('Mensagem E2E no chat');
    await button(page, 'Enviar').click();
    await expect(text(page, 'Mensagem E2E no chat')).toBeVisible();
    await expect(page.getByPlaceholder('Escreva uma mensagem...')).toHaveValue(
      ''
    );
    await button(page, 'Voltar').click();
    await button(page, 'Acessar Quiz').click();
    const quiz = await get<Quiz>(api, `${path}/quiz`);
    const me = await get<Perfil>(api, '/api/me/perfil');
    const initialPoints =
      clube!.ranking.find((r) => r.usuario.id === me.id)?.pontos ?? 0;
    let points = 0;
    for (let i = 0; i < quiz.totalPerguntas; i++) {
      const current = await get<Quiz>(api, `${path}/quiz`);
      expect(current.proximaPergunta).not.toBeNull();
      const alternative = current.proximaPergunta!.alternativas[0];
      const answered = page.waitForResponse(
        (r) => r.url().endsWith('/resposta') && r.request().method() === 'POST'
      );
      await button(page, `${alternative.letra}: ${alternative.texto}`).click();
      const response = await answered;
      expect(response.ok()).toBeTruthy();
      const result: RespostaQuiz = await response.json();
      points += result.pontosGanhos;
      await expect(
        text(page, result.acertou ? 'RESPOSTA CORRETA!' : 'RESPOSTA INCORRETA!')
      ).toBeVisible();
      await button(page, 'CONTINUAR').click();
    }
    await expect(button(page, 'Acessar Quiz')).toBeVisible();
    const after = await get<Clube>(api, path);
    expect(after.ranking.find((r) => r.usuario.id === me.id)?.pontos).toBe(
      initialPoints + points
    );
    expect((await get<Perfil>(api, '/api/me/perfil')).xpAtual).toBe(
      me.xpAtual + quiz.recompensaXp
    );
    expect((await get<Quiz>(api, `${path}/quiz`)).proximaPergunta).toBeNull();
  });

  test('Criar clube: livro e dois membros #148', async ({ page, api }) => {
    const { itens: membros } = await get<{
      itens: { id: string; nome: string; username: string }[];
    }>(api, '/api/busca?tipo=usuarios&q=a');
    expect(membros.length).toBeGreaterThanOrEqual(2);
    await home(page);
    await button(page, 'Criar clube').click();
    await page.getByLabel('Nome do clube', { exact: true }).fill('Clube E2E');
    await page.getByLabel('Título do Livro', { exact: true }).fill('Hobbit');
    await button(page, /^O Hobbit,/).click();
    for (const membro of membros.slice(0, 2)) {
      await page
        .getByLabel('Adicionar membros', { exact: true })
        .fill(membro.username);
      await button(
        page,
        `Adicionar ${membro.nome}, @${membro.username}`
      ).click();
    }
    const created = page.waitForResponse(
      (r) => r.url().endsWith('/api/clubes') && r.request().method() === 'POST'
    );
    await page
      .getByLabel('Criar clube de leitura', { exact: true })
      .getByRole('button', { name: 'Criar clube', exact: true })
      .click();
    const response = await created;
    expect(response.status()).toBe(201);
    const { id } = await response.json();
    await expect(
      page.getByRole('alert').filter({ hasText: 'Clube criado com sucesso!' })
    ).toBeVisible();
    await button(page, 'Fechar').click();
    await button(page, /^Clube E2E/).click();
    const clube = await get<Clube>(api, `/api/clubes/${id}`);
    expect(clube.meuCargo).toBe('administrador');
    expect(clube.ranking).toHaveLength(3);
    expect(clube.ranking.every((r) => r.pontos === 0)).toBeTruthy();
    for (const membro of membros.slice(0, 2))
      expect(
        clube.ranking.some((r) => r.usuario.id === membro.id)
      ).toBeTruthy();
  });

  test('Desafios: sistema e amigo por páginas #38 #198', async ({
    page,
    api,
  }) => {
    const { oponentes } = await get<OponentesResponse>(
      api,
      '/api/desafios/oponentes'
    );
    expect(oponentes.length).toBeGreaterThan(0);
    await home(page);
    await tab(page, 'Desafios');
    await expect(text(page, 'Desafio semanal')).toBeVisible();
    await expect(text(page, 'Desafio ativo')).toBeVisible();
    await button(page, 'Desafiar Amigo').click();
    await button(page, oponentes[0].username).click();
    const saved = page.waitForResponse(
      (r) =>
        r.url().endsWith('/api/desafios') && r.request().method() === 'POST'
    );
    await button(page, 'Enviar Desafio').click();
    const response = await saved;
    expect(response.ok()).toBeTruthy();
    const result = await response.json();
    expect(result.tipoMeta).toBe('paginas');
    expect(result.status).toBe('em_andamento');
    expect(result.progresso).toEqual({ voce: 0, oponente: 0 });
    await expect(button(page, 'Desafiar Amigo')).toBeVisible();
    await expect(text(page, result.descricao).first()).toBeVisible();
  });

  test('Perfil: conquistas, mascote e nome persistido #99 #100 #164 #165', async ({
    page,
    api,
  }) => {
    const { mascotes } = await get<{ mascotes: Mascote[] }>(
      api,
      '/api/me/mascotes'
    );
    const escolhido = mascotes.find((m) => m.desbloqueado && !m.escolhido)!;
    expect(escolhido).toBeTruthy();
    await home(page);
    await tab(page, 'Perfil');
    await button(page, 'Ver mais conquistas').click();
    await expect(text(page, 'Minhas Conquistas')).toBeVisible();
    await button(page, 'Voltar').click();
    await button(page, 'Mascotes').click();
    await button(page, escolhido.nome).click();
    const mascotSaved = page.waitForResponse(
      (r) =>
        r.url().endsWith('/api/me/mascote') && r.request().method() === 'PUT'
    );
    await button(page, 'Concluir').click();
    expect((await mascotSaved).ok()).toBeTruthy();
    await expect(button(page, 'Configurações')).toBeVisible();
    expect((await get<Perfil>(api, '/api/me/perfil')).mascote?.nome).toBe(
      escolhido.nome
    );
    await button(page, 'Configurações').click();
    await page.getByLabel('Nome', { exact: true }).fill('Leitor E2E');
    await button(page, 'Concluir').click();
    await expect(text(page, 'Leitor E2E')).toBeVisible();
    await page.reload();
    await expect(text(page, 'Leitor E2E')).toBeVisible();
    expect((await get<Perfil>(api, '/api/me/perfil')).nome).toBe('Leitor E2E');
  });

  test('Erro: Feed permite recuperar a chamada real #222', async ({ page }) => {
    await page.route('**/api/feed/comunidades', (route) =>
      route.fulfill({
        status: 503,
        contentType: 'text/html',
        body: 'indisponível',
      })
    );
    await page.goto('/');
    await expect(
      text(page, 'Não foi possível carregar. Tente novamente.')
    ).toBeVisible();
    await page.unroute('**/api/feed/comunidades');
    await button(page, 'Tentar de novo').click();
    await expect(text(page, 'Meus clubes')).toBeVisible();
    await expect(
      text(page, 'Não foi possível carregar. Tente novamente.')
    ).not.toBeVisible();
  });
});
