import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  SafeAreaView,
  ScrollView,
} from 'react-native';
import { SearchInput } from '../components/searchInput'; // Ajuste o caminho se necessário

export default function SearchInputDemoScreen() {
  const [searchValue1, setSearchValue1] = useState('');
  const [searchValue2, setSearchValue2] = useState('');
  const [searchValue3, setSearchValue3] = useState('');

  return (
    <SafeAreaView style={styles.screen}>
      <ScrollView 
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <Text style={styles.title}>Validação dos Critérios de Aceite</Text>
        <Text style={styles.subtitle}>
          Digite textos curtos, longos (para testar a rolagem sem quebrar o layout) e apague tudo para ver a dica voltar.
        </Text>

        <View style={styles.testSection}>
          <Text style={styles.sectionLabel}>Tela: Busca</Text>
          <SearchInput 
            placeholder="Nome de usuário ou comunidade" 
            variant="default"
            />
        </View>

        <View style={styles.testSection}>
          <Text style={styles.sectionLabel}>Tela: Desafiar</Text>
          <SearchInput 
        placeholder="Buscar pelo @nome..." 
        variant="challenge"
        />
        </View>

        <View style={styles.testSection}>
          <Text style={styles.sectionLabel}>Tela: Escolher Livro</Text>
          <SearchInput 
            placeholder="Buscar título ou autor" 
            variant="book"
            />
        </View>

        <View style={styles.testSection}>
          <Text style={styles.sectionLabel}>Busca, Estado: Desabilitado</Text>
          <SearchInput
            placeholder="Desabilitado"
            disabled={true}
          />
        </View>

        <View style={styles.testSection}>
          <Text style={styles.sectionLabel}>Desafiar, Estado: Desabilitado</Text>
          <SearchInput
            placeholder="Desabilitado"
            disabled={true}
            variant="challenge"
          />
        </View>

        <View style={styles.testSection}>
          <Text style={styles.sectionLabel}>Buscar Livro, Estado: Desabilitado</Text>
          <SearchInput
            placeholder="Desabilitado"
            disabled={true}
            variant="book"
          />
        </View>

      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#F9F8F6',
  },
  scrollContent: {
    padding: 24,
    gap: 32,
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#1F2937',
  },
  subtitle: {
    fontSize: 14,
    color: '#4B5563',
    lineHeight: 20,
    marginTop: -24, 
  },
  testSection: {
    gap: 12,
  },
  sectionLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#374151',
  },
  feedbackText: {
    fontSize: 12,
    color: '#6B7280',
    marginTop: -4,
  },
  feedbackValue: {
    fontWeight: 'bold',
    color: '#111827',
  },
});