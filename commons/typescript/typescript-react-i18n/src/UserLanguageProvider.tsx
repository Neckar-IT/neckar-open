/**
 * Imports
 */

import type { UserDetails } from "@neckar-hub/typescript-utils/openapi-types";
import { useAtom } from "jotai";
import type { PropsWithChildren, ReactElement } from "react";
import { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { i18nSettingsAtom } from "./store/i18nSettings.store";

/**
 * Hook to read and write the user language to the store
 */

function useUserLanguage(user: UserDetails): readonly [string | undefined, (language: string) => void] {
  const [i18nSettings, setI18nSettings] = useAtom(i18nSettingsAtom);

  const userLanguage = i18nSettings.userLanguages[user.loginName];

  const setUserLanguage = (language: string) => {
    setI18nSettings((prev) => ({
      ...prev,
      userLanguages: {
        ...prev.userLanguages,
        [user.loginName]: language,
      },
    }));
  };

  return [userLanguage, setUserLanguage] as const;
}

/**
 * Set the user language based on the settings loaded from local storage
 */

type UserLanguageProviderProps = PropsWithChildren<{
  user: UserDetails;
}>;

export function UserLanguageProvider(props: UserLanguageProviderProps): ReactElement {
  const { i18n } = useTranslation();
  const [userLanguage, setUserLanguage] = useUserLanguage(props.user);

  // read the language from local storage and set the used language
  useEffect(() => {
    if (userLanguage !== undefined && i18n.language !== userLanguage) {
      void i18n.changeLanguage(userLanguage);
    }
  }, [i18n, userLanguage]);

  // update the user language in local storage when the user changes the language
  useEffect(() => {
    const handler = (language: string) => {
      setUserLanguage(language);
    };
    i18n.on("languageChanged", handler);
    return () => {
      i18n.off("languageChanged", handler);
    };
  }, [i18n, setUserLanguage]);

  return <>{props.children}</>;
}
