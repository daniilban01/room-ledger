// Soft background colors; dark text stays readable on all of them.
const palette = ['#dce6ff', '#ffe1cf', '#d7f0df', '#eeddfb', '#fff0c2', '#d6eef3']

type Props = { name: string; id: string; size?: 'sm' | 'md' }

/** Initials in a colored circle. The color comes from the user id, so a person always has the same one. */
export function Avatar({ name, id, size = 'md' }: Props) {
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')

  let hash = 0
  for (const char of id) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  }

  return (
    // aria-hidden: the name is always written next to the avatar, so screen readers would hear it twice.
    <span
      aria-hidden="true"
      className={`grid shrink-0 place-items-center rounded-full font-semibold text-ink ${
        size === 'sm' ? 'size-8 text-xs' : 'size-10 text-sm'
      }`}
      style={{ backgroundColor: palette[hash % palette.length] }}
    >
      {initials}
    </span>
  )
}
