import type { PropsWithChildren, ReactNode } from "react";
import { memo, useEffect, useState } from "react";
import { RenderCounter } from "./RenderCounter.tsx";
import { ChangingThemeProvider, ThemeConsumer } from "./ThemeProvider.tsx";

const AButton = () => {
  console.log("AButton render");
  const [count, setCount] = useState(0);

  return (
    <button
      className="bg-amber-800 p-4"
      onClick={() => {
        setCount((count) => count + 1);
      }}
    >
      AButton {count}
    </button>
  );
};

// memo compares the props shallowly: a parent re-render alone is not enough, a prop value has to differ.
const AMemoLabel = memo(({ text }: { text: string }) => {
  console.log("AMemoLabel render - text:", text);
  return <label className="bg-amber-200 p-4">AMemoLabel {text}</label>;
});
AMemoLabel.displayName = "AMemoLabel";

const ALabel = ({ text = "ALabel" }: { text?: string | undefined }) => {
  console.log("ALabel render");
  return <label className="bg-amber-100 p-4">{text}</label>;
};

type ContainerProps = PropsWithChildren<{
  content?: ReactNode;
}>;

const Container = ({ children, content }: ContainerProps) => {
  console.log("Container render");

  const [count, setCount] = useState(0);
  useEffect(() => {
    const handle = window.setInterval(() => {
      setCount((count) => count + 1);
    }, 3000);
    return () => {
      window.clearInterval(handle);
    };
  }, []);

  return (
    <div className="flex flex-col space-y-4 bg-amber-600 p-4">
      <span className="bg-amber-400 p-4">Container Child {count}</span>
      {/* This only re-renders, when the text-property changes (which is never the case) */}
      <AMemoLabel text="same text" />
      {/* This re-renders, when the text-property changes (which is the case) */}
      <AMemoLabel text={`text-${count.toFixed()}`} />
      {/* This re-renders every time the parent re-renders */}
      <ALabel />
      {/* Created in AppContent, so Container's own state change hands React the same element object and it is skipped */}
      {children}
      {/* Same mechanism — passing an element through a named prop instead of children changes nothing */}
      <span>Some extra content: {content}</span>
    </div>
  );
};

export function AppContent() {
  return (
    <div>
      <Container content={<RenderCounter />}>
        <AButton />
      </Container>
      <ChangingThemeProvider>
        {/* Does not use useContext; will not re-render when the value of <ChangingThemeProvider> changes */}
        <RenderCounter />
        {/* Uses useContext; will re-render when the value of <ChangingThemeProvider> changes */}
        <ThemeConsumer />
      </ChangingThemeProvider>
    </div>
  );
}
