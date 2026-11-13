import { test, expect, get, home, button, text, starts } from '../support/test';
import type {
  Comunidade,
  ComunidadeCriada,
} from '../../src/features/comunidade/api';
import type { FeedComunidades } from '../../src/features/feed/api';

const enviarComunidade = (page: import('@playwright/test').Page) =>
  page
    .getByRole('dialog')
    .getByRole('button', { name: 'Criar comunidade', exact: true });

async function captura(page: import('@playwright/test').Page, nome: string) {
  const path = `e2e-artifacts/${nome}`;
  // O app rola dentro de ScrollViews; a altura do documento pode variar durante
  // a transição de um Modal. A captura deve preservar o viewport completo.
  const png = await page.screenshot({
    path,
    fullPage: false,
    animations: 'disabled',
  });
  expect(png.readUInt32BE(20)).toBe(page.viewportSize()!.height);
  await test.info().attach(nome, { path, contentType: 'image/png' });
}

test.describe('Criação de comunidade @regression', () => {
  test('cria, persiste, publica e aparece no feed; formulário em 320 e 390 px', async ({
    page,
    api,
  }) => {
    await home(page);
    await button(page, 'Criar comunidade').click();
    const enviar = enviarComunidade(page);
    await expect(enviar).toBeDisabled();
    await page
      .getByLabel('Nome da comunidade', { exact: true })
      .fill('  Entre páginas e cafés  ');
    await page
      .getByLabel('Descrição da comunidade', { exact: true })
      .fill(
        'Um encontro para quem gosta de descobrir livros e compartilhar boas histórias.'
      );
    await expect(enviar).toBeEnabled();
    await page.setViewportSize({ width: 320, height: 844 });
    await expect(enviar).toBeInViewport();
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth)
    ).toBeLessThanOrEqual(320);
    await captura(page, 'comunidade-formulario-320.png');
    await page.setViewportSize({ width: 390, height: 844 });
    await expect(
      page.getByRole('heading', { name: 'Crie sua comunidade' })
    ).toBeVisible();
    await captura(page, 'comunidade-formulario.png');
    const criada = page.waitForResponse(
      (r) =>
        r.url().endsWith('/api/comunidades') && r.request().method() === 'POST'
    );
    await enviar.click();
    const resposta = await criada;
    expect(resposta.status()).toBe(201);
    const comunidade: ComunidadeCriada = await resposta.json();
    expect(comunidade).toMatchObject({
      nome: 'Entre páginas e cafés',
      sigo: true,
      seguidores: 1,
      imagemUrl: null,
    });
    await expect(page).toHaveURL(
      new RegExp(`/feed-comunidade/${comunidade.id}$`)
    );
    await expect(text(page, 'Entre páginas e cafés')).toBeVisible();
    await expect(text(page, comunidade.descricao!)).toBeVisible();
    await expect(button(page, 'Escrever post')).toBeVisible();
    await expect(
      text(page, 'Esta comunidade ainda não tem postagens.')
    ).toBeVisible();
    await expect(
      text(page, 'Comunidade criada! Boas conversas começam aqui.')
    ).toBeVisible();
    // A captura mostra a página criada depois de dispensar a confirmação temporária.
    await expect(
      text(page, 'Comunidade criada! Boas conversas começam aqui.')
    ).not.toBeVisible({ timeout: 15_000 });
    await captura(page, 'comunidade-criada.png');
    await button(page, 'Escrever post').click();
    await page
      .getByLabel('Texto do post', { exact: true })
      .fill('Qual livro merece entrar na nossa próxima conversa?');
    const publicada = page.waitForResponse(
      (r) =>
        r.url().endsWith(`/comunidades/${comunidade.id}/posts`) &&
        r.request().method() === 'POST'
    );
    await button(page, 'Publicar').click();
    expect((await publicada).status()).toBe(201);
    await expect(
      text(page, 'Qual livro merece entrar na nossa próxima conversa?')
    ).toBeVisible();
    await page.reload();
    await expect(
      text(page, 'Qual livro merece entrar na nossa próxima conversa?')
    ).toBeVisible();
    const persistida = await get<Comunidade>(
      api,
      `/api/comunidades/${comunidade.id}`
    );
    expect(persistida.posts).toHaveLength(1);
    expect(persistida.descricao).toBe(comunidade.descricao);
    // O criador é o único administrador: a regra existente impede sua saída.
    expect(
      (
        await api.delete(`/api/comunidades/${comunidade.id}/membros/eu`)
      ).status()
    ).toBe(409);
    await button(page, 'Voltar').click();
    await expect(button(page, starts('Entre páginas e cafés'))).toBeVisible();
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    expect(feed.comunidades.filter((c) => c.id === comunidade.id)).toHaveLength(
      1
    );
  });

  test('valida link, recupera capa indisponível e cria com imagem', async ({
    page,
    api,
  }) => {
    const imagem = 'https://images.example.test/comunidade.png';
    await page.route(imagem, (route) =>
      route.fulfill({
        contentType: 'image/png',
        path: 'assets/images/perfil-cenario.png',
      })
    );
    await page.route('https://images.example.test/ausente.png', (route) =>
      route.fulfill({ status: 404 })
    );
    await home(page);
    await button(page, 'Criar comunidade').click();
    await page
      .getByLabel('Nome da comunidade', { exact: true })
      .fill('Leituras ao entardecer');
    await button(page, 'Adicionar capa por link').click();
    const link = page.getByLabel('Link da capa', { exact: true });
    await link.fill('http://images.example.test/insegura.png');
    await expect(enviarComunidade(page)).toBeDisabled();
    await expect(
      text(
        page,
        'Use um link HTTPS válido, sem usuário ou senha (até 2.048 caracteres).'
      )
    ).toBeVisible();
    await link.fill('https://images.example.test/ausente.png');
    await expect(
      text(page, 'A imagem não carregou. Confira o link ou use a capa padrão.')
    ).toBeVisible();
    await link.fill(imagem);
    await expect(
      page.getByRole('img', {
        name: 'Prévia da capa da comunidade',
        exact: true,
      })
    ).toBeVisible();
    const criada = page.waitForResponse(
      (r) =>
        r.url().endsWith('/api/comunidades') && r.request().method() === 'POST'
    );
    await enviarComunidade(page).click();
    const resposta = await criada;
    expect(resposta.status()).toBe(201);
    const comunidade: ComunidadeCriada = await resposta.json();
    await expect(page).toHaveURL(
      new RegExp(`/feed-comunidade/${comunidade.id}$`)
    );
    await expect(text(page, 'Leituras ao entardecer')).toBeVisible();
    await expect(button(page, 'Escrever post')).toBeVisible();
    expect(
      (await get<Comunidade>(api, `/api/comunidades/${comunidade.id}`))
        .imagemUrl
    ).toBe(imagem);
    await expect(
      text(page, 'Comunidade criada! Boas conversas começam aqui.')
    ).toBeVisible();
    await expect(
      text(page, 'Comunidade criada! Boas conversas começam aqui.')
    ).not.toBeVisible({ timeout: 15_000 });
    await captura(page, 'comunidade-com-capa.png');
    await page.unroute(imagem);
    await page.route(imagem, (route) => route.fulfill({ status: 404 }));
    await page.reload();
    await expect(text(page, 'HISTÓRIAS QUE NOS APROXIMAM')).toBeVisible();
    await expect(button(page, 'Escrever post')).toBeVisible();
  });

  test('falha recuperável preserva formulário e nova tentativa cria uma única comunidade', async ({
    page,
    api,
  }) => {
    await home(page);
    await button(page, 'Criar comunidade').click();
    await page
      .getByLabel('Nome da comunidade', { exact: true })
      .fill('Histórias compartilhadas');
    await page
      .getByLabel('Descrição da comunidade', { exact: true })
      .fill('Um espaço para conversar.');
    await page.route(
      '**/api/comunidades',
      (route) =>
        route.fulfill({
          status: 503,
          json: {
            code: 'UNAVAILABLE',
            message: 'Serviço temporariamente indisponível. Tente novamente.',
          },
        }),
      { times: 1 }
    );
    await enviarComunidade(page).click();
    await expect(page.getByRole('dialog').getByRole('alert')).toHaveText(
      'Serviço temporariamente indisponível. Tente novamente.'
    );
    await expect(
      page.getByLabel('Nome da comunidade', { exact: true })
    ).toHaveValue('Histórias compartilhadas');
    await expect(
      page.getByLabel('Descrição da comunidade', { exact: true })
    ).toHaveValue('Um espaço para conversar.');
    await enviarComunidade(page).click();
    await expect(text(page, 'Histórias compartilhadas')).toBeVisible();
    const feed = await get<FeedComunidades>(api, '/api/feed/comunidades');
    expect(
      feed.comunidades.filter((c) => c.nome === 'Histórias compartilhadas')
    ).toHaveLength(1);
  });
});
