import React from "react";
import { View, Text, ScrollView } from "react-native";
import { ReadRaceLogo } from "../components/readracelogo";
import { Avatar } from "../components/avatar";
import "../../global.css";

/**
 * Tela interna de referência (dev/design) com o glossário de
 * identidade visual do ReadRace: logo, cores e tipografia.
 *
 * Não faz parte do fluxo final do app — serve como guia de estilo
 * consultável durante o desenvolvimento.
 *
 * Depende de:
 *   npx expo install react-native-svg
 */

type Swatch = {
  hex: string;
  name?: string;
  outline?: boolean;
};

const PRIMARY_COLORS: Swatch[] = [
  { hex: "#732634", name: "Vinho — primária" },
  { hex: "#FEFEFE", name: "Branco — base", outline: true },
];

const TINTS = [
  { hex: "#732634", pct: "100%" },
  { hex: "#A15A68", pct: "80%" },
  { hex: "#BB8891", pct: "60%" },
  { hex: "#D3B3B9", pct: "40%" },
];

const NEUTRAL_COLORS: Swatch[] = [
  { hex: "#000000" },
  { hex: "#B5B5B5" },
  { hex: "#EFEFEF" },
  { hex: "#FFFFFF", outline: true },
];

const OTHER_COLORS: Swatch[] = [
  { hex: "#C9425B" },
  { hex: "#A93736" },
  { hex: "#FDCA7E" },
  { hex: "#FE383C" },
];

function SectionTitle({ children }: { children: string }) {
  return (
    <View className="mt-6 mb-3">
      <Text className="text-[11px] font-extrabold tracking-wider uppercase text-[#732634]">
        {children}
      </Text>
      <View className="w-9 h-[3px] rounded-full bg-[#FE383C] mt-2" />
    </View>
  );
}

function ColorSwatch({ hex, name, outline }: Swatch) {
  return (
    <View className="flex-1">
      <View
        className={`h-14 rounded-xl ${outline ? "border-[1.6px] border-black" : "border border-black/10"}`}
        style={{ backgroundColor: hex }}
      />
      <Text className="text-[11.5px] font-bold text-black mt-1.5 font-mono">
        {hex}
      </Text>
      {name ? (
        <Text className="text-[10px] text-neutral-500 mt-0.5">{name}</Text>
      ) : null}
    </View>
  );
}

export default function IdentidadeVisualScreen() {
  return (
    <View className="flex-1 bg-white">
      {/* Cabeçalho */}
      <View className="bg-[#732634] px-5 pt-6 pb-5">
        <View className="flex-row items-center gap-3">
          <Text className="text-white text-[19px] font-bold tracking-tight">
            Identidade Visual
          </Text>
        </View>
        <Text className="text-[#FDCA7E]/90 text-xs mt-1 pl-[42px]">
          Cores, tipografia e logo do ReadRace
        </Text>
      </View>

      <ScrollView
        className="flex-1"
        contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: 24 }}
        showsVerticalScrollIndicator={false}
      >
        {/* LOGO */}
        <SectionTitle>Logo</SectionTitle>
        <View className="bg-[#732634] rounded-3xl px-5 pt-6 pb-5 items-center">
          <ReadRaceLogo size={104} />
          <Text className="text-[#FDCA7E] font-extrabold italic text-2xl mt-2 tracking-wide">
            Read Race
          </Text>
          <Text className="text-white/65 text-[10.5px] mt-1.5">
            Sua leitura imersiva
          </Text>
        </View>

        {/* CORES PRINCIPAIS */}
        <SectionTitle>Cores principais</SectionTitle>
        <View className="flex-row gap-2.5">
          {PRIMARY_COLORS.map((s) => (
            <ColorSwatch key={s.hex} {...s} />
          ))}
        </View>

        <View className="mt-4 gap-1.5">
          {TINTS.map((t) => (
            <View key={t.pct} className="flex-row items-center gap-2.5">
              <View
                className="flex-1 h-5 rounded"
                style={{ backgroundColor: t.hex }}
              />
              <Text className="text-[10.5px] text-neutral-500 font-semibold w-8 text-right">
                {t.pct}
              </Text>
            </View>
          ))}
        </View>

        {/* NEUTRAS */}
        <SectionTitle>Neutras</SectionTitle>
        <View className="flex-row flex-wrap gap-2">
          {NEUTRAL_COLORS.map((s) => (
            <View key={s.hex} className="w-[23%]">
              <ColorSwatch {...s} />
            </View>
          ))}
        </View>

        {/* OUTRAS CORES */}
        <SectionTitle>Outras cores</SectionTitle>
        <View className="flex-row flex-wrap gap-2">
          {OTHER_COLORS.map((s) => (
            <View key={s.hex} className="w-[23%]">
              <ColorSwatch {...s} />
            </View>
          ))}
        </View>

        {/* TIPOGRAFIA */}
        <SectionTitle>Tipografia</SectionTitle>
        <View className="bg-[#EFEFEF] rounded-2xl px-4 pt-4 pb-5">
          <Text className="text-[13px] font-bold text-[#732634] mb-2.5">
            Inter Regular
          </Text>
          <Text className="text-[13px] text-black leading-6">
            ABCDEFGHIJKLMNOPQRSTUVWXYZ
          </Text>
          <Text className="text-[13px] font-bold text-black leading-6">
            ABCDEFGHIJKLMNOPQRSTUVWXYZ
          </Text>
          <Text className="text-[13px] italic text-black leading-6">
            abcdefghijklmnopqrstuvwxyz
          </Text>

          <Text className="text-[56px] font-extrabold text-[#732634] leading-[56px] mt-3">
            Aa
          </Text>
          <Text className="text-[10px] text-neutral-500 tracking-wide mt-0.5">
            INTER — DISPLAY & TEXTO
          </Text>
        </View>

        {/* AVATAR */}
        <SectionTitle>Avatar</SectionTitle>
        <View className="bg-[#EFEFEF] rounded-2xl px-4 py-5 gap-4">
          <View className="flex-row items-center justify-between">
            <Avatar name="Aninha07" photoUrl="https://i.pravatar.cc/150?img=5" />
            <Avatar name="FBMB123" photoUrl="https://i.pravatar.cc/150?img=12" />
            <Avatar name="Maria_L" />
            <Avatar name="" />
          </View>
          <Text className="text-[10px] text-neutral-500">
            Com foto · com foto · sem foto (inicial &quot;M&quot;) · nome vazio
          </Text>

          <Avatar
            name="Maria_L"
            photoUrl="https://url-invalida.exemplo/nao-existe.jpg"
          />
          <Text className="text-[10px] text-neutral-500 -mt-2">
            Foto que falha ao carregar → cai para a inicial
          </Text>

          <View className="gap-2.5">
            <Avatar name="Aninha07" photoUrl="https://i.pravatar.cc/150?img=5" streak={9} />
            <Avatar name="Maria_L" streak="21 dias" />
            <Avatar name="FBMB123" photoUrl="https://i.pravatar.cc/150?img=12" />
          </View>
          <Text className="text-[10px] text-neutral-500 -mt-2">
            Com badge de sequência (número ou &quot;N dias&quot;) · sem badge
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}
