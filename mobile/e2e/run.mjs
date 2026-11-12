import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import {
  compose,
  mobile,
  ready,
  run,
  snapshot,
} from './support/environment.mjs';

const presentation = process.argv.includes('--presentation');
const native = process.argv.includes('--native');
const args = process.argv
  .slice(2)
  .filter((arg) => !['--presentation', '--native'].includes(arg));
process.env.E2E_MODE = presentation ? 'presentation' : 'seed';
process.env.EXPO_NO_DOTENV = '1';
process.env.CI ??= '1';
const cli = (path, ...args) =>
  run(process.execPath, [
    fileURLToPath(new URL(path, import.meta.url)),
    ...args,
  ]);
let infraStarted = false;
let cleaning;
async function cleanup() {
  if (!infraStarted) return;
  if (native)
    return (cleaning ??= import('./support/native.mjs').then((m) =>
      m.stopNative()
    ));
  return (cleaning ??= compose('down', '--volumes', '--remove-orphans'));
}
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.once(signal, () => {
    cleanup().finally(() => process.exit(130));
  });
}

try {
  if (presentation) {
    for (const key of [
      'E2E_API_URL',
      'E2E_EMAIL',
      'E2E_PASSWORD',
      'EXPO_PUBLIC_COGNITO_DOMAIN',
      'EXPO_PUBLIC_COGNITO_CLIENT_ID',
    ]) {
      if (!process.env[key]) throw new Error(`Apresentação exige ${key}.`);
    }
    if (new URL(process.env.E2E_API_URL).protocol !== 'https:') {
      throw new Error('A apresentação autenticada exige uma API HTTPS.');
    }
  } else {
    process.env.E2E_PROJECT = `readrace-e2e-${Date.now()}-${process.pid}`;
    process.env.E2E_MANAGED = '1';
    process.env.E2E_API_URL = `http://127.0.0.1:${process.env.E2E_API_PORT ?? '18080'}`;
    process.env.EXPO_PUBLIC_COGNITO_DOMAIN = '';
    process.env.EXPO_PUBLIC_COGNITO_CLIENT_ID = '';
    infraStarted = true;
    if (native) {
      await (await import('./support/native.mjs')).startNative();
    } else {
      await compose('up', '-d', '--build', '--wait', '--wait-timeout', '180');
      await ready(`${process.env.E2E_API_URL}/actuator/health`);
      await snapshot();
    }
  }
  process.env.EXPO_PUBLIC_API_URL = process.env.E2E_API_URL;
  await cli('../node_modules/expo/bin/cli', 'export', '--platform', 'web');
  await cli('../node_modules/@playwright/test/cli.js', 'test', ...args);
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
} finally {
  if (infraStarted && !native) {
    // Capture logs before removing the disposable services, including failed startups.
    await mkdir(`${mobile}/e2e-artifacts`, { recursive: true });
    const { open } = await import('node:fs/promises');
    const log = await open(`${mobile}/e2e-artifacts/services.log`, 'w');
    try {
      const { spawn } = await import('node:child_process');
      await new Promise((resolve) => {
        const child = spawn(
          'docker',
          [
            'compose',
            '-p',
            process.env.E2E_PROJECT,
            '-f',
            fileURLToPath(
              new URL('../../infra/e2e/compose.yml', import.meta.url)
            ),
            'logs',
            '--no-color',
          ],
          { stdio: ['ignore', log.fd, log.fd] }
        );
        child.on('error', resolve);
        child.on('exit', resolve);
      });
    } finally {
      await log.close();
    }
  }
  await cleanup().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
  await mkdir(`${mobile}/e2e-artifacts`, { recursive: true });
  await writeFile(
    `${mobile}/e2e-artifacts/run.json`,
    JSON.stringify(
      {
        mode: process.env.E2E_MODE,
        at: new Date().toISOString(),
        commit: process.env.GITHUB_SHA ?? null,
        exitCode: process.exitCode ?? 0,
      },
      null,
      2
    )
  );
}
