import type { AtividadeSocial } from './api';

// "-03:00" no fim de `em`: é o fuso de São Paulo que o servidor usou para montar o dia do card.
function deslocamentoEmMinutos(em: string): number {
  const fuso = /([+-])(\d{2}):(\d{2})$/.exec(em);
  if (!fuso) return 0;
  const minutos = Number(fuso[2]) * 60 + Number(fuso[3]);
  return fuso[1] === '-' ? -minutos : minutos;
}

/** O card é de hoje quando o dia de `em` é o dia de agora no mesmo fuso de `em`. */
export function foiHoje(em: string, agora = Date.now()): boolean {
  const deslocamento = deslocamentoEmMinutos(em);
  const hoje = new Date(agora + deslocamento * 60000)
    .toISOString()
    .slice(0, 10);
  return em.slice(0, 10) === hoje;
}

function paginas(n: number) {
  return `${n} ${n === 1 ? 'página' : 'páginas'}`;
}

export function tituloDaAtividade(
  atividade: AtividadeSocial,
  agora = Date.now()
): string {
  if (atividade.tipo === 'conclusao') return 'Terminou o livro';
  const leu = `Leu ${paginas(atividade.paginas)}`;
  return foiHoje(atividade.em, agora) ? `${leu} hoje!` : leu;
}

/** "<título> - <autor>", como no Figma; sem autor, só o título. */
export function linhaDoLivro({ livro }: AtividadeSocial): string {
  return livro.autor ? `${livro.titulo} - ${livro.autor}` : livro.titulo;
}

export function linhaDeNumeros(atividade: AtividadeSocial): string {
  return `${paginas(atividade.paginas)} • +${atividade.xp} XP`;
}
