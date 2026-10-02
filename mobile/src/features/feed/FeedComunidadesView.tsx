import { useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import Svg, { Path } from 'react-native-svg';

import { AppHeader } from '@/components/AppHeader';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { ListItem } from '@/components/ListItem';
import {
  PrimaryButton,
  type PrimaryButtonIconProps,
} from '@/components/PrimaryButton';
import { SegmentedTabs } from '@/components/SegmentedTabs';
import { colors, spacing } from '@/theme';

import { ClubeCard } from './ClubeCard';
import type { ClubeFeed, ComunidadeFeed } from './api';
import type { FeedComunidadesState } from './useFeedComunidades';

const ABAS = ['Social', 'Comunidades'] as const;
const ABA_INICIAL = 'Comunidades';

const COPY = {
  subtitulo: 'Continue sua jornada literária',
  meusGrupos: 'Meus Grupos',
  verTodos: 'Ver todos',
  criarGrupo: 'Criar Grupo',
  tentarDeNovo: 'Tentar de novo',
  // Sem copy aprovada no Figma para o erro e para as listas vazias (pendencia de design):
  // com o seed de #13 os vazios nao aparecem em uso normal, existem como defesa.
  erro: 'Não foi possível carregar o feed de comunidades.',
  semClubes: 'Você ainda não participa de nenhum clube do livro.',
  semComunidades: 'Você ainda não participa de nenhuma comunidade.',
} as const;

function IconePlus({ size, color }: PrimaryButtonIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M12 5v14M5 12h14"
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
      />
    </Svg>
  );
}

function CabecalhoSecao({
  titulo,
  acao,
}: {
  titulo: string;
  acao?: { label: string; onPress: () => void };
}) {
  return (
    <View className="flex-row items-center justify-between">
      <Text className="font-inter-bold text-h2 text-text">{titulo}</Text>
      {acao && (
        <Text
          accessibilityRole="button"
          onPress={acao.onPress}
          className="font-inter-semibold text-bodySmall text-accent"
        >
          {acao.label}
        </Text>
      )}
    </View>
  );
}

export type FeedComunidadesViewProps = FeedComunidadesState & {
  onSocialPress: () => void;
  onVerTodosPress: () => void;
  onCriarGrupoPress: () => void;
  onClubePress: (clube: ClubeFeed) => void;
  onComunidadePress: (comunidade: ComunidadeFeed) => void;
};

export function FeedComunidadesView({
  feed,
  recarregar,
  onSocialPress,
  onVerTodosPress,
  onCriarGrupoPress,
  onClubePress,
  onComunidadePress,
}: FeedComunidadesViewProps) {
  const usuario = feed.situacao === 'sucesso' ? feed.dados.usuario : null;
  // "Social" e uma acao, nao uma selecao (feed social fora da sprint): remontar o SegmentedTabs
  // pela key devolve a pilula para "Comunidades" sem que o estado "Social" chegue a pintar, e
  // mantem o toque em "Social" disparando o toast toda vez.
  const [seletorKey, setSeletorKey] = useState(0);

  function aoTrocarAba(aba: string) {
    if (aba === 'Social') {
      onSocialPress();
      setSeletorKey((k) => k + 1);
    }
  }

  return (
    <View className="flex-1 bg-surface">
      <AppHeader
        title={usuario ? `Olá, ${usuario.nome}!` : 'Olá!'}
        subtitle={COPY.subtitulo}
        streakDays={usuario?.sequenciaDias}
        streakActive={(usuario?.sequenciaDias ?? 0) > 0}
      />

      <View className="px-6 pb-2 pt-6">
        <SegmentedTabs
          key={seletorKey}
          options={[...ABAS]}
          initialOption={ABA_INICIAL}
          onChange={aoTrocarAba}
        />
      </View>

      {feed.situacao === 'carregando' && (
        <View
          className="flex-1 items-center justify-center p-4"
          accessibilityLabel="Carregando"
        >
          <ActivityIndicator color={colors.primary} />
        </View>
      )}

      {feed.situacao === 'erro' && (
        <View className="flex-1">
          <EmptyState
            icon={BookIcon}
            message={feed.mensagem}
            action={
              <PrimaryButton
                label={COPY.tentarDeNovo}
                variant="outline"
                onPress={recarregar}
              />
            }
          />
        </View>
      )}

      {feed.situacao === 'sucesso' && (
        <ScrollView
          className="flex-1"
          contentContainerStyle={{
            paddingHorizontal: spacing[6],
            paddingBottom: spacing[10],
            gap: spacing[4],
          }}
          showsVerticalScrollIndicator={false}
        >
          <CabecalhoSecao
            titulo={COPY.meusGrupos}
            acao={{ label: COPY.verTodos, onPress: onVerTodosPress }}
          />

          {feed.dados.clubes.length === 0 ? (
            <EmptyState icon={BookIcon} message={COPY.semClubes} />
          ) : (
            feed.dados.clubes.map((clube) => (
              <ClubeCard
                key={clube.id}
                clube={clube}
                onPress={() => onClubePress(clube)}
              />
            ))
          )}

          <PrimaryButton
            label={COPY.criarGrupo}
            icon={IconePlus}
            onPress={onCriarGrupoPress}
          />

          {feed.dados.comunidades.length === 0 ? (
            <EmptyState icon={BookIcon} message={COPY.semComunidades} />
          ) : (
            feed.dados.comunidades.map((comunidade) => (
              <ListItem
                key={comunidade.id}
                variant="community"
                title={comunidade.nome}
                imageUrl={comunidade.capaUrl}
                memberCount={comunidade.totalMembros}
                onPress={() => onComunidadePress(comunidade)}
              />
            ))
          )}
        </ScrollView>
      )}
    </View>
  );
}

export default FeedComunidadesView;
