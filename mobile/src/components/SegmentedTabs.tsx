import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';

type SegmentedTabsProps = {
  options: string[];
  initialOption?: string;
  onChange: (option: string) => void;
};

export function SegmentedTabs({ options, initialOption, onChange }: SegmentedTabsProps) {
  const [selecionada, setSelecionada] = useState(initialOption ?? options[0]);

  if (options.length < 2 || options.length > 3) {
    throw new Error(`SegmentedTabs aceita 2 ou 3 opções, recebeu ${options.length}.`);
  }

  function selecionar(option: string) {
    if (option === selecionada) return;
    setSelecionada(option);
    onChange(option);
  }

  return (
    <View className="flex-row self-stretch rounded-pill bg-surface-muted p-1">
      {options.map((option) => {
        const ativa = option === selecionada;
        return (
          <Pressable
            key={option}
            onPress={() => selecionar(option)}
            className={`flex-1 items-center justify-center rounded-pill px-2 py-2 ${
              ativa ? 'bg-surface' : ''
            }`}
          >
            <Text
              numberOfLines={1}
              className={`text-bodySmall font-inter-bold ${ativa ? 'text-primary' : 'text-text-secondary'}`}
            >
              {option}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}
