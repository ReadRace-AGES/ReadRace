import { Image } from 'expo-image';
import type { ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import Svg, {
  Circle,
  Defs,
  Ellipse,
  LinearGradient,
  Path,
  Rect,
  Stop,
} from 'react-native-svg';

const cenario = require('../../../assets/images/perfil-cenario.png');

/** Proporção do topo do perfil, a do cenário original. */
export const PROPORCAO_CENARIO = 786 / 722;

/**
 * Fundos que o leitor escolhe para o topo do perfil. A API grava só a chave; o catálogo de chaves
 * válidas é o mesmo de `AparenciaDoPerfil` no backend. `cenario` é o padrão.
 */
export const PLANOS_DE_FUNDO = [
  { chave: 'cenario', nome: 'Cenário' },
  { chave: 'ceu', nome: 'Céu' },
  { chave: 'por-do-sol', nome: 'Pôr do sol' },
  { chave: 'noite', nome: 'Noite' },
  { chave: 'floresta', nome: 'Floresta' },
  { chave: 'oceano', nome: 'Oceano' },
] as const;

export const PLANO_DE_FUNDO_PADRAO = 'cenario';

// As cores são da arte de cada fundo, não do tema: valem só para estes desenhos, como as de um PNG.
const ESTRELAS = [
  [80, 90],
  [190, 60],
  [260, 170],
  [370, 80],
  [470, 200],
  [120, 260],
  [330, 300],
  [720, 290],
  [540, 60],
  [700, 60],
];

/** Desenha o fundo ocupando todo o pai; uma chave que o app não conhece mostra o cenário. */
export function PlanoDeFundo({ chave }: { chave?: string | null }) {
  switch (chave) {
    case 'ceu':
      return (
        <Cena topo="#7EC4EE" base="#DDF1FB">
          <Nuvem x={150} y={150} />
          <Nuvem x={560} y={110} />
          <Nuvem x={420} y={270} />
          <Path
            d="M0 540 C200 470 420 520 786 460 L786 722 L0 722 Z"
            fill="#9CCB8A"
          />
          <Path
            d="M0 620 C260 560 520 640 786 590 L786 722 L0 722 Z"
            fill="#7DB86E"
          />
        </Cena>
      );
    case 'por-do-sol':
      return (
        <Cena topo="#F28C5B" base="#F9D5A2">
          <Circle cx={393} cy={480} r={140} fill="#FFE08A" />
          <Path
            d="M0 520 C220 450 460 540 786 480 L786 722 L0 722 Z"
            fill="#B85C6E"
          />
          <Path
            d="M0 620 C280 570 520 650 786 600 L786 722 L0 722 Z"
            fill="#8C3F55"
          />
        </Cena>
      );
    case 'noite':
      return (
        <Cena topo="#18203D" base="#3B3F72">
          {ESTRELAS.map(([cx, cy]) => (
            <Circle
              key={`${cx}-${cy}`}
              cx={cx}
              cy={cy}
              r={4}
              fill="#FFFFFF"
              opacity={0.8}
            />
          ))}
          <Circle cx={610} cy={170} r={64} fill="#F6E7B0" />
          <Circle cx={590} cy={150} r={12} fill="#E8D594" />
          <Circle cx={635} cy={195} r={9} fill="#E8D594" />
          <Path
            d="M0 560 C240 500 480 580 786 520 L786 722 L0 722 Z"
            fill="#2A2F55"
          />
        </Cena>
      );
    case 'floresta':
      return (
        <Cena topo="#CDEBD6" base="#EEF8F0">
          <Path
            d="M0 470 C220 400 480 460 786 400 L786 722 L0 722 Z"
            fill="#9CCFA8"
          />
          {[70, 200, 600, 720].map((x) => (
            <Arvore key={x} x={x} />
          ))}
          <Path
            d="M0 610 C260 570 520 640 786 590 L786 722 L0 722 Z"
            fill="#5E9E6C"
          />
        </Cena>
      );
    case 'oceano':
      return (
        <Cena topo="#9FD6F2" base="#E4F4FB">
          <Circle cx={620} cy={170} r={60} fill="#FFE08A" />
          <Rect y={400} width={786} height={322} fill="#3E8DBF" />
          <Path
            d="M0 450 Q98 425 196 450 T392 450 T588 450 T786 450"
            stroke="#6FB3DA"
            strokeWidth={10}
            fill="none"
          />
          <Path
            d="M0 530 Q98 505 196 530 T392 530 T588 530 T786 530"
            stroke="#6FB3DA"
            strokeWidth={10}
            fill="none"
          />
          <Path
            d="M0 640 C250 600 520 660 786 620 L786 722 L0 722 Z"
            fill="#F3D9A4"
          />
        </Cena>
      );
    default:
      return (
        <Image
          source={cenario}
          style={StyleSheet.absoluteFill}
          contentFit="cover"
          accessible={false}
        />
      );
  }
}

function Cena({
  topo,
  base,
  children,
}: {
  topo: string;
  base: string;
  children: ReactNode;
}) {
  return (
    <Svg
      style={StyleSheet.absoluteFill}
      viewBox="0 0 786 722"
      preserveAspectRatio="xMidYMid slice"
      accessible={false}
    >
      <Defs>
        <LinearGradient id="ceu" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor={topo} />
          <Stop offset="1" stopColor={base} />
        </LinearGradient>
      </Defs>
      <Rect width={786} height={722} fill="url(#ceu)" />
      {children}
    </Svg>
  );
}

function Nuvem({ x, y }: { x: number; y: number }) {
  return (
    <>
      <Ellipse cx={x} cy={y} rx={70} ry={30} fill="#FFFFFF" />
      <Ellipse cx={x - 35} cy={y + 8} rx={40} ry={24} fill="#FFFFFF" />
      <Ellipse cx={x + 20} cy={y - 18} rx={42} ry={30} fill="#FFFFFF" />
    </>
  );
}

function Arvore({ x }: { x: number }) {
  return (
    <>
      <Rect x={x - 10} y={500} width={20} height={60} fill="#6B4A2F" />
      <Path d={`M${x} 330 L${x + 70} 520 L${x - 70} 520 Z`} fill="#3E7D57" />
      <Path d={`M${x} 300 L${x + 50} 420 L${x - 50} 420 Z`} fill="#2F6B48" />
    </>
  );
}
