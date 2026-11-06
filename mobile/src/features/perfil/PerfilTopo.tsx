import { Image } from 'expo-image';
import { Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';
import { Avatar } from '@/components/avatar';
import { GearIcon } from '@/components/icons/GearIcon';
import {
  BrasaoMascote,
  PROPORCAO_BRASAO,
} from '@/features/mascotes/BrasaoMascote';
import { arteDoMascote } from '@/features/mascotes/imagens';
import { colors, radius, sizes, spacing } from '@/theme';

import { PlanoDeFundo, PROPORCAO_CENARIO } from './planosDeFundo';

export function PerfilTopo({
  nome,
  avatar,
  planoFundo,
  mascote,
  onConfiguracoes,
  onVoltar,
}: {
  nome?: string;
  avatar?: string | null;
  /** Chave do plano de fundo escolhido; sem ela, vale o cenário padrão. */
  planoFundo?: string | null;
  /** Chave da arte do mascote escolhido; sem ela, o perfil ainda não carregou. */
  mascote?: string;
  onConfiguracoes: () => void;
  /** Só quando o perfil foi empilhado (ex.: tocar em si mesmo na Busca). */
  onVoltar?: () => void;
}) {
  const insets = useSafeAreaInsets();
  return (
    <View style={styles.topo}>
      <PlanoDeFundo chave={planoFundo} />
      {nome !== undefined && (
        <View style={styles.avatar}>
          <View style={styles.avatarFrame}>
            <Avatar
              name={nome}
              photoUrl={avatar}
              size={sizes.avatarLarge * 3}
            />
          </View>
        </View>
      )}
      {mascote !== undefined && (
        <View style={styles.mascote}>
          <BrasaoMascote />
          <Image
            source={arteDoMascote(mascote)}
            style={styles.arteMascote}
            contentFit="contain"
            accessible={false}
          />
        </View>
      )}
      {onVoltar && (
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Voltar"
          onPress={onVoltar}
          hitSlop={spacing[2]}
          style={[styles.voltar, { top: insets.top + spacing[2] }]}
        >
          <Svg
            width={sizes.icon}
            height={sizes.icon}
            viewBox="0 0 24 24"
            fill="none"
          >
            <Path
              d="M15 5l-7 7 7 7"
              stroke={colors.primary}
              strokeWidth={2}
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </Svg>
        </Pressable>
      )}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Configurações"
        onPress={onConfiguracoes}
        hitSlop={spacing[2]}
        style={[styles.engrenagem, { top: insets.top + spacing[2] }]}
      >
        <GearIcon />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  topo: { width: '100%', aspectRatio: PROPORCAO_CENARIO, overflow: 'hidden' },
  avatar: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: '22%',
    alignItems: 'center',
  },
  avatarFrame: {
    padding: spacing[1],
    borderRadius: radius.pill,
    backgroundColor: colors.surface,
  },
  // No canto direito, ao lado do avatar sem cobrir a foto e sem a ponta ser cortada pela borda.
  mascote: {
    position: 'absolute',
    right: '3%',
    bottom: '4%',
    width: '30%',
    aspectRatio: PROPORCAO_BRASAO,
  },
  // Ocupa a parte larga do brasão, com folga até a borda.
  arteMascote: {
    position: 'absolute',
    left: '8%',
    top: '8%',
    width: '84%',
    height: '64%',
  },
  voltar: {
    position: 'absolute',
    left: spacing[4],
    padding: spacing[2],
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
  },
  engrenagem: {
    position: 'absolute',
    right: spacing[4],
    padding: spacing[2],
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
  },
});
