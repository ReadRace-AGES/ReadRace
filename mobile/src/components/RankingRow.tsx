import MaskedView from '@react-native-masked-view/masked-view';
import { LinearGradient } from 'expo-linear-gradient';
import { Text, View, type TextStyle } from 'react-native';

import { Avatar } from '@/components/avatar';
import { colors, gradients, sizes, spacing, textStyles } from '@/theme';

/**
 * Linha do ranking de `Pontos` do Clube do Livro (`menu - clube do livro` 3-6).
 *
 * `Pontos` são a moeda do ranking dentro do clube e não viram nível; `XP` é a
 * progressão pessoal e nunca aparece aqui (`produto.md` §5).
 *
 * A linha é só apresentação: a posição vem pronta de quem usa, ela não ordena,
 * não desempata e não é tocável nesta sprint. Quem empilha as linhas, busca a
 * lista e desenha o card em volta é a Página do clube (#35).
 *
 * ```tsx
 * {ranking.map((r) => (
 *   <RankingRow
 *     key={r.usuario.id}
 *     position={r.posicao}
 *     name={r.usuario.nome}
 *     points={r.pontos}
 *     photoUrl={r.usuario.avatarUrl}
 *   />
 * ))}
 * ```
 */
export type RankingRowProps = {
  /** Posição no ranking, a partir de 1. Vem pronta de quem usa: a linha não calcula ordem nem trata empate. */
  position: number;
  /** Nome de usuário do membro. Vai no rótulo e alimenta a inicial do `Avatar` quando não há foto. */
  name: string;
  /** `Pontos` do clube. Nunca XP. */
  points: number;
  photoUrl?: string | null;
};

// 1º, 2º e 3º lugares: gradiente da esquerda para a direita sobre o rótulo
// (`menu - clube do livro` 3-6). Da 4ª posição em diante não há destaque.
const PODIUM_GRADIENTS = new Map<number, readonly string[]>([
  [1, gradients.rankGold],
  [2, gradients.rankSilver],
  [3, gradients.rankBronze],
]);

// O pódio usa um degrau acima na escala: rótulo 16/pontos 14 contra 14/12 das
// demais posições (caixas de texto de 24/21px e 21/18px no frame 3-6).
const PODIUM_STYLES = {
  label: textStyles.bodyStrong,
  points: textStyles.bodySmall,
};
const COMMON_STYLES = {
  label: textStyles.bodySmallStrong,
  points: textStyles.caption,
};

export function formatRankingLabel(position: number, name: string): string {
  return `${position}º lugar: ${name}`;
}

export function formatRankingPoints(points: number): string {
  return `${points} pontos`;
}

type GradientTextProps = {
  text: string;
  gradient: readonly string[];
  style: TextStyle;
};

/**
 * Texto preenchido pelo gradiente do pódio.
 *
 * A máscara é o próprio texto truncado; o gradiente só aparece onde há glifo.
 * Na web o `MaskedView` não mascara (renderiza a máscara como filho comum), e
 * por isso a cor do texto é a primeira parada do gradiente: vira o fallback
 * sólido em vez de texto preto.
 */
function GradientText({ text, gradient, style }: GradientTextProps) {
  const [start, end] = gradient;
  const textStyle = [style, { color: start }];
  return (
    <MaskedView
      style={{ flexShrink: 1 }}
      maskElement={
        <Text numberOfLines={1} style={textStyle}>
          {text}
        </Text>
      }
    >
      <LinearGradient
        colors={[start, end]}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
      >
        <Text numberOfLines={1} style={[textStyle, { opacity: 0 }]}>
          {text}
        </Text>
      </LinearGradient>
    </MaskedView>
  );
}

export function RankingRow({
  position,
  name,
  points,
  photoUrl,
}: RankingRowProps) {
  const podiumGradient = PODIUM_GRADIENTS.get(position);
  const { label: labelStyle, points: pointsStyle } = podiumGradient
    ? PODIUM_STYLES
    : COMMON_STYLES;
  const labelText = formatRankingLabel(position, name);
  const pointsText = formatRankingPoints(points);

  return (
    // Passo de 64px entre linhas: faixa de 48px (avatar + 4px de cada lado) com
    // 8px de respiro em cima e embaixo, como o frame 3-6 (63px de passo, faixa de 49).
    <View
      accessible
      accessibilityLabel={`${labelText}, ${pointsText}`}
      style={{ paddingVertical: spacing[2] }}
    >
      <View
        style={{
          flexDirection: 'row',
          alignItems: 'center',
          gap: spacing[3],
          paddingHorizontal: spacing[4],
          paddingVertical: spacing[1],
          // Linhas consecutivas alternam o fundo, inclusive dentro do pódio: as
          // pares ganham a faixa `surfaceAlt`, as ímpares deixam o card aparecer.
          backgroundColor:
            position % 2 === 0 ? colors.surfaceAlt : 'transparent',
        }}
      >
        <Avatar name={name} photoUrl={photoUrl} size={sizes.avatar} />
        <View
          style={{
            flex: 1,
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: spacing[2],
          }}
        >
          {podiumGradient ? (
            <GradientText
              text={labelText}
              gradient={podiumGradient}
              style={labelStyle}
            />
          ) : (
            <Text
              numberOfLines={1}
              style={[labelStyle, { color: colors.text, flexShrink: 1 }]}
            >
              {labelText}
            </Text>
          )}
          <Text style={[pointsStyle, { color: colors.text, flexShrink: 0 }]}>
            {pointsText}
          </Text>
        </View>
      </View>
    </View>
  );
}

export default RankingRow;
