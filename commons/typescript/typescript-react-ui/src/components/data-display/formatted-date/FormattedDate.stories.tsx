import { type Story, type StoryDefault } from "@ladle/react";
import { FormattedDate as FormattedDateComponent } from "./FormattedDate";

export default {
  title: "Data display / FormattedDate",
} satisfies StoryDefault;

export const FormattedDate: Story = () => (
  <div className="space-y-2">
    <div>
      <pre>date=&quot;2026-04-01&quot; (LocalDate string)</pre>
      <FormattedDateComponent date="2026-04-01" />
    </div>
    <div>
      <pre>{`date={new Date(...)} (Date instance)`}</pre>
      <FormattedDateComponent date={new Date(Date.UTC(2026, 3, 1))} />
    </div>
    <div>
      <pre>{`date={null}`}</pre>
      <FormattedDateComponent date={null} />
    </div>
  </div>
);
