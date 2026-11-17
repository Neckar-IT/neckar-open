import { avatar1, avatar3, avatar5 } from "../assets/avatars";
import { Avatar, AvatarFallback, AvatarImage } from "../components/primitives/avatar";
import { Button } from "../components/primitives/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "../components/primitives/card";
import { Input } from "../components/primitives/input";
import { Label } from "../components/primitives/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../components/primitives/select";
import { Separator } from "../components/primitives/separator";

export function CardsShare() {
  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle>Share this document</CardTitle>
        <CardDescription>Anyone with the link can view this document.</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="flex space-x-2">
          <Label className="sr-only" htmlFor="link">
            Link
          </Label>
          <Input readOnly id="link" value="http://example.com/link/to/document" />
          <Button className="shrink-0" variant="secondary">
            Copy Link
          </Button>
        </div>
        <Separator className="my-4" />
        <div className="space-y-4">
          <h4 className="text-sm font-medium">People with access</h4>
          <div className="grid gap-6">
            <div className="flex items-center justify-between space-x-4">
              <div className="flex items-center space-x-4">
                <Avatar>
                  <AvatarImage alt="Image" src={avatar3} />
                  <AvatarFallback>OM</AvatarFallback>
                </Avatar>
                <div>
                  <p className="text-sm leading-none font-medium">Olivia Martin</p>
                  <p className="text-muted-foreground text-sm">m@example.com</p>
                </div>
              </div>
              <Select defaultValue="edit">
                <SelectTrigger aria-label="Edit" className="ml-auto w-[110px]">
                  <SelectValue placeholder="Select" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="edit">Can edit</SelectItem>
                  <SelectItem value="view">Can view</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex items-center justify-between space-x-4">
              <div className="flex items-center space-x-4">
                <Avatar>
                  <AvatarImage alt="Image" src={avatar5} />
                  <AvatarFallback>IN</AvatarFallback>
                </Avatar>
                <div>
                  <p className="text-sm leading-none font-medium">Isabella Nguyen</p>
                  <p className="text-muted-foreground text-sm">b@example.com</p>
                </div>
              </div>
              <Select defaultValue="view">
                <SelectTrigger aria-label="Edit" className="ml-auto w-[110px]">
                  <SelectValue placeholder="Select" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="edit">Can edit</SelectItem>
                  <SelectItem value="view">Can view</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="flex items-center justify-between space-x-4">
              <div className="flex items-center space-x-4">
                <Avatar>
                  <AvatarImage alt="Image" src={avatar1} />
                  <AvatarFallback>SD</AvatarFallback>
                </Avatar>
                <div>
                  <p className="text-sm leading-none font-medium">Sofia Davis</p>
                  <p className="text-muted-foreground text-sm">p@example.com</p>
                </div>
              </div>
              <Select defaultValue="view">
                <SelectTrigger aria-label="Edit" className="ml-auto w-[110px]">
                  <SelectValue placeholder="Select" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="edit">Can edit</SelectItem>
                  <SelectItem value="view">Can view</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
