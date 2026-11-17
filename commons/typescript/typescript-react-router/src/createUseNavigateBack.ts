/**
 * Imports
 */

import { useNavigate } from "react-router";

/**
 * Factory function to create a hook that creates a navigate function to go back in history
 */
export function createUseNavigateBack() {
  return function useNavigateBack() {
    const navigate = useNavigate();

    return () => {
      void navigate(-1);
    };
  };
}
