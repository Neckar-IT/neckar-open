import { cn } from "cn";
import { StarIcon } from "lucide-react";

interface RatingStarsProps {
  rating: number;
  className?: string;
}

/**
 * Displays a rating as 5 star icons, filled up to the given rating value.
 */
export function RatingStars({ rating, className }: RatingStarsProps): React.ReactElement {
  return (
    <span className={cn("inline-flex gap-0.5", className)}>
      {Array.from({ length: 5 }, (_, i) => (
        <StarIcon key={i} className={`size-4 ${i < rating ? "fill-yellow-400 text-yellow-400" : "text-muted-foreground/30"}`} />
      ))}
    </span>
  );
}
