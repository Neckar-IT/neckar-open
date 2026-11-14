import { cn } from "cn";
import { ImageIcon, ImageOffIcon } from "lucide-react";
import React from "react";

interface ImageProps extends Omit<React.ComponentProps<"img">, "src"> {
  src?: string | null | undefined;
}

type ImageLoadingStatus = "idle" | "loading" | "loaded" | "error";

/**
 * Image Component that renders an image or a fallback if the image could not be loaded
 */
export function Image(props: ImageProps) {
  const { alt, src, className, ...rest } = props;
  const [status, setStatus] = React.useState<ImageLoadingStatus>("loading");

  return (
    <div className={cn("relative h-full w-full overflow-hidden", className)}>
      {status !== "loaded" && (
        <div className="absolute inset-0 flex items-center justify-center">
          {status === "error" || src === null || src === undefined ? (
            <ImageOffIcon data-testid="image-fallback-icon" className="text-muted-foreground h-8 w-8" />
          ) : (
            <ImageIcon data-testid="image-loading-icon" className="text-muted-foreground h-8 w-8 animate-pulse" />
          )}
        </div>
      )}

      <img
        {...rest}
        src={src ?? undefined}
        alt={alt}
        onLoad={() => setStatus("loaded")}
        onError={() => setStatus("error")}
        className={cn("h-full w-full object-contain", { invisible: status !== "loaded" })}
      />
    </div>
  );
}
