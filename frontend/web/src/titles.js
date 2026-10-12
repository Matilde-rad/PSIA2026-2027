// MovieLens files titles under their main word: "Matrix, The (1999)". People read "The Matrix (1999)".
export function prettyTitle(title) {
  return title.replace(/^([^()]+), (The|An|A)((?: \(.*)?)$/, '$2 $1$3')
}
