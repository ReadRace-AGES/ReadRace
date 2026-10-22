import { Image } from 'expo-image';
import { Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Avatar } from '@/components/avatar';
import { GearIcon } from '@/components/icons/GearIcon';
import {
  BrasaoMascote,
  PROPORCAO_BRASAO,
} from '@/features/mascotes/BrasaoMascote';
import { arteDoMascote } from '@/features/mascotes/imagens';
import { colors, radius, sizes, spacing } from '@/theme';

const cenario = require('../../../assets/images/perfil-cenario.png');

const PROPORCAO_CENARIO = 786 / 722;

export function PerfilTopo({
  nome,
  avatar,
  mascote,
  onConfiguracoes,
}: {
  nome?: string;
  avatar?: string | null;
  /** Chave da arte do mascote escolhido; sem ela, o perfil ainda não carregou. */
  mascote?: string;
  onConfiguracoes: () => void;
}) {
  const insets = useSafeAreaInsets();
  return (
    <View style={styles.topo}>
      <Image
        source={cenario}
        style={StyleSheet.absoluteFill}
        contentFit="cover"
        accessible={false}
      />
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
  engrenagem: {
    position: 'absolute',
    right: spacing[4],
    padding: spacing[2],
    borderRadius: radius.pill,
    backgroundColor: colors.surfacePink,
  },
});
