/**
 * Imports
 */

import { createQueryClient, QueryClientProvider, useMutation, useQuery } from "@neckar-hub/typescript-react-query";
import { Button } from "@neckar-hub/typescript-react-ui/actions";
import type { MeResponse } from "@neckar-hub/typescript-utils/openapi-types";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { type ReactElement, useState } from "react";
import { ProxyAuthProvider } from "./ProxyAuthProvider";
import { createUnauthorizedHttpClient } from "./testing/createUnauthorizedHttpClient";

/**
 * The provider under a query client with `unauthorizedEndsSession`. Every request of the fake
 * adapter answers HTTP 401; each button sends one, and only a same-origin 401 replaces the page.
 */

const meta = {
  title: "Proxy Auth Provider",
  component: ProxyAuthProvider,
} satisfies Meta;

export default meta;

/**
 * Fakes
 */

const { httpClient } = createUnauthorizedHttpClient();

const meResponse: MeResponse = {
  type: "good",
  data: {
    id: "00000000-0000-0000-0000-000000000001",
    email: "test-user@email.com",
    firstName: "Test",
    lastName: "User",
    loginName: "test-user",
    environment: [],
    avatarUrl: null,
    tenant: null,
    capabilities: [],
    roleLabel: null,
  },
};

function BookQuery(): ReactElement {
  const book = useQuery({ queryKey: ["book", 42], queryFn: () => httpClient.get("/api/books/42") });
  return <p>Loading book: {book.status}</p>;
}

function Application(): ReactElement {
  const [loadBook, setLoadBook] = useState(false);
  const saveBook = useMutation({ mutationFn: () => httpClient.put("/api/books/42") });
  // Shown inline instead of thrown, so the story keeps rendering after the foreign 401.
  const callForeignService = useMutation({ mutationFn: () => httpClient.put("https://other.example/api/x"), throwOnError: false });

  return (
    <div className="flex flex-col items-start gap-4 p-8">
      <Button onClick={() => setLoadBook(true)}>Query /api/books/42</Button>
      <Button onClick={() => saveBook.mutate()}>Mutation PUT /api/books/42</Button>
      <Button onClick={() => callForeignService.mutate()}>Mutation PUT https://other.example/api/x</Button>
      {callForeignService.isError && <p>The foreign 401 stays an ordinary error.</p>}
      {loadBook && <BookQuery />}
    </div>
  );
}

export const SignedIn: StoryObj = {
  name: "Signed in, every request answers 401",
  render: () => {
    const [queryClient] = useState(() => createQueryClient({ unauthorizedEndsSession: true }));

    return (
      <QueryClientProvider client={queryClient}>
        <ProxyAuthProvider config={{ onGetMe: () => Promise.resolve(meResponse) }}>
          <Application />
        </ProxyAuthProvider>
      </QueryClientProvider>
    );
  },
};
