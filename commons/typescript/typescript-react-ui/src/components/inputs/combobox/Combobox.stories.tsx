/**
 * Imports
 */

import { filterArrayBySearchTerm } from "@neckar-hub/typescript-utils/filter-utilities";
import type { Meta, StoryObj } from "@storybook/react-vite";
import { useMemo, useState } from "react";
import { Combobox, type ComboboxProps } from "./Combobox";

const meta = {
  title: "Inputs / Combobox",
  component: Combobox,
  decorators: [
    (Story) => (
      <div className="mx-auto max-w-screen-sm">
        <Story />
      </div>
    ),
  ],
} satisfies Meta;

export default meta;

/**
 * Mock Data
 */

interface Person {
  firstName: string;
  lastName: string;
  address: {
    street: string;
    housenumber: string;
    zipCode: string;
    city: string;
  };
}

const persons: Person[] = [
  {
    firstName: "John",
    lastName: "Johnson",
    address: {
      street: "Oak Ave",
      housenumber: "2802",
      zipCode: "12345",
      city: "New York",
    },
  },
  {
    firstName: "Anna",
    lastName: "Brown",
    address: {
      street: "Willow Dr",
      housenumber: "5754",
      zipCode: "67890",
      city: "Los Angeles",
    },
  },
  {
    firstName: "Laura",
    lastName: "Johnson",
    address: {
      street: "Willow Dr",
      housenumber: "3776",
      zipCode: "11223",
      city: "Chicago",
    },
  },
  {
    firstName: "Anna",
    lastName: "Jackson",
    address: {
      street: "Pine Rd",
      housenumber: "7256",
      zipCode: "33445",
      city: "Houston",
    },
  },
  {
    firstName: "Chris",
    lastName: "Harris",
    address: {
      street: "Chestnut Pl",
      housenumber: "5504",
      zipCode: "55667",
      city: "Phoenix",
    },
  },
  {
    firstName: "David",
    lastName: "Smith",
    address: {
      street: "Main St",
      housenumber: "3321",
      zipCode: "77889",
      city: "Philadelphia",
    },
  },
  {
    firstName: "Emily",
    lastName: "Martin",
    address: {
      street: "Elm Blvd",
      housenumber: "4687",
      zipCode: "99001",
      city: "San Antonio",
    },
  },
  {
    firstName: "Alex",
    lastName: "Taylor",
    address: {
      street: "Maple Ln",
      housenumber: "9214",
      zipCode: "22334",
      city: "San Diego",
    },
  },
  {
    firstName: "Michael",
    lastName: "Anderson",
    address: {
      street: "Cedar St",
      housenumber: "7158",
      zipCode: "44556",
      city: "Dallas",
    },
  },
  {
    firstName: "Sarah",
    lastName: "White",
    address: {
      street: "Birch Ct",
      housenumber: "8176",
      zipCode: "66778",
      city: "San Jose",
    },
  },
];

export const FlatComboboxStory: StoryObj<ComboboxProps<Person>> = {
  name: "Flat",
  args: {
    disabled: false,
  },
  render: (props: ComboboxProps<Person>) => {
    const [filter, setFilter] = useState("");
    const [selectedPerson, setSelectedPerson] = useState<Person | undefined>(undefined);

    const filteredPersons = useMemo(() => {
      return filterArrayBySearchTerm(persons, filter);
    }, [filter]);

    return (
      <Combobox
        {...props}
        bottomOfTheList={
          <div className="text-muted-foreground border-t py-2 text-center text-sm">
            There are more items. Please use the search field to narrow down the list.
          </div>
        }
        filter={filter}
        itemRenderer={(person) => (
          <div className="flex w-full items-center justify-between">
            <p>{`${person.lastName}, ${person.firstName}`}</p>
            <p>{`${person.address.street} ${person.address.housenumber}, ${person.address.zipCode} ${person.address.city}`}</p>
          </div>
        )}
        items={filteredPersons}
        selectedItem={selectedPerson}
        setSelectedItem={setSelectedPerson}
        variant="flat"
        onFilterChange={setFilter}
      />
    );
  },
};

export const PopoverComboboxStory: StoryObj<ComboboxProps<Person>> = {
  name: "Popover",
  args: {
    disabled: false,
  },
  render: (props: ComboboxProps<Person>) => {
    const [filter, setFilter] = useState("");
    const [selectedPerson, setSelectedPerson] = useState<Person | undefined>(undefined);

    const filteredPersons = useMemo(() => {
      return filterArrayBySearchTerm(persons, filter);
    }, [filter]);

    return (
      <Combobox
        {...props}
        bottomOfTheList={
          <div className="text-muted-foreground border-t py-2 text-center text-sm">
            There are more items. Please use the search field to narrow down the list.
          </div>
        }
        className="w-[512px]"
        filter={filter}
        itemRenderer={(person) => (
          <div className="flex w-full items-center justify-between">
            <p>{`${person.lastName}, ${person.firstName}`}</p>
            <p>{`${person.address.street} ${person.address.housenumber}, ${person.address.zipCode} ${person.address.city}`}</p>
          </div>
        )}
        items={filteredPersons}
        selectedItem={selectedPerson}
        setSelectedItem={setSelectedPerson}
        onFilterChange={setFilter}
      />
    );
  },
};
