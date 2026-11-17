import { ChevronDownIcon } from "lucide-react";
import { avatar1, avatar2, avatar3 } from "../assets/avatars";
import { Avatar, AvatarFallback, AvatarImage } from "../components/primitives/avatar";
import { Button } from "../components/primitives/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../components/primitives/card";
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from "../components/primitives/command";
import { Popover, PopoverContent, PopoverTrigger } from "../components/primitives/popover";

export function CardsTeamMembers() {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Team Members</CardTitle>
        <CardDescription>Invite your team members to collaborate.</CardDescription>
      </CardHeader>
      <CardContent className="grid gap-6">
        <div className="flex items-center justify-between space-x-4">
          <div className="flex items-center space-x-4">
            <Avatar className="h-8 w-8">
              <AvatarImage alt="Image" src={avatar1} />
              <AvatarFallback>OM</AvatarFallback>
            </Avatar>
            <div>
              <p className="text-sm leading-none font-medium">Sofia Davis</p>
              <p className="text-muted-foreground text-sm">m@example.com</p>
            </div>
          </div>
          <Popover>
            <PopoverTrigger asChild>
              <Button className="ml-auto" size="sm" variant="outline">
                Owner <ChevronDownIcon className="text-muted-foreground ml-2 h-4 w-4" />
              </Button>
            </PopoverTrigger>
            <PopoverContent align="end" className="p-0">
              <Command>
                <CommandInput placeholder="Select new role..." />
                <CommandList>
                  <CommandEmpty>No roles found.</CommandEmpty>
                  <CommandGroup>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Viewer</p>
                      <p className="text-muted-foreground text-sm">Can view and comment.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Developer</p>
                      <p className="text-muted-foreground text-sm">Can view, comment and edit.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Billing</p>
                      <p className="text-muted-foreground text-sm">Can view, comment and manage billing.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Owner</p>
                      <p className="text-muted-foreground text-sm">Admin-level access to all resources.</p>
                    </CommandItem>
                  </CommandGroup>
                </CommandList>
              </Command>
            </PopoverContent>
          </Popover>
        </div>
        <div className="flex items-center justify-between space-x-4">
          <div className="flex items-center space-x-4">
            <Avatar className="h-8 w-8">
              <AvatarImage alt="Image" src={avatar2} />
              <AvatarFallback>JL</AvatarFallback>
            </Avatar>
            <div>
              <p className="text-sm leading-none font-medium">Jackson Lee</p>
              <p className="text-muted-foreground text-sm">p@example.com</p>
            </div>
          </div>
          <Popover>
            <PopoverTrigger asChild>
              <Button className="ml-auto" size="sm" variant="outline">
                Member <ChevronDownIcon className="text-muted-foreground ml-2 h-4 w-4" />
              </Button>
            </PopoverTrigger>
            <PopoverContent align="end" className="p-0">
              <Command>
                <CommandInput placeholder="Select new role..." />
                <CommandList>
                  <CommandEmpty>No roles found.</CommandEmpty>
                  <CommandGroup className="p-1.5">
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Viewer</p>
                      <p className="text-muted-foreground text-sm">Can view and comment.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Developer</p>
                      <p className="text-muted-foreground text-sm">Can view, comment and edit.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Billing</p>
                      <p className="text-muted-foreground text-sm">Can view, comment and manage billing.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Owner</p>
                      <p className="text-muted-foreground text-sm">Admin-level access to all resources.</p>
                    </CommandItem>
                  </CommandGroup>
                </CommandList>
              </Command>
            </PopoverContent>
          </Popover>
        </div>
        <div className="flex items-center justify-between space-x-4">
          <div className="flex items-center space-x-4">
            <Avatar className="h-8 w-8">
              <AvatarImage alt="Image" src={avatar3} />
              <AvatarFallback>IN</AvatarFallback>
            </Avatar>
            <div>
              <p className="text-sm leading-none font-medium">Isabella Nguyen</p>
              <p className="text-muted-foreground text-sm">i@example.com</p>
            </div>
          </div>
          <Popover>
            <PopoverTrigger asChild>
              <Button className="ml-auto" size="sm" variant="outline">
                Member <ChevronDownIcon className="text-muted-foreground ml-2 h-4 w-4" />
              </Button>
            </PopoverTrigger>
            <PopoverContent align="end" className="p-0">
              <Command>
                <CommandInput placeholder="Select new role..." />
                <CommandList>
                  <CommandEmpty>No roles found.</CommandEmpty>
                  <CommandGroup className="p-1.5">
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Viewer</p>
                      <p className="text-muted-foreground text-sm">Can view and comment.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Developer</p>
                      <p className="text-muted-foreground text-sm">Can view, comment and edit.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Billing</p>
                      <p className="text-muted-foreground text-sm">Can view, comment and manage billing.</p>
                    </CommandItem>
                    <CommandItem className="flex flex-col items-start space-y-1 px-4 py-2">
                      <p>Owner</p>
                      <p className="text-muted-foreground text-sm">Admin-level access to all resources.</p>
                    </CommandItem>
                  </CommandGroup>
                </CommandList>
              </Command>
            </PopoverContent>
          </Popover>
        </div>
      </CardContent>
    </Card>
  );
}
