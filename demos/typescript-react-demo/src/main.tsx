import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App.tsx";
import "./styles.css";

const rootElement = window.document.getElementById("root");
if (rootElement === null) {
  throw new Error("No element with id 'root' found to render the app");
}

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
