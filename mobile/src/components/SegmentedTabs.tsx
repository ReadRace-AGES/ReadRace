import { useState } from 'react';
import { sizes } from '@/theme';
import { Pressable, Text, View } from 'react-native';

type SegmentedTabsProps = {
  options: string[];
  initialOption?: string;
  /**
   * Aba controlada por quem usa: com `value`, a pílula segue o valor recebido, e quem usa pode
   * trocar a aba de fora (ex.: a Busca aberta já na aba Usuários). Sem ele, vale `initialOption`.
   */
  value?: string;
  onChange: (option: string) => void;
};

export function SegmentedTabs({
  options,
  initialOption,
  value,
  onChange,
}: SegmentedTabsProps) {
  const [interna, setInterna] = useState(initialOption ?? options[0]);
  const selecionada = value ?? interna;

  if (options.length < 2 || options.length > 3) {
    throw new Error(
      `SegmentedTabs aceita 2 ou 3 opções, recebeu ${options.length}.`
    );
  }

  function selecionar(option: string) {
    if (option === selecionada) return;
    setInterna(option);
    onChange(option);
  }

  return (
    <View className="flex-row self-stretch rounded-pill bg-surface-muted p-1">
      {options.map((option) => {
        const ativa = option === selecionada;
        return (
          <Pressable
            key={option}
            accessibilityRole="tab"
            accessibilityLabel={option}
            accessibilityState={{ selected: ativa }}
            style={{
              minHeight: sizes.searchInputHeight,
              flexGrow: 1,
              flexShrink: 1,
            }}
            onPress={() => selecionar(option)}
            className={`items-center justify-center rounded-pill px-2 py-2 ${
              ativa ? 'bg-surface' : ''
            }`}
          >
            <Text
              className={`text-center text-bodySmall font-inter-bold ${ativa ? 'text-primary' : 'text-text-secondary'}`}
            >
              {option}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}
