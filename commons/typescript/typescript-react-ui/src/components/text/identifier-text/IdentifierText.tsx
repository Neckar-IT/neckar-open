interface IdentifierTextProps {
  value: string;
}

/**
 * A technical identifier (id, subject id, capability key) in small muted monospace, set apart from the readable values around it.
 */
export function IdentifierText({ value }: IdentifierTextProps): React.ReactElement {
  return <span className="text-muted-foreground font-mono text-xs">{value}</span>;
}
