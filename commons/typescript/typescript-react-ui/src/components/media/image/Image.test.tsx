import { fireEvent, render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { Image } from "./Image";

describe("Image", () => {
  it("should show fallback icon when src is null", () => {
    render(<Image src={null} />);

    expect(screen.getByTestId("image-fallback-icon")).toBeInTheDocument();
    expect(screen.getByRole("img")).toHaveClass("invisible");
  });

  it("should show fallback icon when src is undefined", () => {
    render(<Image src={undefined} />);

    expect(screen.getByTestId("image-fallback-icon")).toBeInTheDocument();
    expect(screen.getByRole("img")).toHaveClass("invisible");
  });

  it("should show loading state while image is loading", () => {
    render(<Image src="https://example.com/image.jpg" />);

    expect(screen.getByTestId("image-loading-icon")).toBeInTheDocument();
    expect(screen.getByRole("img")).toHaveClass("invisible");
    expect(screen.getByRole("img")).toHaveAttribute("src", "https://example.com/image.jpg");
  });

  it("should show img element after successful load", () => {
    render(<Image src="https://example.com/image.jpg" alt="Test image" />);

    fireEvent.load(screen.getByRole("img"));

    const img = screen.getByRole("img");
    expect(img).not.toHaveClass("invisible");
    expect(img).toHaveAttribute("src", "https://example.com/image.jpg");
    expect(img).toHaveAttribute("alt", "Test image");
  });

  it("should show fallback icon after load error", () => {
    render(<Image src="https://example.com/broken.jpg" />);

    fireEvent.error(screen.getByRole("img"));

    expect(screen.getByTestId("image-fallback-icon")).toBeInTheDocument();
    expect(screen.getByRole("img")).toHaveClass("invisible");
  });

  it("should pass additional props to the img element", () => {
    render(<Image src="https://example.com/image.jpg" data-testid="my-image" referrerPolicy="no-referrer" />);

    fireEvent.load(screen.getByTestId("my-image"));

    const img = screen.getByTestId("my-image");
    expect(img).not.toHaveClass("invisible");
    expect(img).toHaveAttribute("referrerpolicy", "no-referrer");
  });

  it("should set referrerPolicy on the img element", () => {
    render(<Image src="https://example.com/image.jpg" referrerPolicy="no-referrer" />);

    expect(screen.getByRole("img")).toHaveAttribute("referrerpolicy", "no-referrer");
  });

  it("should reset to loading state when src changes", () => {
    const { rerender } = render(<Image src="https://example.com/first.jpg" />);

    fireEvent.load(screen.getByRole("img"));
    expect(screen.getByRole("img")).not.toHaveClass("invisible");

    rerender(<Image src="https://example.com/second.jpg" />);

    const img = screen.getByRole("img");
    expect(img).toHaveAttribute("src", "https://example.com/second.jpg");

    fireEvent.load(img);
    expect(img).not.toHaveClass("invisible");
  });

  it("should not update state after unmount", () => {
    const { unmount } = render(<Image src="https://example.com/image.jpg" />);

    expect(() => {
      unmount();
    }).not.toThrow();
  });
});
