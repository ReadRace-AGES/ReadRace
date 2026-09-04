import type { BottomTabBarProps } from "expo-router/js-tabs";
import type { ComponentType } from "react";
import { Pressable, View } from "react-native";

import { BookTabIcon, HomeTabIcon, PersonTabIcon, SearchTabIcon, StarTabIcon } from "@/components/TabIcons";

// Valores extraídos do componente "navbar" no Figma via Copy as SVG / Dev Mode
// (referência oficial "navbar.png" do design), não aproximados.
const TAB_COLORS = {
  active: "#732634",
  background: "#FEFEFE",
  border: "#F4EEEE",
} as const;

const PILL_SHADOW = [
  { offsetX: 0, offsetY: 4, blurRadius: 4, color: "rgba(0,0,0,0.07)" },
  { offsetX: 0, offsetY: -2, blurRadius: 4, color: "rgba(0,0,0,0.07)" },
];

type IconComponent = ComponentType<{ active: boolean; size?: number }>;

type TabDefinition = {
  routeName: string;
  Icon: IconComponent;
  accessibilityLabel: string;
};

const TAB_ORDER: readonly TabDefinition[] = [
  { routeName: "feed", Icon: HomeTabIcon, accessibilityLabel: "Feed" },
  { routeName: "meus-livros", Icon: BookTabIcon, accessibilityLabel: "Meus Livros" },
  { routeName: "buscar", Icon: SearchTabIcon, accessibilityLabel: "Buscar" },
  { routeName: "desafios", Icon: StarTabIcon, accessibilityLabel: "Desafios" },
  { routeName: "perfil", Icon: PersonTabIcon, accessibilityLabel: "Perfil" },
];

export function BottomTabBar({ state, descriptors, navigation, insets }: BottomTabBarProps) {
  const focusedRouteName = state.routes[state.index]?.name;

  return (
    <View className="items-center px-5" style={{ paddingTop: 12, paddingBottom: insets.bottom || 22 }}>
      <View
        className="flex-row items-center rounded-full border"
        style={{
          backgroundColor: TAB_COLORS.background,
          borderColor: TAB_COLORS.border,
          columnGap: 24,
          paddingVertical: 14,
          paddingHorizontal: 25,
          boxShadow: PILL_SHADOW,
        }}
      >
        {TAB_ORDER.map((tab) => {
          const route = state.routes.find((r) => r.name === tab.routeName);
          if (!route) return null;

          const isFocused = focusedRouteName === tab.routeName;
          const Icon = tab.Icon;

          const onPress = () => {
            const event = navigation.emit({
              type: "tabPress",
              target: route.key,
              canPreventDefault: true,
            });

            if (!isFocused && !event.defaultPrevented) {
              navigation.navigate(route.name, route.params);
            }
          };

          return (
            <Pressable
              key={route.key}
              onPress={onPress}
              hitSlop={12}
              accessibilityRole="button"
              accessibilityState={isFocused ? { selected: true } : {}}
              accessibilityLabel={tab.accessibilityLabel}
              className="items-center justify-center"
            >
              <Icon active={isFocused} size={22} />
              <View
                className="mt-1 rounded-full"
                style={{
                  width: 24,
                  height: 3.5,
                  backgroundColor: isFocused ? TAB_COLORS.active : "transparent",
                }}
              />
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

export default BottomTabBar;
