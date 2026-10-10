import { useState, type ReactNode } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { Image } from 'expo-image';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Path } from 'react-native-svg';
import { ChamaSequencia } from './ChamaSequencia';

import {
  colors,
  radius,
  sizes,
  spacing,
  textStyles,
  typography,
} from '@/theme';

const VOLTAR_PATH = 'M7.42497 1.0083L1.0083 7.42497L7.42497 13.8416';

// O token `lineHeight.heading` (0.9) vem da caixa de texto do Figma e fica menor que a
// fonte: no dispositivo isso corta o topo dos acentos (no web o glifo transborda e nao
// corta). Por isso o titulo do cabecalho usa `tight`, com ou sem quebra de linha.
const ALTURA_LINHA_TITULO = Math.round(
  typography.fontSize.h1 * typography.lineHeight.tight
);

type AppHeaderProps = {
  title: string;
  /**
   * Aceita nós, e não só texto, para a tela poder destacar parte do subtítulo — como a Página
   * do clube, que traz o título do livro em negrito seguido do autor em regular.
   */
  subtitle?: ReactNode;
  /**
   * `primary` é a faixa vinho das telas raiz. `surface` é o cabeçalho claro sobre o fundo da
   * tela, usado onde o design não desenha a faixa (`menu - clube do livro` 3-6).
   */
  variant?: 'primary' | 'surface';
  titleAlign?: 'center' | 'left';
  titleOverflow?: 'truncate' | 'wrap' | 'scroll' | 'expand';
  titleNumberOfLines?: number;
  showBack?: boolean;
  onBackPress?: () => void;
  streakDays?: number;
  /** A leitura de hoje já foi registrada: a chama acende em vermelho. */
  streakActive?: boolean;
  /** Aviso abaixo dos dias, como quanto falta para perder a sequência. */
  streakHint?: string;
  coverUrl?: string;
  compact?: boolean;
};

export function AppHeader({
  title,
  subtitle,
  variant = 'primary',
  titleAlign = 'left',
  titleOverflow = 'truncate',
  titleNumberOfLines = 1,
  showBack,
  onBackPress,
  streakDays,
  streakActive = false,
  streakHint,
  coverUrl,
  compact = false,
}: AppHeaderProps) {
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const [larguraEsquerda, setLarguraEsquerda] = useState(0);
  const [larguraDireita, setLarguraDireita] = useState(0);
  const [expandido, setExpandido] = useState(false);

  function voltar() {
    if (onBackPress) {
      onBackPress();
      return;
    }
    router.back();
  }

  const centralizado = titleAlign === 'center';
  const temBadge = streakDays !== undefined;
  const larguraLateral = Math.max(larguraEsquerda, larguraDireita);
  const emSuperficie = variant === 'surface';
  // O cabeçalho claro não tem faixa para arredondar: ele é o próprio fundo da tela. A faixa
  // vinho é sempre arredondada; `compact` só abre mão da altura mínima das telas raiz.
  const classesFundo = emSuperficie
    ? 'bg-surface px-6 pb-4'
    : 'rounded-b-xl bg-primary px-6 pb-6';
  const corDoTexto = emSuperficie ? colors.text : colors.textInverse;
  const classesCorDoTexto = emSuperficie ? 'text-text' : 'text-text-inverse';
  const classesTitulo = `${classesCorDoTexto} ${centralizado ? 'text-center' : ''}`;

  function linhasDoTitulo() {
    if (titleOverflow === 'wrap') return titleNumberOfLines;
    if (titleOverflow === 'expand') return expandido ? undefined : 1;
    return 1;
  }

  const linhas = linhasDoTitulo();
  const estiloTitulo = [
    textStyles.headerTitle,
    { lineHeight: ALTURA_LINHA_TITULO },
  ];

  // `onLayout` dispara a cada render; so guardamos larguras novas para nao entrar em
  // loop com o `minWidth` que elas mesmas alimentam.
  function medir(atual: number, definir: (largura: number) => void) {
    return (largura: number) => {
      if (largura !== atual) definir(largura);
    };
  }

  const medirEsquerda = medir(larguraEsquerda, setLarguraEsquerda);
  const medirDireita = medir(larguraDireita, setLarguraDireita);

  return (
    <View
      className={classesFundo}
      style={{
        paddingTop:
          insets.top + (compact || emSuperficie ? spacing[4] : spacing[6]),
        minHeight: compact || emSuperficie ? undefined : sizes.headerHeight,
      }}
    >
      {coverUrl && (
        <View className="mb-4 items-center">
          <Image
            source={{ uri: coverUrl }}
            style={{ width: 150, height: 210, borderRadius: radius.md }}
            contentFit="cover"
          />
        </View>
      )}

      <View className="flex-row items-center">
        <View
          className="shrink-0 items-start"
          style={centralizado ? { minWidth: larguraLateral } : undefined}
        >
          {/* O recuo vai como padding, e nao margem, para entrar na largura medida. */}
          <View
            className={showBack ? 'pr-3' : undefined}
            onLayout={(e) => medirEsquerda(e.nativeEvent.layout.width)}
          >
            {showBack && (
              <Pressable
                accessibilityRole="button"
                accessibilityLabel="Voltar"
                onPress={voltar}
                hitSlop={12}
              >
                <Svg width={9} height={15} viewBox="0 0 9 15" fill="none">
                  <Path
                    d={VOLTAR_PATH}
                    stroke={corDoTexto}
                    strokeWidth={2.01667}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </Svg>
              </Pressable>
            )}
          </View>
        </View>

        {titleOverflow === 'scroll' ? (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            className="min-w-0 flex-1"
            contentContainerStyle={
              centralizado
                ? { flexGrow: 1, justifyContent: 'center' }
                : undefined
            }
          >
            <Text
              numberOfLines={1}
              className={classesTitulo}
              style={estiloTitulo}
            >
              {title}
            </Text>
          </ScrollView>
        ) : titleOverflow === 'expand' ? (
          <Pressable
            className="min-w-0 flex-1"
            onPress={() => setExpandido(!expandido)}
          >
            <Text
              numberOfLines={linhas}
              className={classesTitulo}
              style={estiloTitulo}
            >
              {title}
            </Text>
          </Pressable>
        ) : (
          <Text
            numberOfLines={linhas}
            className={`min-w-0 flex-1 ${classesTitulo}`}
            style={estiloTitulo}
          >
            {title}
          </Text>
        )}

        <View
          className="shrink-0 items-end"
          style={centralizado ? { minWidth: larguraLateral } : undefined}
        >
          <View
            className={temBadge ? 'pl-3' : undefined}
            onLayout={(e) => medirDireita(e.nativeEvent.layout.width)}
          >
            {temBadge && (
              <View
                className="flex-row shrink-0 items-center gap-2 rounded-sm bg-primary-soft px-3 py-2"
                style={{ minHeight: sizes.buttonHeight }}
                accessible
                accessibilityLabel={`Sequência de ${streakDays} ${streakDays === 1 ? 'dia' : 'dias'}, ${
                  streakActive
                    ? 'leitura de hoje registrada'
                    : (streakHint ?? 'sem leitura hoje')
                }`}
              >
                <ChamaSequencia acesa={streakActive} />
                <View>
                  <Text
                    numberOfLines={1}
                    className="text-bodySmall font-inter-bold text-text-inverse"
                  >
                    {streakDays} {streakDays === 1 ? 'dia' : 'dias'}
                  </Text>
                  {streakHint && (
                    <Text
                      numberOfLines={1}
                      className="text-micro font-inter text-text-inverse opacity-80"
                    >
                      {streakHint}
                    </Text>
                  )}
                </View>
              </View>
            )}
          </View>
        </View>
      </View>

      {subtitle && (
        <Text
          className={`mt-1 text-body font-inter ${classesCorDoTexto} ${centralizado ? 'text-center' : ''}`}
          style={
            !centralizado && showBack
              ? { paddingLeft: larguraEsquerda }
              : undefined
          }
        >
          {subtitle}
        </Text>
      )}
    </View>
  );
}
