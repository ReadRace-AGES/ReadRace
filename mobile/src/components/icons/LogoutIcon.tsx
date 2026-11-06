import Svg, { Path } from 'react-native-svg';

import { colors, sizes } from '@/theme';

export type LogoutIconProps = {
  size?: number;
  color?: string;
};

const GLIFO = { x: 120, y: -840, largura: 720, altura: 720 };
const SAIR_PATH =
  'M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h280v80H200v560h280v80H200Zm440-160-55-58 102-102H360v-80h327L585-622l55-58 200 200-200 200Z';

export function LogoutIcon({
  size = sizes.icon,
  color = colors.danger,
}: LogoutIconProps) {
  return (
    <Svg
      width={size}
      height={size}
      viewBox={`${GLIFO.x} ${GLIFO.y} ${GLIFO.largura} ${GLIFO.altura}`}
    >
      <Path d={SAIR_PATH} fill={color} />
    </Svg>
  );
}
