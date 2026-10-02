import { useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { Card } from '@/components/Card';
import { EmptyState } from '@/components/EmptyState';
import { BookIcon } from '@/components/icons/BookIcon';
import { RankingRow } from '@/components/RankingRow';
import { colors, spacing, textStyles, typography } from '@/theme';

import type { LinhaRanking } from './api';

const COPY = {
  verTodos: 'Ver todos',
  // O Figma nao desenha o ranking vazio e nao ha copy aprovada; o texto abaixo foi definido
  // com a gestao. Com o seed de #13 o estado nao aparece em uso normal: existe como defesa.
  semRanking: 'Este clube ainda não tem membros no ranking.',
} as const;

const LADO_DO_QUADRO = spacing[3];

/**
 * Faixa quadriculada do topo do card (`menu - clube do livro` 3-6). É decoração: o card do
 * ranking não tem título nem copy de cabeçalho no design.
 */
function FaixaQuadriculada() {
  const [colunas, setColunas] = useState(0);

  return (
    <View
      style={styles.faixa}
      onLayout={(e) =>
        setColunas(Math.ceil(e.nativeEvent.layout.width / LADO_DO_QUADRO))
      }
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
      aria-hidden
    >
      {[0, 1].map((linha) => (
        <View key={linha} style={styles.faixaLinha}>
          {Array.from({ length: colunas }, (_, coluna) => (
            <View
              key={coluna}
              style={[
                styles.quadro,
                (linha + coluna) % 2 === 0 && styles.quadroEscuro,
              ]}
            />
          ))}
        </View>
      ))}
    </View>
  );
}

export type RankingCardProps = {
  /** `null` enquanto a página carrega. */
  ranking: LinhaRanking[] | null;
  onLinhaPress: () => void;
  onVerTodosPress: () => void;
};

/**
 * Card do ranking de `Pontos` do clube: até 7 linhas, já ordenadas e numeradas pelo backend.
 *
 * O `RankingRow` (#27) é só apresentação e não é tocável; quem dá o toque — que nesta sprint
 * só dispara o `Toast` — é esta tela.
 */
export function RankingCard({
  ranking,
  onLinhaPress,
  onVerTodosPress,
}: RankingCardProps) {
  return (
    <Card surfaceStyle={styles.superficie}>
      <FaixaQuadriculada />

      {ranking === null ? (
        <View style={styles.carregando} accessibilityLabel="Carregando ranking">
          <ActivityIndicator color={colors.primary} />
        </View>
      ) : ranking.length === 0 ? (
        <View style={styles.vazio}>
          <EmptyState icon={BookIcon} message={COPY.semRanking} />
        </View>
      ) : (
        ranking.map((linha) => (
          <Pressable key={linha.usuario.id} onPress={onLinhaPress}>
            <RankingRow
              position={linha.posicao}
              name={linha.usuario.nome}
              points={linha.pontos}
              photoUrl={linha.usuario.avatarUrl}
            />
          </Pressable>
        ))
      )}

      {ranking !== null && (
        <View style={styles.rodape}>
          <Text
            accessibilityRole="button"
            onPress={onVerTodosPress}
            style={styles.verTodos}
          >
            {COPY.verTodos}
          </Text>
        </View>
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  // As linhas do ranking sangram até a borda: o respiro interno é delas, não do card.
  superficie: { padding: spacing[0] },
  faixa: { overflow: 'hidden' },
  faixaLinha: { flexDirection: 'row' },
  quadro: {
    width: LADO_DO_QUADRO,
    height: LADO_DO_QUADRO,
    backgroundColor: colors.surface,
  },
  quadroEscuro: { backgroundColor: colors.text },
  carregando: { padding: spacing[6], alignItems: 'center' },
  vazio: { paddingVertical: spacing[6] },
  rodape: { alignItems: 'flex-end', padding: spacing[4] },
  verTodos: {
    ...textStyles.bodySmall,
    fontFamily: typography.fontFamily.semibold,
    color: colors.accent,
  },
});

export default RankingCard;
