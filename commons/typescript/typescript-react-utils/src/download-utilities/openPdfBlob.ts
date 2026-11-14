/**
 * Function that opens a PDF Blob (by default in a new tab)
 */

export function openPdfBlob(blob: Blob, options?: { target: "_self" | "_blank" }): void {
  const pdfBlob = new Blob([blob], { type: "application/pdf" });
  const blobUrl = window.URL.createObjectURL(pdfBlob);

  const target = options?.target ?? "_blank";
  window.open(blobUrl, target);
}
