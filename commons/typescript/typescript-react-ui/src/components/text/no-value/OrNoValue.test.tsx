/**
 * Imports
 */

import { render, screen } from "@test/test-utils";
import { describe, expect, it } from "vitest";
import { OrNoValue, OrNoValueIfUndefined } from "./OrNoValue";
import { RenderIfValueDefined } from "./RenderIfValueDefined";

/**
 * Tests
 */

describe("OrNoValue and OrNoValueIfUndefined", () => {
  it("OrNoValue: children should be rendered", () => {
    const text = "Hello";
    render(<OrNoValue>{text}</OrNoValue>);

    expect(screen.getByText(text)).toBeInTheDocument();
  });

  it("OrNoValue: Default fallback-component should be rendered if children are undefined", () => {
    const undefinedChild = undefined;
    render(<OrNoValue>{undefinedChild}</OrNoValue>);

    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("OrNoValue: Default fallback-component should be rendered if children are null", () => {
    const nullChild = null;
    render(<OrNoValue>{nullChild}</OrNoValue>);

    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("OrNoValue: Custom fallback-component should be rendered if children are undefined", () => {
    const fallbackText = "Hello";
    const undefinedChild = undefined;
    render(<OrNoValue fallback={<span>{fallbackText}</span>}>{undefinedChild}</OrNoValue>);

    expect(screen.getByText(fallbackText)).toBeInTheDocument();
  });

  it("OrNoValue: Custom fallback-component should be rendered if children are null", () => {
    const fallbackText = "Hello";
    const nullChild = null;
    render(<OrNoValue fallback={<span>{fallbackText}</span>}>{nullChild}</OrNoValue>);

    expect(screen.getByText(fallbackText)).toBeInTheDocument();
  });

  it("OrNoValue: Default fallback should be rendered if children are an empty string", () => {
    render(<OrNoValue>{""}</OrNoValue>);

    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("OrNoValue: Default fallback should be rendered if children are a whitespace-only string", () => {
    render(<OrNoValue>{"   "}</OrNoValue>);

    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("OrNoValue: Custom fallback should be rendered if children are an empty string", () => {
    render(<OrNoValue fallback="Unnamed">{""}</OrNoValue>);

    expect(screen.getByText("Unnamed")).toBeInTheDocument();
  });

  it("OrNoValue: Numeric 0 should be rendered as a valid value", () => {
    render(<OrNoValue>{0}</OrNoValue>);

    expect(screen.getByText("0")).toBeInTheDocument();
  });

  it("OrNoValueIfUndefined: children should be rendered if value is defined", () => {
    const text = "Hello";
    render(<OrNoValueIfUndefined value={true}>{text}</OrNoValueIfUndefined>);

    expect(screen.getByText(text)).toBeInTheDocument();
  });

  it("OrNoValueIfUndefined: children should not be rendered if value is not defined", () => {
    const text = "Hello";
    render(<OrNoValueIfUndefined value={undefined}>{text}</OrNoValueIfUndefined>);

    expect(screen.queryByText(text)).not.toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("OrNoValueIfUndefined: children should be rendered if value is defined but children are not defined", () => {
    const undefinedChild = undefined;
    render(<OrNoValueIfUndefined value={true}>{undefinedChild}</OrNoValueIfUndefined>);

    expect(screen.queryByText("-")).not.toBeInTheDocument();
  });

  it("OrNoValueIfUndefined: fallback should be rendered if value is an empty string", () => {
    render(<OrNoValueIfUndefined value={""}>Content</OrNoValueIfUndefined>);

    expect(screen.queryByText("Content")).not.toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("OrNoValueIfUndefined: fallback should be rendered if value is a whitespace-only string", () => {
    render(<OrNoValueIfUndefined value={"   "}>Content</OrNoValueIfUndefined>);

    expect(screen.queryByText("Content")).not.toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("RenderIfValueDefined: children should be rendered if value is defined", () => {
    const text = "Hello";
    render(
      <RenderIfValueDefined value={true}>
        {(value) => (
          <>
            <span>{text}</span>
            <span>{value ? "truthy" : "falsy"}</span>
          </>
        )}
      </RenderIfValueDefined>,
    );

    expect(screen.getByText(text)).toBeInTheDocument();
    expect(screen.getByText("truthy")).toBeInTheDocument();
  });

  it("RenderIfValueDefined: children should not be rendered if value is undefined", () => {
    const text = "Hello";
    render(<RenderIfValueDefined value={undefined}>{() => <span>{text}</span>}</RenderIfValueDefined>);

    expect(screen.queryByText(text)).not.toBeInTheDocument();
  });

  it("RenderIfValueDefined: fallback should be rendered if value is an empty string", () => {
    render(<RenderIfValueDefined value={""}>{() => <span>Content</span>}</RenderIfValueDefined>);

    expect(screen.queryByText("Content")).not.toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("RenderIfValueDefined: custom fallback should be rendered if value is an empty string", () => {
    render(
      <RenderIfValueDefined fallback={<span>Unnamed</span>} value={""}>
        {() => <span>Content</span>}
      </RenderIfValueDefined>,
    );

    expect(screen.queryByText("Content")).not.toBeInTheDocument();
    expect(screen.getByText("Unnamed")).toBeInTheDocument();
  });
});
