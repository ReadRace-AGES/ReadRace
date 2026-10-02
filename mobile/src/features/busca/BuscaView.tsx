import type { ReactElement, ReactNode } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Pressable,
  Text,
  View,
} from 'react-native';

import { AppHeader } from '@/components/AppHeader';
import { Avatar } from '@/components/avatar';
import { BookCover } from '@/components/BookCover';
import { EmptyState } from '@/components/EmptyState';
import { SearchIcon } from '@/components/icons/SearchIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { SearchInput } from '@/components/SearchInput';
import { SegmentedTabs } from '@/components/SegmentedTabs';
import { colors, sizes, spacing } from '@/theme';

import type {
  ComunidadeBusca,
  LivroBusca,
  ResultadoBusca,
  TipoBusca,
  UsuarioBusca,
} from './api';
import type { BuscaState } from './useBusca';

export const ABAS = ['Livros', 'Usuários', 'Comunidades'] as const;
export type Aba = (typeof ABAS)[number];
export const ABA_INICIAL: Aba = 'Usuários';
export const TIPO_POR_ABA: Record<Aba, TipoBusca> = {
  Livros: 'livros',
  Usuários: 'usuarios',
  Comunidades: 'comunidades',
};

const COPY = {
  titulo: 'Buscar',
  subtitulo: 'Encontre leitores e comunidades',
  placeholder: 'Nome de usuário ou comunidade',
  vazio: 'Busque por leitores, comunidades ou livros',
  tentarDeNovo: 'Tentar de novo',
} as const;

export type BuscaViewProps = BuscaState & {
  termo: string;
  onTermoChange: (termo: string) => void;
  onAbaChange: (aba: Aba) => void;
  onLivroPress: (livro: LivroBusca) => void;
  onUsuarioPress: (usuario: UsuarioBusca) => void;
  onComunidadePress: (comunidade: ComunidadeBusca) => void;
};

function Carregando() {
  return (
    <View
      className="flex-1 items-center justify-center p-4"
      accessibilityLabel="Carregando"
    >
      <ActivityIndicator color={colors.primary} />
    </View>
  );
}

function Vazio() {
  return <EmptyState icon={SearchIcon} message={COPY.vazio} />;
}

function Erro({
  mensagem,
  onRetry,
}: {
  mensagem: string;
  onRetry: () => void;
}) {
  return (
    <EmptyState
      icon={SearchIcon}
      message={mensagem}
      action={
        <PrimaryButton
          label={COPY.tentarDeNovo}
          variant="outline"
          onPress={onRetry}
        />
      }
    />
  );
}

type LinhaProps = {
  imagem: ReactNode;
  titulo: string;
  subtitulo?: string | null;
  metadado?: string | null;
  onPress: () => void;
};

function Linha({ imagem, titulo, subtitulo, metadado, onPress }: LinhaProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={titulo}
      onPress={onPress}
      className="flex-row items-center gap-4 border-b border-border px-6 py-3 active:bg-surface-muted"
    >
      {imagem}
      <View className="min-w-0 flex-1 gap-1">
        <Text numberOfLines={1} className="font-inter-bold text-body text-text">
          {titulo}
        </Text>
        {subtitulo ? (
          <Text
            numberOfLines={2}
            className="font-inter text-bodySmall text-text-secondary"
          >
            {subtitulo}
          </Text>
        ) : null}
        {metadado ? (
          <Text
            numberOfLines={1}
            className="font-inter text-caption text-text-muted"
          >
            {metadado}
          </Text>
        ) : null}
      </View>
    </Pressable>
  );
}

function Lista<T extends { id: string }>({
  itens,
  renderItem,
}: {
  itens: T[];
  renderItem: (item: T) => ReactElement;
}) {
  return (
    <FlatList
      data={itens}
      keyExtractor={(item) => item.id}
      keyboardShouldPersistTaps="handled"
      contentContainerStyle={{ paddingBottom: spacing[6] }}
      renderItem={({ item }) => renderItem(item)}
    />
  );
}

function Resultados({
  dados,
  onLivroPress,
  onUsuarioPress,
  onComunidadePress,
}: { dados: ResultadoBusca } & Pick<
  BuscaViewProps,
  'onLivroPress' | 'onUsuarioPress' | 'onComunidadePress'
>) {
  if (dados.itens.length === 0) return <Vazio />;

  switch (dados.tipo) {
    case 'livros':
      return (
        <Lista
          itens={dados.itens}
          renderItem={(livro) => (
            <Linha
              imagem={
                <BookCover
                  size="thumbnail"
                  source={livro.capa}
                  accessibilityLabel={livro.titulo}
                />
              }
              titulo={livro.titulo}
              subtitulo={livro.autor}
              metadado={
                livro.totalPaginas != null
                  ? `${livro.totalPaginas} páginas`
                  : null
              }
              onPress={() => onLivroPress(livro)}
            />
          )}
        />
      );
    case 'usuarios':
      return (
        <Lista
          itens={dados.itens}
          renderItem={(usuario) => (
            <Linha
              imagem={
                <Avatar
                  name={usuario.nome}
                  photoUrl={usuario.avatar}
                  size={sizes.avatarLarge}
                />
              }
              titulo={usuario.nome}
              subtitulo={`@${usuario.username}`}
              metadado={usuario.titulo}
              onPress={() => onUsuarioPress(usuario)}
            />
          )}
        />
      );
    case 'comunidades':
      return (
        <Lista
          itens={dados.itens}
          renderItem={(comunidade) => (
            <Linha
              imagem={
                <Avatar
                  name={comunidade.nome}
                  photoUrl={comunidade.capa}
                  size={sizes.avatarLarge}
                />
              }
              titulo={comunidade.nome}
              subtitulo={comunidade.descricao}
              onPress={() => onComunidadePress(comunidade)}
            />
          )}
        />
      );
  }
}

export function BuscaView({
  busca,
  recarregar,
  termo,
  onTermoChange,
  onAbaChange,
  onLivroPress,
  onUsuarioPress,
  onComunidadePress,
}: BuscaViewProps) {
  return (
    <View className="flex-1 bg-surface">
      <AppHeader title={COPY.titulo} subtitle={COPY.subtitulo} />

      <View className="gap-4 px-6 pb-2 pt-6">
        <SearchInput
          placeholder={COPY.placeholder}
          value={termo}
          onChangeText={onTermoChange}
          returnKeyType="search"
        />
        <SegmentedTabs
          options={[...ABAS]}
          initialOption={ABA_INICIAL}
          onChange={(opcao) => onAbaChange(opcao as Aba)}
        />
      </View>

      <View className="flex-1">
        {busca.situacao === 'inicial' && <Vazio />}
        {busca.situacao === 'carregando' && <Carregando />}
        {busca.situacao === 'erro' && (
          <Erro mensagem={busca.mensagem} onRetry={recarregar} />
        )}
        {busca.situacao === 'sucesso' && (
          <Resultados
            dados={busca.dados}
            onLivroPress={onLivroPress}
            onUsuarioPress={onUsuarioPress}
            onComunidadePress={onComunidadePress}
          />
        )}
      </View>
    </View>
  );
}

export default BuscaView;
