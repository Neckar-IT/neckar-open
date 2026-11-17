import type { Meta, StoryObj } from "@storybook/react-vite";
import { FormattedDate as FormattedDateComponent } from "./FormattedDate";

const meta = {
  title: "Data display / FormattedDate",
} satisfies Meta;

export default meta;

export const FormattedDate: StoryObj<typeof meta> = {
  render: () => (
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
  ),
};
