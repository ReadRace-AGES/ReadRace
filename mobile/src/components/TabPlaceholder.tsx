import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';

type TabPlaceholderProps = {
  title: string;
};

export function TabPlaceholder({ title }: TabPlaceholderProps) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.subtitle}>Tela raiz da aba</Text>
      <Pressable style={styles.button} onPress={() => router.push('../teste')}>
        <Text style={styles.buttonText}>Abrir tela de teste</Text>
      </Pressable>
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
  },
  button: {
    backgroundColor: '#732634',
    borderRadius: 8,
    marginTop: 24,
    paddingHorizontal: 18,
    paddingVertical: 12,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '600',
  },
});