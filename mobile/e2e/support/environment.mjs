import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { setTimeout as delay } from 'node:timers/promises';

export const mobile = fileURLToPath(new URL('../../', import.meta.url));
const composeFile = fileURLToPath(
  new URL('../../../infra/e2e/compose.yml', import.meta.url)
);

export function run(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: mobile,
      stdio: 'inherit',
      ...options,
    });
    child.on('error', reject);
    child.on('exit', (code, signal) =>
      code === 0
        ? resolve()
        : reject(new Error(`${command} ${args.join(' ')}: ${signal ?? code}`))
    );
  });
}

export function compose(...args) {
  const project = process.env.E2E_PROJECT;
  // No default project: a direct playwright invocation must never reset another database.
  if (!project || !/^readrace-e2e-[a-z0-9-]+$/.test(project)) {
    throw new Error(
      'Use npm run test:e2e: o runner cria um projeto Docker exclusivo.'
    );
  }
  return run('docker', ['compose', '-p', project, '-f', composeFile, ...args]);
}

export async function ready(url, timeout = 120_000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(3000) });
      if (response.ok) return;
    } catch {
      /* API starting. */
    }
    await delay(500);
  }
  throw new Error(`Readiness excedeu ${timeout} ms: ${url}`);
}

export async function snapshot() {
  await compose('stop', 'api');
  await compose(
    'exec',
    '-T',
    'db',
    'createdb',
    '-U',
    'e2e',
    '-T',
    'readrace_e2e',
    'readrace_baseline'
  );
  await compose('start', 'api');
  await ready(`${process.env.E2E_API_URL}/actuator/health`);
}

export async function reset() {
  if (process.env.E2E_MODE !== 'seed' || process.env.E2E_MANAGED !== '1') {
    throw new Error(
      'Reset permitido somente no ambiente descartável do runner.'
    );
  }
  if (process.env.E2E_NATIVE_DIR) {
    const { resetNative } = await import('./native.mjs');
    return resetNative();
  }
  await compose('stop', 'api');
  await compose(
    'exec',
    '-T',
    'db',
    'dropdb',
    '-U',
    'e2e',
    '--force',
    'readrace_e2e'
  );
  await compose(
    'exec',
    '-T',
    'db',
    'createdb',
    '-U',
    'e2e',
    '-T',
    'readrace_baseline',
    'readrace_e2e'
  );
  await compose('start', 'api');
  await ready(`${process.env.E2E_API_URL}/actuator/health`);
}
