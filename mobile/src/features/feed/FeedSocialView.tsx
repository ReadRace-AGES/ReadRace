import { ActivityIndicator, FlatList, View } from 'react-native';

import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { UsersIcon } from '@/components/icons/UsersIcon';
import { PrimaryButton } from '@/components/PrimaryButton';
import { colors, spacing } from '@/theme';

import { AtividadeCard } from './AtividadeCard';
import type { AtividadeSocial } from './api';
import type { FeedSocialState } from './useFeedSocial';

const COPY = {
  vazio: 'Siga outros leitores para ver o que eles estão lendo.',
  buscarLeitores: 'BUSCAR LEITORES',
  tentarDeNovo: 'Tentar de novo',
} as const;

// O card não tem id próprio: é uma pessoa, um livro e um dia.
function chaveDa(atividade: AtividadeSocial) {
  return `${atividade.tipo}:${atividade.usuario.id}:${atividade.livro.id}:${atividade.em}`;
}

export type FeedSocialViewProps = Pick<
  FeedSocialState,
  'social' | 'recarregar'
> & {
  onBuscarLeitoresPress: () => void;
  onUsuarioPress: (usuarioId: string) => void;
  onLivroPress: (livroId: string) => void;
};

/** Corpo da aba Social do Feed (#197, #204), abaixo do cabeçalho e das abas. */
export function FeedSocialView({
  social,
  recarregar,
  onBuscarLeitoresPress,
  onUsuarioPress,
  onLivroPress,
}: FeedSocialViewProps) {
  if (social.situacao === 'carregando') {
    return (
      <View
        className="flex-1 items-center justify-center p-4"
        accessibilityLabel="Carregando"
      >
        <ActivityIndicator color={colors.primary} />
      </View>
    );
  }

  if (social.situacao === 'erro') {
    return (
      <View className="flex-1">
        <EmptyState
          icon={BookIcon}
          message={social.mensagem}
          action={
            <PrimaryButton
              label={COPY.tentarDeNovo}
              variant="outline"
              onPress={recarregar}
            />
          }
        />
      </View>
    );
  }

  if (social.dados.atividades.length === 0) {
    return (
      <View className="flex-1">
        <EmptyState
          icon={UsersIcon}
          message={COPY.vazio}
          action={
            <PrimaryButton
              label={COPY.buscarLeitores}
              onPress={onBuscarLeitoresPress}
            />
          }
        />
      </View>
    );
  }

  return (
    <FlatList
      data={social.dados.atividades}
      keyExtractor={chaveDa}
      renderItem={({ item }) => (
        <AtividadeCard
          atividade={item}
          onAutorPress={() => onUsuarioPress(item.usuario.id)}
          onLivroPress={() => onLivroPress(item.livro.id)}
        />
      )}
      className="flex-1"
      contentContainerStyle={{
        paddingHorizontal: spacing[6],
        paddingTop: spacing[2],
        paddingBottom: spacing[10],
        gap: spacing[4],
      }}
      showsVerticalScrollIndicator={false}
    />
  );
}

export default FeedSocialView;
