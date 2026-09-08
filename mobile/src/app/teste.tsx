import { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';

import { Slider } from '@/components/Slider';

export default function TesteScreen() {
  const [pages, setPages] = useState(150);
  const [alternateValue, setAlternateValue] = useState(0);

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="p-6">
      <Text className="text-h1 font-inter-bold text-primary">
        Tela de teste
      </Text>
      <Text className="mt-2 text-center text-body text-text-secondary">
        A pilha de navegação está funcionando.
      </Text>
      <View className="mt-8">
        <Text className="font-inter-bold text-h2 text-text">
          Slider — meta de páginas
        </Text>
        <Text className="my-4 font-inter-bold text-h1 text-primary">
          {pages} pág
        </Text>
        <Slider
          minimumValue={10}
          maximumValue={500}
          minimumLabel="10"
          maximumLabel="500+"
          value={pages}
          onValueChange={setPages}
          accessibilityLabel="Meta de páginas"
        />
        <View className="mt-6 gap-2">
          {[150, 700, -20].map((input) => (
            <Pressable
              key={input}
              accessibilityRole="button"
              onPress={() => setPages(input)}
              className="h-button-height justify-center rounded-sm bg-primary px-4"
            >
              <Text className="font-inter-bold text-body text-text-inverse">
                Receber valor {input}
              </Text>
            </Pressable>
          ))}
        </View>
      </View>
      <View className="mt-8">
        <Text className="mb-4 font-inter-bold text-h2 text-text">
          Outra faixa: {alternateValue}
        </Text>
        <Slider
          minimumValue={-10}
          maximumValue={10}
          minimumLabel="−10"
          maximumLabel="10"
          value={alternateValue}
          onValueChange={setAlternateValue}
          accessibilityLabel="Valor de teste"
        />
      </View>
    </ScrollView>
  );
}
