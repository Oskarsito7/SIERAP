/** Nota con dos decimales y coma decimal, como en los wireframes (p. ej. "4,52"). */
export function formatScore(score: number): string {
  return score.toFixed(2).replace('.', ',')
}
