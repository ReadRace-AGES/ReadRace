const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const React = require('react');
const { create, act } = require('react-test-renderer');
globalThis.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, dependencies = {}) {
  const { outputText } = ts.transpileModule(
    fs.readFileSync(path.join(__dirname, '../src', file), 'utf8'),
    {
      compilerOptions: {
        module: ts.ModuleKind.CommonJS,
        jsx: ts.JsxEmit.ReactJSX,
      },
    }
  );
  const module = { exports: {} };
  new Function('require', 'module', 'exports', outputText)(
    (name) => dependencies[name] ?? require(name),
    module,
    module.exports
  );
  return module.exports;
}
const { ApiError, mensagemErroCarregamento } = load('api/client.ts');
const perfil = {
  id: '1',
  nome: 'Daniel',
  titulo: 'Leitor iniciante',
  nivel: 5,
  xpAtual: 2450,
  seguidores: 4,
  seguindo: 2,
  sigo: false,
  meSegue: false,
  estatisticas: {
    livrosLidos: 5,
    paginasLidas: 6231,
    sequenciaDias: 12,
    conquistas: 1,
  },
  conquistas: [
    {
      id: 'a',
      nome: 'Primeiros Passos',
      descricao: 'Primeira leitura',
      desbloqueada: true,
    },
    { id: 'b', nome: 'Lenda', descricao: 'Meta futura', desbloqueada: false },
  ],
  livrosFavoritos: [
    { id: 'livro', titulo: 'Dune', autor: 'Frank Herbert', capa: null },
  ],
};
const native = {
  View: 'View',
  Text: 'Text',
  Pressable: 'Pressable',
  ScrollView: 'ScrollView',
  ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (v) => v },
};
const dependencies = {
  'react-native': native,
  'expo-image': { Image: 'Image' },
  'expo-router': {},
  '@/theme': { ...require('../src/theme/tokens'), textStyles: {} },
  '@/components/avatar': { Avatar: 'Avatar' },
  '@/components/icons/BookIcon': { BookIcon: 'BookIcon' },
  '@/components/icons/PawIcon': { PawIcon: 'PawIcon' },
  '@/features/conquistas/IconeConquista': { IconeConquista: 'IconeConquista' },
  '@/components/toast-provider': {},
  './NivelAnel': { NivelAnel: 'NivelAnel' },
  './PerfilTopo': { PerfilTopo: 'PerfilTopo' },
  './usePerfil': {},
};

test('perfil de outro leitor abre as duas listas no stack de origem', async () => {
  for (const origem of ['busca', 'perfil']) {
    const rotas = [];
    const { PerfilScreen } = load('features/perfil/PerfilScreen.tsx', {
      ...dependencies,
      'expo-router': {
        useFocusEffect: () => {},
        useRouter: () => ({ push: (rota) => rotas.push(rota) }),
      },
      '@/components/toast-provider': {
        useToastContext: () => ({
          showToast: () => assert.fail('Não deve exibir aviso'),
        }),
      },
      './usePerfil': {
        usePerfil: () => ({
          estado: { situacao: 'sucesso', dados: perfil },
          recarregar: () => {},
        }),
      },
      '@/components/Card': { Card: 'Card' },
      '@/components/AppHeader': { AppHeader: 'AppHeader' },
      '@/components/BookCover': { BookCover: 'BookCover' },
      '@/components/EmptyState': { EmptyState: 'EmptyState' },
      '@/components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    });
    let tree;
    await act(async () => {
      tree = create(
        React.createElement(PerfilScreen, { usuarioId: '1', origem })
      );
    });
    try {
      const botoes = tree.root
        .findAllByType('Pressable')
        .filter((b) => b.props.accessibilityLabel !== 'Ver mais conquistas');
      await act(async () => botoes.forEach((b) => b.props.onPress()));
      assert.deepEqual(
        rotas,
        ['seguidores', 'seguindo'].map((tipo) => ({
          pathname: `/(tabs)/(${origem})/leitores/[usuarioId]`,
          params: { usuarioId: '1', tipo },
        }))
      );
    } finally {
      await act(async () => tree.unmount());
    }
  }
});
for (const name of [
  'Card',
  'AppHeader',
  'BookCover',
  'EmptyState',
  'PrimaryButton',
])
  dependencies[`@/components/${name}`] = { [name]: name };

test('perfil exibe dados persistidos, conquistas bloqueadas e quatro ações de Toast', () => {
  const { PerfilConteudo } = load(
    'features/perfil/PerfilScreen.tsx',
    dependencies
  );
  let tree,
    calls = 0;
  act(() => {
    tree = create(
      React.createElement(PerfilConteudo, {
        perfil,
        onPlaceholder: () => calls++,
      })
    );
  });
  const text = JSON.stringify(tree.toJSON());
  assert.match(text, /2.450/);
  assert.match(text, /6.231/);
  assert.ok(
    tree.root
      .findAllByType('Text')
      .some(
        (node) =>
          React.Children.toArray(node.props.children).join('') ===
          '“Leitor iniciante”'
      )
  );
  assert.match(text, /Bloqueada/);
  assert.doesNotMatch(text, /Mascotes|Configurações|adicionar favorito/);
  act(() => {
    tree.root
      .findAllByType('Pressable')
      .forEach((button) => button.props.onPress());
    tree.root.findByType('BookCover').props.onPress();
  });
  assert.equal(calls, 4);
  act(() =>
    tree.update(
      React.createElement(PerfilConteudo, {
        perfil: { ...perfil, livrosFavoritos: [] },
        onPlaceholder: () => {},
      })
    )
  );
  assert.doesNotMatch(JSON.stringify(tree.toJSON()), /Livros favoritos/);
  assert.equal(tree.root.findAllByType('BookCover').length, 0);
  act(() => tree.unmount());
});

test('carregamento e erro mantêm voltar e permitem nova tentativa', () => {
  let estado = { situacao: 'carregando' };
  let retries = 0,
    backs = 0,
    tree;
  const { PerfilScreen } = load('features/perfil/PerfilScreen.tsx', {
    ...dependencies,
    'expo-router': {
      useFocusEffect: () => {},
      useRouter: () => ({ canGoBack: () => true, back: () => backs++ }),
    },
    '@/components/toast-provider': {
      useToastContext: () => ({ showToast: () => {} }),
    },
    './usePerfil': {
      usePerfil: () => ({ estado, recarregar: () => retries++ }),
    },
  });
  act(() => {
    tree = create(React.createElement(PerfilScreen, { usuarioId: '1' }));
  });
  assert.equal(tree.root.findAllByType('ActivityIndicator').length, 1);
  act(() => tree.root.findByType('AppHeader').props.onBackPress());
  assert.equal(backs, 1);
  estado = { situacao: 'erro', mensagem: 'Usuário não encontrado.' };
  act(() => tree.update(React.createElement(PerfilScreen, { usuarioId: '1' })));
  const empty = tree.root.findByType('EmptyState');
  assert.equal(empty.props.message, estado.mensagem);
  act(() => empty.props.action.props.onPress());
  assert.equal(retries, 1);
  act(() => tree.root.findByType('AppHeader').props.onBackPress());
  assert.equal(backs, 2);
  act(() => tree.unmount());
});

test('próprio perfil fica sem voltar e "Ver mais" abre Minhas Conquistas', () => {
  const rotas = [];
  let avisos = 0,
    tree;
  const { PerfilScreen } = load('features/perfil/PerfilScreen.tsx', {
    ...dependencies,
    'expo-router': {
      useFocusEffect: () => {},
      useRouter: () => ({ push: (rota) => rotas.push(rota) }),
    },
    '@/components/toast-provider': {
      useToastContext: () => ({ showToast: () => avisos++ }),
    },
    './usePerfil': {
      usePerfil: () => ({
        estado: { situacao: 'sucesso', dados: perfil },
        recarregar: () => {},
      }),
    },
  });
  const verMais = () =>
    tree.root
      .findAllByType('Pressable')
      .find(
        (botao) => botao.props.accessibilityLabel === 'Ver mais conquistas'
      );
  act(() => {
    tree = create(React.createElement(PerfilScreen));
  });
  assert.equal(tree.root.findAllByType('AppHeader').length, 0);
  assert.equal(tree.root.findAllByType('PerfilTopo').length, 1);
  act(() => verMais().props.onPress());
  assert.deepEqual(rotas, ['/conquistas']);
  assert.equal(avisos, 0);
  act(() => tree.update(React.createElement(PerfilScreen, { usuarioId: '1' })));
  assert.equal(tree.root.findByType('AppHeader').props.showBack, true);
  act(() => verMais().props.onPress());
  assert.deepEqual(rotas, ['/conquistas']);
  assert.equal(avisos, 1);
  act(() => tree.unmount());
});

test('próprio perfil mostra anel, Mascotes, engrenagem e slot de favorito', () => {
  const rotas = [];
  let avisos = 0,
    tree,
    dados = {
      ...perfil,
      xpNoNivel: 521,
      xpDoNivel: 833,
      mascote: { nome: 'O Cinzento', imagem: 'o-cinzento' },
    };
  const { PerfilScreen } = load('features/perfil/PerfilScreen.tsx', {
    ...dependencies,
    'expo-router': {
      useFocusEffect: () => {},
      useRouter: () => ({ push: (rota) => rotas.push(rota) }),
    },
    '@/components/toast-provider': {
      useToastContext: () => ({ showToast: () => avisos++ }),
    },
    './usePerfil': {
      usePerfil: () => ({
        estado: { situacao: 'sucesso', dados },
        recarregar: () => {},
      }),
    },
  });
  act(() => {
    tree = create(React.createElement(PerfilScreen));
  });
  const anel = tree.root.findByType('NivelAnel');
  assert.equal(anel.props.xpNoNivel, 521);
  assert.equal(anel.props.xpDoNivel, 833);
  const mascotes = tree.root
    .findAllByType('PrimaryButton')
    .find((botao) => botao.props.label === 'Mascotes');
  const botoesDeAviso = tree.root
    .findAllByType('Pressable')
    .filter(
      (botao) => botao.props.accessibilityLabel !== 'Ver mais conquistas'
    );
  const [capa, adicionar] = tree.root.findAllByType('BookCover');
  assert.equal(adicionar.props.variant, 'add-favorite');
  act(() => {
    botoesDeAviso.forEach((botao) => botao.props.onPress());
    mascotes.props.onPress();
    adicionar.props.onPress();
    tree.root.findByType('PerfilTopo').props.onConfiguracoes();
  });
  // Só o slot de favorito continua como aviso; listas e Mascotes abrem suas telas.
  assert.equal(avisos, 1);
  assert.equal(tree.root.findByType('PerfilTopo').props.mascote, 'o-cinzento');
  act(() => capa.props.onPress());
  assert.deepEqual(rotas, [
    {
      pathname: '/(tabs)/(perfil)/leitores/[usuarioId]',
      params: { usuarioId: '1', tipo: 'seguidores' },
    },
    {
      pathname: '/(tabs)/(perfil)/leitores/[usuarioId]',
      params: { usuarioId: '1', tipo: 'seguindo' },
    },
    '/mascotes',
    '/configuracoes',
    '/perfil-livro/livro',
  ]);
  dados = { ...dados, livrosFavoritos: [] };
  act(() => tree.update(React.createElement(PerfilScreen)));
  assert.match(JSON.stringify(tree.toJSON()), /Livros favoritos/);
  const capas = tree.root.findAllByType('BookCover');
  assert.equal(capas.length, 1);
  assert.equal(capas[0].props.variant, 'add-favorite');
  act(() => tree.unmount());
});

test('próprio perfil busca de novo ao voltar para a aba, sem repetir a carga inicial', () => {
  let focar,
    atualizacoes = 0,
    tree;
  const { PerfilScreen } = load('features/perfil/PerfilScreen.tsx', {
    ...dependencies,
    'expo-router': {
      useFocusEffect: (efeito) => {
        focar = efeito;
      },
      useRouter: () => ({}),
    },
    '@/components/toast-provider': {
      useToastContext: () => ({ showToast: () => {} }),
    },
    './usePerfil': {
      usePerfil: () => ({
        estado: { situacao: 'sucesso', dados: perfil },
        recarregar: () => {},
        atualizar: () => atualizacoes++,
      }),
    },
  });
  act(() => {
    tree = create(React.createElement(PerfilScreen));
  });
  act(() => focar());
  assert.equal(atualizacoes, 0);
  act(() => focar());
  assert.equal(atualizacoes, 1);
  act(() => tree.update(React.createElement(PerfilScreen, { usuarioId: '1' })));
  act(() => focar());
  assert.equal(atualizacoes, 2);
  act(() => tree.unmount());
});

test('API usa somente o usuário exibido e encaminha cancelamento', async () => {
  let request;
  const { buscarPerfil } = load('features/perfil/api.ts', {
    '@/api/client': {
      apiGet: (...args) => {
        request = args;
        return Promise.resolve(perfil);
      },
    },
  });
  const controller = new AbortController();
  await buscarPerfil('outro/id', controller.signal);
  assert.equal(request[0], '/api/usuarios/outro%2Fid/perfil');
  assert.deepEqual(request[1], { signal: controller.signal });
  await buscarPerfil(undefined, controller.signal);
  assert.equal(request[0], '/api/me/perfil');
});

test('hook cancela perfil antigo, ignora resposta atrasada e permite tentar novamente', async () => {
  const pending = [];
  const { usePerfil } = load('features/perfil/usePerfil.ts', {
    '@/api/client': { ApiError, mensagemErroCarregamento },
    './api': {
      buscarUsuarioAtual: async () => ({ id: 'eu' }),
      buscarPerfil: (id, signal) =>
        new Promise((resolve, reject) =>
          pending.push({ id, signal, resolve, reject })
        ),
    },
  });
  let current, tree;
  function Probe({ id }) {
    current = usePerfil(id);
    return null;
  }
  await act(async () => {
    tree = create(React.createElement(Probe, { id: '1' }));
  });
  assert.equal(current.estado.situacao, 'carregando');
  await act(async () => tree.update(React.createElement(Probe, { id: '2' })));
  assert.equal(pending[0].signal.aborted, true);
  await act(async () => pending[1].resolve({ ...perfil, id: '2' }));
  await act(async () => pending[0].resolve(perfil));
  assert.equal(current.estado.dados.id, '2');
  await act(async () => current.recarregar());
  await act(async () => pending[2].reject(new Error('offline')));
  assert.equal(current.estado.situacao, 'erro');
  await act(async () => current.recarregar());
  await act(async () => pending[3].resolve({ ...perfil, id: '2' }));
  assert.equal(current.estado.situacao, 'sucesso');
  act(() => tree.unmount());
  assert.equal(pending[3].signal.aborted, true);
});

test('atualização de fundo mantém o perfil na tela, inclusive se falhar', async () => {
  const pending = [];
  const { usePerfil } = load('features/perfil/usePerfil.ts', {
    '@/api/client': { ApiError, mensagemErroCarregamento },
    './api': {
      buscarPerfil: (id, signal) =>
        new Promise((resolve, reject) =>
          pending.push({ id, signal, resolve, reject })
        ),
    },
  });
  let current, tree;
  function Probe() {
    current = usePerfil();
    return null;
  }
  await act(async () => {
    tree = create(React.createElement(Probe));
  });
  await act(async () => pending[0].resolve(perfil));
  await act(async () => current.atualizar());
  assert.equal(current.estado.situacao, 'sucesso');
  await act(async () => pending[1].resolve({ ...perfil, nivel: 8 }));
  assert.equal(current.estado.dados.nivel, 8);
  await act(async () => current.atualizar());
  await act(async () => pending[2].reject(new Error('offline')));
  assert.equal(current.estado.situacao, 'sucesso');
  assert.equal(current.estado.dados.nivel, 8);
  act(() => tree.unmount());
});

test('seguir usa POST e DELETE idempotentes, com o ID do perfil e cancelamento', async () => {
  const requests = [];
  const { alterarSeguir, buscarUsuarioAtual } = load('features/perfil/api.ts', {
    '@/api/client': {
      apiRequest: async (...args) => {
        requests.push(args);
      },
      apiGet: async (...args) => {
        requests.push(args);
      },
    },
  });
  const { signal } = new AbortController();
  await alterarSeguir('outro/id', true, signal);
  await alterarSeguir('outro/id', false, signal);
  await buscarUsuarioAtual(signal);
  assert.deepEqual(requests, [
    ['/api/usuarios/outro%2Fid/seguidores/eu', { method: 'POST', signal }],
    ['/api/usuarios/outro%2Fid/seguidores/eu', { method: 'DELETE', signal }],
    ['/api/me', { signal }],
  ]);
});

test('botão tem os três textos e contorno; nunca aparece no próprio perfil', () => {
  const { PerfilConteudo } = load(
    'features/perfil/PerfilScreen.tsx',
    dependencies
  );
  let tree;
  for (const [sigo, meSegue, label] of [
    [false, false, 'Seguir'],
    [false, true, 'Seguir de volta'],
    [true, true, 'Seguindo'],
  ]) {
    act(() => {
      tree = create(
        React.createElement(PerfilConteudo, {
          perfil: { ...perfil, sigo, meSegue },
          onPlaceholder() {},
          onAlternarSeguir() {},
          alterandoSeguir: true,
        })
      );
    });
    const button = tree.root.findByType('PrimaryButton');
    assert.equal(button.props.label, label);
    assert.equal(button.props.variant, sigo ? 'outline' : 'filled');
    assert.equal(button.props.loading, true);
    act(() => tree.unmount());
  }
  act(() => {
    tree = create(
      React.createElement(PerfilConteudo, {
        perfil,
        proprio: true,
        onPlaceholder() {},
        onAlternarSeguir() {},
      })
    );
  });
  assert.equal(tree.root.findByType('PrimaryButton').props.label, 'Mascotes');
  act(() => tree.unmount());
});

function carregarHookSeguir() {
  const leituras = [],
    alteracoes = [];
  const hook = load('features/perfil/usePerfil.ts', {
    '@/api/client': { ApiError, mensagemErroCarregamento },
    './api': {
      buscarUsuarioAtual: async () => ({ id: 'eu' }),
      buscarPerfil: (id, signal) =>
        new Promise((resolve, reject) =>
          leituras.push({ id, signal, resolve, reject })
        ),
      alterarSeguir: (id, seguir, signal) =>
        new Promise((resolve, reject) =>
          alteracoes.push({ id, seguir, signal, resolve, reject })
        ),
    },
  });
  return { ...hook, leituras, alteracoes };
}

test('desabilitar o perfil cancela seguir e bloqueia chamadas até reabilitar', async () => {
  const { usePerfil, leituras, alteracoes } = carregarHookSeguir();
  let atual, tree, envio;
  function Probe({ habilitado }) {
    atual = usePerfil('1', habilitado);
    return null;
  }
  await act(async () => {
    tree = create(React.createElement(Probe, { habilitado: false }));
  });
  assert.equal(leituras.length, 0);
  await act(async () => atual.alternarSeguir());
  assert.equal(alteracoes.length, 0);
  await act(async () =>
    tree.update(React.createElement(Probe, { habilitado: true }))
  );
  await act(async () => leituras[0].resolve(perfil));
  act(() => {
    envio = atual.alternarSeguir();
  });
  await act(async () =>
    tree.update(React.createElement(Probe, { habilitado: false }))
  );
  assert.equal(alteracoes[0].signal.aborted, true);
  assert.equal(atual.estado.situacao, 'carregando');
  await act(async () => {
    alteracoes[0].resolve({ seguidores: 5, sigo: true, meSegue: false });
    await envio;
    await atual.alternarSeguir();
  });
  assert.equal(alteracoes.length, 1);
  assert.equal(atual.estado.situacao, 'carregando');
  await act(async () =>
    tree.update(React.createElement(Probe, { habilitado: true }))
  );
  await act(async () => leituras[1].resolve(perfil));
  assert.equal(atual.estado.situacao, 'sucesso');
  assert.equal(atual.alterandoSeguir, false);
  act(() => tree.unmount());
});

test('falha ao seguir restaura o perfil e permite nova tentativa, mesmo com foco no mesmo ciclo', async () => {
  const { usePerfil, leituras, alteracoes } = carregarHookSeguir();
  let atual, tree, envio;
  function Probe() {
    atual = usePerfil('1');
    return null;
  }
  await act(async () => {
    tree = create(React.createElement(Probe));
  });
  await act(async () => leituras[0].resolve(perfil));
  act(() => {
    atual.atualizar();
    envio = atual.alternarSeguir();
  });
  assert.equal(leituras.length, 1, 'recarga agendada não concorre com o envio');
  assert.equal(atual.estado.dados.sigo, true);
  assert.equal(atual.estado.dados.seguidores, 5);
  const erro = assert.rejects(envio, /Não foi possível atualizar/);
  await act(async () => {
    alteracoes[0].reject(new Error('offline'));
    await erro;
  });
  assert.equal(atual.estado.dados.sigo, false);
  assert.equal(atual.estado.dados.seguidores, 4);
  assert.equal(atual.alterandoSeguir, false);
  act(() => {
    envio = atual.alternarSeguir();
  });
  await act(async () => {
    alteracoes[1].resolve({ seguidores: 5, sigo: true, meSegue: false });
    await envio;
  });
  assert.equal(atual.estado.dados.sigo, true);
  act(() => tree.unmount());
});

test('fluxo segue de volta imediatamente, bloqueia toque duplo e restaura com toast ao falhar', async () => {
  const { usePerfil, leituras, alteracoes } = carregarHookSeguir();
  const avisos = [];
  const { PerfilScreen } = load('features/perfil/PerfilScreen.tsx', {
    ...dependencies,
    'expo-router': { useFocusEffect() {}, useRouter: () => ({}) },
    '@/components/toast-provider': {
      useToastContext: () => ({
        showToast() {},
        showErrorToast: (msg) => avisos.push(msg),
      }),
    },
    './usePerfil': { usePerfil },
  });
  let tree;
  await act(async () => {
    tree = create(React.createElement(PerfilScreen, { usuarioId: '1' }));
  });
  await act(async () => leituras[0].resolve({ ...perfil, meSegue: true }));
  const button = () => tree.root.findByType('PrimaryButton');
  const contador = () =>
    tree.root.findAllByType('Pressable')[0].findAllByType('Text')[1].props
      .children;
  assert.equal(button().props.label, 'Seguir de volta');
  const tocar = button().props.onPress;
  act(() => {
    tocar();
    tocar();
  });
  assert.equal(alteracoes.length, 1);
  assert.equal(button().props.label, 'Seguindo');
  assert.equal(button().props.loading, true);
  assert.equal(contador(), '5');
  await act(async () =>
    alteracoes[0].resolve({ seguidores: 7, sigo: true, meSegue: true })
  );
  assert.equal(contador(), '7', 'reconcilia o contador com o servidor');
  assert.equal(button().props.loading, false);
  act(() => button().props.onPress());
  assert.equal(alteracoes[1].seguir, false);
  assert.equal(contador(), '6');
  assert.equal(button().props.label, 'Seguir de volta');
  await act(async () =>
    alteracoes[1].reject(
      new ApiError(500, { message: 'Falha ao deixar de seguir.' })
    )
  );
  assert.equal(contador(), '7');
  assert.equal(button().props.label, 'Seguindo');
  assert.equal(button().props.loading, false);
  assert.deepEqual(avisos, ['Falha ao deixar de seguir.']);
  act(() => button().props.onPress());
  await act(async () =>
    alteracoes[2].resolve({ seguidores: 6, sigo: false, meSegue: true })
  );
  assert.equal(contador(), '6');
  assert.equal(button().props.label, 'Seguir de volta');
  act(() => tree.unmount());
});

test('perfil próprio aberto pela Busca não permite seguir', async () => {
  const { usePerfil, leituras, alteracoes } = carregarHookSeguir();
  let atual, tree;
  function Probe() {
    atual = usePerfil('eu');
    return null;
  }
  await act(async () => {
    tree = create(React.createElement(Probe));
  });
  await act(async () => leituras[0].resolve({ ...perfil, id: 'eu' }));
  assert.equal(atual.proprio, true);
  await act(async () => atual.alternarSeguir());
  assert.equal(alteracoes.length, 0);
  act(() => tree.unmount());
});

test('mutação ignora leitura anterior e resposta de outro perfil, inclusive ao voltar ao mesmo ID', async () => {
  const { usePerfil, leituras, alteracoes } = carregarHookSeguir();
  let atual, tree;
  function Probe({ id }) {
    atual = usePerfil(id);
    return null;
  }
  await act(async () => {
    tree = create(React.createElement(Probe, { id: '1' }));
  });
  await act(async () => leituras[0].resolve(perfil));
  act(() => atual.atualizar());
  let envio;
  act(() => {
    envio = atual.alternarSeguir();
  });
  await act(async () => leituras[1].resolve(perfil));
  assert.equal(atual.estado.dados.sigo, true, 'GET antigo não desfaz o toque');
  act(() => atual.atualizar());
  assert.equal(
    leituras.length,
    2,
    'foco durante envio não dispara GET concorrente'
  );
  await act(async () => tree.update(React.createElement(Probe, { id: '2' })));
  assert.equal(alteracoes[0].signal.aborted, true);
  await act(async () => leituras[2].resolve({ ...perfil, id: '2' }));
  await act(async () => tree.update(React.createElement(Probe, { id: '1' })));
  await act(async () => leituras[3].resolve(perfil));
  await act(async () => {
    alteracoes[0].resolve({ seguidores: 9, sigo: true, meSegue: true });
    await envio;
  });
  assert.equal(atual.estado.dados.sigo, false);
  assert.equal(atual.estado.dados.seguidores, 4);
  assert.equal(atual.alterandoSeguir, false);
  act(() => {
    envio = atual.alternarSeguir();
  });
  act(() => tree.unmount());
  assert.equal(alteracoes[1].signal.aborted, true);
  await act(async () => {
    alteracoes[1].reject(new Error('cancelado'));
    await envio;
  });
});

for (const dias of [0, 1, 2]) {
  test(`estatística de sequência: ${dias} dia(s)`, () => {
    const { PerfilConteudo } = load(
      'features/perfil/PerfilScreen.tsx',
      dependencies
    );
    let tree;
    act(() => {
      tree = create(
        React.createElement(PerfilConteudo, {
          perfil: {
            ...perfil,
            estatisticas: { ...perfil.estatisticas, sequenciaDias: dias },
          },
          onPlaceholder: () => {},
        })
      );
    });
    try {
      const textos = tree.root
        .findAllByType('Text')
        .map((n) => React.Children.toArray(n.props.children).join(''));
      assert.ok(textos.includes(`${dias} ${dias === 1 ? 'dia' : 'dias'}`));
    } finally {
      act(() => tree.unmount());
    }
  });
}
