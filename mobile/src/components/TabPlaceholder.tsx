import { Text, View } from 'react-native';

type TabPlaceholderProps = {
  title: string;
};

export function TabPlaceholder({ title }: TabPlaceholderProps) {
  return (
    <View className="flex-1 items-center justify-center bg-surface p-6">
      <Text className="text-h1 font-inter-bold text-primary">{title}</Text>
      <Text className="mt-2 text-body text-text-secondary">Tela raiz da aba</Text>
    </View>
  );
}
