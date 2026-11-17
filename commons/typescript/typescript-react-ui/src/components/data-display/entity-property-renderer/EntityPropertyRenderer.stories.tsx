import type { Meta, StoryObj } from "@storybook/react-vite";
import { entityPropertyRenderer } from "./EntityPropertyRenderer";

const meta = {
  title: "Data display / EntityPropertyRenderer",
} satisfies Meta;

export default meta;

interface ExamplePlan {
  id: string;
  title: string;
  description: string | null;
}

const filledPlan: ExamplePlan = { id: "1", title: "Standard Plan", description: "Monthly access to all features." };
const planWithoutDescription: ExamplePlan = { id: "2", title: "Lifetime", description: null };

const ExamplePlanTitle = entityPropertyRenderer("plan")<ExamplePlan>({
  displayName: "ExamplePlanTitle",
  render: ({ entity }) => entity.title,
});

const ExamplePlanDescription = entityPropertyRenderer("plan")<ExamplePlan>({
  displayName: "ExamplePlanDescription",
  render: ({ entity }) => entity.description,
});

const ExamplePlanLink = entityPropertyRenderer("plan")<ExamplePlan>({
  displayName: "ExamplePlanLink",
  render: ({ entity }) => <a href={`#/example-plans/${entity.id}`}>Open: {entity.title}</a>,
});

export const TitleLoaded: StoryObj<typeof meta> = {
  render: () => <ExamplePlanTitle plan={filledPlan} />,
};
export const TitleLoading: StoryObj<typeof meta> = {
  render: () => <ExamplePlanTitle plan={undefined} />,
};
export const TitleAbsent: StoryObj<typeof meta> = {
  render: () => <ExamplePlanTitle plan={null} />,
};
export const DescriptionLoaded: StoryObj<typeof meta> = {
  render: () => <ExamplePlanDescription plan={filledPlan} />,
};
export const DescriptionLoading: StoryObj<typeof meta> = {
  render: () => <ExamplePlanDescription plan={undefined} />,
};
export const DescriptionAbsent: StoryObj<typeof meta> = {
  render: () => <ExamplePlanDescription plan={null} />,
};
export const DescriptionMissing: StoryObj<typeof meta> = {
  render: () => <ExamplePlanDescription plan={planWithoutDescription} />,
};
export const LinkLoaded: StoryObj<typeof meta> = {
  render: () => <ExamplePlanLink plan={filledPlan} />,
};
export const LinkLoading: StoryObj<typeof meta> = {
  render: () => <ExamplePlanLink plan={undefined} />,
};
export const LinkAbsent: StoryObj<typeof meta> = {
  render: () => <ExamplePlanLink plan={null} />,
};

export const AllStatesSideBySide: StoryObj<typeof meta> = {
  render: () => (
    <table className="border-collapse [&_td]:border [&_td]:p-2 [&_th]:border [&_th]:p-2 [&_th]:text-left">
      <thead>
        <tr>
          <th>State</th>
          <th>Title (required)</th>
          <th>Description (nullable)</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td>Loading (entity undefined)</td>
          <td>
            <ExamplePlanTitle plan={undefined} />
          </td>
          <td>
            <ExamplePlanDescription plan={undefined} />
          </td>
        </tr>
        <tr>
          <td>Absent (entity null)</td>
          <td>
            <ExamplePlanTitle plan={null} />
          </td>
          <td>
            <ExamplePlanDescription plan={null} />
          </td>
        </tr>
        <tr>
          <td>Loaded with all values</td>
          <td>
            <ExamplePlanTitle plan={filledPlan} />
          </td>
          <td>
            <ExamplePlanDescription plan={filledPlan} />
          </td>
        </tr>
        <tr>
          <td>Loaded, optional missing</td>
          <td>
            <ExamplePlanTitle plan={planWithoutDescription} />
          </td>
          <td>
            <ExamplePlanDescription plan={planWithoutDescription} />
          </td>
        </tr>
      </tbody>
    </table>
  ),
};
