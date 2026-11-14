/**
 * Imports
 */

import { elementAt, fireEvent, render, screen } from "@test/test-utils";
import { FormProvider, useForm, useWatch } from "react-hook-form";
import { describe, expect, it } from "vitest";
import { z } from "zod";
import { RatingInputFormField } from "./RatingInputFormField";

/**
 * Tests
 *
 * The field is a radiogroup of star buttons bound to react-hook-form. A probe renders the current
 * form value so the assertions can observe what the field wrote.
 */

const schema = z.object({ rating: z.number() });

function RatingProbe() {
  const value = useWatch({ name: "rating" });
  return <output data-testid="rating-value">{String(value ?? "")}</output>;
}

function Harness({ initialRating = 0, maxRating }: { initialRating?: number; maxRating?: number }) {
  const methods = useForm({ defaultValues: { rating: initialRating } });
  return (
    <FormProvider {...methods}>
      <RatingInputFormField fieldName="rating" formSchema={schema} label="Rating" maxRating={maxRating} />
      <RatingProbe />
    </FormProvider>
  );
}

const ratingValue = () => screen.getByTestId("rating-value").textContent;

describe("RatingInputFormField", () => {
  it("renders five stars by default", () => {
    render(<Harness />);

    expect(screen.getAllByRole("radio")).toHaveLength(5);
  });

  it("respects a custom maxRating", () => {
    render(<Harness maxRating={3} />);

    expect(screen.getAllByRole("radio")).toHaveLength(3);
  });

  it("marks the current rating as checked", () => {
    render(<Harness initialRating={2} />);

    expect(screen.getByRole("radio", { name: "2 / 5" })).toHaveAttribute("aria-checked", "true");
    expect(screen.getByRole("radio", { name: "3 / 5" })).toHaveAttribute("aria-checked", "false");
  });

  it("sets the rating when a star is clicked", () => {
    render(<Harness />);

    fireEvent.click(screen.getByRole("radio", { name: "3 / 5" }));

    expect(ratingValue()).toBe("3");
  });

  it("increments the rating with ArrowRight", () => {
    render(<Harness initialRating={2} />);

    fireEvent.keyDown(elementAt(screen.getAllByRole("radio"), 0), { key: "ArrowRight" });

    expect(ratingValue()).toBe("3");
  });

  it("decrements the rating with ArrowLeft", () => {
    render(<Harness initialRating={2} />);

    fireEvent.keyDown(elementAt(screen.getAllByRole("radio"), 0), { key: "ArrowLeft" });

    expect(ratingValue()).toBe("1");
  });

  it("clamps the increment at maxRating", () => {
    render(<Harness initialRating={5} />);

    fireEvent.keyDown(elementAt(screen.getAllByRole("radio"), 0), { key: "ArrowRight" });

    expect(ratingValue()).toBe("5");
  });

  it("clamps the decrement at one", () => {
    render(<Harness initialRating={1} />);

    fireEvent.keyDown(elementAt(screen.getAllByRole("radio"), 0), { key: "ArrowLeft" });

    expect(ratingValue()).toBe("1");
  });
});
