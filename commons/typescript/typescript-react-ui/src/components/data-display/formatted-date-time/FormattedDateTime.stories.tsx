import { type Story, type StoryDefault } from "@ladle/react";
import { FormattedDateTime as FormattedDateTimeComponent } from "./FormattedDateTime";

export default {
  title: "Data display / FormattedDateTime",
} satisfies StoryDefault;

export const FormattedDateTime: Story = () => (
  <div className="space-y-2">
    <div>
      <pre>date=&quot;2026-04-01T08:30:00Z&quot; (Instant string)</pre>
      <FormattedDateTimeComponent date="2026-04-01T08:30:00Z" />
    </div>
    <div>
      <pre>{`date={new Date(...)} (Date instance)`}</pre>
      <FormattedDateTimeComponent date={new Date(Date.UTC(2026, 3, 1, 8, 30))} />
    </div>
    <div>
      <pre>{`date={null}`}</pre>
      <FormattedDateTimeComponent date={null} />
    </div>
  </div>
);
