/**
 * Mesma regra de branco do backend; string.length conta unidades UTF-16. O limite
 * padrão é o do post (2.000); o comentário usa 1.000 (#150).
 */
export function textoPostValido(texto: string, limite = 2000): boolean {
  return (
    texto.length >= 1 &&
    texto.length <= limite &&
    /[^\u0009-\u000D\u001C-\u0020\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000]/u.test(
      texto
    )
  );
}
