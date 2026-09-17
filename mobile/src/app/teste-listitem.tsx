import { useState } from 'react';
import { ScrollView, Text } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { ListItem } from '@/components/ListItem';
import { colors, spacing, textStyles } from '@/theme';

const books = [
  [
    'The Midnight Library — um título longo para verificar o truncamento',
    'Matt Haig',
    'Ficção',
  ],
  ['Dune', 'Frank Herbert', 'Sci-Fi'],
  ['1984', 'George Orwell', 'Clássico'],
  ['Atomic Habits', 'James Clear', 'Autoajuda'],
];

export default function TesteListItemScreen() {
  const insets = useSafeAreaInsets();
  const [selected, setSelected] = useState(0);
  const [message, setMessage] = useState(
    'Toque em uma linha ou em Participar.'
  );
  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: colors.surface }}
      contentContainerStyle={{
        padding: spacing[4],
        gap: spacing[4],
        paddingTop: insets.top + spacing[4],
        paddingBottom: insets.bottom + spacing[4],
      }}
    >
      <Text style={textStyles.h2}>ListItem — demonstração</Text>
      <Text accessibilityLiveRegion="polite" style={textStyles.bodySmall}>
        {message}
      </Text>
      <ListItem
        variant="community"
        title="Clube do Livro Quarta-Feira"
        subtitle="Harry Potter e a Pedra Filosofal - J.K Rowling"
        memberCount={43}
        onPress={() => setMessage('Toque no clube')}
      />
      <ListItem
        variant="community"
        title="Conexão Literária"
        subtitle="A Biblioteca da Meia-Noite – Matt Haig"
        memberCount={12}
        onPress={() => setMessage('Toque na comunidade')}
        action={{
          label: 'Participar',
          onPress: () =>
            setMessage('Ação Participar recebida pela demonstração'),
        }}
      />
      <Text style={textStyles.h3}>Seleção controlada pela tela</Text>
      {books.map(([title, subtitle, genre], index) => (
        <ListItem
          key={title}
          variant="book"
          title={title}
          subtitle={subtitle}
          genre={genre}
          selected={selected === index}
          onPress={() => setSelected(index)}
        />
      ))}
      <Text style={textStyles.h3}>Sem informações opcionais</Text>
      <ListItem variant="community" title="Café com Letras" />
      <ListItem variant="book" title="Livro sem capa" />
      <Text style={textStyles.h3}>Imagem indisponível e zero membros</Text>
      <ListItem
        variant="community"
        title="Leitores anônimos"
        imageUrl="https://example.invalid/avatar.png"
        memberCount={0}
      />
      <ListItem
        variant="book"
        title="Capa indisponível"
        imageUrl="https://example.invalid/capa.png"
      />
    </ScrollView>
  );
}
