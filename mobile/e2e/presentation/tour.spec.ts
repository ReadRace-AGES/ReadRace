import { test, expect } from '@playwright/test';

// Separate read-only tour: never imports the seed fixture or resets a remote database.
test('Apresentação: login e cinco áreas @presentation', async ({
  page,
}, testInfo) => {
  const button = (name: string) =>
    page.getByRole('button', { name, exact: true });
  await page.goto('/');
  await button('ENTRAR').click();
  await page.getByPlaceholder('Email').fill(process.env.E2E_EMAIL!);
  await page.getByPlaceholder('Senha').fill(process.env.E2E_PASSWORD!);
  await button('ENTRAR').click();
  await expect(button('Feed')).toBeVisible();
  for (const [index, area] of [
    'Feed',
    'Meus Livros',
    'Buscar',
    'Desafios',
    'Perfil',
  ].entries()) {
    await button(area).click();
    await expect(page).toHaveURL(
      new RegExp(['feed', 'meus-livros', 'buscar', 'desafios', 'perfil'][index])
    );
    await page.evaluate(async () => {
      await document.fonts.ready;
    });
    await expect(page.getByRole('progressbar')).toHaveCount(0);
    await page.screenshot({
      path: testInfo.outputPath(`${index + 1}-${area}.png`),
      fullPage: true,
    });
  }
});
