import {
  test as base,
  expect,
  type APIRequestContext,
  type Page,
} from '@playwright/test';
import { reset } from './environment.mjs';

export const test = base.extend<{
  api: APIRequestContext;
  cleanDatabase: void;
}>({
  cleanDatabase: [
    async ({}, use) => {
      await reset();
      await use();
    },
    { auto: true, timeout: 180_000 },
  ],
  api: async ({ playwright, cleanDatabase }, use) => {
    void cleanDatabase;
    const api = await playwright.request.newContext({
      baseURL: process.env.E2E_API_URL,
    });
    await use(api);
    await api.dispose();
  },
});
export { expect };

export async function get<T>(api: APIRequestContext, path: string): Promise<T> {
  const response = await api.get(path);
  expect(
    response.ok(),
    `${path}: ${response.status()} ${await response.text()}`
  ).toBeTruthy();
  return response.json();
}

export const button = (page: Page, name: string | RegExp) =>
  page.getByRole('button', { name, exact: typeof name === 'string' });
export const text = (page: Page, value: string | RegExp) =>
  page
    .getByText(value, { exact: typeof value === 'string' })
    .and(page.locator(':not([aria-hidden="true"], [aria-hidden="true"] *)'))
    .filter({ visible: true });
export async function tab(page: Page, name: string) {
  await button(page, name).click();
}
export async function home(page: Page) {
  await page.goto('/');
  await expect(button(page, 'Feed')).toBeVisible();
  await expect(text(page, 'Meus Grupos')).toBeVisible();
}
export const starts = (value: string) =>
  new RegExp(`^${value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`);
