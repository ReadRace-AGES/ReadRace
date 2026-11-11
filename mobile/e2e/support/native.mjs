import { spawn } from 'node:child_process';
import { mkdtemp, open, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { run, ready } from './environment.mjs';

const backend = fileURLToPath(new URL('../../../backend/', import.meta.url));
const pg = (name) =>
  join(
    process.env.E2E_PG_BIN ?? '',
    `${name}${process.platform === 'win32' ? '.exe' : ''}`
  );
const connection = () => [
  '-h',
  '127.0.0.1',
  '-p',
  process.env.E2E_DB_PORT ?? '15432',
  '-U',
  'e2e',
];
let directory, api, log;
export async function startNative() {
  if (!process.env.E2E_PG_BIN)
    throw new Error('--native exige E2E_PG_BIN (pasta bin do PostgreSQL).');
  const windows = process.platform === 'win32';
  await run(
    windows ? 'cmd.exe' : './mvnw',
    windows
      ? ['/d', '/c', 'mvnw.cmd', 'package', '-DskipTests', '-B']
      : ['package', '-DskipTests', '-B'],
    { cwd: backend }
  );
  directory = await mkdtemp(join(tmpdir(), 'readrace-e2e-'));
  process.env.E2E_NATIVE_DIR = directory;
  await run(pg('initdb'), [
    '-D',
    join(directory, 'data'),
    '-U',
    'e2e',
    '--auth-local=trust',
    '--auth-host=trust',
    '--encoding=UTF8',
    '--locale=C',
  ]);
  await run(pg('pg_ctl'), [
    '-D',
    join(directory, 'data'),
    '-l',
    join(directory, 'postgres.log'),
    '-o',
    `-h 127.0.0.1 -p ${process.env.E2E_DB_PORT ?? '15432'}`,
    '-w',
    'start',
  ]);
  await run(pg('createdb'), [...connection(), 'readrace_e2e']);
  log = await open(join(directory, 'api.log'), 'w');
  api = spawn(
    process.env.JAVA_HOME
      ? join(process.env.JAVA_HOME, 'bin', windows ? 'java.exe' : 'java')
      : 'java',
    ['-jar', join(backend, 'target/api-0.0.1-SNAPSHOT.jar')],
    {
      env: {
        ...process.env,
        DB_URL: `jdbc:postgresql://127.0.0.1:${process.env.E2E_DB_PORT ?? '15432'}/readrace_e2e?prepareThreshold=0`,
        DB_USER: 'e2e',
        DB_PASSWORD: '',
        SERVER_PORT: process.env.E2E_API_PORT ?? '18080',
        AUTH_MODO: 'seed',
        SPRING_PROFILES_ACTIVE: 'dev',
      },
      stdio: ['ignore', log.fd, log.fd],
    }
  );
  api.on('error', (error) => console.error(error.message));
  await ready(`${process.env.E2E_API_URL}/actuator/health`);
  await run(pg('pg_dump'), [
    ...connection(),
    '-Fc',
    '-f',
    join(directory, 'baseline.dump'),
    'readrace_e2e',
  ]);
}

export async function resetNative() {
  const dir = process.env.E2E_NATIVE_DIR;
  if (!dir || !resolve(dir).startsWith(resolve(tmpdir(), 'readrace-e2e-')))
    throw new Error('Diretório E2E inválido.');
  // Browser from previous test has closed. Restore atomically with the API idle.
  await run(pg('pg_restore'), [
    ...connection(),
    '--clean',
    '--if-exists',
    '--no-owner',
    '--exit-on-error',
    '--single-transaction',
    '-d',
    'readrace_e2e',
    join(dir, 'baseline.dump'),
  ]);
}

export async function stopNative() {
  if (api && api.exitCode === null) {
    await new Promise((resolve) => {
      api.once('exit', resolve);
      api.kill();
    });
  }
  await log?.close();
  if (!directory) return;
  const { copyFile, mkdir } = await import('node:fs/promises');
  const artifacts = fileURLToPath(
    new URL('../../e2e-artifacts/', import.meta.url)
  );
  await mkdir(artifacts, { recursive: true });
  for (const name of ['api.log', 'postgres.log'])
    await copyFile(join(directory, name), join(artifacts, name)).catch(
      () => {}
    );
  await run(pg('pg_ctl'), [
    '-D',
    join(directory, 'data'),
    '-m',
    'immediate',
    '-w',
    'stop',
  ]);
  if (resolve(directory).startsWith(resolve(tmpdir(), 'readrace-e2e-')))
    await rm(directory, { recursive: true, force: true });
}
