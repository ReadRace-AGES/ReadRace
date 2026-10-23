import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { BottomSheet } from '@/components/BottomSheet';
import { colors, radius, spacing, textStyles } from '@/theme';

import { PROPORCAO_CENARIO } from './planosDeFundo';

const BORDA_SELECAO = 3;

type Props = {
  visible: boolean;
  onClose: () => void;
  titulo: string;
  opcoes: readonly { chave: string; nome: string }[];
  selecionada: string | null;
  onEscolher: (chave: string) => void;
  /** Desenha a arte da opção ocupando todo o espaço dela. */
  renderArte: (chave: string) => ReactNode;
  /** Bonecos em círculo, como o avatar; fundos no retângulo do topo do perfil. */
  formato: 'circulo' | 'retangulo';
};

/** Grade de opções do avatar ou do plano de fundo; escolher fecha o painel. */
export function EscolhaAparenciaSheet({
  visible,
  onClose,
  titulo,
  opcoes,
  selecionada,
  onEscolher,
  renderArte,
  formato,
}: Props) {
  return (
    <BottomSheet
      visible={visible}
      onClose={onClose}
      accessibilityLabel={titulo}
    >
      <View style={styles.conteudo}>
        <Text style={styles.titulo}>{titulo}</Text>
        <View style={styles.grade}>
          {opcoes.map(({ chave, nome }) => {
            const ativa = chave === selecionada;
            return (
              <Pressable
                key={chave}
                accessibilityRole="button"
                accessibilityLabel={nome}
                accessibilityState={{ selected: ativa }}
                onPress={() => {
                  onEscolher(chave);
                  onClose();
                }}
                style={
                  formato === 'circulo'
                    ? styles.itemCirculo
                    : styles.itemRetangulo
                }
              >
                <View
                  style={[
                    formato === 'circulo'
                      ? styles.arteCirculo
                      : styles.arteRetangulo,
                    ativa && styles.arteAtiva,
                  ]}
                >
                  {renderArte(chave)}
                </View>
                <Text style={[styles.nome, ativa && styles.nomeAtivo]}>
                  {nome}
                </Text>
              </Pressable>
            );
          })}
        </View>
      </View>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  conteudo: { gap: spacing[4], paddingBottom: spacing[4] },
  titulo: { ...textStyles.h3, color: colors.text },
  grade: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    rowGap: spacing[4],
  },
  itemCirculo: { width: '22%', alignItems: 'center', gap: spacing[1] },
  itemRetangulo: { width: '48%', gap: spacing[1] },
  arteCirculo: {
    width: '100%',
    aspectRatio: 1,
    borderRadius: radius.pill,
    borderWidth: BORDA_SELECAO,
    borderColor: 'transparent',
    overflow: 'hidden',
  },
  arteRetangulo: {
    width: '100%',
    aspectRatio: PROPORCAO_CENARIO,
    borderRadius: radius.md,
    borderWidth: BORDA_SELECAO,
    borderColor: 'transparent',
    overflow: 'hidden',
  },
  arteAtiva: { borderColor: colors.primary },
  nome: { ...textStyles.caption, color: colors.textSecondary },
  nomeAtivo: { color: colors.primary },
});
