import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@neckar-hub/typescript-react-ui/actions";
import { Avatar, AvatarFallback, AvatarImage } from "@neckar-hub/typescript-react-ui/media";
import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { cn } from "cn";
import { Loader2Icon, LogOutIcon } from "lucide-react";
import { useUser } from "../context/UserContext";
import { useKeycloakLogout } from "../hooks/useKeycloakLogout";
import { useEmbeddedTranslation } from "../localization/useEmbeddedTranslation";
import { formatUserName } from "./formatUserName";

/**
 * Header user menu: avatar trigger with the signed-in user and a logout action. For an app whose
 * header is a sidebar, [UserAvatar] and [UserNameEmail] are the parts to compose with.
 */
export function UserMenu(): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const user = useUser();
  const { logout, isPending } = useKeycloakLogout();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="rounded-full">
        <UserAvatar user={user} />
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="min-w-56 rounded-lg" sideOffset={4}>
        <DropdownMenuLabel>
          <UserNameEmail user={user} />
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem disabled={isPending} onSelect={() => logout()}>
          {isPending ? <Loader2Icon className="animate-spin" /> : <LogOutIcon />}
          <span>{t("userMenu.logout")}</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

interface UserProps {
  user: UserDetails;
  className?: string;
}

/**
 * The user's picture, falling back to the initials of their name.
 */
export function UserAvatar({ user, className }: UserProps): React.ReactElement {
  const { t } = useEmbeddedTranslation();
  const fullName = formatUserName(user, user.loginName);
  const initials = `${user.firstName.charAt(0).toUpperCase()}${user.lastName.charAt(0).toUpperCase()}`;

  return (
    <Avatar className={cn("h-8 w-8 rounded-full", className)}>
      <AvatarImage alt={t({ key: "userMenu.avatarAlt", options: { name: fullName } })} src={user.avatarUrl ?? undefined} />
      <AvatarFallback>{initials}</AvatarFallback>
    </Avatar>
  );
}

/**
 * The user's name above their email address; the email is omitted when the account carries none.
 */
export function UserNameEmail({ user, className }: UserProps): React.ReactElement {
  const fullName = formatUserName(user, user.loginName);

  return (
    <div className={cn("grid text-left text-sm leading-tight", className)}>
      <span className="truncate font-semibold">{fullName}</span>
      {user.email ? <span className="truncate text-xs font-normal">{user.email}</span> : undefined}
    </div>
  );
}
