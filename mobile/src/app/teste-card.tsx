import { useState } from "react";
import { ScrollView, StyleSheet, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { Card } from "@/components/Card";
import { colors, spacing, textStyles } from "@/theme";

/** Demonstração manual dos critérios de aceite da #17. */
export default function TesteCardScreen() {
  const insets = useSafeAreaInsets();
  const [toques, setToques] = useState(0);

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={[
        styles.content,
        {
          paddingTop: insets.top + spacing[4],
          paddingBottom: insets.bottom + spacing[4],
        },
      ]}
    >
      <Text style={styles.title}>Card — demonstração</Text>
      <Card>
        <Text style={styles.text}>Conteúdo curto, sem ação de toque.</Text>
      </Card>
      <Card>
        <Text style={styles.text}>
          Este cartão acompanha a altura do conteúdo. O espaçamento interno e os
          cantos são os mesmos do cartão curto. O texto deve quebrar
          naturalmente, inclusive em uma tela estreita ou com fonte ampliada.
          {"\n\n"}
          Não existe altura fixa nem rolagem dentro do cartão. A rolagem desta
          página pertence à tela de demonstração.
        </Text>
      </Card>
      <Card
        onPress={() => setToques((value) => value + 1)}
        accessibilityLabel="Contar toque no card"
      >
        <Text style={styles.text}>
          Toque neste cartão: o fundo muda enquanto estiver pressionado.
        </Text>
      </Card>
      <Text accessibilityLiveRegion="polite" style={styles.text}>
        Toques registrados: {toques}
      </Text>
      <Card>
        <Text style={styles.text}>
          O bloco abaixo é propositalmente largo e deve ficar recortado.
        </Text>
        <View style={styles.wideContent} />
      </Card>
      <Text style={styles.text}>
        Entre este texto e o próximo, nenhum cartão vazio deve aparecer.
      </Text>
      <Card />
      <Card>{null}</Card>
      <Card>{false}</Card>
      <Card>{"   "}</Card>
      <Card>
        <>
          {null}
          {false}
        </>
      </Card>
      <Text style={styles.text}>Fim dos exemplos vazios.</Text>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.surface },
  content: { paddingHorizontal: spacing[6], gap: spacing[4] },
  title: { ...textStyles.h2, color: colors.text },
  text: { ...textStyles.body, color: colors.text },
  wideContent: {
    width: "200%",
    height: spacing[10],
    marginTop: spacing[3],
    backgroundColor: colors.primary,
  },
});
