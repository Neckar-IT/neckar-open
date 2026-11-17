import { render, screen } from "@test/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { entityPropertyRenderer } from "./EntityPropertyRenderer";

interface Book {
  title: string | null;
}

const renderTitle = vi.fn(({ entity }: { entity: Book }) => (entity.title == null ? null : <em>{entity.title}</em>));
const BookTitle = entityPropertyRenderer("book")<Book>({ displayName: "BookTitle", render: renderTitle });

describe("entityPropertyRenderer", () => {
  beforeEach(() => {
    renderTitle.mockClear();
  });

  it("renders a skeleton without calling render while the entity is loading", () => {
    const { container } = render(<BookTitle book={undefined} />);

    expect(container.querySelector(".animate-pulse")).not.toBeNull();
    expect(renderTitle).not.toHaveBeenCalled();
  });

  it("renders NoValue without calling render when the entity is absent", () => {
    const { container } = render(<BookTitle book={null} />);

    expect(screen.getByText("-")).toBeVisible();
    expect(container.querySelector(".animate-pulse")).toBeNull();
    expect(renderTitle).not.toHaveBeenCalled();
  });

  it("renders NoValue when render returns null", () => {
    render(<BookTitle book={{ title: null }} />);

    expect(screen.getByText("-")).toBeVisible();
    expect(renderTitle).toHaveBeenCalledOnce();
  });

  it("renders the element that render returns", () => {
    render(<BookTitle book={{ title: "Dune" }} />);

    expect(screen.getByText("Dune").tagName).toBe("EM");
    expect(screen.queryByText("-")).toBeNull();
  });
});
