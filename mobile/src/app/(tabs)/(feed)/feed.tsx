import { router, useFocusEffect, type Href } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Keyboard } from 'react-native';

import { useToastContext } from '@/components/toast-provider';
import type { ComunidadeSugeridaFeed } from '@/features/feed/api';
import {
  ABA_INICIAL_FEED,
  FeedComunidadesView,
  type AbaFeed,
} from '@/features/feed/FeedComunidadesView';
import { CriarGrupoSheet } from '@/features/feed/CriarGrupoSheet';
import { CriarComunidadeSheet } from '@/features/comunidade/CriarComunidadeSheet';
import { useFeedComunidades } from '@/features/feed/useFeedComunidades';
import { useFeedSocial } from '@/features/feed/useFeedSocial';

// A Pagina do clube (#35) e empilhada dentro da aba, mantendo a navbar.
function rotaDoClube(clubeId: string): Href {
  return `/clube/${clubeId}` as Href;
}

function rotaDaComunidade(comunidadeId: string): Href {
  return `/(tabs)/(feed)/feed-comunidade/${comunidadeId}` as Href;
}

// O perfil e o livro abertos pela aba Social (#204) também são empilhados dentro da aba: o voltar
// traz de volta à aba Social, na mesma posição da lista. O grupo vai explícito porque a aba Perfil
// também tem uma rota `perfil-usuario`.
function rotaDoPerfil(usuarioId: string): Href {
  return {
    pathname: '/(tabs)/(feed)/perfil-usuario/[usuarioId]',
    params: { usuarioId },
  } as Href;
}

function rotaDoLivro(livroId: string): Href {
  return {
    pathname: '/(tabs)/(feed)/feed-livro/[livroId]',
    params: { livroId },
  } as Href;
}

// A Busca fica montada e guarda a aba escolhida: `pedidoEm` muda a cada toque, para ela trocar
// para Usuários mesmo quando o pedido anterior foi o mesmo.
function rotaDaBuscaDeLeitores(): Href {
  return {
    pathname: '/buscar',
    params: { aba: 'Usuários', pedidoEm: String(Date.now()) },
  } as Href;
}

export default function FeedScreen() {
  const { feed, recarregar, atualizar, participar, participacoesPendentes } =
    useFeedComunidades();
  const { showSuccessToast, showErrorToast, showToastAfterNavigation } =
    useToastContext();

  async function participarDaComunidade(comunidade: ComunidadeSugeridaFeed) {
    let sucesso: boolean;
    try {
      sucesso = await participar(comunidade);
    } catch {
      showErrorToast('Não foi possível participar. Tente novamente.');
      return;
    }
    if (sucesso) showSuccessToast(['Você agora segue esta comunidade.']);
  }
  const [aba, setAba] = useState<AbaFeed>(ABA_INICIAL_FEED);
  const social = useFeedSocial(aba === 'Social');
  const { atualizar: atualizarSocial } = social;
  // Lida no foco sem entrar nas dependências: trocar de aba não é voltar para a tela.
  const abaAtual = useRef(aba);
  useEffect(() => {
    abaAtual.current = aba;
  }, [aba]);
  const [modalAberto, setModalAberto] = useState(false);
  const [comunidadeAberta, setComunidadeAberta] = useState(false);
  const fecharModal = useCallback(() => {
    Keyboard.dismiss();
    setModalAberto(false);
    setComunidadeAberta(false);
  }, []);
  useFocusEffect(useCallback(() => () => fecharModal(), [fecharModal]));

  // A aba fica montada: sem buscar de novo ao voltar, a sequência de dias de uma leitura
  // registrada em outra aba não apareceria. O primeiro foco já é coberto pela carga inicial do
  // hook.
  const primeiroFoco = useRef(true);
  useFocusEffect(
    useCallback(() => {
      if (primeiroFoco.current) {
        primeiroFoco.current = false;
        return;
      }
      atualizar();
      if (abaAtual.current === 'Social') atualizarSocial();
    }, [atualizar, atualizarSocial])
  );

  return (
    <>
      <FeedComunidadesView
        feed={feed}
        recarregar={recarregar}
        atualizar={atualizar}
        participacoesPendentes={participacoesPendentes}
        onParticiparPress={participarDaComunidade}
        aba={aba}
        onAbaChange={setAba}
        social={social}
        onBuscarLeitoresPress={() => router.navigate(rotaDaBuscaDeLeitores())}
        onUsuarioPress={(usuarioId) => router.push(rotaDoPerfil(usuarioId))}
        onLivroPress={(livroId) => router.push(rotaDoLivro(livroId))}
        onVerTodosPress={() => router.push('/clubes' as Href)}
        onCriarGrupoPress={() => setModalAberto(true)}
        onCriarComunidadePress={() => setComunidadeAberta(true)}
        onClubePress={(clube) => router.push(rotaDoClube(clube.id))}
        onComunidadePress={(comunidade) =>
          router.push(rotaDaComunidade(comunidade.id))
        }
      />
      <CriarGrupoSheet
        visible={modalAberto}
        onClose={fecharModal}
        onSuccess={atualizar}
      />
      <CriarComunidadeSheet
        visible={comunidadeAberta}
        onClose={fecharModal}
        onSuccess={(comunidade) => {
          fecharModal();
          atualizar();
          showToastAfterNavigation(
            'Comunidade criada! Boas conversas começam aqui.'
          );
          router.push(rotaDaComunidade(comunidade.id));
        }}
      />
    </>
  );
}
