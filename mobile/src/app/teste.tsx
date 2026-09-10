import { useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BookCover } from '@/components/BookCover';
import { spacing } from '@/theme';

const cover = 'https://covers.openlibrary.org/b/isbn/9780451524935-L.jpg';
const brokenCover =
  'https://covers.openlibrary.org/b/isbn/readrace-inexistente-L.jpg?default=false';

export default function TesteScreen() {
  const [presses, setPresses] = useState(0);
  const [replacement, setReplacement] = useState(false);
  const [toastVisible, setToastVisible] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const insets = useSafeAreaInsets();

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
          BookCover — tela de teste
        </Text>
        <Text className="font-inter text-body text-text-secondary">
          A pilha de navegação está funcionando.
        </Text>
        <Text className="font-inter-bold text-h2 text-text">Três tamanhos</Text>
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
        <Text className="font-inter-bold text-h2 text-text">
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
        <Text className="font-inter-bold text-h2 text-text">
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
