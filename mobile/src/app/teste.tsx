import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';

export default function TesteScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Tela de teste</Text>
      <Text style={styles.subtitle}>A pilha de navegação está funcionando.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    backgroundColor: '#FEFEFE',
    flex: 1,
    justifyContent: 'center',
    padding: 24,
  },
  title: {
    color: '#732634',
    fontSize: 28,
    fontWeight: '700',
  },
  subtitle: {
    color: '#666666',
    fontSize: 16,
    marginTop: 8,
    textAlign: 'center',
  },
});