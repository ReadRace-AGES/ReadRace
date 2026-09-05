import { Text, View } from 'react-native';

export default function TesteScreen() {
  return (
    <View className="flex-1 items-center justify-center bg-surface p-6">
      <Text className="text-h1 font-inter-bold text-primary">Tela de teste</Text>
      <Text className="mt-2 text-center text-body text-text-secondary">
        A pilha de navegação está funcionando.
      </Text>
    </View>
  );
}
