import type { ReactNode } from 'react';
import { Pressable, View, type StyleProp, type ViewStyle } from 'react-native';

import { shadows } from '@/theme';

type CardProps = {
  /** Conteúdo do card. O componente não define o que é renderizado dentro dele. */
  children?: ReactNode;
  /** Torna o card tocável (ex.: card de clube). Sem isso, o toque não tem efeito. */
  onPress?: () => void;
  /** Estilo extra do container externo — uso típico: margem entre cards na tela que o posiciona. */
  style?: StyleProp<ViewStyle>;
  testID?: string;
};

function estaVazio(children: ReactNode): boolean {
  if (children === null || children === undefined || children === false)
    return true;
  if (Array.isArray(children)) return children.every(estaVazio);
  return false;
}

/**
 * Container base reutilizado por PostCard, ListItem, RankingRow e pelos blocos de
 * estatística do perfil. Entrega só a moldura (superfície, borda, raio, sombra e
 * espaçamento interno) — todo conteúdo é responsabilidade de quem usa o Card.
 *
 * `bg-surface-muted` + `border-border` seguem o que o README documenta para o frame
 * `card` (403-404). `rounded-lg` e `shadows.floating` não têm origem documentada no
 * README para esse frame — são a leitura mais próxima dos tokens existentes; ajuste
 * aqui se a medição do Figma indicar outro valor.
 *
 * Duas Views aninhadas por necessidade técnica, não por design: a sombra (iOS) some se
 * for aplicada na mesma View que tem `overflow-hidden`, então a sombra fica na View de
 * fora e o recorte por raio fica na de dentro.
 */
export function Card({ children, onPress, style, testID }: CardProps) {
  if (estaVazio(children)) return null;

  const conteudo = (
    <View className="overflow-hidden rounded-lg border border-border bg-surface-muted p-4">
      {children}
    </View>
  );

  if (!onPress) {
    return (
      <View style={[shadows.floating, style]} testID={testID}>
        {conteudo}
      </View>
    );
  }

  return (
    <Pressable
      onPress={onPress}
      testID={testID}
      accessibilityRole="button"
      style={({ pressed }) => [
        shadows.floating,
        style,
        pressed && { opacity: 0.85 },
      ]}
    >
      {conteudo}
    </Pressable>
  );
}