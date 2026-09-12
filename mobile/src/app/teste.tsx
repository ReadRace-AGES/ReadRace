import { useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { Pressable, ScrollView, Switch, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Svg, { Circle, Path } from 'react-native-svg';

import { BookCover } from '@/components/BookCover';
import { EmptyState, type EmptyStateIconProps } from '@/components/EmptyState';
import { SearchIcon } from '@/components/icons/SearchIcon';
import { SegmentedTabs } from '@/components/SegmentedTabs';
import { colors, sizes, spacing } from '@/theme';

const cover = 'https://covers.openlibrary.org/b/isbn/9780451524935-L.jpg';
const brokenCover =
  'https://covers.openlibrary.org/b/isbn/readrace-inexistente-L.jpg?default=false';

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
  // BookCover (#19)
  const [presses, setPresses] = useState(0);
  const [replacement, setReplacement] = useState(false);
  const [toastVisible, setToastVisible] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const insets = useSafeAreaInsets();

  // EmptyState (#25)
  const [example, setExample] = useState('Busca');
  const [withAction, setWithAction] = useState(false);
  const [compact, setCompact] = useState(false);
  const [actionPresses, setActionPresses] = useState(0);

  useFocusEffect(
    useCallback(
      () => () => {
        if (timer.current) clearTimeout(timer.current);
        timer.current = null;
        setToastVisible(false);
      },
      []
    )
  );

  // Demonstracao local: a tela de produto passa o disparador do Toast #29.
  function showToast() {
    if (timer.current) clearTimeout(timer.current);
    setToastVisible(true);
    timer.current = setTimeout(() => {
      setToastVisible(false);
      timer.current = null;
    }, 2500);
  }

  return (
    <View className="flex-1 bg-surface">
      <ScrollView
        contentContainerClassName="p-6 gap-6"
        contentContainerStyle={{
          paddingBottom: insets.bottom + spacing[10] * 2,
        }}
      >
        <Text className="font-inter-bold text-h1 text-primary">
          Tela de teste
        </Text>
        <Text className="font-inter text-body text-text-secondary">
          A pilha de navegação está funcionando.
        </Text>

        <Text className="font-inter-bold text-h2 text-primary">
          BookCover — #19
        </Text>
        <Text className="font-inter-bold text-h3 text-text">Três tamanhos</Text>
        <View className="flex-row flex-wrap items-end gap-4">
          {(['thumbnail', 'grid', 'featured'] as const).map((size) => (
            <View key={size} className="gap-2">
              <BookCover
                size={size}
                source={cover}
                accessibilityLabel={`1984 — ${size}`}
                onPress={() => setPresses((count) => count + 1)}
              />
              <Text className="font-inter text-caption text-text">{size}</Text>
            </View>
          ))}
        </View>
        <Text className="font-inter text-body text-text">
          Toques nas capas: {presses}
        </Text>
        <Text className="font-inter-bold text-h3 text-text">
          Sem capa e falha de imagem
        </Text>
        <View className="flex-row flex-wrap gap-4">
          <BookCover accessibilityLabel="Livro sem capa" />
          <BookCover
            source={replacement ? cover : brokenCover}
            accessibilityLabel="Capa com troca de fonte"
          />
        </View>
        <Pressable
          accessibilityRole="button"
          onPress={() => setReplacement((value) => !value)}
          className="rounded-sm bg-primary px-4 py-3"
        >
          <Text className="font-inter-bold text-body text-text-inverse">
            {replacement ? 'Usar URL inválida' : 'Trocar por capa válida'}
          </Text>
        </Pressable>
        <Text className="font-inter-bold text-h3 text-text">
          Livros favoritos
        </Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerClassName="gap-4"
        >
          <BookCover
            source={cover}
            accessibilityLabel="1984 favorito"
            onPress={() => setPresses((count) => count + 1)}
          />
          <BookCover variant="add-favorite" onPress={showToast} />
        </ScrollView>

        <Text className="font-inter-bold text-h2 text-primary">
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
            Toques na ação: {actionPresses}
          </Text>
        )}
        {/* Fora do modo compacto, a area simula a altura que uma lista deixaria livre. */}
        <View
          style={{
            minHeight: compact ? undefined : spacing[10] * 8,
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
                  onPress={() => setActionPresses((value) => value + 1)}
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
      {toastVisible && (
        <View
          pointerEvents="none"
          className="absolute left-0 right-0 items-center px-6"
          style={{ bottom: insets.bottom + spacing[6] }}
        >
          <View
            accessibilityRole="alert"
            accessibilityLiveRegion="polite"
            className="rounded-pill bg-primary px-5 py-3"
          >
            <Text className="font-inter text-bodySmall text-text-inverse">
              Funcionalidade em desenvolvimento
            </Text>
          </View>
        </View>
      )}
    </View>
  );
}
