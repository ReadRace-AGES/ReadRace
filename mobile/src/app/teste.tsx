import { useState } from 'react';
import { Pressable, ScrollView, Switch, Text, View } from 'react-native';
import Svg, { Circle, Path } from 'react-native-svg';

import { EmptyState, type EmptyStateIconProps } from '@/components/EmptyState';
import { SegmentedTabs } from '@/components/SegmentedTabs';
import { colors, sizes, spacing } from '@/theme';

function SearchIcon({ size, color }: EmptyStateIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Circle cx="10.5" cy="10.5" r="6.5" stroke={color} strokeWidth="2" />
      <Path
        d="m16 16 5 5"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
      />
    </Svg>
  );
}

function SadFaceIcon({ size, color }: EmptyStateIconProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <Path
        d="M8 16a5 5 0 0 1 8 0"
        stroke={color}
        strokeWidth="2"
        strokeLinecap="round"
      />
      <Circle cx="12" cy="12" r="9" stroke={color} strokeWidth="2" />
      <Circle cx="8.5" cy="9" r="1" fill={color} />
      <Circle cx="15.5" cy="9" r="1" fill={color} />
    </Svg>
  );
}

const messages = {
  Busca: 'Busque por leitores, comunidades ou livros',
  'Outra lista': 'Nenhum item nesta lista de demonstração.',
  'Texto longo':
    'Esta lista de demonstração ainda não possui itens. Este texto propositalmente longo deve quebrar naturalmente em várias linhas, continuar centralizado e permanecer completamente legível em uma tela estreita.',
};

export default function TesteScreen() {
  const [example, setExample] = useState('Busca');
  const [withAction, setWithAction] = useState(false);
  const [compact, setCompact] = useState(false);
  const [presses, setPresses] = useState(0);
  return (
    <ScrollView
      className="flex-1 bg-surface"
      contentContainerStyle={{
        flexGrow: 1,
        padding: spacing[4],
        gap: spacing[4],
      }}
    >
      <Text className="text-h2 font-inter-bold text-primary">
        EmptyState — #25
      </Text>
      <SegmentedTabs options={Object.keys(messages)} onChange={setExample} />
      <View className="flex-row items-center justify-between gap-4">
        <Text className="flex-1 text-body font-inter text-text">
          Mostrar ação de teste
        </Text>
        <Switch
          accessibilityLabel="Mostrar ação de teste"
          value={withAction}
          onValueChange={setWithAction}
        />
      </View>
      <View className="flex-row items-center justify-between gap-4">
        <Text className="flex-1 text-body font-inter text-text">
          Área compacta
        </Text>
        <Switch
          accessibilityLabel="Área compacta"
          value={compact}
          onValueChange={setCompact}
        />
      </View>
      {withAction && (
        <Text
          accessibilityLiveRegion="polite"
          className="text-bodySmall font-inter text-text-secondary"
        >
          Toques na ação: {presses}
        </Text>
      )}
      <View
        style={{
          flexGrow: compact ? 0 : 1,
          borderWidth: sizes.borderWidth,
          borderColor: colors.border,
        }}
      >
        <EmptyState
          icon={example === 'Busca' ? SearchIcon : SadFaceIcon}
          message={messages[example as keyof typeof messages]}
          action={
            withAction ? (
              <Pressable
                accessibilityRole="button"
                onPress={() => setPresses((value) => value + 1)}
                className="rounded-pill bg-primary px-4 py-3"
              >
                <Text className="text-center text-body font-inter-bold text-text-inverse">
                  Ação de teste
                </Text>
              </Pressable>
            ) : undefined
          }
        />
      </View>
    </ScrollView>
  );
}
