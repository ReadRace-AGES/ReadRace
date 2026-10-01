import { StyleSheet, Text, View } from 'react-native';
import Svg, { Circle } from 'react-native-svg';
import { colors, sizes, spacing, textStyles } from '@/theme';

const TAMANHO = sizes.avatarLarge * 2;
const TRACO = spacing[2];
const RAIO = (TAMANHO - TRACO) / 2;
const CIRCUNFERENCIA = 2 * Math.PI * RAIO;

const numero = (valor: number) => valor.toLocaleString('pt-BR');

export function NivelAnel({
  nivel,
  xpNoNivel,
  xpDoNivel,
}: {
  nivel: number;
  xpNoNivel: number;
  xpDoNivel: number;
}) {
  const progresso =
    xpDoNivel > 0 ? Math.min(Math.max(xpNoNivel / xpDoNivel, 0), 1) : 0;
  return (
    <View
      style={styles.container}
      accessible
      accessibilityLabel={`Nível ${nivel}, ${xpNoNivel} de ${xpDoNivel} XP`}
    >
      <Text style={styles.xp}>
        {numero(xpNoNivel)}/{numero(xpDoNivel)}
      </Text>
      <View style={styles.anel}>
        <Svg width={TAMANHO} height={TAMANHO}>
          <Circle
            cx={TAMANHO / 2}
            cy={TAMANHO / 2}
            r={RAIO}
            stroke={colors.progressTrack}
            strokeWidth={TRACO}
            fill="none"
          />
          {progresso > 0 && (
            <Circle
              cx={TAMANHO / 2}
              cy={TAMANHO / 2}
              r={RAIO}
              stroke={colors.primary}
              strokeWidth={TRACO}
              strokeLinecap="round"
              strokeDasharray={CIRCUNFERENCIA}
              strokeDashoffset={CIRCUNFERENCIA * (1 - progresso)}
              fill="none"
              transform={`rotate(-90 ${TAMANHO / 2} ${TAMANHO / 2})`}
            />
          )}
        </Svg>
        <View style={[StyleSheet.absoluteFill, styles.centro]}>
          <Text style={styles.nivel}>{nivel}</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { alignItems: 'center', gap: spacing[1] },
  xp: { ...textStyles.bodySmallStrong, color: colors.primary },
  anel: { width: TAMANHO, height: TAMANHO },
  centro: { alignItems: 'center', justifyContent: 'center' },
  nivel: { ...textStyles.h1, color: colors.primary },
});