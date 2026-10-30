import { router, useLocalSearchParams, type Href } from 'expo-router';
import { useEffect, useState } from 'react';

import {
  ABA_INICIAL,
  ABAS,
  BuscaView,
  TIPO_POR_ABA,
  type Aba,
} from '@/features/busca/BuscaView';
import { useBusca } from '@/features/busca/useBusca';

function rotaDoLivro(livroId: string): Href {
  return `/buscar-livro/${encodeURIComponent(livroId)}` as Href;
}

function rotaDoUsuario(usuarioId: string): Href {
  return `/usuario/${usuarioId}` as Href;
}

function rotaDaComunidade(comunidadeId: string): Href {
  return `/comunidade/${comunidadeId}` as Href;
}

function ehAba(valor: string | undefined): valor is Aba {
  return ABAS.some((aba) => aba === valor);
}

export default function BuscarScreen() {
  const [termo, setTermo] = useState('');
  const [aba, setAba] = useState<Aba>(ABA_INICIAL);
  const busca = useBusca(termo, TIPO_POR_ABA[aba]);
  // A aba fica montada: quem chega de outra tela com uma aba pedida (ex.: "BUSCAR LEITORES" na
  // aba Social do Feed, #197) troca a aba aqui. `pedidoEm` muda a cada pedido, então o mesmo
  // pedido feito de novo depois de trocar a aba à mão também vale.
  const { aba: abaPedida, pedidoEm } = useLocalSearchParams<{
    aba?: string;
    pedidoEm?: string;
  }>();

  useEffect(() => {
    if (ehAba(abaPedida)) setAba(abaPedida);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pedidoEm]);

  return (
    <BuscaView
      {...busca}
      aba={aba}
      termo={termo}
      onTermoChange={setTermo}
      onAbaChange={setAba}
      onLivroPress={(livro) => router.push(rotaDoLivro(livro.id))}
      onUsuarioPress={(usuario) => router.push(rotaDoUsuario(usuario.id))}
      onComunidadePress={(comunidade) =>
        router.push(rotaDaComunidade(comunidade.id))
      }
    />
  );
}
