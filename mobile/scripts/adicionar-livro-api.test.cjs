const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');

function load(file, dependencies = {}) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      jsx: ts.JsxEmit.ReactJSX,
    },
  });
  const module = { exports: {} };
  new Function('require', 'module', 'exports', outputText)(
    (name) => dependencies[name] ?? require(name),
    module,
    module.exports
  );
  return module.exports;
}

function volume(id, overrides = {}) {
  return {
    id,
    volumeInfo: {
      title: `Título ${id}`,
      authors: ['Autora Um', 'Autor Dois'],
      industryIdentifiers: [{ type: 'ISBN_13', identifier: '9786586490077' }],
      pageCount: 256,
      imageLinks: { thumbnail: 'https://capa/thumb.jpg' },
      ...overrides,
    },
  };
}

test('buscarCatalogo: exclui volumes sem ISBN ou sem páginas, mapeia o resto', async () => {
  let urlChamada;
  const { buscarCatalogo } = load('src/features/adicionarLivro/api.ts', {
    '@/api/client': {
      apiGet: async (url) => {
        urlChamada = url;
        return {
          items: [
            volume('com-tudo'),
            volume('sem-isbn', { industryIdentifiers: [] }),
            volume('sem-paginas', { pageCount: undefined }),
            volume('isbn-nulo', { industryIdentifiers: undefined }),
          ],
        };
      },
    },
  });

  const resultados = await buscarCatalogo('dom casmurro');

  assert.equal(urlChamada, '/api/books/volumes?q=dom%20casmurro');
  assert.deepEqual(
    resultados.map((r) => r.volumeId),
    ['com-tudo']
  );
  assert.deepEqual(resultados[0], {
    volumeId: 'com-tudo',
    titulo: 'Título com-tudo',
    autor: 'Autora Um, Autor Dois',
    capaUrl: 'https://capa/thumb.jpg',
    isbn: '9786586490077',
    paginas: 256,
  });
});

test('buscarCatalogo: prefere ISBN_13; sem ele, usa o ISBN_10', async () => {
  const { buscarCatalogo } = load('src/features/adicionarLivro/api.ts', {
    '@/api/client': {
      apiGet: async () => ({
        items: [
          volume('so-isbn10', {
            industryIdentifiers: [
              { type: 'ISBN_10', identifier: '8535910663' },
            ],
          }),
          volume('os-dois', {
            industryIdentifiers: [
              { type: 'ISBN_10', identifier: '8535910663' },
              { type: 'ISBN_13', identifier: '9788535910663' },
            ],
          }),
        ],
      }),
    },
  });

  const resultados = await buscarCatalogo('termo');

  assert.equal(
    resultados.find((r) => r.volumeId === 'so-isbn10').isbn,
    '8535910663'
  );
  assert.equal(
    resultados.find((r) => r.volumeId === 'os-dois').isbn,
    '9788535910663'
  );
});

test('buscarCatalogo: sem autores ou sem capa, devolve null em vez de quebrar', async () => {
  const { buscarCatalogo } = load('src/features/adicionarLivro/api.ts', {
    '@/api/client': {
      apiGet: async () => ({
        items: [
          volume('sem-extras', { authors: undefined, imageLinks: undefined }),
        ],
      }),
    },
  });

  const [resultado] = await buscarCatalogo('termo');
  assert.equal(resultado.autor, null);
  assert.equal(resultado.capaUrl, null);
});

test('buscarCatalogo: resposta sem items devolve lista vazia', async () => {
  const { buscarCatalogo } = load('src/features/adicionarLivro/api.ts', {
    '@/api/client': { apiGet: async () => ({}) },
  });

  assert.deepEqual(await buscarCatalogo('termo'), []);
});

test('adicionarNaBiblioteca: envia volumeId e lista no corpo, via POST', async () => {
  let metodo, corpo, caminho;
  const { adicionarNaBiblioteca } = load('src/features/adicionarLivro/api.ts', {
    '@/api/client': {
      apiRequest: async (url, init) => {
        caminho = url;
        metodo = init.method;
        corpo = init.body;
        return {
          livroId: '1',
          titulo: 'X',
          capaUrl: null,
          status: 'desejo',
          favorito: false,
        };
      },
    },
  });

  const resultado = await adicionarNaBiblioteca('v1', 'desejo');

  assert.equal(caminho, '/api/biblioteca');
  assert.equal(metodo, 'POST');
  assert.deepEqual(JSON.parse(corpo), { volumeId: 'v1', lista: 'desejo' });
  assert.equal(resultado.status, 'desejo');
});
