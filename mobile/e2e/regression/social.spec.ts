import {
  test,
  expect,
  get,
  home,
  tab,
  button,
  text,
  starts,
} from '../support/test';
import type { FeedComunidades, FeedSocial } from '../../src/features/feed/api';
import type { Clube, ClubeDisponivel } from '../../src/features/clube/api';
import type { Comunidade } from '../../src/features/comunidade/api';

test.describe('Social e administração @regression', () => {
  test('Social abre autor e livro e preserva origem #197 #204', async ({
    page,
    api,
  }) => {
    const feed = await get<FeedSocial>(api, '/api/feed/social');
    expect(feed.atividades.length).toBeGreaterThan(0);
    const atividade = feed.atividades[0];
    await home(page);
    await page.getByRole('tab', { name: 'Social', exact: true }).click();
    await button(page, `Abrir o perfil de ${atividade.usuario.nome}`)
      .first()
      .click();
    await expect(text(page, atividade.usuario.nome)).toBeVisible();
    await button(page, 'Voltar').click();
    await expect(page).toHaveURL(/feed/);
    await button(page, `Abrir o livro ${atividade.livro.titulo}`)
      .first()
      .click();
    await expect(button(page, 'Minhas listas')).toBeVisible();
    await button(page, 'Voltar').click();
    await expect(page).toHaveURL(/feed/);
  });

  test('Comunidade sugerida passa para minhas comunidades #235', async ({
    page,
    api,
  }) => {
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    const comunidade = feed.comunidadesSugeridas[0];
    expect(comunidade).toBeTruthy();
    await home(page);
    await button(page, `Participar: ${comunidade.nome}`).click();
    await expect(
      button(page, `Participar: ${comunidade.nome}`)
    ).not.toBeVisible();
    expect(
      (
        await get<FeedComunidades>(api, '/api/feed/comunidades')
      ).comunidades.some((c) => c.id === comunidade.id)
    ).toBeTruthy();
    await button(page, starts(comunidade.nome)).click();
    await button(page, 'Escrever post').click();
    await page
      .getByLabel('Texto do post', { exact: true })
      .fill('Post E2E na comunidade sugerida');
    const published = page.waitForResponse(
      (r) =>
        r.url().endsWith(`/comunidades/${comunidade.id}/posts`) &&
        r.request().method() === 'POST'
    );
    await button(page, 'Publicar').click();
    expect((await published).ok()).toBeTruthy();
    await expect(
      page.getByLabel('Texto do post', { exact: true })
    ).not.toBeVisible();
    await expect(text(page, 'Post E2E na comunidade sugerida')).toBeVisible();
    expect(
      (
        await get<Comunidade>(api, `/api/comunidades/${comunidade.id}`)
      ).posts.some((p) => p.texto === 'Post E2E na comunidade sugerida')
    ).toBeTruthy();
  });

  test('Pedido de entrada não transforma solicitante em membro #234', async ({
    page,
    api,
  }) => {
    const { clubes } = await get<{ clubes: ClubeDisponivel[] }>(
      api,
      '/api/clubes'
    );
    const clube = clubes.find((c) => c.situacao === 'nenhuma')!;
    expect(clube).toBeTruthy();
    await home(page);
    await button(page, 'Ver todos').click();
    // Scope by the group name: the list has one action per group.
    await button(page, starts(clube.nome)).click();
    await button(page, 'Pedir para entrar').click();
    await expect(button(page, 'Pedido enviado')).toBeVisible();
    expect((await get<Clube>(api, `/api/clubes/${clube.id}`)).situacao).toBe(
      'pendente'
    );
    expect(
      (await api.get(`/api/clubes/${clube.id}/solicitacoes`)).status()
    ).toBe(403);
  });

  test('Líder configura e publica quiz #161', async ({ page, api }) => {
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    const clubes = await Promise.all(
      feed.clubes.map((c) => get<Clube>(api, `/api/clubes/${c.id}`))
    );
    const clube = clubes.find((c) => c.meuCargo === 'administrador')!;
    await home(page);
    await button(page, starts(clube.nome)).click();
    await button(page, 'Acessar Quiz').click();
    await page.getByLabel('Título do Quiz', { exact: true }).fill('Quiz E2E');
    await button(page, 'Próximo: Adicionar Perguntas').click();
    await page
      .getByLabel('Pergunta', { exact: true })
      .fill('Qual alternativa é correta?');
    for (const letra of ['A', 'B', 'C', 'D'])
      await page
        .getByLabel(`Opção ${letra}`, { exact: true })
        .fill(`Alternativa ${letra}`);
    await expect(button(page, '+ Adicionar à Lista')).toBeDisabled();
    await page
      .getByLabel('Marcar opção A como correta', { exact: true })
      .click();
    await button(page, '+ Adicionar à Lista').click();
    const saved = page.waitForResponse(
      (r) => r.url().endsWith('/quiz') && r.request().method() === 'POST'
    );
    await button(page, 'Finalizar Quiz').click();
    expect((await saved).ok()).toBeTruthy();
    await expect(button(page, 'Acessar Quiz')).toBeVisible();
    expect(
      (await get<Clube>(api, `/api/clubes/${clube.id}`)).temQuizAtivo
    ).toBeTruthy();
  });

  test('Configurações gerais não oferece ações sem destino #249', async ({
    page,
  }) => {
    await home(page);
    await tab(page, 'Perfil');
    await button(page, 'Configurações').click();
    await button(page, /^Configurações gerais/).click();
    await expect(button(page, 'Desconectar')).toBeVisible();
    await expect(text(page, 'Notificações')).not.toBeVisible();
    await expect(text(page, 'Fale conosco')).not.toBeVisible();
    await button(page, 'Fechar').click();
    await expect(page.getByLabel('Nome', { exact: true })).toBeVisible();
  });
});
