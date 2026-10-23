import Svg, {
  Circle,
  ClipPath,
  Defs,
  Ellipse,
  Line,
  Path,
  Rect,
} from 'react-native-svg';

type Cabelo =
  | 'curto'
  | 'longo'
  | 'coque'
  | 'cacheado'
  | 'gorro'
  | 'careca'
  | 'moicano'
  | 'afro'
  | 'trancas';

type Expressao = 'sorriso' | 'piscando' | 'risada' | 'surpreso';

type Acessorio =
  'oculos' | 'oculos-sol' | 'chapeu-bruxo' | 'coroa' | 'laco' | 'fone';

type DetalheRoupa = 'gola' | 'listras' | 'cachecol' | 'livro';

type Visual = {
  nome: string;
  pele: string;
  cabelo: string;
  estilo: Cabelo;
  roupa: string;
  fundo: string;
  /** @default 'sorriso' */
  rosto?: Expressao;
  acessorio?: Acessorio;
  detalhe?: DetalheRoupa;
  /** Cor do acessório e do detalhe da roupa (laço, cachecol, listras, livro...). */
  destaque?: string;
  barba?: boolean;
  sardas?: boolean;
};

// As cores são da arte do boneco, não do tema: valem só para este desenho, como as de um PNG.
const TRACO = '#1E1E1E';
const BOCHECHA = '#E8828F';
const SARDA = '#B9744F';
const BOCA = '#8C2F3C';
const BRANCO = '#FFFFFF';
const DOURADO = '#F2C230';

/**
 * Bonecos que o leitor escolhe como avatar. A API grava só a chave (`boneco-1`...) no lugar da URL
 * da foto; o catálogo de chaves válidas é o mesmo de `AparenciaDoPerfil` no backend.
 */
const BONECOS = {
  'boneco-1': {
    nome: 'Clássico',
    pele: '#F2C9A0',
    cabelo: '#4A2C1A',
    estilo: 'curto',
    roupa: '#732634',
    fundo: '#F6E2E2',
    detalhe: 'gola',
  },
  'boneco-2': {
    nome: 'Cachos',
    pele: '#8D5A3B',
    cabelo: '#1E1E1E',
    estilo: 'cacheado',
    roupa: '#E9A93B',
    fundo: '#E3EEF9',
    rosto: 'risada',
    acessorio: 'laco',
    destaque: '#C9425B',
  },
  'boneco-3': {
    nome: 'Estudiosa',
    pele: '#F5D7BC',
    cabelo: '#C9772E',
    estilo: 'longo',
    roupa: '#4C8C6A',
    fundo: '#FFF1D6',
    acessorio: 'oculos',
    detalhe: 'livro',
    destaque: '#732634',
    sardas: true,
  },
  'boneco-4': {
    nome: 'Coque',
    pele: '#C68B5E',
    cabelo: '#2B2B2B',
    estilo: 'coque',
    roupa: '#C9425B',
    fundo: '#E6F4EA',
    rosto: 'piscando',
    detalhe: 'listras',
    destaque: '#F6E2E2',
  },
  'boneco-5': {
    nome: 'Inverno',
    pele: '#EBC19A',
    cabelo: '#3D6FB6',
    estilo: 'gorro',
    roupa: '#2F3E5C',
    fundo: '#EDE7F6',
    detalhe: 'cachecol',
    destaque: '#C9425B',
  },
  'boneco-6': {
    nome: 'Professor',
    pele: '#5C3A24',
    cabelo: '#1E1E1E',
    estilo: 'curto',
    roupa: '#7B5EA7',
    fundo: '#FCE4D6',
    acessorio: 'oculos',
    detalhe: 'gola',
    barba: true,
  },
  'boneco-7': {
    nome: 'Bruxinho',
    pele: '#F2C9A0',
    cabelo: '#E8C468',
    estilo: 'curto',
    roupa: '#4B2E83',
    fundo: '#DCEFF5',
    rosto: 'risada',
    acessorio: 'chapeu-bruxo',
    destaque: '#4B2E83',
  },
  'boneco-8': {
    nome: 'Realeza',
    pele: '#F5D7BC',
    cabelo: '#E8C468',
    estilo: 'longo',
    roupa: '#B3122E',
    fundo: '#FFF1D6',
    acessorio: 'coroa',
    detalhe: 'gola',
  },
  'boneco-9': {
    nome: 'Roqueira',
    pele: '#C68B5E',
    cabelo: '#E5579A',
    estilo: 'moicano',
    roupa: '#2B2B2B',
    fundo: '#EDE7F6',
    acessorio: 'oculos-sol',
    detalhe: 'listras',
    destaque: '#E5579A',
  },
  'boneco-10': {
    nome: 'Groove',
    pele: '#8D5A3B',
    cabelo: '#3B2416',
    estilo: 'afro',
    roupa: '#4C8C6A',
    fundo: '#FCE4D6',
    acessorio: 'fone',
    destaque: '#E9A93B',
  },
  'boneco-11': {
    nome: 'Tranças',
    pele: '#EBC19A',
    cabelo: '#7A4A2A',
    estilo: 'trancas',
    roupa: '#2E9C8F',
    fundo: '#E6F4EA',
    rosto: 'piscando',
    destaque: '#E9A93B',
    sardas: true,
  },
  'boneco-12': {
    nome: 'Curioso',
    pele: '#D9A67E',
    cabelo: '#A0522D',
    estilo: 'careca',
    roupa: '#E9A93B',
    fundo: '#E3EEF9',
    rosto: 'surpreso',
    detalhe: 'livro',
    destaque: '#3D6FB6',
    barba: true,
  },
} satisfies Record<string, Visual>;

export type ChaveBoneco = keyof typeof BONECOS;

/** Na ordem do catálogo, com o nome que aparece na escolha. */
export const OPCOES_BONECOS = (Object.keys(BONECOS) as ChaveBoneco[]).map(
  (chave) => ({ chave, nome: BONECOS[chave].nome })
);

export function ehBoneco(
  chave: string | null | undefined
): chave is ChaveBoneco {
  return !!chave && Object.prototype.hasOwnProperty.call(BONECOS, chave);
}

const TOPO_CABELO =
  'M30 42 C30 24 40 18 50 18 C60 18 70 24 70 42 C66 34 58 30 50 30 C42 30 34 34 30 42 Z';
const CORPO = 'M16 100 C16 78 31 70 50 70 C69 70 84 78 84 100 Z';
const COM_TOPO: Cabelo[] = ['curto', 'longo', 'coque', 'trancas'];
// O cabelo cobre as laterais da cabeça nesses estilos, então as orelhas ficam escondidas.
const SEM_ORELHAS: Cabelo[] = ['longo', 'afro', 'trancas'];

/** Desenha o boneco num quadrado de lado `size` (sem ele, ocupa o pai); quem chama recorta em círculo. */
export function Boneco({ chave, size }: { chave: ChaveBoneco; size?: number }) {
  const v: Visual = BONECOS[chave];
  const destaque = v.destaque ?? v.roupa;
  return (
    <Svg width={size ?? '100%'} height={size ?? '100%'} viewBox="0 0 100 100">
      <Defs>
        <ClipPath id="corpo">
          <Path d={CORPO} />
        </ClipPath>
      </Defs>
      <Rect width={100} height={100} fill={v.fundo} />

      {/* Cabelo que fica atrás da cabeça */}
      {v.estilo === 'longo' && (
        <Path
          d="M28 40 C28 22 38 16 50 16 C62 16 72 22 72 40 L75 72 L25 72 Z"
          fill={v.cabelo}
        />
      )}
      {v.estilo === 'afro' && <Circle cx={50} cy={36} r={28} fill={v.cabelo} />}
      {v.estilo === 'coque' && <Circle cx={50} cy={16} r={9} fill={v.cabelo} />}
      {!SEM_ORELHAS.includes(v.estilo) && (
        <>
          <Circle cx={30} cy={46} r={4} fill={v.pele} />
          <Circle cx={70} cy={46} r={4} fill={v.pele} />
        </>
      )}

      {/* Pescoço e roupa */}
      <Rect x={44} y={56} width={12} height={16} fill={v.pele} />
      <Path d={CORPO} fill={v.roupa} />
      {v.detalhe === 'listras' && (
        <>
          <Rect
            y={80}
            width={100}
            height={4}
            fill={destaque}
            clipPath="url(#corpo)"
          />
          <Rect
            y={90}
            width={100}
            height={4}
            fill={destaque}
            clipPath="url(#corpo)"
          />
        </>
      )}
      {v.detalhe === 'gola' && (
        <Path d="M41 71 L50 80 L59 71 L55 69 L50 75 L45 69 Z" fill={BRANCO} />
      )}

      {/* Cabeça e cabelo da frente */}
      <Circle cx={50} cy={42} r={20} fill={v.pele} />
      {v.barba && (
        <Path
          d="M31 44 C31 64 42 66 50 66 C58 66 69 64 69 44 C65 55 58 58 50 58 C42 58 35 55 31 44 Z"
          fill={v.cabelo}
        />
      )}
      {COM_TOPO.includes(v.estilo) && <Path d={TOPO_CABELO} fill={v.cabelo} />}
      {v.estilo === 'cacheado' && (
        <>
          <Circle cx={33} cy={34} r={8} fill={v.cabelo} />
          <Circle cx={41} cy={25} r={9} fill={v.cabelo} />
          <Circle cx={51} cy={22} r={9} fill={v.cabelo} />
          <Circle cx={61} cy={25} r={9} fill={v.cabelo} />
          <Circle cx={68} cy={34} r={8} fill={v.cabelo} />
          <Circle cx={30} cy={42} r={5} fill={v.cabelo} />
          <Circle cx={70} cy={42} r={5} fill={v.cabelo} />
        </>
      )}
      {v.estilo === 'moicano' && (
        <Path
          d="M45 30 C44 20 46 12 50 6 C54 12 56 20 55 30 Z"
          fill={v.cabelo}
        />
      )}
      {v.estilo === 'gorro' && (
        <>
          <Path
            d="M29 37 C29 22 39 15 50 15 C61 15 71 22 71 37 Z"
            fill={v.cabelo}
          />
          <Rect x={27} y={32} width={46} height={8} rx={4} fill={v.cabelo} />
          <Circle cx={50} cy={13} r={5} fill={v.cabelo} />
        </>
      )}
      {v.estilo === 'trancas' &&
        [30, 70].map((x) => (
          <Trancas key={x} x={x} cor={v.cabelo} prendedor={destaque} />
        ))}

      <Rosto visual={v} />
      <AcessorioDoBoneco tipo={v.acessorio} cor={destaque} />

      {v.detalhe === 'cachecol' && (
        <>
          <Path d="M38 64 Q50 71 62 64 L63 71 Q50 79 37 71 Z" fill={destaque} />
          <Rect x={53} y={72} width={7} height={16} rx={2} fill={destaque} />
        </>
      )}
      {v.detalhe === 'livro' && (
        <>
          <Rect x={33} y={80} width={34} height={20} rx={2} fill={destaque} />
          <Rect x={35} y={82} width={14} height={18} fill={BRANCO} />
          <Rect x={51} y={82} width={14} height={18} fill={BRANCO} />
          <Circle cx={34} cy={88} r={4} fill={v.pele} />
          <Circle cx={66} cy={88} r={4} fill={v.pele} />
        </>
      )}
    </Svg>
  );
}

function Rosto({ visual }: { visual: Visual }) {
  const rosto = visual.rosto ?? 'sorriso';
  const olhoFechado = (x: number) => (
    <Path
      d={`M${x - 3} 46 Q${x} 42 ${x + 3} 46`}
      stroke={TRACO}
      strokeWidth={1.8}
      strokeLinecap="round"
      fill="none"
    />
  );
  const sorriso = (
    <Path
      d="M45 52 Q50 56 55 52"
      // Sobre a barba o traço escuro some.
      stroke={visual.barba ? BRANCO : TRACO}
      strokeWidth={1.8}
      strokeLinecap="round"
      fill="none"
    />
  );
  return (
    <>
      {visual.sardas &&
        [
          [37, 50],
          [39, 52],
          [41, 50],
          [59, 50],
          [61, 52],
          [63, 50],
        ].map(([cx, cy]) => (
          <Circle key={`${cx}-${cy}`} cx={cx} cy={cy} r={0.9} fill={SARDA} />
        ))}
      {!visual.barba && (
        <>
          <Circle cx={38} cy={51} r={3} fill={BOCHECHA} opacity={0.35} />
          <Circle cx={62} cy={51} r={3} fill={BOCHECHA} opacity={0.35} />
        </>
      )}
      {rosto === 'sorriso' && (
        <>
          <Circle cx={43} cy={45} r={2.2} fill={TRACO} />
          <Circle cx={57} cy={45} r={2.2} fill={TRACO} />
          {sorriso}
        </>
      )}
      {rosto === 'piscando' && (
        <>
          <Circle cx={43} cy={45} r={2.2} fill={TRACO} />
          {olhoFechado(57)}
          {sorriso}
        </>
      )}
      {rosto === 'risada' && (
        <>
          {olhoFechado(43)}
          {olhoFechado(57)}
          <Path d="M44 51 Q50 60 56 51 Z" fill={BOCA} />
        </>
      )}
      {rosto === 'surpreso' && (
        <>
          <Circle cx={43} cy={45} r={2.6} fill={TRACO} />
          <Circle cx={57} cy={45} r={2.6} fill={TRACO} />
          <Circle cx={50} cy={54} r={2.8} fill={BOCA} />
        </>
      )}
    </>
  );
}

function AcessorioDoBoneco({ tipo, cor }: { tipo?: Acessorio; cor: string }) {
  switch (tipo) {
    case 'oculos':
      return (
        <>
          <Circle
            cx={43}
            cy={45}
            r={5}
            stroke={TRACO}
            strokeWidth={1.5}
            fill="none"
          />
          <Circle
            cx={57}
            cy={45}
            r={5}
            stroke={TRACO}
            strokeWidth={1.5}
            fill="none"
          />
          <Line
            x1={48}
            y1={45}
            x2={52}
            y2={45}
            stroke={TRACO}
            strokeWidth={1.5}
          />
        </>
      );
    case 'oculos-sol':
      return (
        <>
          <Rect x={36} y={41} width={12} height={8} rx={3} fill={TRACO} />
          <Rect x={52} y={41} width={12} height={8} rx={3} fill={TRACO} />
          <Line
            x1={48}
            y1={44}
            x2={52}
            y2={44}
            stroke={TRACO}
            strokeWidth={1.5}
          />
          <Line
            x1={39}
            y1={43}
            x2={42}
            y2={43}
            stroke={BRANCO}
            strokeWidth={1}
          />
          <Line
            x1={55}
            y1={43}
            x2={58}
            y2={43}
            stroke={BRANCO}
            strokeWidth={1}
          />
        </>
      );
    case 'chapeu-bruxo':
      return (
        <>
          <Path d="M34 27 C40 22 44 8 60 2 C56 10 58 20 66 27 Z" fill={cor} />
          <Ellipse cx={50} cy={27} rx={26} ry={5} fill={cor} />
          <Rect x={39} y={21} width={22} height={4} fill={DOURADO} />
          <Circle cx={53} cy={12} r={1.5} fill={DOURADO} />
        </>
      );
    case 'coroa':
      return (
        <>
          <Path
            d="M37 25 L36 11 L43 18 L50 8 L57 18 L64 11 L63 25 Z"
            fill={DOURADO}
          />
          <Circle cx={50} cy={19} r={2} fill="#C9425B" />
        </>
      );
    case 'laco':
      return (
        <>
          <Path d="M64 24 L56 18 L57 30 Z" fill={cor} />
          <Path d="M64 24 L72 17 L71 30 Z" fill={cor} />
          <Circle cx={64} cy={24} r={3} fill={cor} />
        </>
      );
    case 'fone':
      return (
        <>
          <Path
            d="M29 44 C29 16 71 16 71 44"
            stroke={cor}
            strokeWidth={4}
            fill="none"
          />
          <Rect x={24} y={38} width={9} height={14} rx={4} fill={cor} />
          <Rect x={67} y={38} width={9} height={14} rx={4} fill={cor} />
        </>
      );
    default:
      return null;
  }
}

function Trancas({
  x,
  cor,
  prendedor,
}: {
  x: number;
  cor: string;
  prendedor: string;
}) {
  return (
    <>
      {[52, 59, 66, 73].map((cy) => (
        <Circle key={cy} cx={x} cy={cy} r={4.2} fill={cor} />
      ))}
      <Circle cx={x} cy={78} r={2} fill={prendedor} />
    </>
  );
}
