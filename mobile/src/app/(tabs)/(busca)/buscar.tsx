import { router, type Href } from 'expo-router';
import { useState } from 'react';

import { useToastContext } from '@/components/toast-provider';
import {
  ABA_INICIAL,
  BuscaView,
  TIPO_POR_ABA,
  type Aba,
} from '@/features/busca/BuscaView';
import { useBusca } from '@/features/busca/useBusca';

function rotaDoLivro(livroId: string): Href {
  return `/livro/${livroId}` as Href;
}

function rotaDoUsuario(usuarioId: string): Href {
  return `/usuario/${usuarioId}` as Href;
}

export default function BuscarScreen() {
  const { showToast } = useToastContext();
  const [termo, setTermo] = useState('');
  const [aba, setAba] = useState<Aba>(ABA_INICIAL);
  const busca = useBusca(termo, TIPO_POR_ABA[aba]);

  return (
    <BuscaView
      {...busca}
      termo={termo}
      onTermoChange={setTermo}
      onAbaChange={setAba}
      onLivroPress={(livro) => router.push(rotaDoLivro(livro.id))}
      onUsuarioPress={(usuario) => router.push(rotaDoUsuario(usuario.id))}
      onComunidadePress={showToast}
    />
  );
}
